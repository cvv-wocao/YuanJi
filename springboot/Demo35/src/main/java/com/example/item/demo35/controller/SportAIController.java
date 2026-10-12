package com.example.item.demo35.controller;

import cn.hutool.json.JSONUtil;
import com.example.item.demo35.client.AnythingLLMClient;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.service.ExerciseRecordService;
import com.example.item.demo35.service.LongTermMemoryService;
import com.example.item.demo35.entity.LongTermMemory;
import lombok.Data;
import com.example.item.demo35.entity.AIrequest.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sport-ai")
public class SportAIController {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private AnythingLLMClient anythingLLMClient;

    @Resource
    private ExerciseRecordService exerciseRecordService;

    @Resource
    private LongTermMemoryService longTermMemoryService;

    // ==================== 文件上传接口 ====================

    /**
     * 上传运动知识文档
     * POST /api/sport-ai/upload
     *
     * 前端调用示例（axios）：
     * const formData = new FormData();
     * formData.append('file', file);
     * formData.append('folderName', 'running'); // 可选
     * axios.post('/api/sport-ai/upload', formData);
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderName", required = false) String folderName) {

        try {
            // 添加日志
            System.out.println("开始上传文档");
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize());
            System.out.println("文件夹: " + folderName);

            String result = anythingLLMClient.uploadDocument(file, null);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "上传成功");
            response.put("data", JSONUtil.parse(result));
            return response;
        } catch (Exception e) {
            // 打印详细错误信息
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "上传失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 通过链接上传文档
     * POST /api/sport-ai/upload-link
     *
     * 请求体：
     * {
     *     "link": "https://example.com/sport-guide.pdf",
     *     "title": "跑步指南"
     * }
     */
    @PostMapping("/upload-link")
    public Map<String, Object> uploadDocumentFromLink(@RequestBody UploadLinkRequest request) {
        String result = anythingLLMClient.uploadDocumentFromLink(request.getLink(), request.getTitle());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "链接上传成功");
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    /**
     * 上传原始文本作为文档
     * POST /api/sport-ai/upload-text
     *
     * 请求体：
     * {
     *     "text": "跑步时应该注意...",
     *     "title": "跑步注意事项"
     * }
     */
    @PostMapping("/upload-text")
    public Map<String, Object> uploadRawText(@RequestBody UploadTextRequest request) {
        String result = anythingLLMClient.uploadRawText(request.getText(), request.getTitle());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "文本上传成功");
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    /**
     * 删除文档
     * DELETE /api/sport-ai/document/{docName}
     */
    @DeleteMapping("/document/{docName}")
    public Map<String, Object> deleteDocument(@PathVariable String docName) {
        System.out.println("开始删除文档");
        System.out.println("文档名: " + docName);
        String result = anythingLLMClient.deleteDocument(docName);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "删除成功");
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    // ==================== 流式响应（SSE）接口 ====================

