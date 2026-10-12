<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>社区动态管理</h2>
        <p class="subtitle">管理平台内所有用户发布的帖子及图片内容</p>
      </div>
      <div class="header-right">
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索帖子内容或用户ID..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <button class="btn-primary" @click="fetchPosts">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
          刷新
        </button>
      </div>
    </div>

    <!-- 数据展示区 -->
    <div class="table-card">
      <div class="table-toolbar" v-if="selectedPosts.length > 0">
        <div class="batch-info">
          <span class="badge">{{ selectedPosts.length }}</span> 项已选择
        </div>
        <div class="batch-actions">
          <button class="btn-danger-outline" @click="handleBatchDelete">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            批量删除
          </button>
        </div>
      </div>
      
      <div class="table-wrapper">
        <table class="modern-table">
          <thead>
            <tr>
              <th class="checkbox-cell">
                <label class="custom-checkbox">
                  <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
                  <span class="checkmark"></span>
                </label>
              </th>
              <th>动态ID</th>
              <th>发布者</th>
              <th class="content-col">内容摘要</th>
              <th>媒体内容</th>
              <th>互动数据</th>
              <th>发布时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="post in filteredPosts" :key="post.id" :class="{'is-selected': selectedPosts.includes(post.id)}">
              <td class="checkbox-cell">
                <label class="custom-checkbox">
                  <input type="checkbox" :value="post.id" v-model="selectedPosts" />
                  <span class="checkmark"></span>
                </label>
              </td>
              <td class="text-gray">#{{ post.id }}</td>
              <td>
                <div class="user-profile-cell">
                  <div class="avatar-placeholder bg-blue-light">
                    {{ (post.user?.name || 'U').charAt(0) }}
                  </div>
                  <div class="user-info">
                    <span class="user-name">{{ post.user?.name || '用户 ' + post.userId }}</span>
                  </div>
                </div>
              </td>
              <td class="content-col">
                <div class="post-content-text" :title="post.content">{{ post.content }}</div>
              </td>
              <td>
                <div class="media-preview" v-if="post.images && post.images.length > 0">
                  <div class="img-wrapper" @click="previewImage(post.images)">
                    <img :src="post.images[0]" alt="图片" />
                    <div v-if="post.images.length > 1" class="img-overlay">
                      +{{ post.images.length - 1 }}
                    </div>
                  </div>
                </div>
                <span v-else class="text-gray">纯文本</span>
              </td>
              <td>
                <div class="interaction-stats">
                  <span class="stat-item" title="点赞数">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
                    {{ post.likeCount || 0 }}
                  </span>
                  <span class="stat-item" title="评论数">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                    {{ post.commentCount || 0 }}
                  </span>
                </div>
              </td>
              <td class="text-gray text-sm">{{ formatTime(post.createTime) }}</td>
              <td class="text-right">
                <div class="action-buttons">
                  <button class="btn-icon-text text-danger" @click="handleDelete(post.id)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredPosts.length === 0">
              <td colspan="8" class="empty-state">
                <div class="empty-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#CBD5E1" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                  <p>暂无动态数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页控制 -->
      <div class="pagination-wrapper" v-if="posts.length > 0 || page > 1">
        <button class="btn-page" :disabled="page === 1" @click="page--">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"></polyline></svg>
        </button>
        <div class="page-info">第 <span>{{ page }}</span> 页</div>
        <button class="btn-page" :disabled="!hasMore" @click="page++">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
        </button>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <div v-if="previewImages.length > 0" class="modal-overlay image-preview-overlay" @click.self="previewImages = []">
      <div class="preview-container">
        <button class="btn-close-preview" @click="previewImages = []">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="white" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
        <div class="image-gallery">
          <img v-for="(img, index) in previewImages" :key="index" :src="img" alt="预览图片" class="gallery-img" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { getAllPosts, deletePost, batchDeletePosts } from '@/api/admin';

const posts = ref([]);
const selectedPosts = ref([]);
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const searchKeyword = ref('');
const previewImages = ref([]);

// 过滤后的帖子列表
const filteredPosts = computed(() => {
  if (!searchKeyword.value) return posts.value;
  const keyword = searchKeyword.value.toLowerCase();
  return posts.value.filter(post =>
    post.content?.toLowerCase().includes(keyword) ||
    String(post.userId).includes(keyword)
  );
});

// 是否全选
const isAllSelected = computed(() => {
  return posts.value.length > 0 && selectedPosts.value.length === posts.value.length;
});

// 获取帖子列表
const fetchPosts = async () => {
  try {
    const result = await getAllPosts(page.value, size);
    if (result.success) {
      posts.value = result.data?.posts || [];
      hasMore.value = posts.value.length === size;
      selectedPosts.value = [];
    } else {
      console.error(result.message || '获取帖子列表失败');
    }
  } catch (error) {
    console.error('获取帖子列表失败:', error);
  }
};

// 搜索
const handleSearch = () => {
  // 前端搜索，已在 computed 中实现
};

