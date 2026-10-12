package com.example.item.demo35.config;

import com.example.item.demo35.util.AliOssUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 将 OSS 配置（来自 .env / 环境变量）注入到静态工具类 AliOssUtils，
 * 避免 AccessKey 等敏感信息硬编码在代码里。
 */
@Component
public class OssConfig {

    public OssConfig(
            @Value("${oss.endpoint}") String endpoint,
            @Value("${oss.access-key-id}") String accessKeyId,
            @Value("${oss.access-key-secret}") String accessKeySecret,
            @Value("${oss.bucket}") String bucket) {
        AliOssUtils.init(endpoint, accessKeyId, accessKeySecret, bucket);
    }
}