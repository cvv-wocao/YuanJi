package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class PostLike {

    private Integer id;
    private Integer postId;
    private Integer userId;
    private Date createTime;

    // 关联的用户信息
    private User user;

    // 关联的帖子信息
    private Post post;
}
