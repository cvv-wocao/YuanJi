package com.example.util
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

object JedisUtil {
  var pool: JedisPool = null

  def getPool(): JedisPool ={
    if(pool == null){
      synchronized({
        if(pool == null){
          val poolConfig = new JedisPoolConfig()
          poolConfig.setMaxTotal(100)
          poolConfig.setMaxIdle(50)
          poolConfig.setMinIdle(10)
          poolConfig.setMaxWaitMillis(2000)
          poolConfig.setTestOnBorrow(true)
          poolConfig.setTestOnReturn(true)
          pool = new JedisPool(poolConfig, "hadoop3", 6379)
        }
      })
    }
    pool
  }

  def getSource(): Jedis ={
    getPool().getResource
  }

}
