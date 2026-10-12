package com.example.PostLikeToHive

import com.example.util.SparkUtil
import org.apache.spark.sql.functions.to_date
import org.apache.spark.sql.SaveMode

object SyncPostCommentToHive {
  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSparkSession("SyncPostCommentToHive")
    import spark.implicits._

    val commentDF = spark.read
      .format("jdbc")
      .option("url", "jdbc:mysql://hadoop3:3306/yuanji?useSSL=false")
      .option("dbtable", "post_comment")
      .option("user", "root")
      .option("password", "root")
      .load()

    val withDt = commentDF.withColumn("dt", to_date($"create_time"))

    withDt.write
      .mode(SaveMode.Overwrite)  // 生产建议 Append 并管理分区
      .partitionBy("dt")
      .format("parquet")
      .saveAsTable("yuanji.ods_post_comment")

    spark.stop()
  }
}