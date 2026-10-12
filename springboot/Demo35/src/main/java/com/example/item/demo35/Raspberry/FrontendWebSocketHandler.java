package com.example.item.demo35.Raspberry;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FrontendWebSocketHandler implements WebSocketHandler {

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("前端已连接，当前连接数: " + sessions.size());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // 前端一般不主动发消息，这里留空或记录日志
        System.out.println("收到前端消息: " + message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("前端传输错误: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("前端断开，剩余连接数: " + sessions.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // 实例方法广播消息给所有前端
    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("广播失败: " + e.getMessage());
                }
            }
        }
    }
}
