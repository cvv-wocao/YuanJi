<template>
  <view class="follow-list-page">
    <!-- 顶部导航 -->
    <view class="list-header">
      <button class="back-btn" @click="goBack">
        <text>←</text>
      </button>
      <text class="header-title">{{ pageTitle }}</text>
      <view class="placeholder"></view>
    </view>

    <!-- 标签切换 -->
    <view class="tabs">
      <view 
        class="tab" 
        :class="{ active: activeTab === 'following' }"
        @click="switchTab('following')"
      >
        关注 {{ followingCount > 0 ? `(${followingCount})` : '' }}
      </view>
      <view 
        class="tab" 
        :class="{ active: activeTab === 'followers' }"
        @click="switchTab('followers')"
      >
        粉丝 {{ followersCount > 0 ? `(${followersCount})` : '' }}
      </view>
    </view>

    <!-- 用户列表 -->
    <view class="user-list" v-if="!loading">
      <view v-if="userList.length === 0" class="empty-state">
        <view class="empty-icon"></view>
        <text>{{ emptyText }}</text>
      </view>
      
      <view 
        v-for="user in userList" 
        :key="user.id"
        class="user-item"
        @click="goToUserProfile(user)"
      >
        <view class="user-avatar">
          <image v-if="user.avatar" :src="user.avatar" mode="aspectFill" />
          <view v-else class="avatar-default">{{ getUserName(user).charAt(0) }}</view>
        </view>
        <view class="user-info">
          <text class="user-name">{{ getUserName(user) }}</text>
          <text class="user-number" v-if="user.number">用户ID：{{ user.number }}</text>
        </view>
        <button 
          v-if="showFollowBtn(user)"
          class="follow-btn"
          :class="{ following: isFollowing(user) }"
          @click.stop="handleFollowClick(user)"
        >
          {{ getFollowBtnText(user) }}
        </button>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-state">
      <view class="loading-spinner"></view>
      <text>加载中...</text>
    </view>
  </view>
</template>

<script>
import { getUserById } from '@/api/user.js';
import { getMyFollowing, getMyFollowers, followUser, unfollowUser, getUserFollowing, getUserFollowers, getMutualFollows } from '@/api/follow.js';

