package com.example.util

import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

/**
  * 代码千万行    注释第一行
  * 编写不规范    同事两行泪
  */
object SparkUtil {
  private var sparkSession: SparkSession = null
  private var sparkContext:SparkContext = null

  /**
   * @param config
   * @return
   */
  def getSparkSession(appName:String): SparkSession ={
    if(sparkSession == null){
      this.synchronized(
        if (sparkSession == null){
          val conf = new SparkConf()
            //.setMaster("local[*]")
            .setAppName(appName)
            .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer") // 指定序列化格式，默认是java序列化（仅 hbase bulkload 方式）
            .registerKryoClasses(Array(classOf[ImmutableBytesWritable], classOf[Result])) //告知哪些类型需要序列化（仅 hbase bulkload 方式）
            /*
            .set("spark.sql.broadcastTimeout", "600")
            .set("spark.sql.autoBroadcastJoinThreshold", "-1")
            .set("spark.executor.memory", "1g")
            .set("spark.executor.cores", "1")
            .set("spark.executor.instances", "3")
            */

          sparkSession = SparkSession
            .builder()
            .config(conf)
            .enableHiveSupport()
            .getOrCreate()
        })
    }
    sparkSession
  }

  /**
    * @param config
    * @return
    */
  def getSparkSession(appName:String,config:Map[String,String]): SparkSession ={
    if(sparkSession == null){
      this.synchronized(
       if (sparkSession == null){
          if(config == null){
            getSparkSession(appName)
          }else{
            val conf = new SparkConf()
            config.foreach(kv => {
              conf.set(kv._1,kv._2)
            })
            sparkSession = SparkSession
              .builder()
              .config(conf)
              .enableHiveSupport()
              .getOrCreate()
          }
       })
    }else{
      this.synchronized(
        if(sparkSession != null){
          val sparkConf = sparkSession.sparkContext.getConf
          config.foreach(kv => {
            sparkConf.set(kv._1,kv._2)
          })
        }
      )
    }
    sparkSession
  }

  /**
    * @param config
    * @return
    */
  def  getSparkContext(appName:String,config:Map[String,String]): SparkContext ={
    if(sparkContext == null){
      this.synchronized(
        if(sparkContext == null){
          sparkContext = getSparkSession(appName,config).sparkContext
        })
    }
    sparkContext
  }

  /**
   * @param config
   * @return
   */
  def stopSparkSession(): Unit ={
    if(sparkSession != null){
      this.synchronized(
        if(sparkSession != null){
          sparkSession.stop()
        })
    }
  }

}