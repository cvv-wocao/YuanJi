<template>
  <div class="mobile-container">
    <!-- 顶部标题栏 -->
    <header class="app-header">
      <button class="back-btn" @click="goBack">
        <span>←</span>
      </button>
      <h1>帖子详情</h1>
      <button v-if="isOwner" class="more-btn" @click="showMoreOptions = true">
        <span>⋮</span>
      </button>
    </header>

    <!-- 主内容区 -->
    <main class="app-content">
      <div v-if="loading" class="loading">加载中...</div>

      <template v-else-if="post">
        <!-- 帖子内容 -->
        <div class="post-detail">
          <!-- 帖子头部 -->
          <div class="post-header">
            <div class="post-avatar" @click="goToUserProfile(post.userId)">
              <img v-if="post.user?.avatar" :src="post.user.avatar" alt="头像" />
              <div v-else class="avatar-default">{{ post.user?.name?.charAt(0) || '?' }}</div>
            </div>
            <div class="post-info">
              <span class="post-username">{{ post.user?.name || '用户' }}</span>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>
            <button v-if="!isOwner" class="follow-btn" :class="{ following: isFollowing }" @click="toggleFollow">
              {{ isFollowing ? '已关注' : '关注' }}
            </button>
          </div>

          <!-- 帖子正文 -->
          <div class="post-content">
            <p class="post-text">{{ post.content }}</p>
            <div v-if="post.images && post.images.length > 0" class="post-images" :class="'grid-' + Math.min(post.images.length, 3)">
              <img v-for="(img, index) in post.images" :key="index" :src="img" alt="帖子图片" @click="previewImage(img)" />
            </div>
          </div>

          <!-- 帖子操作栏 -->
          <div class="post-actions-bar">
            <button class="action-btn" :class="{ active: post.isLiked }" @click="handleLike">
              <span class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
              <span>{{ post.likeCount || 0 }}</span>
            </button>
            <button class="action-btn">
              <span class="action-icon">💬</span>
              <span>{{ post.commentCount || 0 }}</span>
            </button>
            <button class="action-btn" @click="handleShare">
              <span class="action-icon">↗️</span>
              <span>分享</span>
            </button>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="comments-section">
          <div class="comments-header">
            <h3>评论 ({{ comments.length }})</h3>
            <div class="sort-tabs">
              <span :class="{ active: commentSort === 'hot' }" @click="commentSort = 'hot'">最热</span>
              <span :class="{ active: commentSort === 'new' }" @click="commentSort = 'new'">最新</span>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="comments-list">
            <div v-for="comment in sortedComments" :key="comment.id" class="comment-item">
              <div class="comment-avatar" @click="goToUserProfile(comment.userId)">
                <img v-if="comment.user?.avatar" :src="comment.user.avatar" alt="头像" />
                <div v-else class="avatar-default">{{ comment.user?.name?.charAt(0) || '?' }}</div>
              </div>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-username">{{ comment.user?.name || '用户' }}</span>
                  <span v-if="comment.userId === post.userId" class="author-tag">作者</span>
                </div>
                <p class="comment-text" @click="replyTo(comment)">{{ comment.content }}</p>
                <div class="comment-meta">
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  <span class="like-btn" :class="{ active: comment.isLiked }" @click.stop="handleLikeComment(comment)">
                    {{ comment.isLiked ? '❤️' : '🤍' }} {{ comment.likeCount || 0 }}
                  </span>
                  <span class="reply-btn" @click="replyTo(comment)">回复</span>
                  <span v-if="comment.userId === currentUserId" class="delete-btn" @click="deleteComment(comment.id)">删除</span>
                </div>

                <!-- 回复列表 -->
                <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                  <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <span class="reply-username">{{ reply.user?.name || '用户' }}</span>
                    <span class="reply-text">{{ reply.content }}</span>
                  </div>
                  <div v-if="comment.replyCount > 2" class="view-more-replies">
                    展开更多回复 ({{ comment.replyCount - 2 }})
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-comments">
            <div class="empty-icon">💬</div>
            <p>还没有评论，来说两句吧~</p>
          </div>
        </div>
      </template>

      <!-- 错误状态 -->
      <div v-else class="error-state">
        <p>帖子不存在或已删除</p>
        <button @click="goBack">返回</button>
      </div>
    </main>

    <!-- 底部评论输入框 -->
    <div class="comment-input-bar">
        <div class="input-wrapper">
        <input
          v-model="commentContent"
          :placeholder="replyToUser ? '回复 ' + replyToUser + ':' : '写评论...'"
          @keyup.enter="submitComment"
        />
        <button v-if="replyToUser" class="cancel-reply" @click="cancelReply">取消</button>
      </div>
      <button class="send-btn" :disabled="!commentContent.trim() || submitting" @click="submitComment">
        {{ submitting ? '...' : '发送' }}
      </button>
    </div>

    <!-- 更多操作弹窗 -->
    <div v-if="showMoreOptions" class="action-sheet-overlay" @click.self="showMoreOptions = false">
      <div class="action-sheet">
        <button class="action-sheet-item" @click="editPost">编辑帖子</button>
        <button class="action-sheet-item delete" @click="confirmDeletePost">删除帖子</button>
        <button class="action-sheet-item cancel" @click="showMoreOptions = false">取消</button>
      </div>
    </div>

    <!-- 图片预览 -->
    <div v-if="previewImageUrl" class="image-preview-overlay" @click.self="previewImageUrl = null">
      <img :src="previewImageUrl" alt="预览" />
      <button class="close-preview" @click="previewImageUrl = null">✕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  getPostById,
  getPostComments,
  likePost,
  unlikePost,
  likeComment,
  unlikeComment,
  createComment,
  deleteComment as deleteCommentApi,
  deletePost
} from '@/api/post';
import { followUser, unfollowUser, checkFollowing } from '@/api/follow';

