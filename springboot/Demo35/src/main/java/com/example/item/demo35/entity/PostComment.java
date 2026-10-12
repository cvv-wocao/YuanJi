package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class PostComment {

    private Integer id;
    private Integer postId;
    private Integer userId;
    private String content;
    private Integer parentId;
    private Integer likeCount;
    private Date createTime;
    private Date updateTime;

    // 关联的用户信息
    private User user;

    // 关联的帖子信息
    private Post post;

    // 父评论（用于回复功能）
    private PostComment parent;

    // 子评论列表（该评论的回复）
    private List<PostComment> replies;

    // 当前登录用户是否点赞该评论
    private Boolean isLiked;
}
