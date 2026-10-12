CREATE TABLE IF NOT EXISTS long_term_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '记忆内容',
    memory_type VARCHAR(50) NOT NULL COMMENT '记忆类型：PREFERENCE/BEHAVIOR/GOAL/CONVERSATION/FEEDBACK/HEALTH',
    keywords VARCHAR(500) COMMENT '关键词（逗号分隔）',
    relevance_score DOUBLE DEFAULT 1.0 COMMENT '相关性评分',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_memory_type (memory_type),
    INDEX idx_keywords (keywords(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长期记忆表';