const router = useRouter();
const route = useRoute();
const postId = computed(() => route.params.id);

const loading = ref(true);
const post = ref(null);
const comments = ref([]);
const commentSort = ref('hot');
const currentUserId = ref(null);

// 评论输入
const commentContent = ref('');
const replyToUser = ref('');
const replyToId = ref(null);
const submitting = ref(false);

// 更多操作
const showMoreOptions = ref(false);
const isFollowing = ref(false);

// 图片预览
const previewImageUrl = ref(null);

// 是否是帖子作者
const isOwner = computed(() => {
  return post.value && currentUserId.value && post.value.userId === currentUserId.value;
});

// 排序后的评论
const sortedComments = computed(() => {
  let list = [...comments.value];
  if (commentSort.value === 'new') {
    list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
  } else {
    // 按点赞数+回复数排序
    list.sort((a, b) => (b.likeCount + b.replyCount) - (a.likeCount + a.replyCount));
  }
  return list;
});

// 获取帖子详情
const fetchPostDetail = async () => {
  loading.value = true;
  try {
    const result = await getPostById(postId.value);
    if (result.success) {
      post.value = result.data;
      // 获取关注状态
      await checkFollowStatus();
    }
  } catch (error) {
    console.error('获取帖子详情失败:', error);
  }
};

// 获取评论列表
const fetchComments = async () => {
  try {
    const result = await getPostComments(postId.value);
    if (result.success) {
      comments.value = result.data || [];
    }
  } catch (error) {
    console.error('获取评论失败:', error);
  }
};

// 处理点赞
const handleLike = async () => {
  if (!post.value) return;
  try {
    const result = post.value.isLiked
      ? await unlikePost(post.value.id)
      : await likePost(post.value.id);

    if (result.success) {
      post.value.isLiked = !post.value.isLiked;
      post.value.likeCount = (post.value.likeCount || 0) + (post.value.isLiked ? 1 : -1);
    }
  } catch (error) {
    console.error('点赞失败:', error);
  }
};

