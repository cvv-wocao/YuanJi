package com.example.post

import com.example.util.{HdfsFileSystemUtil, SparkUtil}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.functions.{col, rand, when}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
 * 帖子推荐模型训练（网格搜索最佳参数）
 */
object PostTraining {

  // 参数搜索范围
  val maxIters = Array(5, 10, 15)
  val regParams = Array(0.01, 0.05, 0.1)
  val ranks = Array(10, 20)
  val trainingRatio = 0.8
  val testRatio = 0.2

  // 负样本采样比例（每个正样本对应负样本数）
  val negativeSampleRatio = 3

  // 模型保存前缀
  val modelPathPrefix = "/postRec/model/"

  // 推荐帖子数量（训练时无用，仅用于评估？此处不涉及）
  // 评估时使用 AUC

  case class Params(
                     maxIter: Int,
                     regParam: Double,
                     rank: Int,
                     modelPath: String,
                     accuracy: Double,
                     createTime: Long = System.currentTimeMillis()
                   )

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSparkSession("PostTraining")
    import spark.implicits._

    // 1. 从 Hive 读取点赞和评论数据作为正样本
    val likeDF = spark.sql("SELECT user_id, post_id, 1.0 as rating FROM yuanji.ods_post_like")
    val commentDF = spark.sql("SELECT user_id, post_id, 1.0 as rating FROM yuanji.ods_post_comment")
    val positiveDF = likeDF.union(commentDF).distinct().cache()
    println(s"正样本数量: ${positiveDF.count()}")

    // 2. 构造负样本（用户未交互的帖子）
    val allUsers = positiveDF.select("user_id").distinct().as[Int].collect()
    val allPosts = positiveDF.select("post_id").distinct().as[Int].collect()
    val userInteracted = positiveDF.rdd.map(row => (row.getInt(0), row.getInt(1))).groupByKey().mapValues(_.toSet).collectAsMap()

    // 广播变量
    val allPostsBC = spark.sparkContext.broadcast(allPosts.toSet)
    val userInteractedBC = spark.sparkContext.broadcast(userInteracted)

    // 为每个用户随机采样其未交互的帖子作为负样本
    val negativeRDD = allUsers.flatMap { userId =>
      val interacted = userInteractedBC.value.getOrElse(userId, Set.empty[Int])
      val availablePosts = allPostsBC.value.diff(interacted).toSeq
      // 随机取 negativeSampleRatio * 正样本数 个负样本
      val posCount = interacted.size
      val negCount = math.min(availablePosts.length, posCount * negativeSampleRatio)
      scala.util.Random.shuffle(availablePosts).take(negCount).map(postId => (userId, postId, 0.0))
    }

    val negativeDF = spark.createDataFrame(negativeRDD.toSeq).toDF("user_id", "post_id", "rating")
    println(s"负样本数量: ${negativeDF.count()}")

    // 3. 合并正负样本，划分训练/测试集
    val fullDF = positiveDF.union(negativeDF)
    val Array(training, test) = fullDF.randomSplit(Array(trainingRatio, testRatio), seed = 42)

    // 4. 确保模型保存目录存在
    HdfsFileSystemUtil.createDir(modelPathPrefix)

    var bestAccuracy = 0.0
    var bestParams: Option[Params] = None

    // 5. 网格搜索
    for (maxIter <- maxIters; regParam <- regParams; rank <- ranks) {
      println(s"训练参数: maxIter=$maxIter, regParam=$regParam, rank=$rank")

      val als = new ALS()
        .setMaxIter(maxIter)
        .setRegParam(regParam)
        .setRank(rank)
        .setImplicitPrefs(true) // 隐式反馈
        .setUserCol("user_id")
        .setItemCol("post_id")
        .setRatingCol("rating")
        .setColdStartStrategy("drop")

      val model = als.fit(training)

      // 预测测试集
      val predictions = model.transform(test)

      // 将预测分数转为二元（>0.5 视为正，可调整阈值）
      val binaryPred = predictions.withColumn("pred_label", when(col("prediction") > 0.5, 1.0).otherwise(0.0))

      // 评估 AUC
      val evaluator = new BinaryClassificationEvaluator()
        .setLabelCol("rating")
        .setRawPredictionCol("prediction")
        .setMetricName("areaUnderROC")

      val accuracy = evaluator.evaluate(binaryPred)
      println(s"AUC = $accuracy")

      if (accuracy > bestAccuracy) {
        bestAccuracy = accuracy
        val modelPath = modelPathPrefix + s"model_${System.currentTimeMillis()}"
        model.write.overwrite().save(modelPath)
        bestParams = Some(Params(maxIter, regParam, rank, modelPath, accuracy))
        println(s"发现更好模型: $modelPath, AUC=$accuracy")
      }
    }

    // 6. 将最佳参数保存到 MySQL
    if (bestParams.isDefined) {
      val paramsDF = Seq(bestParams.get).toDF()
      paramsDF.write
        .mode(SaveMode.Append)
        .format("jdbc")
        .option("url", "jdbc:mysql://hadoop3:3306/yuanji?useSSL=false")
        .option("dbtable", "model_params")
        .option("user", "root")
        .option("password", "root")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .save()
      println("最佳模型参数已写入 MySQL")
    } else {
      println("未找到有效模型")
    }

    spark.stop()
    HdfsFileSystemUtil.closeFileSystem()
  }
}