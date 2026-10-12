package com.example.item.demo35.service;

import com.example.item.demo35.entity.Message;
import java.util.List;
import java.util.Map;

public interface MessageService {

    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return 发送结果，包含消息对象或错误信息
     */
    Map<String, Object> sendMessage(Integer senderId, Integer receiverId, String content);

    /**
     * 获取两个用户之间的消息列表
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 消息列表
     */
    List<Message> getMessagesBetweenUsers(Integer userId1, Integer userId2);

    /**
     * 获取用户的所有消息会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Message> getUserConversations(Integer userId);

    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    int getUnreadMessageCount(Integer userId);

    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @param userId 当前用户ID（验证权限）
     * @return 是否成功
     */
    boolean markMessageAsRead(Integer messageId, Integer userId);

    /**
     * 标记与特定用户的所有消息为已读
     * @param userId 当前用户ID
     * @param senderId 发送者ID
     * @return 是否成功
     */
    boolean markAllMessagesAsRead(Integer userId, Integer senderId);

    /**
     * 删除消息
     * @param messageId 消息ID
     * @param userId 当前用户ID（验证权限）
     * @return 是否成功
     */
    boolean deleteMessage(Integer messageId, Integer userId);

    /**
     * 检查两个用户是否互相关注
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 是否互相关注
     */
    boolean isMutualFollow(Integer userId1, Integer userId2);

    /**
     * 检查是否可以发送消息
     * 互相关注：可以无限制发送
     * 未互相关注：只能发送一条消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 是否可以发送
     */
    boolean canSendMessage(Integer senderId, Integer receiverId);
}