// 提交评论
const submitComment = async () => {
  if (!commentContent.value.trim() || submitting.value) return;

  submitting.value = true;
  try {
    const result = await createComment(
      postId.value,
      commentContent.value.trim(),
      replyToId.value
    );

    if (result.success) {
      commentContent.value = '';
      replyToUser.value = '';
      replyToId.value = null;
      await fetchComments();
      // 更新评论数
      if (post.value) {
        post.value.commentCount = (post.value.commentCount || 0) + 1;
      }
    } else {
      alert(result.message || '评论失败');
    }
  } catch (error) {
    console.error('评论失败:', error);
    alert('评论失败，请重试');
  } finally {
    submitting.value = false;
  }
};

// 回复评论
const replyTo = (comment) => {
  replyToUser.value = comment.user?.name || '用户';
  replyToId.value = comment.id;
};

// 取消回复
const cancelReply = () => {
  replyToUser.value = '';
  replyToId.value = null;
};

// 删除评论
const deleteComment = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return;

  try {
    const result = await deleteCommentApi(commentId);
    if (result.success) {
      comments.value = comments.value.filter(c => c.id !== commentId);
      if (post.value) {
        post.value.commentCount = Math.max(0, (post.value.commentCount || 0) - 1);
      }
    } else {
      alert(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除评论失败:', error);
  }
};

// 评论点赞
const handleLikeComment = async (comment) => {
  try {
    const result = comment.isLiked
      ? await unlikeComment(comment.id)
      : await likeComment(comment.id);

    if (result.success) {
      comment.isLiked = !comment.isLiked;
      comment.likeCount = (comment.likeCount || 0) + (comment.isLiked ? 1 : -1);
    }
  } catch (error) {
    console.error('评论点赞失败:', error);
  }
};

// 编辑帖子
const editPost = () => {
  showMoreOptions.value = false;
  router.push(`/post/${postId.value}/edit`);
};

// 确认删除帖子
const confirmDeletePost = async () => {
  showMoreOptions.value = false;
  if (!confirm('确定要删除这条帖子吗？此操作不可恢复。')) return;

  try {
    const result = await deletePost(postId.value);
    if (result.success) {
      alert('删除成功');
      router.push('/home');
    } else {
      alert(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除帖子失败:', error);
  }
};

// 分享
const handleShare = () => {
  if (navigator.share) {
    navigator.share({
      title: post.value?.content?.substring(0, 30) + '...',
      text: post.value?.content,
      url: window.location.href
    });
  } else {
    navigator.clipboard.writeText(window.location.href).then(() => {
      alert('链接已复制到剪贴板');
    });
  }
};

// 关注/取消关注
const toggleFollow = async () => {
  if (!post.value || !post.value.userId) {
    alert('无法获取用户信息');
    return;
  }
  
  const targetUserId = post.value.userId;
  
  try {
    if (isFollowing.value) {
      // 取消关注
      const result = await unfollowUser(targetUserId);
      if (result.success) {
        isFollowing.value = false;
        alert('已取消关注');
      } else {
        alert(result.message || '取消关注失败');
      }
    } else {
      // 关注
      const result = await followUser(targetUserId);
      if (result.success) {
        isFollowing.value = true;
        alert('关注成功');
      } else {
        alert(result.message || '关注失败');
      }
    }
  } catch (error) {
    console.error('关注操作失败:', error);
    alert('操作失败，请检查网络连接');
    // 恢复原状态
    await checkFollowStatus();
  }
};

// 检查关注状态
const checkFollowStatus = async () => {
  if (!post.value || !post.value.userId) return;
  
  try {
    const result = await checkFollowing(post.value.userId);
    if (result.success) {
      isFollowing.value = result.data === true;
    }
  } catch (error) {
    console.error('检查关注状态失败:', error);
  }
};

// 预览图片
const previewImage = (url) => {
  previewImageUrl.value = url;
};

// 跳转到用户主页
const goToUserProfile = (userId) => {
  router.push(`/user/${userId}`);
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString('zh-CN');
};

onMounted(async () => {
  // 从token获取当前用户ID
  const token = localStorage.getItem('token');
  if (token) {
    try {
      // 这里可以通过解析token或调用API获取当前用户ID
      // 简化处理，存储在localStorage
      currentUserId.value = parseInt(localStorage.getItem('userId')) || null;
    } catch (e) {
      console.error('解析token失败:', e);
    }
  }

  await Promise.all([
    fetchPostDetail(),
    fetchComments()
  ]);
  loading.value = false;
});
</script>

<style scoped>
.mobile-container {
  min-height: 100vh;
  background: #f4f6f8;
  display: flex;
  flex-direction: column;
  max-width: 430px;
  margin: 0 auto;
  position: relative;
  padding-bottom: 60px;
}

/* 顶部标题栏 - 沉浸式设计 */
.app-header {
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  color: white;
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 20px rgba(26, 188, 156, 0.3);
  padding-top: calc(12px + env(safe-area-inset-top));
}

.app-header h1 {
  font-size: 16px;
  margin: 0;
  flex: 1;
  text-align: center;
  font-weight: 600;
  letter-spacing: 0.3px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.back-btn, .more-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.back-btn:active, .more-btn:active {
  transform: scale(0.9);
  background: rgba(255, 255, 255, 0.3);
}

/* 主内容区 */
.app-content {
  flex: 1;
  overflow-y: auto;
}

.loading, .error-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

/* 帖子详情 */
.post-detail {
  background: white;
  padding: 15px;
  margin-bottom: 10px;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}

.post-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  cursor: pointer;
  border: 2px solid var(--sports-teal);
}

.post-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-default {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.post-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.post-username {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.follow-btn {
  padding: 8px 20px;
  border-radius: 20px;
  border: 1.5px solid var(--sports-teal);
  background: white;
  color: var(--sports-teal);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 2px 10px rgba(26, 188, 156, 0.15);
  position: relative;
  overflow: hidden;
  min-width: 70px;
}

.follow-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(26, 188, 156, 0.1) 0%, rgba(21, 143, 132, 0.1) 100%);
  border-radius: 20px;
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.3s ease;
  z-index: 0;
}

.follow-btn:active::before {
  transform: translate(-50%, -50%) scale(1);
}

.follow-btn:active {
  transform: scale(0.95);
  box-shadow: 0 1px 5px rgba(26, 188, 156, 0.1);
}

.follow-btn.following {
  background: rgba(245, 245, 245, 0.9);
  border-color: rgba(221, 221, 221, 0.8);
  color: #999;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
}

.follow-btn.following::before {
  background: rgba(221, 221, 221, 0.1);
}

.post-content {
  margin-bottom: 15px;
}

.post-text {
  font-size: 15px;
  line-height: 1.7;
  color: #333;
  margin: 0 0 15px 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.post-images {
  display: grid;
  gap: 4px;
  border-radius: 8px;
  overflow: hidden;
}

.post-images.grid-1 {
  grid-template-columns: 1fr;
}

.post-images.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.post-images.grid-3 {
  grid-template-columns: repeat(3, 1fr);
}

.post-images img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  cursor: pointer;
}

.post-actions-bar {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid rgba(243, 244, 246, 0.8);
  background: rgba(255, 255, 255, 0.95);
  border-radius: 0 0 20px 20px;
  margin-top: 10px;
  gap: 8px;
  padding-left: 16px;
  padding-right: 16px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 -2px 20px rgba(0, 0, 0, 0.03);
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(229, 231, 235, 0.8);
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 12px 20px;
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  min-width: 80px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(255, 255, 255, 0.8) inset;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 100%);
  border-radius: 16px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 0;
}

.action-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  background: rgba(26, 188, 156, 0.08);
  border-radius: 16px;
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: -1;
}

/* 点赞按钮特殊样式 */
.action-btn:first-child::after {
  background: rgba(239, 68, 68, 0.08);
}

/* 评论按钮特殊样式 */
.action-btn:nth-child(2)::after {
  background: rgba(26, 188, 156, 0.08);
}

/* 分享按钮特殊样式 */
.action-btn:nth-child(3)::after {
  background: rgba(243, 156, 18, 0.08);
}

.action-btn:hover {
  transform: translateY(-2px);
  border-color: rgba(209, 213, 219, 0.9);
  box-shadow: 
    0 6px 16px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(255, 255, 255, 0.8) inset;
}

.action-btn:hover::before {
  opacity: 1;
}

.action-btn:active::after {
  transform: translate(-50%, -50%) scale(1);
}

.action-btn:active {
  transform: translateY(0) scale(0.97);
  border-color: rgba(209, 213, 219, 1);
  transition-duration: 0.1s;
}

.action-btn.active {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(26, 188, 156, 0.3);
  color: var(--sports-teal);
  box-shadow: 
    0 4px 12px rgba(26, 188, 156, 0.15),
    0 1px 2px rgba(255, 255, 255, 0.8) inset;
}

.action-btn.active::before {
  opacity: 1;
  background: linear-gradient(135deg, rgba(26, 188, 156, 0.1) 0%, rgba(21, 143, 132, 0.05) 100%);
}

/* 点赞按钮激活状态 */
.action-btn:first-child.active {
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
  box-shadow: 
    0 4px 12px rgba(239, 68, 68, 0.15),
    0 1px 2px rgba(255, 255, 255, 0.8) inset;
}

.action-btn:first-child.active::before {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(249, 115, 22, 0.05) 100%);
}

