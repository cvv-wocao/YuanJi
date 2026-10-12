package com.example.item.demo35.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alipay")
@Data
public class AlipayConfig {
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String signType;
    private String format;
    private String charset;

    /**
     * 创建支付宝客户端（单例）
     */
    public AlipayClient getAlipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                merchantPrivateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }
}