// 删除帖子
const handleDelete = async (postId) => {
  if (!confirm('确定要删除这条帖子吗？此操作不可恢复。')) return;

  try {
    const result = await deletePost(postId);
    if (result.success) {
      fetchPosts();
    } else {
      console.error(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除帖子失败:', error);
  }
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedPosts.value.length === 0) return;
  if (!confirm(`确定要删除选中的 ${selectedPosts.value.length} 条帖子吗？此操作不可恢复。`)) return;

  try {
    const result = await batchDeletePosts(selectedPosts.value);
    if (result.success) {
      fetchPosts();
    } else {
      console.error(result.message || '批量删除失败');
    }
  } catch (error) {
    console.error('批量删除帖子失败:', error);
  }
};

// 全选/取消全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedPosts.value = [];
  } else {
    selectedPosts.value = posts.value.map(post => post.id);
  }
};

// 预览图片
const previewImage = (images) => {
  previewImages.value = images;
};

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '-';
  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', { 
    month: '2-digit', day: '2-digit', 
    hour: '2-digit', minute: '2-digit' 
  });
};

// 监听页码变化
watch(page, () => {
  fetchPosts();
});

onMounted(() => {
  fetchPosts();
});
</script>

<style scoped>
/* Page Layout */
.page-container {
  padding: 32px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: var(--text-dark);
  font-weight: 700;
}

.subtitle {
  margin: 0;
  color: var(--text-gray);
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 16px;
  align-items: center;
}

/* Search Box */
.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 14px;
  color: var(--text-gray);
}

.search-input {
  padding: 10px 16px 10px 40px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  width: 260px;
  transition: var(--transition);
  outline: none;
}

.search-input:focus {
  border-color: var(--sports-teal);
  box-shadow: 0 0 0 3px rgba(0, 180, 216, 0.1);
  width: 300px;
}

/* Buttons */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--sports-teal);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: 0 4px 12px rgba(0, 180, 216, 0.2);
}

.btn-primary:hover {
  background: var(--sports-teal-dark);
  transform: translateY(-1px);
}

.btn-danger-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  color: #EF4444;
  border: 1px solid #FCA5A5;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-danger-outline:hover {
  background: #FEF2F2;
  border-color: #EF4444;
}

.btn-icon-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  transition: var(--transition);
}

.text-danger { color: #EF4444; }
.text-danger:hover { background: #FEF2F2; }

/* Table Card */
.table-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid rgba(0,0,0,0.02);
  overflow: hidden;
}

.table-toolbar {
  padding: 16px 24px;
  background: #F8FAFC;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-info {
  font-size: 14px;
  color: var(--text-dark);
  font-weight: 500;
}

.batch-info .badge {
  background: var(--sports-orange);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.table-wrapper {
  overflow-x: auto;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.modern-table th {
  padding: 16px 24px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-gray);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-color);
  background: #FFFFFF;
}

.modern-table td {
  padding: 16px 24px;
  font-size: 14px;
  color: var(--text-dark);
  border-bottom: 1px solid var(--border-color);
  vertical-align: middle;
}

.modern-table tbody tr {
  transition: var(--transition);
}

.modern-table tbody tr:hover {
  background: #F8FAFC;
}

.modern-table tbody tr.is-selected {
  background: #F0FDF4;
}

.text-right {
  text-align: right;
}

.text-gray { color: var(--text-gray) !important; }
.text-sm { font-size: 13px; }

/* Checkbox */
.checkbox-cell {
  width: 48px;
  padding-right: 0 !important;
}

.custom-checkbox {
  position: relative;
  display: inline-block;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.custom-checkbox input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: absolute;
  top: 0;
  left: 0;
  height: 18px;
  width: 18px;
  background-color: #fff;
  border: 1.5px solid #CBD5E1;
  border-radius: 4px;
  transition: var(--transition);
}

.custom-checkbox:hover input ~ .checkmark {
  border-color: var(--sports-teal);
}

.custom-checkbox input:checked ~ .checkmark {
  background-color: var(--sports-teal);
  border-color: var(--sports-teal);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.custom-checkbox input:checked ~ .checkmark:after {
  display: block;
}

/* Post Specific Styles */
.user-profile-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: white;
}

.bg-blue-light { background: #60A5FA; }

.user-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-dark);
}

.content-col {
  max-width: 250px;
}

.post-content-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
}

.media-preview .img-wrapper {
  position: relative;
  width: 64px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-color);
}

.media-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.img-wrapper:hover img {
  transform: scale(1.1);
}

.img-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.interaction-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #64748B;
}

.empty-state {
  text-align: center;
  padding: 64px 0 !important;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--text-gray);
}

/* Pagination */
.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  border-top: 1px solid var(--border-color);
}

.btn-page {
  background: white;
  border: 1px solid var(--border-color);
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-dark);
  transition: var(--transition);
}

.btn-page:not(:disabled):hover {
  border-color: var(--sports-teal);
  color: var(--sports-teal);
}

.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #F8FAFC;
}

.page-info {
  font-size: 14px;
  color: var(--text-gray);
}

.page-info span {
  font-weight: 600;
  color: var(--text-dark);
}

/* Image Preview Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-overlay {
  background: rgba(11, 19, 43, 0.9);
  backdrop-filter: blur(8px);
}

.preview-container {
  position: relative;
  width: 90vw;
  max-width: 1200px;
  height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close-preview {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.3s;
  z-index: 10;
}

.btn-close-preview:hover {
  opacity: 1;
}

.image-gallery {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  padding: 20px;
  max-width: 100%;
  max-height: 100%;
}

.gallery-img {
  max-height: 80vh;
  max-width: 80vw;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.3);
}

.image-gallery::-webkit-scrollbar {
  height: 8px;
}

.image-gallery::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.3);
  border-radius: 4px;
}
</style>