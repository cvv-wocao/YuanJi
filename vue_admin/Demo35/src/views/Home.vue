<template>
  <div class="mobile-container sports-theme-mobile">
    <!-- 顶部标题栏 -->
    <header class="app-header">
      <h1>发现</h1>
      <div class="header-actions">
        <button class="icon-btn" @click="goToProfile">
          <span class="icon">👤</span>
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-content">
      <!-- 发布框 -->
      <div class="publish-card" @click="showPublishModal = true">
        <div class="publish-input">
          <div class="avatar-small">{{ userName.charAt(0) }}</div>
          <span class="placeholder">分享你的新鲜事...</span>
        </div>
        <div class="publish-actions">
          <span class="action-item">📷 图片</span>
          <span class="action-item">😊 表情</span>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div class="post-list" v-if="posts.length > 0">
        <div v-for="post in posts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
          <!-- 帖子头部 -->
          <div class="post-header">
            <div class="post-avatar">
              <img v-if="post.user?.avatar" :src="post.user.avatar" alt="头像" />
              <div v-else class="avatar-default">{{ post.user?.name?.charAt(0) || '?' }}</div>
            </div>
            <div class="post-info">
              <span class="post-username">{{ post.user?.name || '用户' }}</span>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>

          <!-- 帖子内容 -->
          <div class="post-content">
            <p class="post-text">{{ post.content }}</p>
            <!-- 图片网格 -->
            <div v-if="post.images && post.images.length > 0" class="post-images" :class="'grid-' + Math.min(post.images.length, 3)">
              <img v-for="(img, index) in post.images.slice(0, 9)" :key="index" :src="img" alt="帖子图片" />
            </div>
          </div>

          <!-- 帖子操作栏 -->
          <div class="post-actions">
            <button class="action-btn" :class="{ active: post.isLiked }" @click.stop="handleLike(post)">
              <span class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</span>
              <span class="action-count">{{ post.likeCount || 0 }}</span>
            </button>
            <button class="action-btn" @click.stop="goToPostDetail(post.id)">
              <span class="action-icon">💬</span>
              <span class="action-count">{{ post.commentCount || 0 }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">📝</div>
        <p>还没有帖子，来发布第一条吧！</p>
      </div>

      <!-- 加载更多 -->
      <div v-if="posts.length > 0 && hasMore" class="load-more">
        <button @click="loadMorePosts" :disabled="loading">
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </main>

    <!-- 底部导航栏 -->
    <nav class="bottom-nav">
      <div class="nav-item active">
        <svg class="nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        <span class="nav-text">首页</span>
      </div>
      <div class="nav-item" @click="goToProfile">
        <svg class="nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span class="nav-text">我</span>
      </div>
    </nav>

    <!-- 发布帖子弹窗 -->
    <div v-if="showPublishModal" class="modal-overlay" @click.self="showPublishModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>发布帖子</h3>
          <button class="close-btn" @click="showPublishModal = false">✕</button>
        </div>
        <div class="modal-body">
          <textarea
            v-model="newPostContent"
            placeholder="分享你的新鲜事..."
            rows="5"
            maxlength="500"
          ></textarea>
          <div class="char-count">{{ newPostContent.length }}/500</div>

          <!-- 图片预览 -->
          <div v-if="selectedImages.length > 0" class="image-preview">
            <div v-for="(img, index) in selectedImages" :key="index" class="preview-item">
              <img :src="img" alt="预览" />
              <button class="remove-btn" @click="removeImage(index)">✕</button>
            </div>
          </div>

          <!-- 图片选择 -->
          <div class="image-actions">
            <input
              type="file"
              ref="imageInput"
              multiple
              accept="image/*"
              style="display: none"
              @change="handleImageSelect"
            />
            <button class="image-btn" @click="$refs.imageInput.click()">
              📷 添加图片 ({{ selectedImages.length }}/9)
            </button>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showPublishModal = false">取消</button>
          <button
            class="submit-btn"
            :disabled="!newPostContent.trim() && selectedImages.length === 0 || publishing"
            @click="submitPost"
          >
            {{ publishing ? '发布中...' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPostList, createPost, likePost, unlikePost } from '@/api/post';

const router = useRouter();
const userName = ref(localStorage.getItem('userName') || localStorage.getItem('userNumber') || '用户');
const posts = ref([]);
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const loading = ref(false);

// 发布相关
const showPublishModal = ref(false);
const newPostContent = ref('');
const selectedImages = ref([]);
const selectedFiles = ref([]);
const publishing = ref(false);

// 获取帖子列表
const fetchPosts = async (isLoadMore = false) => {
  if (loading.value) return;
  loading.value = true;

  try {
    const result = await getPostList(page.value, size);
    console.log('帖子列表返回数据:', result);
    if (result.success) {
      const newPosts = result.data?.posts || [];
      if (newPosts.length > 0) {
        console.log('第一个帖子的字段:', Object.keys(newPosts[0]));
        console.log('第一个帖子的name:', newPosts[0].name);
        console.log('第一个帖子的userId:', newPosts[0].userId);
      }
      if (isLoadMore) {
        posts.value.push(...newPosts);
      } else {
        posts.value = newPosts;
      }
      hasMore.value = newPosts.length === size;
    }
  } catch (error) {
    console.error('获取帖子失败:', error);
  } finally {
    loading.value = false;
  }
};

// 加载更多
const loadMorePosts = () => {
  page.value++;
  fetchPosts(true);
};

// 处理点赞
const handleLike = async (post) => {
  try {
    const result = post.isLiked
      ? await unlikePost(post.id)
      : await likePost(post.id);

    if (result.success) {
      post.isLiked = !post.isLiked;
      post.likeCount = (post.likeCount || 0) + (post.isLiked ? 1 : -1);
    }
  } catch (error) {
    console.error('点赞失败:', error);
  }
};

// 处理图片选择
const handleImageSelect = (e) => {
  const files = Array.from(e.target.files);
  const remainingSlots = 9 - selectedImages.value.length;
  const validFiles = files.slice(0, remainingSlots);

  validFiles.forEach(file => {
    if (file.size > 10 * 1024 * 1024) {
      alert('图片大小不能超过10MB');
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      selectedImages.value.push(e.target.result);
      selectedFiles.value.push(file);
    };
    reader.readAsDataURL(file);
  });

  // 重置input
  e.target.value = '';
};

// 移除图片
const removeImage = (index) => {
  selectedImages.value.splice(index, 1);
  selectedFiles.value.splice(index, 1);
};

// 提交帖子
const submitPost = async () => {
  if (!newPostContent.value.trim() && selectedFiles.value.length === 0) return;

  publishing.value = true;
  try {
    const formData = new FormData();
    formData.append('content', newPostContent.value.trim());
    selectedFiles.value.forEach(file => {
      formData.append('images', file);
    });

    const result = await createPost(formData);
    if (result.success) {
      newPostContent.value = '';
      selectedImages.value = [];
      selectedFiles.value = [];
      showPublishModal.value = false;
      page.value = 1;
      await fetchPosts();
      alert('发布成功！');
    } else {
      alert(result.message || '发布失败');
    }
  } catch (error) {
    console.error('发布失败:', error);
    alert('发布失败，请重试');
  } finally {
    publishing.value = false;
  }
};

// 分享
const handleShare = (post) => {
  if (navigator.share) {
    navigator.share({
      title: post.content.substring(0, 30) + '...',
      text: post.content,
      url: window.location.origin + '/post/' + post.id
    });
  } else {
    const url = window.location.origin + '/post/' + post.id;
    navigator.clipboard.writeText(url).then(() => {
      alert('链接已复制到剪贴板');
    });
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

// 跳转帖子详情
const goToPostDetail = (postId) => {
  router.push(`/post/${postId}`);
};

const goToProfile = () => {
  router.push('/profile');
};

onMounted(() => {
  fetchPosts();
});
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

/* 手机容器 */
.mobile-container {
  min-height: 100vh;
  background: var(--bg-color);
  display: flex;
  flex-direction: column;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  padding-bottom: 70px;
  font-family: 'Inter', -apple-system, sans-serif;
}

/* 顶部标题栏 */
.app-header {
  background: var(--surface-color);
  color: var(--text-main);
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding-top: calc(16px + env(safe-area-inset-top));
}

.app-header h1 {
  font-size: 20px;
  margin: 0;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  background: #F8FAFC;
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:active {
  transform: scale(0.9);
  background: #E2E8F0;
}

.icon {
  font-size: 16px;
}

/* 主内容区 */
.app-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

/* 发布卡片 */
.publish-card {
  background: var(--surface-color);
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid rgba(0,0,0,0.02);
}

.publish-card:active {
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.publish-input {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.avatar-small {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--sports-primary), var(--sports-secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.placeholder {
  color: var(--text-muted);
  font-size: 15px;
  font-weight: 500;
}

.publish-actions {
  display: flex;
  gap: 20px;
  padding-top: 16px;
}

.action-item {
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 帖子列表 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  background: var(--surface-color);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0,0,0,0.02);
  transition: all 0.3s;
  cursor: pointer;
}

.post-card:active {
  transform: scale(0.99);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.post-avatar img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #F1F5F9;
}

.avatar-default {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--text-muted);
}

.post-info {
  display: flex;
  flex-direction: column;
}

.post-username {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-main);
}

.post-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.post-text {
  font-size: 15px;
  color: var(--text-main);
  line-height: 1.6;
  margin: 0 0 16px 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  border-radius: 16px;
  overflow: hidden;
}

.grid-1 { grid-template-columns: 1fr; }
.grid-2 { grid-template-columns: repeat(2, 1fr); }
.grid-3 { grid-template-columns: repeat(3, 1fr); }

.post-images img {
  width: 100%;
  height: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

.grid-1 img {
  aspect-ratio: auto;
  max-height: 300px;
}

.post-actions {
  display: flex;
  gap: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.action-btn {
  background: none;
  border: none;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.action-btn.active {
  color: var(--sports-primary);
}

.action-btn.active .action-icon {
  transform: scale(1.1);
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.load-more {
  text-align: center;
  padding: 20px 0;
}

.load-more button {
  background: white;
  border: 1px solid var(--border-color);
  color: var(--text-main);
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
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

/* 发布弹窗 */
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

.modal-body textarea {
  width: 100%;
  border: none;
  background: #F8FAFC;
  padding: 16px;
  border-radius: 16px;
  font-size: 15px;
  resize: none;
  box-sizing: border-box;
  font-family: inherit;
}

.modal-body textarea:focus {
  outline: none;
  box-shadow: inset 0 0 0 2px rgba(255, 107, 53, 0.2);
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0,0,0,0.6);
  color: white;
  border: none;
  width: 20px;
  height: 20px;
  border-radius: 10px;
  font-size: 10px;
}

.image-actions {
  margin-top: 20px;
}

.image-btn {
  background: #FFF7ED;
  color: var(--sports-primary);
  border: none;
  padding: 10px 16px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
}

.modal-footer {
  display: flex;
  gap: 16px;
  margin-top: 24px;
}

.cancel-btn, .submit-btn {
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

.submit-btn {
  background: var(--sports-primary);
  color: white;
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.3);
}

.submit-btn:disabled {
  background: #CBD5E1;
  box-shadow: none;
}
</style>