package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class CommentLike {

    private Integer id;
    private Integer commentId;
    private Integer userId;
    private Date createTime;

    // 关联的用户信息
    private User user;

    // 关联的评论信息
    private PostComment comment;
}
