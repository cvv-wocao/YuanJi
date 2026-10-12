<template>
  <view class="user-profile-page">
    <!-- 顶部导航 -->
    <view class="profile-header">
      <view class="header-top">
        <button class="back-btn" @click="goBack">
          <text>←</text>
        </button>
        <text class="header-title">个人主页</text>
        <view class="placeholder"></view>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-state">
      <view class="loading-spinner"></view>
      <text>加载中...</text>
    </view>

    <!-- 主内容区 -->
    <view v-else class="profile-content">
      <!-- 用户信息区 -->
      <view class="user-section">
        <view class="user-main">
          <view class="avatar-large">
            <image v-if="userInfo.avatar" :src="userInfo.avatar" mode="aspectFill" />
            <view v-else class="avatar-default-large">{{ userInfo.name?.charAt(0) || '?' }}</view>
          </view>
          <view class="user-actions">
            <button 
              class="follow-action-btn"
              :class="{ following: isFollowing, loading: followLoading }"
              @click="handleFollow"
              :disabled="followLoading"
            >
              {{ followLoading ? '加载中...' : (isFollowing ? '已关注' : '+ 关注') }}
            </button>
            <button class="message-btn" @click="handleMessage">
              <text></text>
              <text>私信</text>
            </button>
          </view>
        </view>

        <!-- 昵称和简介 -->
        <view class="user-info">
          <text class="nickname">{{ userInfo.name || '用户' }}</text>
          <text class="user-id">电话号：{{ userInfo.number || '-' }}</text>
          <text class="bio">{{ userInfo.email ? '邮箱：' + userInfo.email : '该用户还没有填写简介~' }}</text>
        </view>

        <!-- 统计区域 -->
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-num">{{ userPosts.length || 0 }}</text>
            <text class="stat-label">帖子</text>
          </view>
          <view class="stat-item clickable" @click="goToFollowing">
            <text class="stat-num">{{ followStats.followingCount || 0 }}</text>
            <text class="stat-label">关注</text>
          </view>
          <view class="stat-item clickable" @click="goToFollowers">
            <text class="stat-num">{{ followStats.followersCount || 0 }}</text>
            <text class="stat-label">粉丝</text>
          </view>
          <view class="stat-item">
            <text class="stat-num">{{ totalLikes }}</text>
            <text class="stat-label">获赞</text>
          </view>
        </view>
      </view>

      <!-- 标签切换 -->
      <view class="tabs-section">
        <view class="tab-list">
          <view class="tab-item active">
            <text></text>
            <text>帖子</text>
          </view>
        </view>
      </view>

      <!-- 帖子列表 -->
      <view class="posts-grid">
        <view v-if="userPosts.length === 0" class="empty-posts">
          <view class="empty-icon"></view>
          <text>该用户还没有发布帖子</text>
        </view>
        <view v-else class="posts-list">
          <view 
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
              <text>️</text>
            </button>
            <view class="post-cover">
              <image v-if="post.images && post.images.length > 0" :src="post.images[0]" mode="aspectFill" />
              <view v-else class="no-cover"></view>
              <view v-if="post.images && post.images.length > 1" class="image-count">
                <text>+{{ post.images.length - 1 }}</text>
              </view>
            </view>
            <view class="post-brief">
              <text class="post-title">{{ post.content?.substring(0, 40) || '无标题' }}{{ post.content?.length > 40 ? '...' : '' }}</text>
              <view class="post-meta">
                <text class="post-time">{{ formatTime(post.createTime) }}</text>
                <view class="post-likes">
                  <text>❤️</text>
                  <text>{{ post.likeCount || 0 }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getUserInfo, getUserById } from '@/api/user.js';
import { getUserFollowStats, checkFollowing, followUser, unfollowUser } from '@/api/follow.js';
import { getUserPosts, deletePost } from '@/api/post.js';

