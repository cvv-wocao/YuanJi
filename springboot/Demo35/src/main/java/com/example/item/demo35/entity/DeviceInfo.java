package com.example.item.demo35.entity;
import lombok.Data;

@Data
public class DeviceInfo {
    private String deviceId;
    private String location;
    private long lastSeen;
    private Double temperature;
    private Double humidity;

    public DeviceInfo() {}

    public DeviceInfo(String deviceId, String location, long lastSeen, Double temperature, Double humidity) {
        this.deviceId = deviceId;
        this.location = location;
        this.lastSeen = lastSeen;
        this.temperature = temperature;
        this.humidity = humidity;
    }
}
