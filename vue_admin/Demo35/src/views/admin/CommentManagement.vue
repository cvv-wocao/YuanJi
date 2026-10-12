<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>互动评论管理</h2>
        <p class="subtitle">审查并管理平台用户在动态下的留言互动</p>
      </div>
      <div class="header-right">
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索评论内容或用户ID..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <button class="btn-primary" @click="fetchComments">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
          刷新
        </button>
      </div>
    </div>

    <!-- 数据展示区 -->
    <div class="table-card">
      <div class="table-toolbar" v-if="selectedComments.length > 0">
        <div class="batch-info">
          <span class="badge">{{ selectedComments.length }}</span> 项已选择
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
              <th>评论ID</th>
              <th>评论者</th>
              <th class="content-col">评论内容</th>
              <th>来源/父级</th>
              <th>点赞数</th>
              <th>评论时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="comment in filteredComments" :key="comment.id" :class="{'is-selected': selectedComments.includes(comment.id)}">
              <td class="checkbox-cell">
                <label class="custom-checkbox">
                  <input type="checkbox" :value="comment.id" v-model="selectedComments" />
                  <span class="checkmark"></span>
                </label>
              </td>
              <td class="text-gray">#{{ comment.id }}</td>
              <td>
                <div class="user-profile-cell">
                  <div class="avatar-placeholder bg-pink-light">
                    {{ (comment.user?.name || 'U').charAt(0) }}
                  </div>
                  <div class="user-info">
                    <span class="user-name">{{ comment.user?.name || '用户 ' + comment.userId }}</span>
                  </div>
                </div>
              </td>
              <td class="content-col">
                <div class="comment-content-text" :title="comment.content">{{ comment.content }}</div>
              </td>
              <td>
                <div class="source-info">
                  <div class="source-item" title="所属动态ID">
                    <span class="label">帖子:</span> #{{ comment.postId }}
                  </div>
                  <div class="source-item text-gray" title="父评论ID" v-if="comment.parentId">
                    <span class="label">回复:</span> #{{ comment.parentId }}
                  </div>
                </div>
              </td>
              <td>
                <div class="stat-item">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#64748B" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
                  <span class="text-gray">{{ comment.likeCount || 0 }}</span>
                </div>
              </td>
              <td class="text-gray text-sm">{{ formatTime(comment.createTime) }}</td>
              <td class="text-right">
                <div class="action-buttons">
                  <button class="btn-icon-text text-danger" @click="handleDelete(comment.id)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredComments.length === 0">
              <td colspan="8" class="empty-state">
                <div class="empty-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#CBD5E1" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                  <p>暂无评论数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页控制 -->
      <div class="pagination-wrapper" v-if="comments.length > 0 || page > 1">
        <button class="btn-page" :disabled="page === 1" @click="page--">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"></polyline></svg>
        </button>
        <div class="page-info">第 <span>{{ page }}</span> 页</div>
        <button class="btn-page" :disabled="!hasMore" @click="page++">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { getAllComments, deleteComment, batchDeleteComments } from '@/api/admin';

const comments = ref([]);
const selectedComments = ref([]);
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const searchKeyword = ref('');

// 过滤后的评论列表
const filteredComments = computed(() => {
  if (!searchKeyword.value) return comments.value;
  const keyword = searchKeyword.value.toLowerCase();
  return comments.value.filter(comment =>
    comment.content?.toLowerCase().includes(keyword) ||
    String(comment.userId).includes(keyword)
  );
});

// 是否全选
const isAllSelected = computed(() => {
  return comments.value.length > 0 && selectedComments.value.length === comments.value.length;
});

// 获取评论列表
const fetchComments = async () => {
  try {
    const result = await getAllComments(page.value, size);
    if (result.success) {
      comments.value = result.data?.comments || [];
      hasMore.value = comments.value.length === size;
      selectedComments.value = [];
    } else {
      console.error(result.message || '获取评论列表失败');
    }
  } catch (error) {
    console.error('获取评论列表失败:', error);
  }
};

// 搜索
const handleSearch = () => {
  // 前端搜索，已在 computed 中实现
};

// 删除评论
const handleDelete = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？此操作不可恢复。')) return;

  try {
    const result = await deleteComment(commentId);
    if (result.success) {
      fetchComments();
    } else {
      console.error(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除评论失败:', error);
  }
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedComments.value.length === 0) return;
  if (!confirm(`确定要删除选中的 ${selectedComments.value.length} 条评论吗？此操作不可恢复。`)) return;

  try {
    const result = await batchDeleteComments(selectedComments.value);
    if (result.success) {
      fetchComments();
    } else {
      console.error(result.message || '批量删除失败');
    }
  } catch (error) {
    console.error('批量删除评论失败:', error);
  }
};

// 全选/取消全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedComments.value = [];
  } else {
    selectedComments.value = comments.value.map(comment => comment.id);
  }
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
  fetchComments();
});

onMounted(() => {
  fetchComments();
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

/* Comment Specific Styles */
.user-profile-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: white;
}

.bg-pink-light { background: #F472B6; }

.user-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-dark);
}

.content-col {
  max-width: 280px;
}

.comment-content-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
  background: #F8FAFC;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
}

.source-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

.source-item {
  display: flex;
  gap: 6px;
}

.source-item .label {
  color: #94A3B8;
  font-weight: 500;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
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
</style>