export default {
  data() {
    return {
      userId: null,
      currentUserId: null,
      userInfo: {
        name: '',
        number: '',
        email: '',
        avatar: ''
      },
      userPosts: [],
      totalLikes: 0,
      followStats: {
        followingCount: 0,
        followersCount: 0
      },
      isFollowing: false,
      loading: true,
      followLoading: false
    };
  },
  
  onLoad(options) {
    this.userId = parseInt(options.id);
    this.currentUserId = uni.getStorageSync('userId') ? parseInt(uni.getStorageSync('userId')) : null;
    
    if (!this.userId || isNaN(this.userId)) {
      uni.showToast({ title: '页面参数错误，即将返回首页', icon: 'none' });
      uni.navigateBack();
      return;
    }
    
    this.loadData();
  },
  
  methods: {
    async loadData() {
      this.loading = true;
      await Promise.all([
        this.fetchUserInfo(),
        this.fetchFollowStats(),
        this.checkFollowStatus(),
        this.fetchUserPosts()
      ]);
      this.loading = false;
    },
    
    async fetchUserInfo() {
      try {
        const currentUserInfo = uni.getStorageSync('userInfo');
        if (currentUserInfo && currentUserInfo.id === this.userId) {
          this.userInfo = currentUserInfo;
        } else {
          // 从后端 API 获取用户信息
          const result = await getUserById(this.userId);
          if (result.success && result.data) {
            this.userInfo = {
              name: result.data.name || result.data.nickname || `用户${this.userId}`,
              number: result.data.number || result.data.phone || '未知',
              email: result.data.email || '',
              avatar: result.data.avatar || ''
            };
          } else {
            this.userInfo = {
              name: `用户${this.userId}`,
              number: '未知',
              email: '',
              avatar: ''
            };
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        this.userInfo = {
          name: `用户${this.userId}`,
          number: '未知',
          email: '',
          avatar: ''
        };
      }
    },
    
    async fetchFollowStats() {
      try {
        const result = await getUserFollowStats(this.userId);
        if (result.success && result.data) {
          this.followStats = {
            followingCount: result.data.followingCount || result.data.following || 0,
            followersCount: result.data.followerCount || result.data.followersCount || result.data.followers || 0
          };
        } else {
          this.followStats = { followingCount: 0, followersCount: 0 };
        }
      } catch (error) {
        this.followStats = { followingCount: 0, followersCount: 0 };
      }
    },
    
    async checkFollowStatus() {
      try {
        const result = await checkFollowing(this.userId);
        if (result.success) {
          this.isFollowing = result.data === true;
        }
      } catch (error) {
        console.error('检查关注状态失败:', error);
      }
    },
    
    async fetchUserPosts() {
      try {
        const result = await getUserPosts(this.userId);
        if (result.success && result.data) {
          let posts = [];
          let stats = {};
          
          if (Array.isArray(result.data)) {
            posts = result.data;
          } else if (result.data.posts) {
            posts = result.data.posts || [];
            stats = result.data.stats || {};
          } else {
            posts = result.data;
          }
          
          this.userPosts = posts;
          
          let likes = 0;
          if (stats && (stats.totalLikes !== undefined || stats.likesCount !== undefined || stats.likeCount !== undefined || stats.likes !== undefined)) {
            likes = stats.totalLikes || stats.likesCount || stats.likeCount || stats.likes || 0;
          } else {
            likes = posts.reduce((sum, post) => sum + (post.likeCount || post.likes || 0), 0);
          }
          this.totalLikes = likes;
        }
      } catch (error) {
        console.error('获取用户帖子失败:', error);
      }
    },
    
    async handleFollow() {
      if (!this.userId || isNaN(this.userId)) {
        uni.showToast({ title: '用户ID无效，无法关注', icon: 'none' });
        return;
      }
      
      this.followLoading = true;
      
      try {
        const targetId = Number(this.userId);
        
        if (this.isFollowing) {
          const result = await unfollowUser(targetId);
          if (result && result.success) {
            this.isFollowing = false;
            this.followStats.followersCount = Math.max(0, this.followStats.followersCount - 1);
            uni.showToast({ title: '已取消关注', icon: 'success' });
          } else {
            uni.showToast({ title: result?.message || '取消关注失败', icon: 'none' });
          }
        } else {
          const result = await followUser(targetId);
          if (result && result.success) {
            this.isFollowing = true;
            this.followStats.followersCount++;
            uni.showToast({ title: '关注成功', icon: 'success' });
          } else {
            uni.showToast({ title: result?.message || '关注失败', icon: 'none' });
            this.isFollowing = false;
          }
        }
      } catch (error) {
        uni.showToast({ title: '网络请求失败: ' + (error.message || '请检查网络连接或后端服务'), icon: 'none' });
        await this.checkFollowStatus();
      } finally {
        this.followLoading = false;
      }
    },
    
    handleMessage() {
      if (!this.currentUserId) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }

      if (this.currentUserId === this.userId) {
        uni.showToast({ title: '不能给自己发消息', icon: 'none' });
        return;
      }

      uni.navigateTo({
        url: `/pages/message-detail/message-detail?userId=${this.userId}`
      });
    },
    
    goToFollowing() {
      uni.navigateTo({
        url: `/pages/follow/follow?type=following&userId=${this.userId}`
      });
    },
    
    goToFollowers() {
      uni.navigateTo({
        url: `/pages/follow/follow?type=followers&userId=${this.userId}`
      });
    },
    
    goToPostDetail(postId) {
      uni.navigateTo({
        url: `/pages/post/post?id=${postId}`
      });
    },
    
    async handleDeletePost(postId) {
      if (!confirm('确定要删除这个帖子吗？')) {
        return;
      }
      
      try {
        const result = await deletePost(postId);
        
        if (result.success) {
          this.userPosts = this.userPosts.filter(post => post.id !== postId);
          uni.showToast({ title: '帖子删除成功', icon: 'success' });
        } else {
          uni.showToast({ title: result.message || '删除失败', icon: 'none' });
        }
      } catch (error) {
        uni.showToast({ title: '删除失败，请稍后重试', icon: 'none' });
      }
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
.user-profile-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  max-width: 768px;
  margin: 0 auto;
}

.profile-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 12px 20px;
  position: sticky;
  top: 0;
  z-index: 100;
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
}

.placeholder {
  width: 40px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  background: #fff;
  flex: 1;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.profile-content {
  flex: 1;
  overflow-y: auto;
}

.user-section {
  padding: 24px;
  background: #fff;
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
  border: 3px solid #667eea;
  flex-shrink: 0;
}

.avatar-large image {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 20px;
  font-size: 14px;
  color: white;
  font-weight: 500;
  cursor: pointer;
}

.follow-action-btn.following {
  background: #f5f5f5;
  color: #666;
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
}

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

.tabs-section {
  background: #fff;
  border-top: 8px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
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
}

.tab-item.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
}

.posts-grid {
  padding: 16px 24px;
  background: #f5f5f5;
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
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.empty-posts text {
  color: #999;
  font-size: 15px;
  margin: 0;
}

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
  position: relative;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.delete-post-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(244, 67, 54, 0.9);
  border: none;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
}

.post-cover {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.post-cover image {
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
  font-size: 40px;
  background: linear-gradient(135deg, #f5f5f5 0%, #e0e0e0 100%);
}

.image-count {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0,0,0,0.5);
  color: white;
  padding: 4px;
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
  font-size: 13px;
  color: #999;
}

.post-likes {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ef4444;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.08);
}

.post-likes text:first-child {
  font-size: 16px;
}
</style>