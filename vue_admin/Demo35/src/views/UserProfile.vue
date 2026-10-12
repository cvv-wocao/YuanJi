<template>
  <div class="user-profile-page">
    <!-- 顶部导航 -->
    <header class="profile-header">
      <div class="header-top">
        <button class="back-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="header-title">个人主页</span>
        <div class="placeholder"></div>
      </div>
    </header>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 主内容区 -->
    <main v-else class="profile-content">
      <!-- 用户信息区 -->
      <div class="user-section">
        <div class="user-main">
          <div class="avatar-large">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="头像" />
            <div v-else class="avatar-default-large">{{ userInfo.name?.charAt(0) || '?' }}</div>
          </div>
          <div class="user-actions">
            <button 
              class="follow-action-btn"
              :class="{ following: isFollowing, loading: followLoading }"
              @click="handleFollow"
              :disabled="followLoading"
              id="follow-btn"
            >
              {{ followLoading ? '加载中...' : (isFollowing ? '已关注' : '+ 关注') }}
            </button>
            <button class="message-btn" @click="handleMessage">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              私信
            </button>
          </div>
        </div>

        <!-- 昵称和简介 -->
        <div class="user-info">
          <h1 class="nickname">{{ userInfo.name || '用户' }}</h1>
          <p class="user-id">电话号：{{ userInfo.number || '-' }}</p>
          <p class="bio">{{ userInfo.email ? '邮箱：' + userInfo.email : '该用户还没有填写简介~' }}</p>
        </div>

        <!-- 统计区域 -->
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-num">{{ userPosts.length || 0 }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-item clickable" @click="goToFollowing">
            <span class="stat-num">{{ followStats.followingCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-item clickable" @click="goToFollowers">
            <span class="stat-num">{{ followStats.followersCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ totalLikes }}</span>
            <span class="stat-label">获赞</span>
          </div>
        </div>
      </div>

      <!-- 标签切换 -->
      <div class="tabs-section">
        <div class="tab-list">
          <div class="tab-item active">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
            <span>帖子</span>
          </div>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div class="posts-grid">
        <div v-if="userPosts.length === 0" class="empty-posts">
          <div class="empty-icon">📝</div>
          <p>该用户还没有发布帖子</p>
        </div>
        <div v-else class="posts-list">
          <div 
            v-for="post in userPosts" 
            :key="post.id" 
            class="post-item"
            @click="goToPostDetail(post.id)"
          >
            <button 
              v-if="currentUserId && currentUserId === userId" 
              class="delete-post-btn" 
              @click.stop="handleDeletePost(post.id)"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
              </svg>
            </button>
            <div class="post-cover">
              <img v-if="post.images && post.images.length > 0" :src="post.images[0]" alt="帖子图片" />
              <div v-else class="no-cover">📝</div>
              <div v-if="post.images && post.images.length > 1" class="image-count">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="18" rx="2"/>
                  <rect x="7" y="7" width="10" height="10" fill="white"/>
                </svg>
              </div>
            </div>
            <div class="post-brief">
              <p class="post-title">{{ post.content?.substring(0, 40) || '无标题' }}{{ post.content?.length > 40 ? '...' : '' }}</p>
              <div class="post-meta">
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
                <div class="post-likes">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                  <span>{{ post.likeCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getUserInfo } from '@/api/user';
import { getUserById as getUserByIdAdmin } from '@/api/admin';
import { getUserFollowStats, checkFollowing, followUser, unfollowUser } from '@/api/follow';

const router = useRouter();
const route = useRoute();

const userId = ref(null);
const currentUserId = ref(localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null);
const userInfo = ref({
  name: '',
  number: '',
  email: '',
  avatar: ''
});
const userPosts = ref([]);
const totalLikes = ref(0);
const followStats = ref({
  followingCount: 0,
  followersCount: 0
});
const isFollowing = ref(false);
const loading = ref(true);
const followLoading = ref(false);

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    // 使用管理员API获取指定用户信息
    const result = await getUserByIdAdmin(userId.value);
    if (result.success && result.data) {
      userInfo.value = result.data;
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};

// 获取关注统计
const fetchFollowStats = async () => {
  try {
    const result = await getUserFollowStats(userId.value);
    if (result.success && result.data) {
      // 确保数据结构正确，后端返回的是 followerCount（单数）和 followingCount
      followStats.value = {
        followingCount: result.data.followingCount || result.data.following || 0,
        followersCount: result.data.followerCount || result.data.followersCount || result.data.followers || 0
      };
    } else {
      // 如果API返回失败，重置为0
      followStats.value = { followingCount: 0, followersCount: 0 };
    }
  } catch (error) {
    console.error('获取关注统计失败:', error);
    // 发生错误时重置为0
    followStats.value = { followingCount: 0, followersCount: 0 };
  }
};

// 检查是否已关注
const checkFollowStatus = async () => {
  try {
    const result = await checkFollowing(userId.value);
    if (result.success) {
      isFollowing.value = result.data === true;
    }
  } catch (error) {
    console.error('检查关注状态失败:', error);
  }
};

// 获取用户帖子
const fetchUserPosts = async () => {
  try {
    const { getUserPosts } = await import('@/api/post');
    const result = await getUserPosts(userId.value);
    console.log('获取用户帖子结果:', result);
    if (result.success && result.data) {
      let posts = [];
      let stats = {};
      // 处理不同的数据结构
      if (Array.isArray(result.data)) {
        // 如果 data 是数组，直接作为帖子列表
        posts = result.data;
      } else if (result.data.posts) {
        // 如果 data 是对象，包含 posts 和 stats
        posts = result.data.posts || [];
        stats = result.data.stats || {};
      } else {
        // 其他情况，尝试作为帖子列表
        posts = result.data;
      }
      userPosts.value = posts;
      console.log('帖子列表:', posts);
      console.log('统计信息:', stats);
      
      // 计算总点赞数：优先从 stats 中获取，否则从帖子累加
      let likes = 0;
      if (stats && (stats.totalLikes !== undefined || stats.likesCount !== undefined || stats.likeCount !== undefined || stats.likes !== undefined)) {
        // 尝试不同的字段名
        likes = stats.totalLikes || stats.likesCount || stats.likeCount || stats.likes || 0;
      } else {
        // 从帖子列表中累加
        likes = posts.reduce((sum, post) => sum + (post.likeCount || post.likes || 0), 0);
      }
      totalLikes.value = likes;
      console.log('总点赞数:', totalLikes.value);
    } else {
      console.warn('获取用户帖子失败:', result.message);
    }
  } catch (error) {
    console.error('获取用户帖子失败:', error);
  }
};

// 处理关注/取消关注
const handleFollow = async () => {
  console.log('========== handleFollow 开始 ==========');
  console.log('userId:', userId.value);
  
  if (!userId.value || isNaN(userId.value)) {
    alert('用户ID无效，无法关注');
    return;
  }
  
  followLoading.value = true;
  
  try {
    const targetId = Number(userId.value);
    console.log('准备发送请求，目标ID:', targetId);
    
    if (isFollowing.value) {
      // 取消关注
      console.log('调用 unfollowUser API...');
      const result = await unfollowUser(targetId);
      console.log('取消关注返回结果:', JSON.stringify(result));
      
      if (result && result.success) {
        isFollowing.value = false;
        followStats.value.followersCount = Math.max(0, followStats.value.followersCount - 1);
        alert('取消关注成功');
      } else {
        alert(result?.message || '取消关注失败');
      }
    } else {
      // 关注
      console.log('调用 followUser API，参数:', targetId);
      console.log('请求URL应该是:', `/api/follow/${targetId}`);
      
      const result = await followUser(targetId);
      console.log('关注返回结果:', JSON.stringify(result));
      
      if (result && result.success) {
        isFollowing.value = true;
        followStats.value.followersCount++;
        alert('关注成功');
      } else {
        alert(result?.message || '关注失败');
        // 如果失败，恢复状态
        isFollowing.value = false;
      }
    }
  } catch (error) {
    console.error('========== 请求异常 ==========');
    console.error('错误对象:', error);
    console.error('错误消息:', error.message);
    console.error('错误响应:', error.response);
    alert('网络请求失败: ' + (error.message || '请检查网络连接或后端服务'));
    // 发生错误时恢复状态
    await checkFollowStatus();
  } finally {
    followLoading.value = false;
    console.log('========== handleFollow 结束 ==========');
  }
};

// 处理私信
const handleMessage = () => {
  alert('私信功能开发中...');
};

// 跳转到TA的关注列表
const goToFollowing = () => {
  router.push({
    path: '/following',
    query: { type: 'following', userId: userId.value }
  });
};

// 跳转到TA的粉丝列表
const goToFollowers = () => {
  router.push({
    path: '/followers',
    query: { type: 'followers', userId: userId.value }
  });
};

// 跳转到帖子详情
const goToPostDetail = (postId) => {
  router.push(`/post/${postId}`);
};

// 处理删除帖子
const handleDeletePost = async (postId) => {
  if (!confirm('确定要删除这个帖子吗？')) {
    return;
  }
  
  try {
    const { deletePost } = await import('@/api/post');
    const result = await deletePost(postId);
    
    if (result.success) {
      // 从列表中移除已删除的帖子
      userPosts.value = userPosts.value.filter(post => post.id !== postId);
      alert('帖子删除成功');
    } else {
      alert(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除帖子失败:', error);
    alert('删除失败，请稍后重试');
  }
};

// 返回
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
  console.log('===== UserProfile 页面加载 =====');
  console.log('route.params:', route.params);
  console.log('route.params.id:', route.params.id);
  
  userId.value = parseInt(route.params.id);
  console.log('解析后的 userId:', userId.value, '类型:', typeof userId.value);
  
  if (!userId.value || isNaN(userId.value)) {
    console.error('userId 无效，跳转首页');
    alert('页面参数错误，即将返回首页');
    router.push('/home');
    return;
  }
  
  loading.value = true;
  console.log('开始加载数据...');
  await Promise.all([
    fetchUserInfo(),
    fetchFollowStats(),
    checkFollowStatus(),
    fetchUserPosts()
  ]);
  loading.value = false;
  console.log('页面加载完成，按钮应该可点击了');
});
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background: #f4f6f8;
  display: flex;
  flex-direction: column;
  max-width: 768px;
  margin: 0 auto;
}

/* 顶部导航 - 渐变背景 */
.profile-header {
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  padding: 12px 20px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(26, 188, 156, 0.3);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 44px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  letter-spacing: 1px;
}

.back-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  border-radius: 50%;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: scale(1.05);
}

.back-btn:active {
  transform: scale(0.95);
}

.placeholder {
  width: 40px;
}

/* 加载中 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  background: #f4f6f8;
  flex: 1;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid var(--sports-teal);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 主内容区 */
.profile-content {
  flex: 1;
  overflow-y: auto;
}

/* 用户信息区 */
.user-section {
  padding: 24px;
  background: #fff;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 12px;
}

.user-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--sports-teal);
  flex-shrink: 0;
  box-shadow: 0 4px 15px rgba(26, 188, 156, 0.3);
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-default-large {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  color: white;
  font-size: 32px;
  font-weight: bold;
}

