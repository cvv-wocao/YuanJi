package com.example.item.demo35.kafka;

import com.example.item.demo35.handler.MyWebSocketHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "bigdata.enabled", havingValue = "true")
public class KafkaConsumerService {

    private final MyWebSocketHandler webSocketHandler;

    public KafkaConsumerService(MyWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @KafkaListener(topics = {"user-change", "post-change", "product-change", "category-change", "order-change"}, groupId = "dashboard-group")
    public void consume(String message) {
        System.out.println("消费到 Kafka 消息: " + message);
        // 将消息原样推送到所有前端
        webSocketHandler.broadcast(message);
    }
}
