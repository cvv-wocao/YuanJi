<template>
  <div class="profile-page sports-theme-mobile">
    <!-- 顶部导航 -->
    <header class="profile-header">
      <div class="header-top">
        <button class="back-btn" @click="goToHome">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="header-title">个人中心</span>
        <button class="menu-btn" @click="handleLogout">退出</button>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="profile-content">
      <!-- 用户信息区 -->
      <div class="user-section">
        <div class="user-main">
          <div class="avatar-large" @click="editProfile">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="头像" />
            <div v-else class="avatar-default-large">{{ userInfo.name?.charAt(0) || '?' }}</div>
            <div class="avatar-mask">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>
            </div>
          </div>
          <div class="user-actions">
            <button class="edit-profile-btn" @click="editProfile">编辑资料</button>
          </div>
        </div>

        <!-- 昵称和简介 -->
        <div class="user-info">
          <h1 class="nickname">{{ userInfo.name || '用户' }}</h1>
          <p class="user-id">用户ID：{{ userInfo.number || '-' }}</p>
          <p class="bio">{{ userInfo.email ? '邮箱：' + userInfo.email : '点击编辑资料添加个人简介~' }}</p>
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
          <p>还没有发布帖子</p>
          <button class="publish-btn" @click="goToHome">去发布</button>
        </div>
        <div v-else class="posts-list">
          <div 
            v-for="post in userPosts" 
            :key="post.id" 
            class="post-item"
            @click="goToPostDetail(post.id)"
          >
            <button class="delete-post-btn" @click.stop="handleDeletePost(post.id)">
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

    <!-- 底部导航栏 -->
    <nav class="bottom-nav">
      <div class="nav-item" @click="goToHome">
        <svg class="nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        <span class="nav-text">首页</span>
      </div>
      <div class="nav-item active">
        <svg class="nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span class="nav-text">我</span>
      </div>
    </nav>

    <!-- 编辑资料弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>编辑资料</h3>
          <button class="close-btn" @click="closeEditModal">✕</button>
        </div>
        <div class="modal-body">
          <!-- 头像编辑 -->
          <div class="avatar-edit">
            <div class="avatar-preview-large" @click="$refs.avatarInput.click()">
              <img v-if="avatarPreview" :src="avatarPreview" alt="头像预览" />
              <div v-else class="avatar-default-large">{{ editForm.name?.charAt(0) || '?' }}</div>
              <div class="avatar-mask">更换头像</div>
            </div>
            <input
              type="file"
              ref="avatarInput"
              accept="image/*"
              style="display: none"
              @change="handleAvatarSelect"
            />
          </div>

          <!-- 表单字段 -->
          <div class="form-group">
            <label>昵称</label>
            <input v-model="editForm.name" type="text" placeholder="请输入昵称" />
          </div>

          <div class="form-group">
            <label>电话号码</label>
            <input v-model="editForm.number" type="number" placeholder="请输入电话号码" />
          </div>

          <div class="form-group">
            <label>邮箱</label>
            <input v-model="editForm.email" type="email" placeholder="请输入邮箱" />
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label>年龄</label>
              <input v-model="editForm.age" type="number" placeholder="年龄" />
            </div>
            <div class="form-group half">
              <label>性别</label>
              <select v-model="editForm.sex">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>修改密码（留空表示不修改）</label>
            <input v-model="editForm.password" type="password" placeholder="请输入新密码" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeEditModal">取消</button>
          <button class="save-btn" :disabled="saving" @click="saveProfile">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo, updateUser, getMyFollowStats } from '@/api/user';

const router = useRouter();

// 用户信息
const userInfo = ref({
  name: '',
  number: '',
  email: '',
  age: '',
  sex: '',
  avatar: ''
});

// 关注统计
const followStats = ref({
  followingCount: 0,
  followersCount: 0
});

// 页面加载时获取数据
onMounted(async () => {
  try {
    const result = await getUserInfo();
    if (result.success && result.data) {
      userInfo.value = result.data;
    }
    await fetchFollowStats();
    await fetchUserPosts();
  } catch (error) {
    console.error('获取用户信息失败:', error);
    userInfo.value.name = localStorage.getItem('userName') || localStorage.getItem('userNumber') || '用户';
    userInfo.value.number = localStorage.getItem('userNumber') || '-';
  }
});

// 获取关注统计
const fetchFollowStats = async () => {
  try {
    const result = await getMyFollowStats();
    if (result.success && result.data) {
      followStats.value = {
        followingCount: result.data.followingCount || result.data.following || 0,
        followersCount: result.data.followerCount || result.data.followersCount || result.data.followers || 0
      };
    } else {
      followStats.value = { followingCount: 0, followersCount: 0 };
    }
  } catch (error) {
    console.error('获取关注统计失败:', error);
    followStats.value = { followingCount: 0, followersCount: 0 };
  }
};

