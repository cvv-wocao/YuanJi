package com.example.item.demo35.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExerciseRecordService {

    private static final String PYTHON_API_URL = "http://localhost:5002";
    private static final Map<String, String> EXERCISE_TYPE_MAP = new HashMap<>();

    static {
        EXERCISE_TYPE_MAP.put("running", "跑步");
        EXERCISE_TYPE_MAP.put("dumbbell_curl", "哑铃弯举");
        EXERCISE_TYPE_MAP.put("pushup", "俯卧撑");
    }

    public String getUserExerciseRecords(Long userId, int limit) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = PYTHON_API_URL + "/api/records?user_id=" + userId + "&limit=" + limit;
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("获取运动记录响应: " + response);
            return response;
        } catch (Exception e) {
            System.out.println("获取运动记录失败: " + e.getMessage());
            return "{\"success\": false, \"records\": []}";
        }
    }


    public String getUserExerciseRecords(Long userId, int limit, String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // 注意：不再使用 user_id 参数，完全依赖 token 识别用户
            String url = PYTHON_API_URL + "/api/records?limit=" + limit;

            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("获取运动记录响应: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.out.println("获取运动记录失败: " + e.getMessage());
            return "{\"success\": false, \"records\": []}";
        }
    }


    public String getExerciseContext(Long userId,String token) {
        String recordsJson = getUserExerciseRecords(userId, 3,token);
        JSONObject result = JSONUtil.parseObj(recordsJson);

        if (!result.getBool("success", false)) {
            return "";
        }

        JSONArray records = result.getJSONArray("records");
        if (records == null || records.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("用户最近的运动记录（最近3条）：\n");

        for (int i = 0; i < records.size(); i++) {
            JSONObject record = records.getJSONObject(i);
            context.append(String.format("【%d】运动类型：%s，时间：%s",
                    i + 1,
                    translateExerciseType(record.getStr("exercise_type", "未知")),
                    formatDateTime(record.getStr("created_at", "未知"))));

            if (record.containsKey("score")) {
                context.append(String.format("，得分：%s", record.getStr("score", "0")));
            }
            if (record.containsKey("count")) {
                context.append(String.format("，次数：%s", record.getStr("count", "0")));
            }

            JSONArray feedback = record.getJSONArray("feedback");
            if (feedback != null && !feedback.isEmpty()) {
                context.append("，反馈：");
                for (int j = 0; j < feedback.size() && j < 3; j++) {
                    if (j > 0) context.append("；");
                    context.append(feedback.getStr(j));
                }
            }

            context.append("\n");
        }

        return context.toString();
    }

    public Map<String, Object> getExerciseStats(Long userId,String token) {
        Map<String, Object> stats = new HashMap<>();
        String recordsJson = getUserExerciseRecords(userId, 100);
        JSONObject result = JSONUtil.parseObj(recordsJson);

        if (!result.getBool("success", false)) {
            stats.put("totalRecords", 0);
            stats.put("mostCommonExercise", "无");
            stats.put("avgScore", 0);
            return stats;
        }

        JSONArray records = result.getJSONArray("records");
        if (records == null || records.isEmpty()) {
            stats.put("totalRecords", 0);
            stats.put("mostCommonExercise", "无");
            stats.put("avgScore", 0);
            return stats;
        }

        Map<String, Integer> exerciseCount = new HashMap<>();
        double totalScore = 0;
        int scoreCount = 0;

        for (int i = 0; i < records.size(); i++) {
            JSONObject record = records.getJSONObject(i);
            String type = record.getStr("exercise_type", "unknown");
            exerciseCount.merge(type, 1, Integer::sum);

            if (record.containsKey("score")) {
                try {
                    totalScore += Double.parseDouble(record.getStr("score"));
                    scoreCount++;
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        String mostCommonExercise = exerciseCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");

        stats.put("totalRecords", records.size());
        stats.put("mostCommonExercise", translateExerciseType(mostCommonExercise));
        stats.put("avgScore", scoreCount > 0 ? Math.round(totalScore / scoreCount * 10) / 10.0 : 0);
        stats.put("exerciseTypeCounts", exerciseCount);

        return stats;
    }

    private String translateExerciseType(String type) {
        return EXERCISE_TYPE_MAP.getOrDefault(type, type);
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "未知";
        }
        if (dateTime.contains("T")) {
            return dateTime.replace("T", " ");
        }
        return dateTime;
    }
}