.user-actions {
  display: flex;
  gap: 12px;
}

.follow-action-btn {
  padding: 8px 24px;
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  border: none;
  border-radius: 20px;
  font-size: 14px;
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 15px rgba(26, 188, 156, 0.35);
}

.follow-action-btn.following {
  background: #f5f5f5;
  color: #666;
  box-shadow: none;
}

.follow-action-btn:hover:not(.following):not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(26, 188, 156, 0.45);
}

.follow-action-btn.loading {
  opacity: 0.7;
}

.follow-action-btn:disabled {
  cursor: not-allowed;
}

.message-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.message-btn:hover {
  background: #eee;
}

/* 用户信息 */
.user-info {
  margin-bottom: 20px;
}

.nickname {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px 0;
}

.user-id {
  font-size: 12px;
  color: #999;
  margin: 0 0 8px 0;
}

.bio {
  font-size: 13px;
  color: #666;
  margin: 0;
  line-height: 1.5;
}

/* 统计区域 */
.stats-row {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  border-top: 1px solid #f5f5f5;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-item.clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.stat-item.clickable:hover {
  opacity: 0.7;
}

.stat-num {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

/* 标签切换 */
.tabs-section {
  background: #fff;
  border-radius: 16px;
  margin: 0 12px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.tab-list {
  display: flex;
  justify-content: center;
  padding: 0 16px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 24px;
  font-size: 14px;
  color: #999;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item.active {
  color: var(--sports-teal);
  border-bottom-color: var(--sports-teal);
  font-weight: 600;
}

/* 帖子网格 */
.posts-grid {
  padding: 0 12px 16px;
  min-height: 300px;
}

.empty-posts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.empty-posts p {
  color: #999;
  font-size: 15px;
  margin: 0;
}

/* 帖子列表 */
.posts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-item {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  position: relative;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}

.post-item:active {
  transform: scale(0.99);
}

.post-cover {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: linear-gradient(135deg, #f5f5f5 0%, #e0e0e0 100%);
}

.image-count {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(0,0,0,0.5);
  color: white;
  padding: 3px;
  border-radius: 4px;
}

.post-brief {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.post-title {
  font-size: 15px;
  color: #333;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ef4444;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.post-likes svg {
  color: #ef4444;
  filter: drop-shadow(0 1px 1px rgba(239, 68, 68, 0.3));
  transition: transform 0.3s ease;
}

.post-likes:hover {
  background: rgba(239, 68, 68, 0.12);
  transform: translateY(-1px);
}

.post-likes:hover svg {
  transform: scale(1.1);
}

/* 删除帖子按钮 */
.delete-post-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(244, 67, 54, 0.9);
  border: none;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(244, 67, 54, 0.3);
}

.delete-post-btn:hover {
  background: rgba(211, 47, 47, 1);
  transform: scale(1.1);
}

.delete-post-btn svg {
  width: 14px;
  height: 14px;
}
</style>
