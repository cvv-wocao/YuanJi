package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Message {

    private Integer id;
    private Integer senderId; // 发送者ID
    private Integer receiverId; // 接收者ID
    private String content; // 消息内容
    private Boolean isRead; // 是否已读
    private Date createTime; // 发送时间

    // 关联的用户信息
    private User sender; // 发送者信息
    private User receiver; // 接收者信息
}
