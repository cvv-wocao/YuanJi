package com.example.post

import com.example.util.{HdfsFileSystemUtil, JedisUtil, SparkUtil}
import com.example.util.SparkUtil
import org.apache.hadoop.fs.Path
import org.apache.spark.ml.recommendation.ALSModel
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.col

import java.util.Properties
import scala.collection.JavaConverters._

/**
 * 加载最佳模型，为所有用户生成推荐并存入 Redis
 */
object PostRecommender {

  val recommendCount = 50 // 每个用户推荐帖子数量
  val redisKeyPrefix = "rec:post:user:"
  val redisExpireSeconds = 7 * 24 * 60 * 60 // 7天

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSparkSession("PostRecommender")

    // 1. 从 MySQL 读取最佳模型路径（按 AUC 降序取最新）
    val props = new Properties()
    props.setProperty("user", "root")
    props.setProperty("password", "root")
    val modelPathDF = spark.read.jdbc(
        "jdbc:mysql://hadoop3:3306/yuanji?useSSL=false",
        "model_params",
        props
      ).select("modelPath", "accuracy", "createTime")
      .orderBy(col("accuracy").desc, col("createTime").desc)
      .limit(1)

    if (modelPathDF.isEmpty) {
      println("MySQL 中没有模型记录，退出")
      SparkUtil.stopSparkSession()
      System.exit(1)
    }

    val modelPath = modelPathDF.first().getString(0)
    println(s"加载模型: $modelPath")

    // 2. 检查 HDFS 路径是否存在
    if (!HdfsFileSystemUtil.hdfsDirOrFileExists(modelPath)) {
      println(s"模型路径不存在: $modelPath")
      SparkUtil.stopSparkSession()
      HdfsFileSystemUtil.closeFileSystem()
      System.exit(1)
    }

    // 3. 加载 ALS 模型
    val model = ALSModel.load(modelPath)

    // 4. 为所有用户生成推荐
    val userRecsRDD = model.recommendForAllUsers(recommendCount)
      .rdd
      .map { row =>
        val userId = row.getInt(0)
        val recommendations = row.getSeq[Row](1).map { r =>
          (r.getInt(0), r.getFloat(1)) // (postId, score)
        }.toList
        (userId, recommendations)
      }

    // 5. 获取用户已交互的帖子（点赞+评论），用于过滤
    val interactedRDD = spark.sql(
        """
          |SELECT user_id, post_id FROM yuanji.ods_post_like
          |UNION
          |SELECT user_id, post_id FROM yuanji.ods_post_comment
      """.stripMargin)
      .rdd
      .map(row => (row.getInt(0), row.getInt(1)))
      .groupByKey()
      .mapValues(_.toSet)

    // 6. 过滤掉已交互的帖子，并按分数降序取前 recommendCount
    val recommendListRDD = userRecsRDD.leftOuterJoin(interactedRDD)
      .map { case (userId, (recs, interactedOpt)) =>
        val interactedSet = interactedOpt.getOrElse(Set.empty[Int])
        val filtered = recs
          .filterNot(rec => interactedSet.contains(rec._1))
          .sortBy(-_._2)
          .take(recommendCount)
        (userId, filtered)
      }
      .filter(_._2.nonEmpty)

    // 7. 写入 Redis
    recommendListRDD.foreachPartition { partition =>
      val jedis = JedisUtil.getSource()
      val pipeline = jedis.pipelined()
      partition.foreach { case (userId, recs) =>
        val key = redisKeyPrefix + userId
        pipeline.del(key)
        recs.foreach { case (postId, score) =>
          // 使用分数作为有序集合的 score，帖子ID作为 member
          pipeline.zadd(key, score, postId.toString)
        }
        pipeline.expire(key, redisExpireSeconds)
      }
      pipeline.sync()
      jedis.close()
    }

    println("推荐结果已写入 Redis")

    // 8. 清理资源
    SparkUtil.stopSparkSession()
    HdfsFileSystemUtil.closeFileSystem()
  }
}