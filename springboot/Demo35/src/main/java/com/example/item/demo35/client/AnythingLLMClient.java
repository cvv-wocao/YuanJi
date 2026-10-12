package com.example.item.demo35.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class AnythingLLMClient {

    @Value("${anythingllm.base-url}")
    private String baseUrl;

    @Value("${anythingllm.api-key}")
    private String apiKey;

    @Value("${anythingllm.workspace-slug}")
    private String workspaceSlug;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 构建请求头（带 API Key 认证）
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * 构建带 Accept 头的请求头（用于流式响应）
     */
    private HttpHeaders buildStreamHeaders() {
        HttpHeaders headers = buildHeaders();
        headers.set("Accept", "text/event-stream");
        return headers;
    }

    // ==================== 文件上传相关 ====================

    /**
     * 上传文件到 AnythingLLM
     * POST /api/v1/document/upload
     *
     * @param file MultipartFile 文件
     * @param folderName 可选，目标文件夹名称
     * @return 上传结果
     */
    public String uploadDocument(MultipartFile file, String folderName) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }

            // 将 MultipartFile 转为临时文件
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            // 创建临时文件目录
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // 将 MultipartFile 转为临时文件，保留原始文件名
            File tempFile = new File(tempDir, file.getOriginalFilename());
            file.transferTo(tempFile);

            String url = baseUrl + "/api/v1/document/upload";
            if (folderName != null && !folderName.isEmpty()) {
                url += "/" + folderName;
            }

            System.out.println("请求 URL: " + url);

            // 构建 multipart/form-data 请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("响应状态码: " + response.getStatusCode());
            System.out.println("响应体: " + response.getBody());

            // 删除临时文件
            tempFile.delete();

            return response.getBody();
        } catch (HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            System.err.println("=== AnythingLLM 服务器错误 ===");
            System.err.println("状态码: " + e.getStatusCode());
            System.err.println("响应体: " + errorBody);
            e.printStackTrace();
            throw new RuntimeException("上传失败: " + errorBody, e);
        } catch (IOException e) {
            System.err.println("文件 IO 错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("其他错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件（简化版，不上传到特定文件夹）
     */
    public String uploadDocument(MultipartFile file) {
        return uploadDocument(file, null);
    }

    /**
     * 通过 URL 上传文档
     * POST /api/v1/document/upload-link
     *
     * @param linkUrl 文档链接
     * @param title 文档标题（可选）
     */
    public String uploadDocumentFromLink(String linkUrl, String title) {
        String url = baseUrl + "/api/v1/document/upload-link";

        Map<String, Object> body = new HashMap<>();
        body.put("link", linkUrl);
        if (title != null) {
            body.put("title", title);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("链接上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传原始文本作为文档
     * POST /api/v1/document/raw-text
     *
     * @param text 文本内容
     * @param title 文档标题
     */
    public String uploadRawText(String text, String title) {
        String url = baseUrl + "/api/v1/document/raw-text";

        Map<String, Object> body = new HashMap<>();
        body.put("textContent", text);

        // 将title作为metadata传递
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", title);
        body.put("metadata", metadata);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("文本上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文档
     * DELETE /api/v1/document/{docName}
     */
    public String deleteDocument(String docName) {
        String url = baseUrl + "/api/v1/document/" + docName;
        System.out.println("请求 URL: " + url);

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("删除文档失败: " + e.getMessage(), e);
        }
    }

    // ==================== 流式响应（SSE）相关 ====================

    /**
     * 流式聊天（SSE）
     * POST /api/v1/workspace/{slug}/stream-chat
     *
     * @param workspaceSlug 工作区标识
     * @param message 用户消息
     * @param userId 用户ID（用于会话隔离）
     * @return SseEmitter 对象，用于推送流式数据
     */
    public SseEmitter streamChat(String workspaceSlug, String message, Long userId) {
        // 增加SseEmitter的超时时间到5分钟
        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                String url = baseUrl + "/api/v1/workspace/" + workspaceSlug + "/stream-chat";

                Map<String, Object> body = new HashMap<>();
                body.put("message", message);
                body.put("mode", "chat");
                body.put("sessionId", "user_" + userId);

                // 创建一个临时的RestTemplate实例，设置更长的超时时间
                RestTemplate tempRestTemplate = new RestTemplate();
                // 设置连接超时和读取超时为5分钟
                ((org.springframework.http.client.SimpleClientHttpRequestFactory) tempRestTemplate.getRequestFactory()).setConnectTimeout(300000);
                ((org.springframework.http.client.SimpleClientHttpRequestFactory) tempRestTemplate.getRequestFactory()).setReadTimeout(300000);

                // 使用 RestTemplate 的 execute 方法处理流式响应
                tempRestTemplate.execute(url, HttpMethod.POST,
                        request -> {
                            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            request.getHeaders().set("Authorization", "Bearer " + apiKey);
                            request.getHeaders().set("Accept", "text/event-stream");
                            request.getBody().write(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
                        },
                        response -> {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                                String line;
                                StringBuilder currentData = new StringBuilder();

                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("data:")) {
                                        String data = line.substring(5).trim();

                                        if ("[DONE]".equals(data)) {
                                            // 流结束
                                            emitter.send(SseEmitter.event()
                                                    .name("done")
                                                    .data(""));
                                            break;
                                        }

                                        // 解析 JSON 数据
                                        JSONObject json = JSONUtil.parseObj(data);
                                        String textChunk = json.getStr("textResponse", "");

                                        if (!textChunk.isEmpty()) {
                                            // 发送文本片段
                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(textChunk));
                                        }

                                        // 发送元数据（如果有）
                                        if (json.containsKey("sources")) {
                                            emitter.send(SseEmitter.event()
                                                    .name("sources")
                                                    .data(json.get("sources")));
                                        }
                                    }
                                }
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                            return null;
                        });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 流式聊天（使用默认工作区）
     */
    public SseEmitter streamChat(String message, Long userId) {
        return streamChat(this.workspaceSlug, message, userId);
    }

    /**
     * 工作区线程流式聊天
     * POST /api/v1/workspace/{slug}/thread/{threadSlug}/stream-chat
     *
     * @param workspaceSlug 工作区标识
     * @param threadSlug 线程标识（用于多轮对话）
     * @param message 用户消息
     * @param userId 用户ID
     */
    public SseEmitter streamThreadChat(String workspaceSlug, String threadSlug, String message, Long userId) {
        return streamThreadChatWithContext(workspaceSlug, threadSlug, message, userId);
    }

    /**
     * 工作区线程流式聊天（带上下文）
     * POST /api/v1/workspace/{slug}/thread/{threadSlug}/stream-chat
     *
     * @param workspaceSlug 工作区标识
     * @param threadSlug 线程标识（用于多轮对话）
     * @param message 用户消息（可能包含上下文）
     * @param userId 用户ID
     */
    public SseEmitter streamThreadChatWithContext(String workspaceSlug, String threadSlug, String message, Long userId) {
        return streamThreadChatWithContext(workspaceSlug, threadSlug, message, userId, null);
    }

    /**
     * 工作区线程流式聊天（带上下文和回调）
     * POST /api/v1/workspace/{slug}/thread/{threadSlug}/stream-chat
     *
     * @param workspaceSlug 工作区标识
     * @param threadSlug 线程标识（用于多轮对话）
     * @param message 用户消息（可能包含上下文）
     * @param userId 用户ID
     * @param onComplete 完成回调，用于保存对话摘要
     */
    public SseEmitter streamThreadChatWithContext(String workspaceSlug, String threadSlug, String message, Long userId, 
                                                  java.util.function.BiConsumer<String, String> onComplete) {
        // 增加SseEmitter的超时时间到5分钟
        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            StringBuilder fullResponse = new StringBuilder();
            try {
                String url = baseUrl + "/api/v1/workspace/" + workspaceSlug + "/thread/" + threadSlug + "/stream-chat";

                Map<String, Object> body = new HashMap<>();
                body.put("message", message);
                body.put("mode", "chat");

                System.out.println("=== 调用 AnythingLLM 流式接口 ===");
                System.out.println("URL: " + url);
                System.out.println("消息长度: " + message.length());

                // 创建一个临时的RestTemplate实例，设置更长的超时时间
                RestTemplate tempRestTemplate = new RestTemplate();
                // 设置连接超时和读取超时为5分钟
                ((org.springframework.http.client.SimpleClientHttpRequestFactory) tempRestTemplate.getRequestFactory()).setConnectTimeout(300000);
                ((org.springframework.http.client.SimpleClientHttpRequestFactory) tempRestTemplate.getRequestFactory()).setReadTimeout(300000);

                tempRestTemplate.execute(url, HttpMethod.POST,
                        request -> {
                            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            request.getHeaders().set("Authorization", "Bearer " + apiKey);
                            request.getHeaders().set("Accept", "text/event-stream");
                            request.getBody().write(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
                        },
                        response -> {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("data:")) {
                                        String data = line.substring(5).trim();
                                        if (!"[DONE]".equals(data)) {
                                            try {
                                                JSONObject json = JSONUtil.parseObj(data);
                                                String textChunk = json.getStr("textResponse", "");
                                                if (!textChunk.isEmpty()) {
                                                    fullResponse.append(textChunk);
                                                    emitter.send(SseEmitter.event()
                                                            .name("message")
                                                            .data(textChunk));
                                                }
                                            } catch (Exception e) {
                                                System.out.println("解析JSON失败: " + data);
                                            }
                                        }
                                    }
                                }
                                emitter.complete();
                                
                                // 调用回调保存对话摘要
                                if (onComplete != null && fullResponse.length() > 0) {
                                    String originalUserMessage = extractOriginalMessage(message);
                                    onComplete.accept(originalUserMessage, fullResponse.toString());
                                }
                                System.out.println("=== AI 响应完成 ===");
                                System.out.println("完整响应长度: " + fullResponse.length());
                            } catch (Exception e) {
                                System.out.println("SSE读取错误: " + e.getMessage());
                                emitter.completeWithError(e);
                            }
                            return null;
                        });
            } catch (Exception e) {
                System.out.println("调用 AnythingLLM 失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 从带上下文的消息中提取原始用户问题
     */
    private String extractOriginalMessage(String messageWithContext) {
        int idx = messageWithContext.indexOf("用户问题：");
        if (idx >= 0) {
            return messageWithContext.substring(idx + 5).trim();
        }
        return messageWithContext;
    }

    // ==================== 原有方法 ====================

    /**
     * 普通聊天（非流式）
     */
    public String chat(String workspaceSlug, String message, Long userId) {
        String url = baseUrl + "/api/v1/workspace/" + workspaceSlug + "/chat";

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("mode", "chat");
        body.put("sessionId", "user_" + userId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        try {
            // 创建一个临时的RestTemplate实例，设置更长的超时时间
            RestTemplate tempRestTemplate = new RestTemplate();
            // 设置连接超时和读取超时为5分钟
            ((org.springframework.http.client.SimpleClientHttpRequestFactory) tempRestTemplate.getRequestFactory()).setConnectTimeout(300000);
            ((org.springframework.http.client.SimpleClientHttpRequestFactory) tempRestTemplate.getRequestFactory()).setReadTimeout(300000);
            
            ResponseEntity<String> response = tempRestTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject json = JSONUtil.parseObj(response.getBody());
                return json.getStr("textResponse");
            }
        } catch (Exception e) {
            throw new RuntimeException("调用 AnythingLLM 失败: " + e.getMessage(), e);
        }
        return null;
    }

    public String chat(String message, Long userId) {
        return chat(this.workspaceSlug, message, userId);
    }

    /**
     * 向量检索
     */
    public String vectorSearch(String workspaceSlug, String query) {
        String url = baseUrl + "/api/v1/workspace/" + workspaceSlug + "/vector-search";

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("topN", 5);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("向量检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文档列表
     */
    public String listDocuments() {
        String url = baseUrl + "/api/v1/documents";

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        System.out.println(entity);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            System.out.println("===================//////response: " + response);
            return response.getBody();
        } catch (Exception e) {
            System.out.println("===================//////e: " + e);
            throw new RuntimeException("获取文档列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建工作区
     */
    public String createWorkspace(String name) {
        String url = baseUrl + "/api/v1/workspace/new";

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("创建工作区失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建工作区线程（用于多轮对话）
     * POST /api/v1/workspace/{slug}/thread/new
     */
    public String createThread(String workspaceSlug, String threadName, Long userId) {
        String url = baseUrl + "/api/v1/workspace/" + workspaceSlug + "/thread/new";

        Map<String, Object> body = new HashMap<>();
        body.put("name", threadName);
        body.put("userId", userId);
        
        // 添加日志调试
        System.out.println("========== 创建线程请求 ==========");
        System.out.println("URL: " + url);
        System.out.println("请求体: " + body.toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("创建线程响应: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.out.println("创建线程失败: " + e.getMessage());
            throw new RuntimeException("创建线程失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取工作区线程列表
     * GET /api/v1/workspace/{slug}
     */
    public String getWorkspaceThreads(String workspaceSlug) {
        String url = baseUrl + "/api/v1/workspace/" + workspaceSlug;

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("获取线程列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取线程聊天记录
     * GET /api/v1/workspace/{slug}/chats
     */
    public String getThreadChats(String workspaceSlug, String threadSlug) {
        String url = baseUrl + "/api/v1/workspace/" + workspaceSlug + "/chats?apiSessionId=" + threadSlug;

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("获取聊天记录失败: " + e.getMessage(), e);
        }
    }
}