// 跳转到关注列表
const goToFollowing = () => {
  router.push({ path: '/following', query: { type: 'following' } });
};

// 跳转到粉丝列表
const goToFollowers = () => {
  router.push({ path: '/followers', query: { type: 'followers' } });
};

onActivated(() => {
  fetchFollowStats();
});

const goToHome = () => {
  router.push('/home');
};

const handleLogout = () => {
  localStorage.removeItem('isLoggedIn');
  localStorage.removeItem('userNumber');
  localStorage.removeItem('token');
  router.push('/login');
};

// 编辑资料弹窗
const showEditModal = ref(false);
const editForm = ref({
  name: '',
  number: '',
  email: '',
  age: '',
  sex: '',
  password: ''
});
const avatarFile = ref(null);
const avatarPreview = ref('');
const saving = ref(false);

// 打开编辑弹窗
const editProfile = () => {
  editForm.value = {
    name: userInfo.value.name || '',
    number: userInfo.value.number || '',
    email: userInfo.value.email || '',
    age: userInfo.value.age || '',
    sex: userInfo.value.sex || '',
    password: ''
  };
  avatarPreview.value = userInfo.value.avatar || '';
  avatarFile.value = null;
  showEditModal.value = true;
};

// 关闭编辑弹窗
const closeEditModal = () => {
  showEditModal.value = false;
  avatarFile.value = null;
  avatarPreview.value = '';
};

// 选择头像
const handleAvatarSelect = (e) => {
  const file = e.target.files[0];
  if (!file) return;

  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB');
    return;
  }

  avatarFile.value = file;
  const reader = new FileReader();
  reader.onload = (e) => {
    avatarPreview.value = e.target.result;
  };
  reader.readAsDataURL(file);
};

// 保存资料
const saveProfile = async () => {
  saving.value = true;
  try {
    const formData = new FormData();

    // 只添加有值的字段
    if (editForm.value.name) formData.append('name', editForm.value.name);
    if (editForm.value.number) formData.append('number', editForm.value.number);
    if (editForm.value.email) formData.append('email', editForm.value.email);
    if (editForm.value.age) formData.append('age', editForm.value.age);
    if (editForm.value.sex) formData.append('sex', editForm.value.sex);
    if (editForm.value.password) formData.append('password', editForm.value.password);
    if (avatarFile.value) formData.append('avatar', avatarFile.value);

    const result = await updateUser(formData);
    if (result.success) {
      alert('保存成功');
      if (result.data) {
        userInfo.value = { ...userInfo.value, ...result.data };
        localStorage.setItem('userName', result.data.name || '');
      }
      closeEditModal();
    } else {
      alert(result.message || '保存失败');
    }
  } catch (error) {
    console.error('保存失败:', error);
    alert('保存失败，请重试');
  } finally {
    saving.value = false;
  }
};

