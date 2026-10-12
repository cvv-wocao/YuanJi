package com.example.item.demo35.entity;

import lombok.Data;

public class AIrequest {
    @Data
    public static class ChatRequest {
        private Long userId;
        private String message;
    }

    @Data
    public static class SearchRequest {
        private String workspaceSlug;
        private String query;
    }

    @Data
    public static class UploadLinkRequest {
        private String link;
        private String title;
    }

    @Data
    public static class UploadTextRequest {
        private String text;
        private String title;
    }

    @Data
    public static class CreateThreadRequest {
        private String workspaceSlug;
        private String threadName;
        private Long userId;
    }

    @Data
    public static class ThreadChatRequest {
        private String workspaceSlug;
        private String threadSlug;
        private String message;
        private Long userId;
    }
}