    /**
     * 流式聊天（SSE）- 打字机效果
     * POST /api/sport-ai/stream-chat
     *
     * 前端调用示例（EventSource）：
     * const eventSource = new EventSource('/api/sport-ai/stream-chat?userId=123&message=你好');
     * eventSource.addEventListener('message', (e) => {
     *     // 追加文本
     *     outputDiv.innerHTML += e.data;
     * });
     * eventSource.addEventListener('done', () => {
     *     eventSource.close();
     * });
     *
     * 或者用 fetch + ReadableStream 处理 POST 请求的 SSE
     */
    @PostMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request,@RequestHeader("Authorization") String authHeader) {
        // 1. 验证并提取 token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);

        // 2. 验证 token 有效性
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }

        // 3. 从 token 中获取用户名，然后查询用户ID
        Integer userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无法获取用户信息");
        }
        Long realUserId = userId.longValue();
        // 4. 调用 client，使用真实 userId（忽略 request 中的 userId）
        return anythingLLMClient.streamChat(request.getMessage(), realUserId);
    }

    /**
     * GET 方式的流式聊天（便于 EventSource 调用）
     */
    @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatGet(
            @RequestParam String message,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }
        Integer userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无法获取用户信息");
        }
        Long realUserId = userId.longValue();
        return anythingLLMClient.streamChat(message, realUserId);
    }

    /**
     * 创建对话线程（用于多轮对话）
     * POST /api/sport-ai/thread/new
     */
    @PostMapping("/thread/new")
    public Map<String, Object> createThread(@RequestBody CreateThreadRequest request,
                                            @RequestHeader("Authorization") String authHeader) {
        // 1. 验证 token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }

        // 2. 从 token 中获取用户名，然后查询用户ID
        Integer userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无法获取用户信息");
        }
        Long realUserId = userId.longValue();

        // 3. 调用 client，使用真实 userId（覆盖前端传来的值）
        String result = anythingLLMClient.createThread(
                request.getWorkspaceSlug(),
                request.getThreadName(),
                realUserId);  // 使用真实 ID

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "线程创建成功");
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    /**
     * 线程流式聊天（带运动记录上下文和长期记忆）
     * POST /api/sport-ai/thread/stream-chat
     */
    @PostMapping(value = "/thread/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamThreadChat(
            @RequestBody ThreadChatRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // 1. 验证 token
        System.out.println("=== 开始处理 AI 聊天请求 ===");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("错误：无效的 Authorization 头");
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            System.out.println("错误：Token 无效或已过期");
            throw new RuntimeException("Token 无效或已过期");
        }

        // 2. 从 token 中获取用户名，然后查询用户ID
        Integer userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            System.out.println("错误：无法获取用户信息");
            throw new RuntimeException("无法获取用户信息");
        }
        Long realUserId = userId.longValue();
        System.out.println("用户ID: " + realUserId);

        // 3. 获取用户的运动记录作为上下文（短期记忆）
        System.out.println("正在获取运动记录...");
        String exerciseContext = exerciseRecordService.getExerciseContext(realUserId,token);
        System.out.println("运动记录获取完成，长度: " + (exerciseContext != null ? exerciseContext.length() : 0));

        // 4. 获取用户的长期记忆（只获取当前线程的记录）
        System.out.println("正在获取长期记忆...");
        String longTermContext = longTermMemoryService.getMemoryContext(realUserId, request.getThreadSlug(), request.getMessage());
        System.out.println("长期记忆获取完成，长度: " + (longTermContext != null ? longTermContext.length() : 0));

        // 5. 合并上下文
        String messageWithContext = request.getMessage();
        StringBuilder contextBuilder = new StringBuilder();

        if (longTermContext != null && !longTermContext.isEmpty()) {
            contextBuilder.append(longTermContext).append("\n\n");
        }
        if (exerciseContext != null && !exerciseContext.isEmpty()) {
            contextBuilder.append("以下是用户的运动记录，请参考这些信息回答用户的问题：\n");
            contextBuilder.append(exerciseContext).append("\n\n");
        }

        if (contextBuilder.length() > 0) {
            messageWithContext = contextBuilder.toString() + "用户问题：" + request.getMessage();
        }

        System.out.println("发送给 AI 的消息长度: " + messageWithContext.length());
        System.out.println("工作区: " + request.getWorkspaceSlug());
        System.out.println("线程: " + request.getThreadSlug());

        // 6. 保存原始用户消息用于后续保存摘要
        String originalUserMessage = request.getMessage();

        // 7. 调用 client，使用真实 userId（忽略 request 中的 userId）
        try {
            // 使用带回调的版本，自动保存对话摘要
            SseEmitter emitter = anythingLLMClient.streamThreadChatWithContext(
                    request.getWorkspaceSlug(),
                    request.getThreadSlug(),
                    messageWithContext,
                    realUserId,
                    (userMsg, aiResponse) -> {
                        // 回调：自动保存对话摘要到长期记忆（关联当前线程）
                        System.out.println("=== 自动保存对话摘要 ===");
                        System.out.println("用户问题: " + userMsg);
                        System.out.println("AI回答长度: " + aiResponse.length());
                        try {
                            int saveResult = longTermMemoryService.saveConversationSummary(
                                    realUserId, request.getThreadSlug(), userMsg, aiResponse);
                            System.out.println("对话摘要保存结果: " + (saveResult > 0 ? "成功" : "失败"));
                        } catch (Exception e) {
                            System.out.println("保存对话摘要失败: " + e.getMessage());
                        }
                    });

            // 监听 SSE 完成事件
            emitter.onCompletion(() -> {
                System.out.println("SSE 连接完成");
            });

            emitter.onError(e -> {
                System.out.println("SSE 错误: " + e.getMessage());
            });

            return emitter;
        } catch (Exception e) {
            System.out.println("调用 AnythingLLM 失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("调用 AI 失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作区线程列表（根据用户ID过滤）
     * GET /api/sport-ai/threads
     */
    @GetMapping("/threads")
    public String getThreads(@RequestParam(value = "workspaceSlug", required = false) String workspaceSlug,
                                          @RequestHeader("Authorization") String authHeader) {
        // 1. 验证 token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }

        // 2. 从 token 中获取用户名，然后查询用户ID
        Integer userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无法获取用户信息");
        }

        // 3. 使用默认工作区或请求的工作区
        String targetWorkspaceSlug = workspaceSlug != null ? workspaceSlug : "9a719805-88c9-4aaf-ac7b-5b3d82118ad8";

        // 4. 调用 client 获取线程列表
        String result = anythingLLMClient.getWorkspaceThreads(targetWorkspaceSlug);
        System.out.println("线程列表-----------=========================="+result+"-----------==========================");

        // 5. 根据用户ID过滤线程
        cn.hutool.json.JSONObject data = JSONUtil.parseObj(result);
        if (data.containsKey("workspace")) {
            cn.hutool.json.JSONArray workspaces = data.getJSONArray("workspace");
            if (workspaces != null && !workspaces.isEmpty()) {
                cn.hutool.json.JSONObject workspace = workspaces.getJSONObject(0);
                if (workspace.containsKey("threads")) {
                    cn.hutool.json.JSONArray allThreads = workspace.getJSONArray("threads");
                    if (allThreads != null) {
                        cn.hutool.json.JSONArray userThreads = new cn.hutool.json.JSONArray();
                        for (int i = 0; i < allThreads.size(); i++) {
                            cn.hutool.json.JSONObject thread = allThreads.getJSONObject(i);
                            // 获取线程的 user_id 字段
                            Object userIdObj = thread.get("user_id");
                            // 过滤出当前用户的线程或未分配用户的线程
                            if (userIdObj == null || userIdObj.toString().equals(String.valueOf(userId))) {
                                userThreads.add(thread);
                            }
                        }
                        workspace.set("threads", userThreads);
                    }
                }
            }
        }

        // 直接返回 JSON 字符串，避免 Jackson 序列化问题
        return "{\"code\":200,\"message\":\"获取线程列表成功\",\"data\":" + data.toString() + "}";
    }

    /**
     * 获取线程聊天记录
     * GET /api/sport-ai/thread/chats
     */
    @GetMapping("/thread/chats")
    public Map<String, Object> getThreadChats(@RequestParam("threadSlug") String threadSlug,
                                             @RequestParam(value = "workspaceSlug", required = false) String workspaceSlug,
                                             @RequestHeader("Authorization") String authHeader) {
        // 1. 验证 token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }

        // 2. 使用默认工作区或请求的工作区
        String targetWorkspaceSlug = workspaceSlug != null ? workspaceSlug : "9a719805-88c9-4aaf-ac7b-5b3d82118ad8";

        // 3. 调用 client 获取聊天记录
        String result = anythingLLMClient.getThreadChats(targetWorkspaceSlug, threadSlug);
        System.out.println("聊天记录-----------=========================="+result+"-----------==========================");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "获取聊天记录成功");
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    // ==================== 原有接口 ====================

    /**
     * 普通聊天（非流式）
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        String answer = anythingLLMClient.chat(request.getMessage(), request.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", answer);
        return result;
    }

    /**
     * 知识库检索
     */
    @PostMapping("/search")
    public Map<String, Object> search(@RequestBody SearchRequest request) {
        String result = anythingLLMClient.vectorSearch(
                request.getWorkspaceSlug(),
                request.getQuery());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/documents")
    public Map<String, Object> listDocuments() {
        String result = anythingLLMClient.listDocuments();
        System.out.println("-----------=========================="+result+"-----------==========================");

        Map<String, Object> response = new HashMap<>();
        System.out.println("-----------=========================="+response+"-----------==========================");
        response.put("code", 200);
        response.put("data", JSONUtil.parse(result));
        return response;
    }

    // ==================== 长期记忆管理接口 ====================

    /**
     * 保存用户偏好
     * POST /sport-ai/memory/preference
     */
    @PostMapping("/memory/preference")
    public Map<String, Object> savePreference(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            String token = validateAndExtractToken(authHeader);
            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }

            String content = request.get("content");
            if (content == null || content.isEmpty()) {
                response.put("code", 400);
                response.put("message", "内容不能为空");
                return response;
            }

            int result = longTermMemoryService.saveUserPreference(userId.longValue(), content);
            if (result > 0) {
                response.put("code", 200);
                response.put("message", "偏好保存成功");
            } else {
                response.put("code", 500);
                response.put("message", "保存失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "保存失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 保存用户目标
     * POST /sport-ai/memory/goal
     */
    @PostMapping("/memory/goal")
    public Map<String, Object> saveGoal(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            String token = validateAndExtractToken(authHeader);
            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }

            String content = request.get("content");
            if (content == null || content.isEmpty()) {
                response.put("code", 400);
                response.put("message", "内容不能为空");
                return response;
            }

            int result = longTermMemoryService.saveUserGoal(userId.longValue(), content);
            if (result > 0) {
                response.put("code", 200);
                response.put("message", "目标保存成功");
            } else {
                response.put("code", 500);
                response.put("message", "保存失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "保存失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 保存对话摘要
     * POST /sport-ai/memory/conversation
     */
    @PostMapping("/memory/conversation")
    public Map<String, Object> saveConversation(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            String token = validateAndExtractToken(authHeader);
            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }

            String conversation = request.get("conversation");
            if (conversation == null || conversation.isEmpty()) {
                response.put("code", 400);
                response.put("message", "对话内容不能为空");
                return response;
            }

            int result = longTermMemoryService.saveConversationSummary(userId.longValue(), conversation);
            if (result > 0) {
                response.put("code", 200);
                response.put("message", "对话摘要保存成功");
            } else {
                response.put("code", 500);
                response.put("message", "保存失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "保存失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 获取用户所有记忆
     * GET /sport-ai/memory/list
     */
    @GetMapping("/memory/list")
    public Map<String, Object> getMemoryList(@RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            String token = validateAndExtractToken(authHeader);
            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }

            List<LongTermMemory> memories = longTermMemoryService.getMemoriesByUserId(userId.longValue());
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", memories);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 删除记忆
     * DELETE /sport-ai/memory/{id}
     */
    @DeleteMapping("/memory/{id}")
    public Map<String, Object> deleteMemory(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            validateAndExtractToken(authHeader);

            boolean success = longTermMemoryService.deleteMemory(id);
            if (success) {
                response.put("code", 200);
                response.put("message", "删除成功");
            } else {
                response.put("code", 404);
                response.put("message", "记忆不存在");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "删除失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 获取记忆统计
     * GET /sport-ai/memory/stats
     */
    @GetMapping("/memory/stats")
    public Map<String, Object> getMemoryStats(@RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            String token = validateAndExtractToken(authHeader);
            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }

            Map<String, Object> stats = longTermMemoryService.getMemoryStats(userId.longValue());
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", stats);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 验证并提取 Token
     */
    private String validateAndExtractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new RuntimeException("Token 无效或已过期");
        }
        return token;
    }

    /**
     * 测试获取运动记录（用于验证SQLite连接）
     * GET /sport-ai/test/exercise-records
     */
    @GetMapping("/test/exercise-records")
    public Map<String, Object> testExerciseRecords(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证 token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("code", 401);
                response.put("message", "无效的 Authorization 头");
                return response;
            }
            String token = authHeader.substring(7);
            if (!jwtUtils.validateToken(token)) {
                response.put("code", 401);
                response.put("message", "Token 无效或已过期");
                return response;
            }

            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }

            // 获取运动记录上下文
            String context = exerciseRecordService.getExerciseContext(userId.longValue(),token);
            Map<String, Object> stats = exerciseRecordService.getExerciseStats(userId.longValue(),token);

            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("exerciseContext", context);
            response.put("stats", stats);
            response.put("userId", userId);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 测试完整的 AI 聊天流程（包含自动保存对话摘要）
     * POST /sport-ai/test/chat-with-memory
     */
    @PostMapping("/test/chat-with-memory")
    public Map<String, Object> testChatWithMemory(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 验证 token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("code", 401);
                response.put("message", "无效的 Authorization 头");
                return response;
            }
            String token = authHeader.substring(7);
            if (!jwtUtils.validateToken(token)) {
                response.put("code", 401);
                response.put("message", "Token 无效或已过期");
                return response;
            }

            // 2. 获取用户ID
            Integer userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                response.put("code", 400);
                response.put("message", "无法获取用户信息");
                return response;
            }
            Long realUserId = userId.longValue();

            // 3. 获取消息
            String message = request.get("message");
            if (message == null || message.isEmpty()) {
                response.put("code", 400);
                response.put("message", "消息不能为空");
                return response;
            }

            // 4. 获取运动记录和长期记忆
            String exerciseContext = exerciseRecordService.getExerciseContext(realUserId,token);
            String longTermContext = longTermMemoryService.getMemoryContext(realUserId, message);

            // 5. 合并上下文
            String messageWithContext = message;
            StringBuilder contextBuilder = new StringBuilder();
            if (longTermContext != null && !longTermContext.isEmpty()) {
                contextBuilder.append(longTermContext).append("\n\n");
            }
            if (exerciseContext != null && !exerciseContext.isEmpty()) {
                contextBuilder.append("以下是用户最近的运动记录\n");
                contextBuilder.append(exerciseContext).append("\n\n");
            }
            if (contextBuilder.length() > 0) {
                messageWithContext = contextBuilder.toString() + "请结合以上所有信息回答用户的这个问题：" + message;
            }

            // 6. 调用 AI（使用普通聊天接口，非流式）
            String workspaceSlug = "9a719805-88c9-4aaf-ac7b-5b3d82118ad8";
            String aiResponse = anythingLLMClient.chat(workspaceSlug, messageWithContext, realUserId);

            System.out.println("=== AI 响应 ===");
            System.out.println("用户问题: " + message);
            System.out.println("AI 回答: " + aiResponse);

            // 7. 自动保存对话摘要到长期记忆
            if (aiResponse != null && !aiResponse.isEmpty()) {
                int saveResult = longTermMemoryService.saveConversationSummary(realUserId, message, aiResponse);
                System.out.println("对话摘要保存结果: " + (saveResult > 0 ? "成功" : "失败"));
            }

            // 8. 构建响应
            response.put("code", 200);
            response.put("message", "聊天成功");
            response.put("data", aiResponse);
            response.put("userId", userId);
            response.put("hasExerciseRecords", exerciseContext != null && !exerciseContext.isEmpty());
            response.put("hasLongTermMemory", longTermContext != null && !longTermContext.isEmpty());

        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "聊天失败: " + e.getMessage());
        }

        return response;
    }
}