const goToMyPosts = () => {
  router.push('/my-posts');
};

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
      userPosts.value = userPosts.value.filter(post => post.id !== postId);
      await fetchFollowStats();
      alert('帖子删除成功');
    } else {
      alert(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除帖子失败:', error);
    alert('删除失败，请稍后重试');
  }
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

// 获取用户帖子
const userPosts = ref([]);
const totalLikes = ref(0);
const postStats = ref({});

const fetchUserPosts = async () => {
  try {
    const { getMyPosts } = await import('@/api/post');
    const result = await getMyPosts();
    if (result.success) {
      const data = result.data || {};
      userPosts.value = data.posts || [];
      postStats.value = data.stats || {};
      
      const stats = postStats.value;
      let likes = 0;
      if (stats) {
        likes = stats.totalLikes || stats.likesCount || stats.likeCount || stats.likes || 0;
        if (likes === 0 && userPosts.value.length > 0) {
          likes = userPosts.value.reduce((sum, post) => sum + (post.likeCount || 0), 0);
        }
      }
      totalLikes.value = likes;
    }
  } catch (error) {
    console.error('获取用户帖子失败:', error);
  }
};
</script>

<style scoped>
/* Mobile Sports Theme Variables */
.sports-theme-mobile {
  --sports-primary: #FF6B35;
  --sports-primary-dark: #E85A2A;
  --sports-secondary: #00B4D8;
  --bg-color: #F8FAFC;
  --surface-color: #FFFFFF;
  --text-main: #0F172A;
  --text-muted: #64748B;
  --border-color: #E2E8F0;
  font-family: 'Inter', -apple-system, sans-serif;
}

/* 页面容器 - 手机应用尺寸 */
.profile-page {
  min-height: 100vh;
  background: var(--bg-color);
  display: flex;
  flex-direction: column;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  padding-bottom: 70px;
}

/* 顶部导航 - 渐变背景 */
.profile-header {
  background: linear-gradient(135deg, var(--sports-primary) 0%, var(--sports-secondary) 100%);
  color: white;
  padding: calc(16px + env(safe-area-inset-top)) 20px 40px;
  position: relative;
  border-radius: 0 0 32px 32px;
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.2);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.menu-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

/* 主内容区 */
.profile-content {
  flex: 1;
  padding: 0 16px;
  margin-top: -30px;
  position: relative;
  z-index: 10;
}

/* 用户信息区 */
.user-section {
  background: var(--surface-color);
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
  border: 1px solid rgba(0,0,0,0.02);
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
  position: relative;
  cursor: pointer;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-top: -40px;
  background: white;
  overflow: hidden;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-default-large {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--sports-primary), var(--sports-secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  color: white;
}

.avatar-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 24px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-large:hover .avatar-mask {
  opacity: 1;
}

.edit-profile-btn {
  background: #F8FAFC;
  border: 1px solid var(--border-color);
  color: var(--text-main);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-profile-btn:active {
  background: #E2E8F0;
  transform: scale(0.95);
}

.user-info {
  margin-bottom: 20px;
}

.nickname {
  font-size: 22px;
  font-weight: 800;
  margin: 0 0 4px 0;
  color: var(--text-main);
  letter-spacing: -0.5px;
}

.user-id {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0 0 8px 0;
  background: #F1F5F9;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
}

.bio {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.5;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.stat-item.clickable {
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}

.stat-item.clickable:active {
  background: #F8FAFC;
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  font-weight: 500;
}

/* 标签切换 */
.tabs-section {
  background: var(--surface-color);
  border-radius: 20px;
  padding: 4px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.tab-list {
  display: flex;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
  border-radius: 16px;
  transition: all 0.3s;
}

.tab-item.active {
  color: var(--sports-primary);
  background: #FFF7ED;
}

/* 帖子列表 */
.posts-grid {
  margin-bottom: 20px;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-item {
  background: var(--surface-color);
  border-radius: 20px;
  padding: 16px;
  display: flex;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  position: relative;
  border: 1px solid rgba(0,0,0,0.02);
  transition: transform 0.2s;
}

.post-item:active {
  transform: scale(0.98);
}

.delete-post-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: #FEE2E2;
  color: #EF4444;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.8;
  transition: all 0.2s;
  z-index: 2;
}

.delete-post-btn:active {
  opacity: 1;
  transform: scale(0.9);
}

.post-cover {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  background: #F8FAFC;
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
  font-size: 24px;
  color: #CBD5E1;
}

.image-count {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 2px;
  border-radius: 4px;
  display: flex;
  backdrop-filter: blur(4px);
}

.post-brief {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-right: 24px;
}

.post-title {
  margin: 0;
  font-size: 15px;
  color: var(--text-main);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-weight: 500;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-muted);
}

.post-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--sports-primary);
  background: #FFF7ED;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

/* 空状态 */
.empty-posts {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
  background: var(--surface-color);
  border-radius: 24px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.publish-btn {
  margin-top: 16px;
  background: var(--sports-primary);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

/* 底部导航 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: space-around;
  padding: 12px 0 calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid rgba(0,0,0,0.05);
  z-index: 1000;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #94A3B8;
  cursor: pointer;
  transition: all 0.3s;
}

.nav-item.active {
  color: var(--sports-primary);
}

.nav-icon {
  transition: transform 0.3s;
}

.nav-item.active .nav-icon {
  transform: translateY(-2px);
}

.nav-text {
  font-size: 11px;
  font-weight: 600;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.modal-content {
  background: white;
  width: 100%;
  max-width: 480px;
  border-radius: 24px 24px 0 0;
  padding: 24px;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.close-btn {
  background: #F1F5F9;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 16px;
  font-size: 16px;
  color: var(--text-muted);
}

.modal-body {
  overflow-y: auto;
  flex: 1;
  padding-right: 4px;
}

.avatar-edit {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-preview-large {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  border: 2px dashed #CBD5E1;
  background: #F8FAFC;
}

.avatar-preview-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-preview-large .avatar-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
}

.form-group {
  margin-bottom: 16px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-group.half {
  flex: 1;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: #F8FAFC;
  font-size: 15px;
  box-sizing: border-box;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--sports-primary);
  background: white;
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.1);
}

.modal-footer {
  display: flex;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.cancel-btn, .save-btn {
  flex: 1;
  padding: 14px;
  border-radius: 16px;
  font-weight: 700;
  font-size: 16px;
  border: none;
}

.cancel-btn {
  background: #F1F5F9;
  color: var(--text-main);
}

.save-btn {
  background: var(--sports-primary);
  color: white;
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.3);
}

.save-btn:disabled {
  background: #CBD5E1;
  box-shadow: none;
}
</style>