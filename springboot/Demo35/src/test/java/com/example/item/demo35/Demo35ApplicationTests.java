package com.example.item.demo35;

import com.example.item.demo35.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class Demo35ApplicationTests {

    @Autowired
    private RedisService redisService;

    @Test
    void contextLoads() {

        List<Integer> RedisPost = redisService.getRecommendedPosts(1);
        System.out.println(RedisPost);

    }

}
