<template>
  <div class="follow-list-page">
    <!-- 顶部导航 -->
    <header class="list-header">
      <button class="back-btn" @click="goBack">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <span class="header-title">{{ pageTitle }}</span>
      <div class="placeholder"></div>
    </header>

    <!-- 标签切换 -->
    <div class="tabs">
      <div 
        class="tab" 
        :class="{ active: activeTab === 'following' }"
        @click="switchTab('following')"
      >
        关注 {{ followingCount > 0 ? `(${followingCount})` : '' }}
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 'followers' }"
        @click="switchTab('followers')"
      >
        粉丝 {{ followersCount > 0 ? `(${followersCount})` : '' }}
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="user-list" v-if="!loading">
      <div v-if="userList.length === 0" class="empty-state">
        <div class="empty-icon">👥</div>
        <p>{{ emptyText }}</p>
      </div>
      
      <div 
        v-for="user in userList" 
        :key="user.id"
        class="user-item"
        @click="goToUserProfile(user)"
      >
        <div class="user-avatar">
          <img v-if="user.avatar" :src="user.avatar" alt="头像" />
          <div v-else class="avatar-default">{{ getUserName(user).charAt(0) }}</div>
        </div>
        <div class="user-info">
          <span class="user-name">{{ getUserName(user) }}</span>
          <span class="user-number" v-if="user.number">用户ID：{{ user.number }}</span>
        </div>
        <button 
          v-if="showFollowBtn(user)"
          class="follow-btn"
          :class="{ following: isFollowing(user) }"
          @click.stop="handleFollowClick(user)"
        >
          {{ getFollowBtnText(user) }}
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getMyFollowing, getMyFollowers, followUser, unfollowUser, getUserFollowing, getUserFollowers } from '@/api/follow';

const router = useRouter();
const route = useRoute();

const activeTab = ref('following');
const userList = ref([]);
const loading = ref(false);
const followingCount = ref(0);
const followersCount = ref(0);
const myFollowingIds = ref(new Set());

// 从路由参数获取类型和查看的目标用户ID
const listType = computed(() => route.query.type || 'following');
const targetUserId = computed(() => route.query.userId ? parseInt(route.query.userId) : null);
const isMyList = computed(() => !targetUserId.value);

// 页面标题
const pageTitle = computed(() => {
  if (isMyList.value) {
    return activeTab.value === 'following' ? '我的关注' : '我的粉丝';
  }
  return activeTab.value === 'following' ? 'TA的关注' : 'TA的粉丝';
});

// 空状态文字
const emptyText = computed(() => {
  if (activeTab.value === 'following') {
    return isMyList.value ? '你还没有关注任何人' : '该用户还没有关注任何人';
  }
  return isMyList.value ? '你还没有粉丝' : '该用户还没有粉丝';
});

// 切换标签
const switchTab = (tab) => {
  activeTab.value = tab;
  fetchUserList();
};

// 获取用户名
const getUserName = (user) => {
  const name = user.name || user.followingName || user.followerName;
  if (name && name !== 'undefined' && name !== 'null') {
    return name;
  }
  // 如果没有名称，返回用户ID
  const uid = user.id;
  return `用户${uid}`;
};

// 判断是否显示关注按钮
const showFollowBtn = (user) => {
  // 不对自己显示关注按钮
  const currentUserId = Number(localStorage.getItem('userId'));
  const userId = Number(user.id);
  return userId !== currentUserId;
};

// 判断是否已关注（确保ID类型一致）
const isFollowing = (user) => {
  const uid = Number(user.id);
  // 将集合中的ID也转为Number进行比较
  const followingIds = [...myFollowingIds.value].map(id => Number(id));
  const result = followingIds.includes(uid);
  console.log('检查是否关注 - 用户ID:', uid, '关注集合:', followingIds, '结果:', result);
  return result;
};

// 获取关注按钮文字
const getFollowBtnText = (user) => {
  return isFollowing(user) ? '已关注' : '关注';
};

// 处理关注/取消关注
const handleFollowClick = async (user) => {
  const userId = Number(user.id);
  console.log('点击关注按钮，用户ID:', userId);
  
  if (!userId || isNaN(userId)) {
    alert('用户ID无效');
    return;
  }
  
  try {
    if (isFollowing(user)) {
      console.log('执行取消关注');
      const result = await unfollowUser(userId);
      if (result.success) {
        myFollowingIds.value.delete(userId);
        alert('已取消关注');
      } else {
        alert(result.message || '取消关注失败');
      }
    } else {
      console.log('执行关注');
      const result = await followUser(userId);
      if (result.success) {
        myFollowingIds.value.add(userId);
        alert('关注成功');
      } else {
        alert(result.message || '关注失败');
      }
    }
  } catch (error) {
    console.error('操作失败:', error);
    alert('操作失败，请重试');
  }
};

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true;
  try {
    let result;
    if (activeTab.value === 'following') {
      if (isMyList.value) {
        result = await getMyFollowing();
      } else {
        result = await getUserFollowing(targetUserId.value);
      }
    } else {
      if (isMyList.value) {
        result = await getMyFollowers();
      } else {
        result = await getUserFollowers(targetUserId.value);
      }
    }
    
    console.log('获取列表结果:', activeTab.value, result);
    
    if (result.success) {
      const rawList = result.data || [];
      console.log('原始用户列表数据:', rawList);
      // 处理用户数据，补充用户信息
      userList.value = await processUserList(rawList);
      console.log('处理后的用户列表:', userList.value);
      // 更新关注状态
      await updateFollowingStatus();
    }
  } catch (error) {
    console.error('获取列表失败:', error);
  } finally {
    loading.value = false;
  }
};