.action-icon {
  font-size: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.05));
}

.action-btn:hover .action-icon {
  transform: translateY(-1px) scale(1.1);
}

.action-btn:active .action-icon {
  transform: scale(0.95);
  transition-duration: 0.1s;
}

.action-btn.active .action-icon {
  transform: scale(1.1);
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.1));
}

/* 点赞图标特殊效果 */
.action-btn:first-child .action-icon {
  text-shadow: 0 1px 2px rgba(239, 68, 68, 0.2);
}

.action-btn:first-child.active .action-icon {
  animation: heartBeat 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes heartBeat {
  0%, 100% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.95); }
  75% { transform: scale(1.15); }
}

/* 评论图标特殊效果 */
.action-btn:nth-child(2) .action-icon {
  text-shadow: 0 1px 2px rgba(26, 188, 156, 0.2);
}

.action-btn:nth-child(2).active .action-icon {
  animation: bounce 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes bounce {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-4px) scale(1.1); }
}

/* 分享图标特殊效果 */
.action-btn:nth-child(3) .action-icon {
  text-shadow: 0 1px 2px rgba(243, 156, 18, 0.2);
}

.action-btn:nth-child(3).active .action-icon {
  animation: rotate 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes rotate {
  0% { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(15deg) scale(1.1); }
  100% { transform: rotate(0deg) scale(1); }
}

/* 移动端触摸优化 */
@media (hover: none) {
  .action-btn:hover {
    transform: none;
    border-color: rgba(229, 231, 235, 0.8);
    box-shadow: 
      0 2px 8px rgba(0, 0, 0, 0.04),
      0 1px 2px rgba(255, 255, 255, 0.8) inset;
  }
  
  .action-btn:hover .action-icon {
    transform: none;
  }
  
  .action-btn:active::after {
    transform: translate(-50%, -50%) scale(1);
  }
  
  .action-btn:active {
    transform: scale(0.97);
    border-color: rgba(209, 213, 219, 1);
    transition-duration: 0.1s;
  }
}

/* 评论区 */
.comments-section {
  background: white;
  min-height: 300px;
  border-radius: 20px;
  margin: 0 10px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f5f5f5;
}

.comments-header h3 {
  margin: 0;
  font-size: 15px;
  color: #333;
}

.sort-tabs {
  display: flex;
  gap: 15px;
}

.sort-tabs span {
  font-size: 13px;
  color: #999;
  cursor: pointer;
}

.sort-tabs span.active {
  color: var(--sports-teal);
  font-weight: 500;
}

.comments-list {
  padding: 15px;
}

.comment-item {
  display: flex;
  gap: 14px;
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(241, 245, 249, 0.8);
  transition: all 0.3s ease;
}

.comment-item:active {
  transform: scale(0.99);
  background: rgba(248, 249, 250, 0.5);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.02);
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  flex-shrink: 0;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 10px rgba(26, 188, 156, 0.2);
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-username {
  font-size: 14px;
  font-weight: 500;
  color: #666;
}

.author-tag {
  font-size: 10px;
  color: var(--sports-teal);
  background: #e6f7f4;
  padding: 2px 6px;
  border-radius: 4px;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin: 0 0 8px 0;
  cursor: pointer;
}

.comment-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #9ca3af;
  margin-top: 10px;
  flex-wrap: wrap;
}

