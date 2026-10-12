package com.example.item.demo35.service;

import com.example.item.demo35.entity.DeviceInfo;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {
    private final ConcurrentHashMap<String, DeviceInfo> devices = new ConcurrentHashMap<>();

    public void updateDevice(String deviceId, String location, Double temp, Double humi) {
        DeviceInfo info = devices.getOrDefault(deviceId,
                new DeviceInfo(deviceId, location, System.currentTimeMillis(), temp, humi));
        info.setLocation(location);
        info.setTemperature(temp);
        info.setHumidity(humi);
        info.setLastSeen(System.currentTimeMillis());
        devices.put(deviceId, info);
    }

    public List<DeviceInfo> getAllDevices() {
        return new ArrayList<>(devices.values());
    }

    public DeviceInfo getDevice(String deviceId) {
        return devices.get(deviceId);
    }
}
