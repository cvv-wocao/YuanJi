package com.example.item.demo35.Raspberry;

import com.example.item.demo35.service.DeviceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceWebSocketHandler implements WebSocketHandler {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private FrontendWebSocketHandler frontendHandler;   // 注入实例

    private static final ConcurrentHashMap<String, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String deviceId = getDeviceId(session);
        deviceSessions.put(deviceId, session);
        System.out.println("设备已连接: " + deviceId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        String deviceId = getDeviceId(session);

        // 解析 JSON，更新设备信息
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(payload);
            if (json.has("type") && "sensor".equals(json.get("type").asText())) {
                String location = json.has("location") ? json.get("location").asText() : "";
                double temp = json.get("data").get("temperature").asDouble();
                double humi = json.get("data").get("humidity").asDouble();
                deviceService.updateDevice(deviceId, location, temp, humi);
            }
        } catch (Exception e) {
            // 非传感器消息，忽略
        }

        System.out.println("收到设备消息 [" + deviceId + "]: " + payload);

        // 转发给所有前端（使用注入的实例）
        frontendHandler.broadcast(payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("设备传输错误: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceId = getDeviceId(session);
        deviceSessions.remove(deviceId);
        System.out.println("设备断开: " + deviceId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getDeviceId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("=")) {
            return query.split("=")[1];
        }
        return "unknown";
    }

    // 供外部调用发送指令
    public void sendCommandToDevice(String deviceId, String command) throws Exception {
        WebSocketSession session = deviceSessions.get(deviceId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(command));
        } else {
            throw new Exception("设备不在线");
        }
    }
}