.like-btn, .reply-btn, .delete-btn {
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(249, 250, 251, 0.8);
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  font-size: 12px;
  font-weight: 500;
}

.like-btn:active, .reply-btn:active, .delete-btn:active {
  transform: scale(0.95);
  background: rgba(26, 188, 156, 0.1);
}

.like-btn.active {
  color: var(--sports-teal);
  background: rgba(26, 188, 156, 0.1);
}

.like-btn.active:active {
  background: rgba(26, 188, 156, 0.2);
}

.reply-btn {
  color: var(--sports-teal);
}

.delete-btn {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

.delete-btn:active {
  background: rgba(245, 108, 108, 0.2);
}

/* 移动端触摸优化 */
@media (hover: none) {
  .like-btn:hover, .reply-btn:hover, .delete-btn:hover {
    background: rgba(249, 250, 251, 0.8);
  }
  
  .like-btn:active, .reply-btn:active, .delete-btn:active {
    transform: scale(0.95);
    background: rgba(26, 188, 156, 0.1);
  }
}

.replies-list {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-top: 8px;
}

.reply-item {
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 6px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-username {
  color: var(--sports-teal);
  font-weight: 500;
  margin-right: 5px;
}

.reply-text {
  color: #333;
}

.view-more-replies {
  font-size: 12px;
  color: var(--sports-teal);
  cursor: pointer;
  margin-top: 5px;
}

.empty-comments {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 50px;
  margin-bottom: 10px;
}

/* 底部评论输入 - 现代设计 */
.comment-input-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-top: 1px solid rgba(224, 224, 224, 0.6);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  z-index: 1000;
  box-shadow: 0 -2px 30px rgba(0, 0, 0, 0.08);
}

.input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(248, 249, 250, 0.9);
  border-radius: 22px;
  padding: 0 16px;
  border: 1px solid rgba(224, 224, 224, 0.5);
  transition: all 0.3s ease;
}

