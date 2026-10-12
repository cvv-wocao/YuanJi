package com.example.item.demo35.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LongTermMemory {

    private Long id;
    
    private Long userId;
    
    private String threadSlug;
    
    private String content;
    
    private String memoryType;
    
    private String keywords;
    
    private Double relevanceScore;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}