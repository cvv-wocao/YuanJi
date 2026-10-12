package com.example.item.demo35.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
public class PythonApiService {

    /** Python 姿态分析服务地址，可通过 PYTHON_API_URL 配置（Docker 下指向服务名） */
    @Value("${python.api.url}")
    private String pythonApiUrl;

    public Map<String, Object> analyzeFrame(Map<String, Object> requestData) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Python 服务未启动或连接失败: " + e.getMessage()
            );
        }
    }
}