.input-wrapper:focus-within {
  background: white;
  border-color: var(--sports-teal);
  box-shadow: 0 0 0 3px rgba(26, 188, 156, 0.15);
}

.input-wrapper input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 12px 0;
  font-size: 15px;
  outline: none;
  color: #333;
}

.input-wrapper input::placeholder {
  color: #9ca3af;
}

.cancel-reply {
  background: none;
  border: none;
  color: var(--sports-teal);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.cancel-reply:active {
  background: rgba(26, 188, 156, 0.1);
  transform: scale(0.95);
}

.send-btn {
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  border: none;
  border-radius: 18px;
  padding: 10px 20px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(26, 188, 156, 0.4);
  min-width: 60px;
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
  box-shadow: 0 2px 10px rgba(26, 188, 156, 0.3);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 操作菜单 - iOS风格 */
.action-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.action-sheet {
  width: 100%;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 24px 24px 0 0;
  padding: 12px 0;
  animation: slideUp 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 -20px 60px rgba(0, 0, 0, 0.15);
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.action-sheet-item {
  width: 100%;
  padding: 18px 24px;
  border: none;
  background: none;
  font-size: 17px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.action-sheet-item::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.05);
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.3s ease;
  border-radius: 0;
}

.action-sheet-item:active::before {
  transform: translate(-50%, -50%) scale(1);
}

.action-sheet-item.delete {
  color: #ff3b30;
  font-weight: 600;
}

.action-sheet-item.cancel {
  margin-top: 8px;
  color: var(--sports-teal);
  font-weight: 600;
  background: rgba(255, 255, 255, 0.9);
  border-top: 1px solid rgba(224, 224, 224, 0.6);
  border-radius: 0 0 24px 24px;
  padding-top: 20px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
}

.action-sheet-item.cancel::before {
  display: none;
}

/* 图片预览 */
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-overlay img {
  max-width: 100%;
  max-height: 100%;
}

.close-preview {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 18px;
  cursor: pointer;
}
</style>
