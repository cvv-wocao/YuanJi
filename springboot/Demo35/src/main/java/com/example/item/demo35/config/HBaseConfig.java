package com.example.item.demo35.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "bigdata.enabled", havingValue = "true")
public class HBaseConfig {

    @Bean
    public Connection hbaseConnection() throws IOException {
        // 创建 HBase 配置 - 使用完全限定名避免冲突
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();

        // 设置 HBase 连接参数
        config.set("hbase.zookeeper.quorum", "192.168.81.135,192.168.81.137,192.168.81.136");
        config.set("hbase.zookeeper.property.clientPort", "2181");

        // 可选：设置其他参数
        config.set("hbase.client.retries.number", "3");
        config.set("hbase.client.pause", "100");
        System.out.println("HBase connection created successfully");

        // 创建连接
        Connection connection = ConnectionFactory.createConnection(config);
        return connection;
    }
}