export default {
  data() {
    return {
      activeTab: 'following',
      userList: [],
      loading: false,
      followingCount: 0,
      followersCount: 0,
      myFollowingIds: new Set(),
      mutualFollowIds: new Set(),
      listType: 'following',
      targetUserId: null,
      isMyList: true
    };
  },
  
  computed: {
    pageTitle() {
      if (this.isMyList) {
        return this.activeTab === 'following' ? '我的关注' : '我的粉丝';
      }
      return this.activeTab === 'following' ? 'TA的关注' : 'TA的粉丝';
    },
    
    emptyText() {
      if (this.activeTab === 'following') {
        return this.isMyList ? '你还没有关注任何人' : '该用户还没有关注任何人';
      }
      return this.isMyList ? '你还没有粉丝' : '该用户还没有粉丝';
    }
  },
  
  onLoad(options) {
    this.listType = options.type || 'following';
    this.targetUserId = options.userId ? parseInt(options.userId) : null;
    this.isMyList = !this.targetUserId;
    this.activeTab = this.listType;
    this.fetchUserList();
    this.fetchCounts();
  },
  
  methods: {
    switchTab(tab) {
      this.activeTab = tab;
      this.fetchUserList();
    },
    
    getUserName(user) {
      const name = user.name || user.followingName || user.followerName || user.nickname || user.username || user.userName ||
                   user.following?.name || user.follower?.name || user.following?.nickname || user.follower?.nickname ||
                   user.user?.name || user.user?.nickname;
      if (name && name !== 'undefined' && name !== 'null' && String(name).trim() !== '') {
        return name;
      }
      const uid = user.id;
      return `用户${uid}`;
    },
    
    showFollowBtn(user) {
      const currentUserId = Number(uni.getStorageSync('userId'));
      const userId = Number(user.id);
      return userId !== currentUserId;
    },
    
    isFollowing(user) {
      const uid = Number(user.id);
      const followingIds = [...this.myFollowingIds].map(id => Number(id));
      return followingIds.includes(uid);
    },
    
    getFollowBtnText(user) {
      if (this.isMutualFollow(user)) {
        return '互关';
      }
      return this.isFollowing(user) ? '已关注' : '关注';
    },

    isMutualFollow(user) {
      const uid = Number(user.id);
      const mutualIds = [...this.mutualFollowIds].map(id => Number(id));
      return mutualIds.includes(uid);
    },
    
    async handleFollowClick(user) {
      const userId = Number(user.id);
      
      if (!userId || isNaN(userId)) {
        uni.showToast({ title: '用户ID无效', icon: 'none' });
        return;
      }
      
      try {
        if (this.isFollowing(user)) {
          const result = await unfollowUser(userId);
          if (result.success) {
            this.myFollowingIds.delete(userId);
            this.mutualFollowIds.delete(userId);
            uni.showToast({ title: '已取消关注', icon: 'success' });
          } else {
            uni.showToast({ title: result.message || '取消关注失败', icon: 'none' });
          }
        } else {
          const result = await followUser(userId);
          if (result.success) {
            this.myFollowingIds.add(userId);
            // 检查是否互相关注
            const mutualResult = await getMutualFollows();
            if (mutualResult.success && mutualResult.data) {
              this.mutualFollowIds = new Set(mutualResult.data.map(u => {
                const id = Number(u.id || u.userId);
                return id;
              }).filter(id => !isNaN(id) && id > 0));
            }
            uni.showToast({ title: '关注成功', icon: 'success' });
          } else {
            uni.showToast({ title: result.message || '关注失败', icon: 'none' });
          }
        }
      } catch (error) {
        console.error('操作失败:', error);
        uni.showToast({ title: '操作失败，请重试', icon: 'none' });
      }
    },
    
    async fetchUserList() {
      this.loading = true;
      try {
        let result;
        if (this.activeTab === 'following') {
          if (this.isMyList) {
            result = await getMyFollowing();
          } else {
            result = await getUserFollowing(this.targetUserId);
          }
        } else {
          if (this.isMyList) {
            result = await getMyFollowers();
          } else {
            result = await getUserFollowers(this.targetUserId);
          }
        }
        
        if (result.success) {
          const rawList = result.data || [];
          this.userList = await this.processUserList(rawList);
          await this.updateFollowingStatus();
        }
      } catch (error) {
        console.error('获取列表失败:', error);
      } finally {
        this.loading = false;
      }
    },
    
    async processUserList(list) {
      if (!list || list.length === 0) return [];

      const idField = this.activeTab === 'following' ? 'followingId' : 'followerId';
      
      // 直接使用关注列表返回的数据，不再额外调用 getUserById
      // 因为关注列表 API 应该已经包含了用户的基本信息
      return list.map(item => {
        const userId = Number(item[idField] || item.id);
        
        // 根据当前标签页决定优先使用哪些字段
        let displayName, avatar, number;
        if (this.activeTab === 'following') {
          // 关注列表：优先使用 following 相关字段
          displayName = item.followingName || item.following?.name || item.following?.nickname ||
                        item.name || item.nickname || item.username || item.userName ||
                        item.user?.name || item.user?.nickname;
          avatar = item.followingAvatar || item.following?.avatar ||
                   item.avatar || item.user?.avatar;
          number = item.followingNumber || item.number;
        } else {
          // 粉丝列表：优先使用 follower 相关字段
          displayName = item.followerName || item.follower?.name || item.follower?.nickname ||
                        item.name || item.nickname || item.username || item.userName ||
                        item.user?.name || item.user?.nickname;
          avatar = item.followerAvatar || item.follower?.avatar ||
                   item.avatar || item.user?.avatar;
          number = item.followerNumber || item.number;
        }

        return {
          ...item,
          id: userId,
          name: displayName && displayName !== 'undefined' && displayName !== 'null' && String(displayName).trim() !== ''
                ? displayName
                : `用户${userId}`,
          avatar: avatar,
          number: number
        };
      });
    },

    async updateFollowingStatus() {
      try {
        // 获取我关注的用户列表
        const followingResult = await getMyFollowing();
        if (followingResult.success && followingResult.data) {
          this.myFollowingIds = new Set(followingResult.data.map(u => {
            const id = Number(u.followingId || u.id || u.userId || u.followedId);
            return id;
          }).filter(id => !isNaN(id) && id > 0));
        }

        // 获取互相关注的用户列表
        const mutualResult = await getMutualFollows();
        if (mutualResult.success && mutualResult.data) {
          this.mutualFollowIds = new Set(mutualResult.data.map(u => {
            const id = Number(u.id || u.userId);
            return id;
          }).filter(id => !isNaN(id) && id > 0));
        }
      } catch (error) {
        console.error('获取关注状态失败:', error);
      }
    },
    
    goToUserProfile(user) {
      const uid = Number(user.id);
      if (!uid || isNaN(uid)) {
        uni.showToast({ title: '无法获取用户信息', icon: 'none' });
        return;
      }
      uni.navigateTo({
        url: `/pages/user/user?id=${uid}`
      });
    },
    
    goBack() {
      uni.navigateBack();
    },
    
    async fetchCounts() {
      try {
        // 这里简化处理，实际应该调用对应的API
        const result = await getMyFollowing();
        if (result.success && result.data) {
          this.followingCount = result.data.length || 0;
        }
      } catch (error) {
        console.error('获取统计失败:', error);
      }
    }
  }
};
</script>

<style scoped>
.follow-list-page {
  min-height: 100vh;
  background: #f5f5f5;
  max-width: 768px;
  margin: 0 auto;
}

/* 顶部导航 - 渐变背景 */
.list-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.3);
  height: 44px;
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
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  letter-spacing: 1px;
}

.placeholder {
  width: 36px;
}

/* 标签切换 */
.tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.tab {
  flex: 1;
  padding: 14px;
  text-align: center;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.tab.active {
  color: #667eea;
  font-weight: 500;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 3px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

/* 用户列表 */
.user-list {
  background: #fff;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(245, 245, 245, 0.8);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.user-item:active {
  background: rgba(250, 250, 250, 0.5);
}

.user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: 2px solid rgba(102, 126, 234, 0.2);
}

.user-avatar image {
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
  color: white;
  font-size: 20px;
  font-weight: bold;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-number {
  font-size: 12px;
  color: #999;
}

.follow-btn {
  padding: 8px 20px;
  border: 1.5px solid #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
  min-width: 70px;
}

.follow-btn:active {
  transform: scale(0.95);
}

.follow-btn.following {
  background: rgba(245, 245, 245, 0.9);
  color: #666;
  border-color: rgba(221, 221, 221, 0.8);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state text {
  color: #999;
  font-size: 15px;
}

/* 加载中 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: #fff;
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
</style>