// 处理用户列表，补充用户信息
const processUserList = async (list) => {
  if (!list || list.length === 0) return [];

  // 根据当前标签确定用户ID字段
  // 关注列表(following): 取 followingId（我关注的人）
  // 粉丝列表(followers): 取 followerId（关注我的人）
  const idField = activeTab.value === 'following' ? 'followingId' : 'followerId';

  // 获取所有用户ID
  const userIds = list.map(u => u[idField] || u.id).filter(id => id);
  console.log('提取的用户ID列表:', userIds, '字段:', idField);

  // 使用管理员API逐个获取用户信息
  const { getUserById } = await import('@/api/admin');
  const userInfoMap = {};

  await Promise.all(
    userIds.map(async (uid) => {
      try {
        const result = await getUserById(uid);
        if (result.success && result.data) {
          userInfoMap[Number(uid)] = result.data;
        }
      } catch (error) {
        console.error(`获取用户${uid}信息失败:`, error);
      }
    })
  );

  console.log('用户信息映射:', userInfoMap);

  // 合并用户信息
  return list.map(item => {
    const userId = Number(item[idField] || item.id);
    const userInfo = userInfoMap[userId] || {};

    return {
      ...item,
      id: userId,
      name: userInfo.name || item.followingName || item.followerName || `用户${userId}`,
      avatar: userInfo.avatar || item.avatar,
      number: userInfo.number || item.number
    };
  });
};

// 更新关注状态（判断列表中的用户是否已被关注）
const updateFollowingStatus = async () => {
  try {
    const result = await getMyFollowing();
    if (result.success && result.data) {
      console.log('我的关注列表原始数据:', result.data);
      // 适配不同的字段名：followingId、id、userId 等，并转为Number
      myFollowingIds.value = new Set(result.data.map(u => {
        const id = Number(u.followingId || u.id || u.userId || u.followedId);
        console.log('提取的关注用户ID:', id, '原始数据:', u);
        return id;
      }).filter(id => !isNaN(id) && id > 0));
      console.log('关注ID集合:', [...myFollowingIds.value]);
    }
  } catch (error) {
    console.error('获取关注状态失败:', error);
  }
};

// 跳转到用户资料页
const goToUserProfile = (user) => {
  console.log('点击用户项，完整用户数据:', user);
  const uid = Number(user.id);
  console.log('提取的用户ID:', uid, '类型:', typeof uid);
  if (!uid || isNaN(uid)) {
    console.error('无效的用户ID:', user);
    alert('无法获取用户信息');
    return;
  }
  router.push(`/user/${uid}`);
};

// 返回
const goBack = () => {
  router.back();
};

// 监听路由参数变化
watch(() => route.query.type, (newType) => {
  if (newType) {
    activeTab.value = newType;
    fetchUserList();
  }
}, { immediate: true });

onMounted(() => {
  activeTab.value = listType.value;
  fetchUserList();
  // 获取统计数量
  fetchCounts();
});

// 获取统计数量
const fetchCounts = async () => {
  try {
    const { getMyFollowStats, getUserFollowStats } = await import('@/api/follow');
    const statsFunc = isMyList.value ? getMyFollowStats : () => getUserFollowStats(targetUserId.value);
    const result = await statsFunc();
    if (result.success && result.data) {
      followingCount.value = result.data.followingCount || 0;
      followersCount.value = result.data.followersCount || 0;
    }
  } catch (error) {
    console.error('获取统计失败:', error);
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
  transform: scale(1.05);
}

.back-btn:active {
  transform: scale(0.95);
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
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
}

.user-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 0;
}

.user-item:active::before {
  opacity: 1;
}

.user-item:active {
  transform: scale(0.99);
  background: rgba(250, 250, 250, 0.5);
}

/* 移动端触摸优化 */
@media (hover: none) {
  .user-item:hover {
    background: transparent;
  }
  
  .user-item:active {
    background: rgba(250, 250, 250, 0.5);
  }
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

.user-avatar img {
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
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  flex-shrink: 0;
  box-shadow: 
    0 4px 15px rgba(102, 126, 234, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  min-width: 70px;
}

.follow-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.follow-btn:active::before {
  opacity: 1;
}

.follow-btn:active {
  transform: scale(0.95);
  box-shadow: 
    0 2px 10px rgba(102, 126, 234, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.follow-btn.following {
  background: rgba(245, 245, 245, 0.9);
  color: #666;
  border-color: rgba(221, 221, 221, 0.8);
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
}

.follow-btn.following::before {
  background: rgba(221, 221, 221, 0.1);
}

/* 移动端触摸优化 */
@media (hover: none) {
  .follow-btn:hover:not(.following) {
    transform: none;
    box-shadow: 
      0 4px 15px rgba(102, 126, 234, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
  }
  
  .follow-btn:active {
    transform: scale(0.95);
  }
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

.empty-state p {
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
