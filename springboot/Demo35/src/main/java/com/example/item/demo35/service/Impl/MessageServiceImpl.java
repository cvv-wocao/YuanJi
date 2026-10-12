package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.FollowDao;
import com.example.item.demo35.Dao.MessageDao;
import com.example.item.demo35.Dao.UserDao;
import com.example.item.demo35.entity.Message;
import com.example.item.demo35.entity.User;
import com.example.item.demo35.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageDao messageDao;

    @Autowired
    private FollowDao followDao;

    @Autowired
    private UserDao userDao;

    @Override
    public Map<String, Object> sendMessage(Integer senderId, Integer receiverId, String content) {
        Map<String, Object> result = new HashMap<>();

        // 验证参数
        if (senderId == null || receiverId == null || content == null || content.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "参数错误");
            return result;
        }

        // 不能给自己发消息
        if (senderId.equals(receiverId)) {
            result.put("success", false);
            result.put("message", "不能给自己发送消息");
            return result;
        }

        // 检查接收者是否存在
        User receiver = userDao.selectById(receiverId);
        if (receiver == null) {
            result.put("success", false);
            result.put("message", "接收者不存在");
            return result;
        }

        // 检查是否可以发送消息
        if (!canSendMessage(senderId, receiverId)) {
            result.put("success", false);
            result.put("message", "您只能发送一条消息，对方回复后才能继续发送");
            return result;
        }

        // 创建消息
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content.trim());
        message.setIsRead(false);

        boolean success = messageDao.insert(message);
        if (success) {
            result.put("success", true);
            result.put("message", "发送成功");
            result.put("data", message);
        } else {
            result.put("success", false);
            result.put("message", "发送失败");
        }

        return result;
    }

    @Override
    public List<Message> getMessagesBetweenUsers(Integer userId1, Integer userId2) {
        return messageDao.selectMessagesBetweenUsers(userId1, userId2);
    }

    @Override
    public List<Message> getUserConversations(Integer userId) {
        return messageDao.selectUserConversations(userId);
    }

    @Override
    public int getUnreadMessageCount(Integer userId) {
        return messageDao.countUnreadMessages(userId);
    }

    @Override
    public boolean markMessageAsRead(Integer messageId, Integer userId) {
        // 验证消息是否存在且属于当前用户
        Message message = messageDao.selectById(messageId);
        if (message == null || !message.getReceiverId().equals(userId)) {
            return false;
        }
        return messageDao.markAsRead(messageId);
    }

    @Override
    public boolean markAllMessagesAsRead(Integer userId, Integer senderId) {
        return messageDao.markAllAsRead(userId, senderId);
    }

    @Override
    public boolean deleteMessage(Integer messageId, Integer userId) {
        // 验证消息是否存在且属于当前用户（发送者或接收者都可以删除）
        Message message = messageDao.selectById(messageId);
        if (message == null) {
            return false;
        }
        // 只有发送者或接收者可以删除
        if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
            return false;
        }
        return messageDao.deleteById(messageId);
    }

    @Override
    public boolean isMutualFollow(Integer userId1, Integer userId2) {
        // 检查userId1是否关注了userId2，且userId2是否关注了userId1
        boolean user1FollowUser2 = followDao.isFollowing(userId1, userId2);
        boolean user2FollowUser1 = followDao.isFollowing(userId2, userId1);
        return user1FollowUser2 && user2FollowUser1;
    }

    @Override
    public boolean canSendMessage(Integer senderId, Integer receiverId) {
        // 所有用户都可以无限制发送消息
        return true;
    }
}
