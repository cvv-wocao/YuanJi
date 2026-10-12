package com.example.item.demo35.config;

import com.example.item.demo35.Raspberry.DeviceWebSocketHandler;
import com.example.item.demo35.Raspberry.FrontendWebSocketHandler;
import com.example.item.demo35.handler.MyWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private DeviceWebSocketHandler deviceWebSocketHandler;

    @Autowired
    private FrontendWebSocketHandler frontendWebSocketHandler;

    private final MyWebSocketHandler myWebSocketHandler;

    public WebSocketConfig(MyWebSocketHandler myWebSocketHandler) {
        this.myWebSocketHandler = myWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/ws/dashboard")
                .setAllowedOrigins("*");
        registry.addHandler(deviceWebSocketHandler, "/ws/device")
                .setAllowedOrigins("*");
        registry.addHandler(frontendWebSocketHandler, "/ws/frontend")
                .setAllowedOrigins("*");
    }
}
