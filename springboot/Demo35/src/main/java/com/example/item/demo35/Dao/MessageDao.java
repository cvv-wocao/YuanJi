package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageDao {

    // 发送消息
    boolean insert(Message message);

    // 根据ID查询消息
    Message selectById(@Param("id") Integer id);

    // 获取两个用户之间的消息列表
    List<Message> selectMessagesBetweenUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    // 获取用户的所有消息会话列表（每个会话显示最新消息）
    List<Message> selectUserConversations(@Param("userId") Integer userId);

    // 获取用户未读消息数量
    int countUnreadMessages(@Param("userId") Integer userId);

    // 获取用户与特定发送者的未读消息数量
    int countUnreadMessagesFromSender(@Param("receiverId") Integer receiverId, @Param("senderId") Integer senderId);

    // 标记消息为已读
    boolean markAsRead(@Param("id") Integer id);

    // 标记与特定用户的所有消息为已读
    boolean markAllAsRead(@Param("userId") Integer userId, @Param("senderId") Integer senderId);

    // 删除消息
    boolean deleteById(@Param("id") Integer id);

    // 获取用户发送给特定接收者的消息数量
    int countMessagesFromTo(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);
}
