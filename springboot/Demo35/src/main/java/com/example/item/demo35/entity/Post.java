package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class Post {

    private Integer id;
    private Integer userId;
    private String content;
    private List<String> images;
    private List<String> videos;
    private String cover;
    private Integer likeCount;
    private Integer commentCount;
    private Date createTime;
    private Date updateTime;

    // 关联的用户信息（查询时使用）
    private User user;

    // 当前登录用户是否点赞
    private Boolean isLiked;
}
