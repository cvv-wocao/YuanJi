package com.example.util

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

object HdfsFileSystemUtil {
  private var fs: FileSystem = _

  def getFileSystem(): FileSystem = {
    if (fs == null) {
      val conf = new Configuration()
      //conf.set("fs.defaultFS", "hdfs://hadoop1:8020") // 根据你的配置调整
      fs = FileSystem.get(conf)
    }
    fs
  }

  def createDir(path: String): Boolean = {
    try {
      val fileSystem = getFileSystem()
      val hdfsPath = new Path(path)
      if (!fileSystem.exists(hdfsPath)) {
        fileSystem.mkdirs(hdfsPath)
        println(s"Created directory: $path")
      }
      true
    } catch {
      case e: Exception =>
        println(s"Failed to create directory $path: ${e.getMessage}")
        false
    }
  }

  def hdfsDirOrFileExists(path: String): Boolean = {
    try {
      val fileSystem = getFileSystem()
      fileSystem.exists(new Path(path))
    } catch {
      case e: Exception =>
        println(s"Error checking path $path: ${e.getMessage}")
        false
    }
  }

  def closeFileSystem(): Unit = {
    if (fs != null) {
      try {
        fs.close()
        fs = null
      } catch {
        case e: Exception =>
          println(s"Error closing filesystem: ${e.getMessage}")
      }
    }
  }
}