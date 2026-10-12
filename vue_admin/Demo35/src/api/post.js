import { get, post, put, del } from '@/util/request.js';

/**
 * 创建帖子
 * @param {FormData} formData - 包含content和images的表单数据
 */
export function createPost(formData) {
  return post('/api/post/create', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/**
 * 更新帖子
 * @param {number} postId - 帖子ID
 * @param {FormData} formData - 包含content和images的表单数据
 */
export function updatePost(postId, formData) {
  return put(`/api/post/${postId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/**
 * 删除帖子
 * @param {number} postId - 帖子ID
 */
export function deletePost(postId) {
  return del(`/api/post/${postId}`);
}

/**
 * 获取帖子详情
 * @param {number} postId - 帖子ID
 */
export function getPostById(postId) {
  return get(`/api/post/${postId}`);
}

/**
 * 获取帖子列表（分页）
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 */
export function getPostList(page = 1, size = 10) {
  return get('/api/post/list', { page, size });
}

/**
 * 获取指定用户的帖子
 * @param {number} userId - 用户ID
 */
export function getUserPosts(userId) {
  return get(`/api/post/user/${userId}`);
}

/**
 * 点赞帖子
 * @param {number} postId - 帖子ID
 */
export function likePost(postId) {
  return post(`/api/post/${postId}/like`);
}

/**
 * 取消点赞
 * @param {number} postId - 帖子ID
 */
export function unlikePost(postId) {
  return del(`/api/post/${postId}/like`);
}

/**
 * 获取帖子的点赞列表
 * @param {number} postId - 帖子ID
 */
export function getPostLikes(postId) {
  return get(`/api/post/${postId}/likes`);
}

/**
 * 发表评论
 * @param {number} postId - 帖子ID
 * @param {string} content - 评论内容
 * @param {number} parentId - 父评论ID（回复时使用）
 */
export function createComment(postId, content, parentId = null) {
  const params = new URLSearchParams();
  params.append('content', content);
  if (parentId) {
    params.append('parentId', parentId);
  }
  return post(`/api/post/${postId}/comment?${params.toString()}`);
}

/**
 * 更新评论
 * @param {number} commentId - 评论ID
 * @param {string} content - 新内容
 */
export function updateComment(commentId, content) {
  const params = new URLSearchParams();
  params.append('content', content);
  return put(`/api/post/comment/${commentId}?${params.toString()}`);
}

/**
 * 删除评论
 * @param {number} commentId - 评论ID
 */
export function deleteComment(commentId) {
  return del(`/api/post/comment/${commentId}`);
}

/**
 * 获取帖子的所有评论
 * @param {number} postId - 帖子ID
 */
export function getPostComments(postId) {
  return get(`/api/post/${postId}/comments`);
}

/**
 * 获取帖子的根评论（不包括回复）
 * @param {number} postId - 帖子ID
 */
export function getRootComments(postId) {
  return get(`/api/post/${postId}/comments/root`);
}

/**
 * 获取某个评论的回复
 * @param {number} parentId - 父评论ID
 */
export function getReplies(parentId) {
  return get(`/api/post/comment/${parentId}/replies`);
}

/**
 * 获取用户的评论
 * @param {number} userId - 用户ID
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 */
export function getUserComments(userId, page = 1, size = 10) {
  return get(`/api/post/user/${userId}/comments`, { page, size });
}

/**
 * 获取帖子统计信息
 */
export function getPostStatistics() {
  return get('/api/post/statistics');
}

/**
 * 获取当前用户的帖子和点赞统计
 */
export function getMyPosts() {
  return get('/api/post/my-posts');
}

/**
 * 点赞评论
 * @param {number} commentId - 评论ID
 */
export function likeComment(commentId) {
  return post(`/api/post/comment/${commentId}/like`);
}

/**
 * 取消点赞评论
 * @param {number} commentId - 评论ID
 */
export function unlikeComment(commentId) {
  return del(`/api/post/comment/${commentId}/like`);
}
