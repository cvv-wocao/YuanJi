package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Message;
import com.example.item.demo35.service.MessageService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 发送消息
     * @param request HTTP请求
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return 发送结果
     */
    @PostMapping("/send")
    public Result sendMessage(HttpServletRequest request,
                              @RequestParam("receiverId") Integer receiverId,
                              @RequestParam("content") String content) {
        Integer senderId = getCurrentUserId(request);
        if (senderId == null) {
            return Result.error("请先登录");
        }

        Map<String, Object> result = messageService.sendMessage(senderId, receiverId, content);
        boolean success = (Boolean) result.get("success");
        String message = (String) result.get("message");

        if (success) {
            return Result.success(message, result.get("data"));
        } else {
            return Result.error(message);
        }
    }

    /**
     * 获取与特定用户的消息列表
     * @param request HTTP请求
     * @param userId 对方用户ID
     * @return 消息列表
     */
    @GetMapping("/history/{userId}")
    public Result getMessageHistory(HttpServletRequest request, @PathVariable("userId") Integer userId) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        List<Message> messages = messageService.getMessagesBetweenUsers(currentUserId, userId);

        // 标记对方发送的消息为已读
        messageService.markAllMessagesAsRead(currentUserId, userId);

        return Result.success("获取成功", messages);
    }

    /**
     * 获取用户的所有消息会话列表
     * @param request HTTP请求
     * @return 会话列表
     */
    @GetMapping("/conversations")
    public Result getConversations(HttpServletRequest request) {
        Integer userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<Message> conversations = messageService.getUserConversations(userId);
        return Result.success("获取成功", conversations);
    }

    /**
     * 获取未读消息数量
     * @param request HTTP请求
     * @return 未读消息数量
     */
    @GetMapping("/unread/count")
    public Result getUnreadCount(HttpServletRequest request) {
        Integer userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        int count = messageService.getUnreadMessageCount(userId);
        Map<String, Integer> data = new HashMap<>();
        data.put("unreadCount", count);
        return Result.success("获取成功", data);
    }

    /**
     * 标记消息为已读
     * @param request HTTP请求
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PostMapping("/read/{messageId}")
    public Result markAsRead(HttpServletRequest request, @PathVariable("messageId") Integer messageId) {
        Integer userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        boolean success = messageService.markMessageAsRead(messageId, userId);
        if (success) {
            return Result.success("标记成功", null);
        } else {
            return Result.error("标记失败，消息不存在或无权限");
        }
    }

    /**
     * 标记与特定用户的所有消息为已读
     * @param request HTTP请求
     * @param senderId 发送者ID
     * @return 操作结果
     */
    @PostMapping("/read/all/{senderId}")
    public Result markAllAsRead(HttpServletRequest request, @PathVariable("senderId") Integer senderId) {
        Integer userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        boolean success = messageService.markAllMessagesAsRead(userId, senderId);
        if (success) {
            return Result.success("标记成功", null);
        } else {
            return Result.error("标记失败");
        }
    }

    /**
     * 删除消息
     * @param request HTTP请求
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{messageId}")
    public Result deleteMessage(HttpServletRequest request, @PathVariable("messageId") Integer messageId) {
        Integer userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        boolean success = messageService.deleteMessage(messageId, userId);
        if (success) {
            return Result.success("删除成功", null);
        } else {
            return Result.error("删除失败，消息不存在或无权限");
        }
    }

    /**
     * 检查是否可以发送消息
     * @param request HTTP请求
     * @param receiverId 接收者ID
     * @return 检查结果
     */
    @GetMapping("/can-send/{receiverId}")
    public Result canSendMessage(HttpServletRequest request, @PathVariable("receiverId") Integer receiverId) {
        Integer senderId = getCurrentUserId(request);
        if (senderId == null) {
            return Result.error("请先登录");
        }

        boolean canSend = messageService.canSendMessage(senderId, receiverId);
        boolean isMutualFollow = messageService.isMutualFollow(senderId, receiverId);

        Map<String, Object> data = new HashMap<>();
        data.put("canSend", canSend);
        data.put("isMutualFollow", isMutualFollow);

        return Result.success("获取成功", data);
    }

    /**
     * 从请求中获取当前用户ID
     * @param request HTTP请求
     * @return 用户ID
     */
    private Integer getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        token = token.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }
}
