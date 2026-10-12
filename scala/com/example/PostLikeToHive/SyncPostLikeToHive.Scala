package com.example.PostLikeToHive

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import com.example.util.SparkUtil


object SyncPostLikeToHive {
  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSparkSession("SyncPostLikeToHive")
    import spark.implicits._

    // 读取 MySQL 点赞表（增量可按 create_time 过滤，例如昨天数据）
    val likeDF = spark.read
      .format("jdbc")
      .option("url", "jdbc:mysql://hadoop3:3306/yuanji?useSSL=false")
      .option("dbtable", "post_like")
      .option("user", "root")
      .option("password", "root")
      .load()

    // 添加分区字段 dt (yyyy-MM-dd)
    val withDt = likeDF.withColumn("dt", to_date($"create_time"))

    // 写入 Hive 分区表（使用动态分区）
    withDt.write
      .mode(SaveMode.Overwrite)  // 生产可改为 Append，并手动管理分区
      .partitionBy("dt")
      .format("parquet")
      .saveAsTable("yuanji.ods_post_like")

    spark.stop()
  }
}