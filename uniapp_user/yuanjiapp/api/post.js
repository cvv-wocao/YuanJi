import { get, post, put, del } from '@/utils/request.js';

/**
 * 上传单个文件（图片或视频）
 * @param {string} filePath - 文件路径
 * @param {string} fileType - 文件类型（image/video）
 * @returns {Promise} 返回文件URL
 */
export function uploadFile(filePath, fileType = 'image') {
  const token = uni.getStorageSync('token');

  return new Promise((resolve, reject) => {
    const header = {}
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }
    uni.uploadFile({
      url: 'http://localhost:8080/post/upload/file',
      filePath: filePath,
      name: 'file',
      formData: {
        type: fileType
      },
      header: header,
      success: (res) => {
        console.log('文件上传响应:', res);
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data);
            if (data.success) {
              console.log('文件上传成功，URL:', data.data.url);
              resolve(data.data.url);
            } else {
              reject(new Error(data.message || '上传失败'));
            }
          } catch (e) {
            console.error('解析响应失败:', e);
            reject(new Error('解析响应失败'));
          }
        } else {
          reject(new Error(`上传失败: ${res.statusCode}`));
        }
      },
      fail: (err) => {
        console.error('上传失败:', err);
        reject(new Error(`上传失败: ${err.errMsg || '未知错误'}`));
      }
    });
  });
}

/**
 * 创建帖子
 * @param {Object} data - 包含content、images和videos的数据
 */
export function createPost(data) {
  return post('/post/create', data);
}

/**
 * 更新帖子
 * @param {number} postId - 帖子ID
 * @param {Object} formData - 包含content和images的表单数据
 */
export function updatePost(postId, formData) {
  return put(`/post/${postId}`, formData, {
    header: { 'Content-Type': 'multipart/form-data' }
  });
}

/**
 * 删除帖子
 * @param {number} postId - 帖子ID
 */
export function deletePost(postId) {
  return del(`/post/${postId}`);
}

/**
 * 获取帖子详情
 * @param {number} postId - 帖子ID
 */
export function getPostById(postId) {
  return get(`/post/${postId}`);
}

/**
 * 获取帖子列表（分页）
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 */
export function getPostList(page = 1, size = 10) {
  return get('/post/list', { page, size });
}

/**
 * 获取指定用户的帖子
 * @param {number} userId - 用户ID
 */
export function getUserPosts(userId) {
  return get(`/post/user/${userId}`);
}

/**
 * 点赞帖子
 * @param {number} postId - 帖子ID
 */
export function likePost(postId) {
  return post(`/post/${postId}/like`);
}

/**
 * 取消点赞
 * @param {number} postId - 帖子ID
 */
export function unlikePost(postId) {
  return del(`/post/${postId}/like`);
}

/**
 * 获取帖子的点赞列表
 * @param {number} postId - 帖子ID
 */
export function getPostLikes(postId) {
  return get(`/post/${postId}/likes`);
}

/**
 * 发表评论
 * @param {number} postId - 帖子ID
 * @param {string} content - 评论内容
 * @param {number} parentId - 父评论ID（回复时使用）
 */
export function createComment(postId, content, parentId = null) {
  const params = [];
  params.push(`content=${encodeURIComponent(content)}`);
  if (parentId) {
    params.push(`parentId=${parentId}`);
  }
  return post(`/post/${postId}/comment?${params.join('&')}`);
}

/**
 * 更新评论
 * @param {number} commentId - 评论ID
 * @param {string} content - 新内容
 */
export function updateComment(commentId, content) {
  const params = [];
  params.push(`content=${encodeURIComponent(content)}`);
  return put(`/post/comment/${commentId}?${params.join('&')}`);
}

/**
 * 删除评论
 * @param {number} commentId - 评论ID
 */
export function deleteComment(commentId) {
  return del(`/post/comment/${commentId}`);
}

/**
 * 获取帖子的所有评论
 * @param {number} postId - 帖子ID
 */
export function getPostComments(postId) {
  return get(`/post/${postId}/comments`);
}

/**
 * 获取帖子的根评论（不包括回复）
 * @param {number} postId - 帖子ID
 */
export function getRootComments(postId) {
  return get(`/post/${postId}/comments/root`);
}

/**
 * 获取某个评论的回复
 * @param {number} parentId - 父评论ID
 */
export function getReplies(parentId) {
  return get(`/post/comment/${parentId}/replies`);
}

/**
 * 获取用户的评论
 * @param {number} userId - 用户ID
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 */
export function getUserComments(userId, page = 1, size = 10) {
  return get(`/post/user/${userId}/comments`, { page, size });
}

/**
 * 获取帖子统计信息
 */
export function getPostStatistics() {
  return get('/post/statistics');
}

/**
 * 获取当前用户的帖子和点赞统计
 */
export function getMyPosts() {
  return get('/post/my-posts');
}

/**
 * 获取关注用户的帖子
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 */
export function getFollowingPosts(page = 1, size = 10) {
  return get('/post/following', { page, size });
}

/**
 * 点赞评论
 * @param {number} commentId - 评论ID
 */
export function likeComment(commentId) {
  return post(`/post/comment/${commentId}/like`);
}

/**
 * 取消点赞评论
 * @param {number} commentId - 评论ID
 */
export function unlikeComment(commentId) {
  return del(`/post/comment/${commentId}/like`);
}


/**
 * 获取推荐的帖子
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 */
export function getRecommendedPosts(page = 1, size = 10) {
  return get('/post/recommend', { page, size });
}