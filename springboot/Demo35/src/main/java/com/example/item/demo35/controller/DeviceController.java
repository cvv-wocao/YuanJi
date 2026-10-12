package com.example.item.demo35.controller;

import com.example.item.demo35.Raspberry.DeviceWebSocketHandler;
import com.example.item.demo35.entity.DeviceInfo;
import com.example.item.demo35.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private static DeviceWebSocketHandler deviceWebSocketHandler;

    // 获取所有设备列表（管理员端）
    @GetMapping("/devices")
    public List<DeviceInfo> listDevices() {
        return deviceService.getAllDevices();
    }

    // 获取单个设备详情（管理员端）
    @GetMapping("/devices/{deviceId}")
    public DeviceInfo getDevice(@PathVariable String deviceId) {
        return deviceService.getDevice(deviceId);
    }

    // 发送指令给设备（管理员端）
    @PostMapping("/devices/{deviceId}/command")
    public ResponseEntity<?> sendCommand(@PathVariable String deviceId, @RequestBody Map<String, String> body) {
        String command = body.get("command");
        try {
            deviceWebSocketHandler.sendCommandToDevice(deviceId, command);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("发送失败: " + e.getMessage());
        }
    }
}

@RestController
@RequestMapping("/api")
class DeviceApiController {

    @Autowired
    private DeviceService deviceService;

    // 获取所有设备列表（用户端）
    @GetMapping("/devices")
    public List<DeviceInfo> getUserDevices() {
        return deviceService.getAllDevices();
    }

    // 获取单个设备详情（用户端）
    @GetMapping("/devices/{deviceId}")
    public DeviceInfo getUserDevice(@PathVariable String deviceId) {
        return deviceService.getDevice(deviceId);
    }
}
