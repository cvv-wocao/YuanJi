package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Follow {

    private Integer id;
    private Integer followerId; // 关注者ID
    private Integer followingId; // 被关注者ID
    private Date createTime;

    // 关联的用户信息
    private User follower; // 关注者信息
    private User following; // 被关注者信息
}