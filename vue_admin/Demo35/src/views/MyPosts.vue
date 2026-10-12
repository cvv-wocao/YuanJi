<template>
  <div class="mobile-container">
    <!-- 顶部标题栏 -->
    <header class="app-header">
      <button class="back-btn" @click="goBack">
        <span>←</span>
      </button>
      <h1>我的帖子</h1>
      <div class="header-spacer"></div>
    </header>

    <!-- 主内容区 -->
    <main class="app-content">
      <!-- 统计卡片 -->
      <div class="stats-card">
        <div class="stat-item">
          <span class="stat-num">{{ stats.postCount || 0 }}</span>
          <span class="stat-label">帖子</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ stats.likeCount || 0 }}</span>
          <span class="stat-label">获赞</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ stats.commentCount || 0 }}</span>
          <span class="stat-label">评论</span>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div v-if="posts.length > 0" class="post-list">
        <div v-for="post in posts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
          <div class="post-content-preview">
            <p class="post-text">{{ post.content }}</p>
            <div v-if="post.images && post.images.length > 0" class="post-image">
              <img :src="post.images[0]" alt="帖子图片" />
              <span v-if="post.images.length > 1" class="image-count">+{{ post.images.length - 1 }}</span>
            </div>
          </div>
          <div class="post-meta">
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
            <div class="post-stats">
              <span>❤️ {{ post.likeCount || 0 }}</span>
              <span>💬 {{ post.commentCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">📝</div>
        <p>你还没有发布过帖子</p>
        <button class="publish-btn" @click="goToHome">去发布</button>
      </div>

      <!-- 加载更多 -->
      <div v-if="posts.length > 0 && hasMore" class="load-more">
        <button @click="loadMorePosts" :disabled="loading">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getUserPosts } from '@/api/post';

const router = useRouter();
const posts = ref([]);
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const loading = ref(false);
const stats = ref({
  postCount: 0,
  likeCount: 0,
  commentCount: 0
});

// 获取用户帖子列表
const fetchUserPosts = async (isLoadMore = false) => {
  if (loading.value) return;
  loading.value = true;

  try {
    const userId = localStorage.getItem('userId');
    const userName = localStorage.getItem('userName') || localStorage.getItem('userNumber') || '用户';

    if (!userId) {
      // 如果没有userId，从用户信息API获取
      console.log('用户ID不存在，请先登录');
      return;
    }

    const result = await getUserPosts(userId);
    if (result.success) {
      const newPosts = result.data || [];
      if (isLoadMore) {
        posts.value.push(...newPosts);
      } else {
        posts.value = newPosts;
      }
      hasMore.value = newPosts.length === size;

      // 计算统计数据
      calculateStats();
    }
  } catch (error) {
    console.error('获取用户帖子失败:', error);
  } finally {
    loading.value = false;
  }
};

// 计算统计数据
const calculateStats = () => {
  stats.value.postCount = posts.value.length;
  stats.value.likeCount = posts.value.reduce((sum, post) => sum + (post.likeCount || 0), 0);
  stats.value.commentCount = posts.value.reduce((sum, post) => sum + (post.commentCount || 0), 0);
};

// 加载更多
const loadMorePosts = () => {
  page.value++;
  fetchUserPosts(true);
};

// 跳转到帖子详情
const goToPostDetail = (postId) => {
  router.push(`/post/${postId}`);
};

// 跳转到首页
const goToHome = () => {
  router.push('/home');
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

onMounted(() => {
  fetchUserPosts();
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
}

/* 顶部标题栏 */
.app-header {
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  color: white;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(26, 188, 156, 0.3);
}

.app-header h1 {
  font-size: 18px;
  margin: 0;
  flex: 1;
  text-align: center;
  font-weight: 600;
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
  border-radius: 20px;
  padding: 24px;
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
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
  gap: 16px;
}

.post-card {
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
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
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.post-image img {
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
  border-radius: 6px;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.post-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  font-weight: 600;
}

.post-stats span {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(249, 250, 251, 0.8);
  border: 1px solid rgba(229, 231, 235, 0.8);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.post-stats span:first-child {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.2);
}

.post-stats span:nth-child(2) {
  color: var(--sports-teal);
  background: rgba(26, 188, 156, 0.08);
  border-color: rgba(26, 188, 156, 0.2);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  background: white;
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.empty-state p {
  margin-bottom: 20px;
}

.publish-btn {
  background: linear-gradient(135deg, var(--sports-teal) 0%, #158f84 100%);
  border: none;
  border-radius: 20px;
  padding: 12px 30px;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(26, 188, 156, 0.4);
  transition: all 0.3s ease;
}

.publish-btn:active {
  transform: scale(0.95);
  box-shadow: 0 2px 8px rgba(26, 188, 156, 0.3);
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 20px;
}

.load-more button {
  background: white;
  border: 1px solid rgba(26, 188, 156, 0.3);
  border-radius: 20px;
  padding: 10px 30px;
  color: var(--sports-teal);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.load-more button:hover:not(:disabled) {
  background: rgba(26, 188, 156, 0.05);
}

.load-more button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  border-color: #ddd;
  color: #999;
}
</style>
