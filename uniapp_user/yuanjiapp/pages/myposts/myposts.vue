<template>
  <view class="mobile-container">
    <!-- 顶部标题栏 -->
    <view class="app-header">
      <button class="back-btn" @click="goBack">
        <text>←</text>
      </button>
      <text class="header-title">我的帖子</text>
      <view class="header-spacer"></view>
    </view>

    <!-- 主内容区 -->
    <view class="app-content">
      <!-- 统计卡片 -->
      <view class="stats-card">
        <view class="stat-item">
          <text class="stat-num">{{ stats.postCount || 0 }}</text>
          <text class="stat-label">帖子</text>
        </view>
        <view class="stat-item">
          <text class="stat-num">{{ stats.likeCount || 0 }}</text>
          <text class="stat-label">获赞</text>
        </view>
        <view class="stat-item">
          <text class="stat-num">{{ stats.commentCount || 0 }}</text>
          <text class="stat-label">评论</text>
        </view>
      </view>

      <!-- 帖子列表 -->
      <view v-if="posts.length > 0" class="post-list">
        <view v-for="post in posts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
          <view class="post-content-preview">
            <text class="post-text">{{ post.content }}</text>
            <view v-if="post.images && post.images.length > 0" class="post-image">
              <image :src="post.images[0]" mode="aspectFill" />
              <text v-if="post.images.length > 1" class="image-count">+{{ post.images.length - 1 }}</text>
            </view>
          </view>
          <view class="post-meta">
            <text class="post-time">{{ formatTime(post.createTime) }}</text>
            <view class="post-stats">
              <text>❤️ {{ post.likeCount || 0 }}</text>
              <text> {{ post.commentCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state">
        <view class="empty-icon"></view>
        <text>你还没有发布过帖子</text>
        <button class="publish-btn" @click="goToHome">去发布</button>
      </view>

      <!-- 加载更多 -->
      <view v-if="posts.length > 0 && hasMore" class="load-more">
        <button @click="loadMorePosts" :disabled="loading">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { getUserPosts } from '@/api/post.js';

export default {
  data() {
    return {
      posts: [],
      page: 1,
      size: 10,
      hasMore: true,
      loading: false,
      stats: {
        postCount: 0,
        likeCount: 0,
        commentCount: 0
      }
    };
  },
  
  onLoad() {
    this.fetchUserPosts();
  },
  
  methods: {
    async fetchUserPosts(isLoadMore = false) {
      if (this.loading) return;
      this.loading = true;

      try {
        const userId = uni.getStorageSync('userId');
        const userName = uni.getStorageSync('userName') || uni.getStorageSync('userNumber') || '用户';

        if (!userId) {
          uni.showToast({ title: '请先登录', icon: 'none' });
          return;
        }

        const result = await getUserPosts(userId);
        if (result.success) {
          const newPosts = result.data || [];
          if (isLoadMore) {
            this.posts.push(...newPosts);
          } else {
            this.posts = newPosts;
          }
          this.hasMore = newPosts.length === this.size;
          this.calculateStats();
        }
      } catch (error) {
        console.error('获取用户帖子失败:', error);
        uni.showToast({ title: '获取失败', icon: 'none' });
      } finally {
        this.loading = false;
      }
    },
    
    calculateStats() {
      this.stats.postCount = this.posts.length;
      this.stats.likeCount = this.posts.reduce((sum, post) => sum + (post.likeCount || 0), 0);
      this.stats.commentCount = this.posts.reduce((sum, post) => sum + (post.commentCount || 0), 0);
    },
    
    loadMorePosts() {
      this.page++;
      this.fetchUserPosts(true);
    },
    
    goToPostDetail(postId) {
      uni.navigateTo({
        url: `/pages/post/post?id=${postId}`
      });
    },
    
    goToHome() {
      uni.switchTab({
        url: '/pages/home/home'
      });
    },
    
    goBack() {
      uni.navigateBack();
    },
    
    formatTime(timeStr) {
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
    }
  }
};
</script>

<style scoped>
.mobile-container {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  max-width: 430px;
  margin: 0 auto;
}

/* 顶部标题栏 */
.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.app-header .header-title {
  font-size: 18px;
  margin: 0;
  flex: 1;
  text-align: center;
}

.back-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  cursor: pointer;
}

.header-spacer {
  width: 36px;
}

/* 主内容区 */
.app-content {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

/* 统计卡片 */
.stats-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  justify-content: space-around;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.stat-num {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

/* 帖子列表 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.post-card {
  background: white;
  border-radius: 16px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}

.post-content-preview {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.post-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.post-image image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-count {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.post-stats {
  display: flex;
  gap: 20px;
  font-size: 13px;
  font-weight: 600;
}

.post-stats text {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 12px;
  transition: all 0.3s;
  background: rgba(249, 250, 251, 0.8);
  border: 1px solid rgba(229, 231, 235, 0.8);
}

.post-stats text:first-child {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.2);
}

.post-stats text:nth-child(2) {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.2);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.empty-state text {
  margin-bottom: 20px;
}

.publish-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 20px;
  padding: 12px 30px;
  color: white;
  font-size: 15px;
  cursor: pointer;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 20px;
}

.load-more button {
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 10px 30px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
}

.load-more button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>