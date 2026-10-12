package com.example.item.demo35.service;

import java.util.List;

public interface RedisService {
    List<Integer> getRecommendedPosts(Integer userId);
}
