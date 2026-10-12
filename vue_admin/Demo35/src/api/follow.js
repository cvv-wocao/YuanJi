import { get, post, del, request } from '@/util/request.js';

/**
 * 关注用户
 * @param {number} followingId - 要关注的用户ID
 */
export function followUser(followingId) {
  // 确保是数字类型
  const id = Number(followingId);
  const url = `/api/follow/${id}`;
  console.log('===== followUser API =====');
  console.log('URL:', url);
  console.log('ID:', id);
  
  // 使用原始 axios 请求以便看到完整响应
  return request.post(url).then(response => {
    console.log('关注请求成功，响应:', response);
    return response;
  }).catch(error => {
    console.error('关注请求失败:', error.message);
    console.error('错误详情:', error.response?.data);
    throw error;
  });
}

/**
 * 取消关注
 * @param {number} followingId - 要取消关注的用户ID
 */
export function unfollowUser(followingId) {
  return del(`/api/follow/${followingId}`);
}

/**
 * 检查是否关注
 * @param {number} followingId - 要检查的用户ID
 */
export function checkFollowing(followingId) {
  return get(`/api/follow/check/${followingId}`);
}

/**
 * 获取我的粉丝列表（关注我的人）
 */
export function getMyFollowers() {
  return get('/api/follow/followers');
}

/**
 * 获取我的关注列表（我关注的人）
 */
export function getMyFollowing() {
  return get('/api/follow/following');
}

/**
 * 获取我的关注统计信息
 */
export function getMyFollowStats() {
  return get('/api/follow/stats');
}

/**
 * 获取互相关注的用户列表
 */
export function getMutualFollows() {
  return get('/api/follow/mutual');
}

/**
 * 获取指定用户的粉丝列表
 * @param {number} userId - 用户ID
 */
export function getUserFollowers(userId) {
  return get(`/api/follow/${userId}/followers`);
}

/**
 * 获取指定用户的关注列表
 * @param {number} userId - 用户ID
 */
export function getUserFollowing(userId) {
  return get(`/api/follow/${userId}/following`);
}

/**
 * 获取指定用户的关注统计
 * @param {number} userId - 用户ID
 */
export function getUserFollowStats(userId) {
  return get(`/api/follow/${userId}/stats`);
}
