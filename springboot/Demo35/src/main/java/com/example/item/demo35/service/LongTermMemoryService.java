package com.example.item.demo35.service;

import com.example.item.demo35.Dao.LongTermMemoryDao;
import com.example.item.demo35.entity.LongTermMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LongTermMemoryService {

    @Autowired
    private LongTermMemoryDao memoryDao;

    private static final Set<String> SPORT_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "跑步", "慢跑", "快跑", "姿势", "状态", "成绩",
            "得分", "次数", "卡路里", "时长", "角度", "膝盖",
            "髋关节", "躯干", "反馈", "建议", "最近", "历史",
            "喜欢", "不喜欢", "偏好", "习惯", "目标", "计划",
            "频率", "强度", "训练", "运动", "健身", "健康"
    )));

    public enum MemoryType {
        PREFERENCE("偏好"),
        BEHAVIOR("行为"),
        GOAL("目标"),
        CONVERSATION("对话摘要"),
        FEEDBACK("反馈"),
        HEALTH("健康数据");

        private final String description;

        MemoryType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public int saveMemory(Long userId, String content, MemoryType memoryType) {
        return saveMemory(userId, null, content, memoryType);
    }

    public int saveMemory(Long userId, String threadSlug, String content, MemoryType memoryType) {
        LongTermMemory memory = new LongTermMemory();
        memory.setUserId(userId);
        memory.setThreadSlug(threadSlug);
        memory.setContent(content);
        memory.setMemoryType(memoryType.name());
        memory.setKeywords(extractKeywords(content));
        memory.setRelevanceScore(1.0);
        memory.setCreatedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());

        return memoryDao.insert(memory);
    }

    public int updateMemory(Long id, String content, MemoryType memoryType) {
        LongTermMemory memory = memoryDao.selectById(id);
        if (memory != null) {
            memory.setContent(content);
            memory.setMemoryType(memoryType.name());
            memory.setKeywords(extractKeywords(content));
            memory.setUpdatedAt(LocalDateTime.now());
            return memoryDao.update(memory);
        }
        return 0;
    }

    public boolean deleteMemory(Long id) {
        return memoryDao.deleteById(id) > 0;
    }

    public LongTermMemory getMemoryById(Long id) {
        return memoryDao.selectById(id);
    }

    public List<LongTermMemory> getMemoriesByUserId(Long userId) {
        return memoryDao.selectByUserId(userId);
    }

    public List<LongTermMemory> getMemoriesByType(Long userId, MemoryType memoryType) {
        return memoryDao.selectByUserIdAndType(userId, memoryType.name());
    }

    public List<LongTermMemory> searchMemories(Long userId, String query) {
        return searchMemories(userId, null, query);
    }

    public List<LongTermMemory> searchMemories(Long userId, String threadSlug, String query) {
        Set<LongTermMemory> allMemories = new LinkedHashSet<>();

        // 1. 始终查询用户的偏好记录（跨线程共享）
        List<LongTermMemory> preferences = getMemoriesByType(userId, MemoryType.PREFERENCE);
        if (preferences != null && !preferences.isEmpty()) {
            allMemories.addAll(preferences);
        }

        // 2. 如果有线程ID，查询该线程的对话记录
        if (threadSlug != null && !threadSlug.isEmpty()) {
            List<LongTermMemory> threadMemories = memoryDao.selectByUserIdAndThread(userId, threadSlug);
            if (threadMemories != null && !threadMemories.isEmpty()) {
                allMemories.addAll(threadMemories);
            }
        }
        // 3. 如果没有线程ID，进行关键词搜索
        else {
            String keywords = extractKeywords(query);
            if (keywords.isEmpty()) {
                allMemories.addAll(memoryDao.selectRecentByUserId(userId, 10));
            } else {
                allMemories.addAll(memoryDao.searchByKeywords(userId, keywords));
            }
        }

        return new ArrayList<>(allMemories);
    }

    public String getMemoryContext(Long userId, String query) {
        return getMemoryContext(userId, null, query);
    }

    public String getMemoryContext(Long userId, String threadSlug, String query) {
        List<LongTermMemory> memories = searchMemories(userId, threadSlug, query);

        if (memories.isEmpty()) {
            return "";
        }

        Map<String, List<LongTermMemory>> memoriesByType = memories.stream()
                .collect(Collectors.groupingBy(LongTermMemory::getMemoryType));

        StringBuilder context = new StringBuilder();
        context.append("用户之前的聊天记录：\n");

        for (MemoryType type : MemoryType.values()) {
            List<LongTermMemory> typeMemories = memoriesByType.get(type.name());
            if (typeMemories != null && !typeMemories.isEmpty()) {
                //context.append("【").append(type.getDescription()).append("】\n");
                for (LongTermMemory memory : typeMemories) {
                    context.append("  - ").append(memory.getContent()).append("\n");
                }
            }
        }

        return context.toString();
    }

    public int saveConversationSummary(Long userId, String userMessage, String aiResponse) {
        return saveConversationSummary(userId, null, userMessage, aiResponse);
    }

    // 判断是否为偏好语句
    private boolean isPreferenceStatement(String message) {
        // 偏好相关关键词列表
        String[] preferenceKeywords = {
                "喜欢", "不喜欢", "偏好", "讨厌", "爱", "不爱",
                "喜欢的", "不喜欢的", "偏好的", "讨厌的",
                "我喜欢", "我不喜欢", "我偏好", "我讨厌",
                "最喜欢", "最不喜欢", "偏好于"
        };

        for (String keyword : preferenceKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public int saveConversationSummary(Long userId, String threadSlug, String userMessage, String aiResponse) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            System.out.println("用户消息为空，跳过保存");
            return 0;
        }

        // ===== 新增：自动检测偏好语句 =====
        if (isPreferenceStatement(userMessage)) {
            int preferenceResult = saveMemory(userId, null, userMessage, MemoryType.PREFERENCE);
            System.out.println("=== 自动识别偏好语句 ===");
            System.out.println("用户提问: " + userMessage);
            System.out.println("已自动保存为偏好，保存结果: " + (preferenceResult > 0 ? "成功" : "失败"));
        }
        String memoryContent = "用户提问: " + userMessage.trim();
        String keywords = extractKeywords(userMessage);

        LongTermMemory memory = new LongTermMemory();
        memory.setUserId(userId);
        memory.setThreadSlug(threadSlug);
        memory.setContent(memoryContent);
        memory.setMemoryType(MemoryType.CONVERSATION.name());
        memory.setKeywords(keywords);
        memory.setRelevanceScore(1.0);
        memory.setCreatedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());

        int result = memoryDao.insert(memory);
        System.out.println("=== 保存用户提问 ===");
        System.out.println("用户ID: " + userId);
        System.out.println("线程: " + (threadSlug != null ? threadSlug : "无"));
        System.out.println("用户提问: " + userMessage);
        System.out.println("保存结果: " + (result > 0 ? "成功" : "失败"));

        return result;
    }

    public int saveConversationSummary(Long userId, String conversation) {
        if (conversation == null || conversation.isEmpty()) {
            return 0;
        }

        String userMessage = conversation;
        if (conversation.contains("user:")) {
            int startIdx = conversation.indexOf("user:") + 5;
            int endIdx = conversation.indexOf("\nai:");
            if (endIdx > startIdx) {
                userMessage = conversation.substring(startIdx, endIdx).trim();
            } else {
                userMessage = conversation.substring(startIdx).trim();
            }
        }

        return saveConversationSummary(userId, userMessage, null);
    }

    public int saveUserPreference(Long userId, String preference) {
        return saveMemory(userId, preference, MemoryType.PREFERENCE);
    }

    public int saveUserGoal(Long userId, String goal) {
        return saveMemory(userId, goal, MemoryType.GOAL);
    }

    private String extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Set<String> foundKeywords = new LinkedHashSet<>();

        for (String keyword : SPORT_KEYWORDS) {
            if (text.contains(keyword)) {
                foundKeywords.add(keyword);
            }
        }

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            foundKeywords.add(matcher.group());
        }

        return String.join(",", foundKeywords);
    }

    private String summarizeConversation(String conversation) {
        if (conversation == null || conversation.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        String[] lines = conversation.split("\n");

        List<String> userLines = new ArrayList<>();
        List<String> aiLines = new ArrayList<>();

        for (String line : lines) {
            if (line.toLowerCase().contains("user:")) {
                userLines.add(line.replaceAll("(?i)user:\\s*", "").trim());
            } else if (line.toLowerCase().contains("ai:") || line.toLowerCase().contains("assistant:")) {
                aiLines.add(line.replaceAll("(?i)(ai|assistant):\\s*", "").trim());
            }
        }

        if (!userLines.isEmpty()) {
            summary.append("用户提问：").append(String.join("；", userLines)).append("。");
        }

        if (!aiLines.isEmpty()) {
            if (summary.length() > 0) summary.append(" ");
            summary.append("AI回答要点：").append(String.join("；", aiLines)).append("。");
        }

        String result = summary.toString();
        return result.length() > 500 ? result.substring(0, 500) : result;
    }

    public Map<String, Object> getMemoryStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        List<LongTermMemory> allMemories = memoryDao.selectByUserId(userId);

        stats.put("totalMemories", allMemories.size());

        Map<String, Long> typeCounts = allMemories.stream()
                .collect(Collectors.groupingBy(
                        LongTermMemory::getMemoryType,
                        Collectors.counting()
                ));
        stats.put("typeCounts", typeCounts);

        if (!allMemories.isEmpty()) {
            stats.put("latestMemory", allMemories.get(0).getUpdatedAt());
        }

        return stats;
    }
}
