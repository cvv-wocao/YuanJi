import { get, post, del } from '@/utils/request.js';

// 发送消息 - 使用 URL 查询参数
export function sendMessage(receiverId, content) {
  return post(`/api/message/send?receiverId=${receiverId}&content=${encodeURIComponent(content)}`);
}

// 获取会话列表
export function getConversations() {
  return get('/api/message/conversations');
}

// 获取与特定用户的消息历史
export function getMessages(userId) {
  return get(`/api/message/history/${userId}`);
}

// 检查是否可以发送消息
export function checkCanSendMessage(receiverId) {
  return get(`/api/message/can-send/${receiverId}`);
}

// 标记消息为已读
export function markMessageAsRead(messageId) {
  return post(`/api/message/read/${messageId}`);
}

// 标记与特定用户的所有消息为已读
export function markAllMessagesAsRead(senderId) {
  return post(`/api/message/read/all/${senderId}`);
}

// 获取未读消息数量
export function getUnreadCount() {
  return get('/api/message/unread/count');
}

// 删除消息
export function deleteMessage(messageId) {
  return del(`/api/message/delete/${messageId}`);
}
