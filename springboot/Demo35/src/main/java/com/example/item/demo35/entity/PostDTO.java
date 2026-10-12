package com.example.item.demo35.entity;
import lombok.Data;
import java.util.List;

@Data
public class PostDTO {
    private String content;
    private List<String> images;
    private List<String> videos;
}
