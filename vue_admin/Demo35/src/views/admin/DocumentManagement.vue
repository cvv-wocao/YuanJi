<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>AI文件管理</h2>
        <p class="subtitle">管理运动知识文档和AI训练数据</p>
      </div>
      <div class="header-right">
        <button class="btn-primary" @click="loadDocuments">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
          刷新数据
        </button>
      </div>
    </div>

    <!-- 上传方式选择 -->
    <div class="upload-section">
      <div class="upload-tabs">
        <button 
          v-for="tab in uploadTabs" 
          :key="tab.key"
          class="tab-button"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 文件上传 -->
      <div v-if="currentTab === 'file'" class="upload-card">
        <div class="upload-area" @click="$refs.fileInput.click()">
          <input type="file" ref="fileInput" @change="handleFileUpload" accept=".pdf,.doc,.docx,.txt" style="display: none">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="17 8 12 3 7 8"></polyline>
            <line x1="12" y1="3" x2="12" y2="15"></line>
          </svg>
          <p>点击或拖拽文件到此处上传</p>
          <small>支持 PDF、Word、TXT 格式</small>
        </div>
        <div v-if="uploadProgress > 0 && uploadProgress < 100" class="progress-bar">
          <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
        </div>
      </div>

      <!-- 链接上传 -->
      <div v-if="currentTab === 'link'" class="upload-card">
        <form @submit.prevent="handleLinkUpload" class="upload-form">
          <div class="form-group">
            <label>文档链接 <span class="required">*</span></label>
            <input v-model="linkForm.link" type="url" class="form-control" required placeholder="请输入文档URL" />
          </div>
          <div class="form-group">
            <label>文档标题</label>
            <input v-model="linkForm.title" type="text" class="form-control" placeholder="请输入文档标题（可选）" />
          </div>
          <button type="submit" class="btn-primary">上传文档</button>
        </form>
      </div>

      <!-- 文本上传 -->
      <div v-if="currentTab === 'text'" class="upload-card">
        <form @submit.prevent="handleTextUpload" class="upload-form">
          <div class="form-group">
            <label>文档标题 <span class="required">*</span></label>
            <input v-model="textForm.title" type="text" class="form-control" required placeholder="请输入文档标题" />
          </div>
          <div class="form-group">
            <label>文本内容 <span class="required">*</span></label>
            <textarea v-model="textForm.text" class="form-control textarea-large" required placeholder="请输入或粘贴文本内容" rows="8"></textarea>
          </div>
          <button type="submit" class="btn-primary">上传文档</button>
        </form>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="table-card">
      <div class="table-toolbar" v-if="selectedIds.length > 0">
        <div class="batch-info">
          <span class="badge">{{ selectedIds.length }}</span> 项已选择
        </div>
        <div class="batch-actions">
          <!----<button class="btn-danger-outline" @click="batchDeleteConfirm">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            批量删除
          </button>-->
        </div>
      </div>

      <div class="table-wrapper">
        <table class="modern-table">
          <thead>
            <tr>
              <th class="checkbox-cell">
                <label class="custom-checkbox">
                  <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
                  <span class="checkmark"></span>
                </label>
              </th>
              <th>文档名称</th>
              <th>上传类型</th>
              <th>上传时间</th>
              <!----<th class="text-right">操作</th>-->
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in documents" :key="doc.id" :class="{'is-selected': selectedIds.includes(doc.id)}">
              <td class="checkbox-cell">
                <label class="custom-checkbox">
                  <input type="checkbox" v-model="selectedIds" :value="doc.id" />
                  <span class="checkmark"></span>
                </label>
              </td>
              <td>
                <div class="doc-info">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                  </svg>
                  <span>{{ doc.name }}</span>
                </div>
              </td>
              <td>
                <span class="type-tag" :class="getTypeClass(doc.type)">{{ getTypeLabel(doc.type) }}</span>
              </td>
              <td class="text-gray">{{ formatDate(doc.uploadTime) }}</td>
              <!----<td class="text-right">
                <div class="action-buttons">
                  <button class="btn-icon-text text-danger" @click="deleteDocumentConfirm(doc.id)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    删除
                  </button>
                </div>
              </td>-->
            </tr>
            <tr v-if="documents.length === 0">
              <td colspan="5" class="empty-state">
                <div class="empty-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#CBD5E1" stroke-width="1.5">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                  </svg>
                  <p>暂无文档数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { 
  uploadSportDocument, 
  uploadDocumentByLink, 
  uploadDocumentByText, 
  deleteDocument, 
  getDocumentList 
} from '@/api/admin';

const documents = ref([]);
const selectedIds = ref([]);
const selectAll = ref(false);
const currentTab = ref('file');
const uploadProgress = ref(0);

const uploadTabs = [
  { key: 'file', label: '文件上传' },
  { key: 'link', label: '链接上传' },
  { key: 'text', label: '文本上传' }
];

const linkForm = ref({
  link: '',
  title: ''
});

const textForm = ref({
  text: '',
  title: ''
});

onMounted(() => {
  loadDocuments();
});

// 辅助函数：从AnythingLLM返回的文件名中提取原始文件名
// 例如: "AICallName.txt-23b83aff-b668-45b9-980e-e81453b1b541.json" -> "AICallName.txt"
// 例如: ".txt-adcd4950-175c-4457-92e6-7516bd59d2ff.json" -> "演讲稿.txt"
const extractOriginalFileName = (fileName) => {
  if (!fileName) return '未命名文档';

  // 如果文件名以.json结尾，先移除.json
  let name = fileName.replace(/\.json$/i, '');

  // 查找第一个UUID模式（32个十六进制字符，可能带有连字符）
  // UUID格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (8-4-4-4-12)
  const uuidPattern = /-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;
  const match = name.match(uuidPattern);

  if (match) {
    // 如果找到UUID，移除UUID及其前面的连字符
    name = name.substring(0, match.index);
  }

  // 移除可能存在的前缀（如"upload_"等）
  name = name.replace(/^upload_\d+_/, '');

  // 处理特殊格式：.txt-UUID.json
  // 这种情况下，文件名可能在title字段中，而不是name字段
  if (name.startsWith('.')) {
    // 如果处理后的名字以点开头，说明name字段不包含原始文件名
    // 这种情况下应该返回空字符串，让调用者使用title字段
    return '';
  }

  return name || '未命名文档';
};

const loadDocuments = async () => {
  try {
    const result = await getDocumentList();
    if (result.code === 200) {
      // 处理后端返回的数据结构
      if (result.data && result.data.localFiles) {
        // 后端返回的数据结构是嵌套的: localFiles.items[0].items
        // 需要深入到第二层items获取文档列表
        let docList = [];

        if (result.data.localFiles.items && result.data.localFiles.items.length > 0) {
          // 检查第一层items是否包含文件夹
          const firstLevelItems = result.data.localFiles.items;

          // 遍历第一层items，查找包含文档的文件夹
          for (const item of firstLevelItems) {
            if (item.type === 'folder' && item.items && Array.isArray(item.items)) {
              // 找到文件夹，提取其中的文档
              docList = docList.concat(item.items.filter(doc => doc.type === 'file'));
            } else if (item.type === 'file') {
              // 如果第一层直接是文件，也添加到列表
              docList.push(item);
            }
          }
        }

        // 转换为前端需要的格式
        documents.value = docList.map(doc => {
          // 优先使用title字段，如果title不存在或为空，则从name字段提取
          let displayName = '未命名文档';
          if (doc.title && doc.title.trim()) {
            displayName = doc.title.trim();
          } else {
            const extractedName = extractOriginalFileName(doc.name);
            if (extractedName && extractedName.trim()) {
              displayName = extractedName.trim();
            }
          }
          return {
            id: doc.id || doc.name, // 使用id作为唯一标识
            name: displayName,
            rawName: doc.name,
            type: doc.type || 'file',
          uploadTime: doc.published || doc.created_at || doc.createdAt || Date.now(),
          size: doc.size || doc.filesize || 0,
          }
        });
      } else if (Array.isArray(result.data)) {
        // 如果data本身就是数组，直接使用
        documents.value = result.data.map(doc => ({
          name: doc.name || doc.title || doc.filename || '未命名文档',
          type: doc.type || doc.uploadType || 'file',
          uploadTime: doc.uploadTime || doc.created_at || doc.createdAt || Date.now(),
          size: doc.size || doc.filesize || 0,
          ...doc // 保留其他可能的字段
        }));
      } else if (result.data && typeof result.data === 'object') {
        // 如果data是对象，尝试提取其中的文档列表
        const docList = result.data.documents || result.data.list || result.data.items || result.data.files || [];
        documents.value = Array.isArray(docList) ? docList.map(doc => ({
          name: doc.name || doc.title || doc.filename || '未命名文档',
          type: doc.type || doc.uploadType || 'file',
          uploadTime: doc.uploadTime || doc.created_at || doc.createdAt || Date.now(),
          size: doc.size || doc.filesize || 0,
          ...doc // 保留其他可能的字段
        })) : [];
      } else {
        documents.value = [];
      }
      
      console.log('处理后的文档列表:', documents.value);
    } else {
      console.error('获取文档列表失败:', result.message);
      alert('获取文档列表失败: ' + (result.message || '未知错误'));
    }
  } catch (error) {
    console.error('获取文档列表失败:', error);
    console.error('错误详情:', error.response?.data);
    alert('获取文档列表失败: ' + (error.message || '网络错误'));
  }
};

const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  try {
    uploadProgress.value = 10;
    const result = await uploadSportDocument(file);
    uploadProgress.value = 100;

    if (result.code === 200) {
      alert('文件上传成功！');
      setTimeout(() => {
        uploadProgress.value = 0;
        loadDocuments();
      }, 500);
    } else {
      alert('文件上传失败: ' + (result.message || '未知错误'));
      uploadProgress.value = 0;
    }
  } catch (error) {
    console.error('文件上传失败:', error);
    alert('文件上传失败: ' + (error.message || '网络错误'));
    uploadProgress.value = 0;
  }

  event.target.value = '';
};

const handleLinkUpload = async () => {
  if (!linkForm.value.link) {
    alert('请填写文档链接');
    return;
  }
  try {
    const result = await uploadDocumentByLink(linkForm.value.link, linkForm.value.title);
    if (result.code === 200) {
      alert('链接上传成功！');
      linkForm.value = { link: '', title: '' };
      loadDocuments();
    } else {
      alert('链接上传失败: ' + (result.message || '未知错误'));
    }
  } catch (error) {
    console.error('链接上传失败:', error);
    alert('链接上传失败: ' + (error.message || '网络错误'));
  }
};

const handleTextUpload = async () => {
  if (!textForm.value.text || !textForm.value.title) {
    alert('请填写文档标题和文本内容');
    return;
  }
  try {
    const result = await uploadDocumentByText(textForm.value.text, textForm.value.title);
    if (result.code === 200) {
      alert('文本上传成功！');
      textForm.value = { text: '', title: '' };
      loadDocuments();
    } else {
      alert('文本上传失败: ' + (result.message || '未知错误'));
    }
  } catch (error) {
    console.error('文本上传失败:', error);
    alert('文本上传失败: ' + (error.message || '网络错误'));
  }
};

const deleteDocumentConfirm = async (docId) => {
  // 根据id查找文档名称
  const doc = documents.value.find(d => d.id === docId);
  const docName = doc ? doc.name : docId;

  if (!confirm(`确定要删除文档 "${docName}" 吗？此操作不可恢复。`)) return;

  try {
    const result = await deleteDocument(doc.rawName);
    if (result.code === 200) {
      alert('删除成功！');
      loadDocuments();
      // 清除可能受影响的选中状态
      selectedIds.value = selectedIds.value.filter(id => id !== docId);
      selectAll.value = false;
    } else {
      alert('删除失败: ' + (result.message || '未知错误'));
    }
  } catch (error) {
    console.error('删除失败:', error);
    alert('删除失败: ' + (error.message || '网络错误'));
  }
};

const batchDeleteConfirm = async () => {
  if (selectedIds.value.length === 0) {
    alert('请先选择要删除的文档');
    return;
  }
  if (!confirm(`确定要删除选中的 ${selectedIds.value.length} 个文档吗？此操作不可恢复。`)) return;

  try {
    // 使用文档的id进行删除
    const promises = selectedIds.value.map(id => deleteDocument(id));
    const results = await Promise.all(promises);
    const allSuccess = results.every(r => r.code === 200);
    if (allSuccess) {
      alert(`成功删除 ${selectedIds.value.length} 个文档！`);
      selectedIds.value = [];
      selectAll.value = false;
      loadDocuments();
    } else {
      alert('批量删除失败，部分文档可能未被删除');
    }
  } catch (error) {
    console.error('批量删除失败:', error);
    alert('批量删除失败: ' + (error.message || '网络错误'));
  }
};

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedIds.value = documents.value.map(d => d.id);
  } else {
    selectedIds.value = [];
  }
};

const formatDate = (timestamp) => {
  if (!timestamp) return '-';
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getTypeLabel = (type) => {
  const typeMap = {
    'file': '文件',
    'link': '链接',
    'text': '文本'
  };
  return typeMap[type] || '未知';
};

const getTypeClass = (type) => {
  const classMap = {
    'file': 'type-file',
    'link': 'type-link',
    'text': 'type-text'
  };
  return classMap[type] || '';
};
</script>

<style scoped>
/* 样式保持不变，与用户提供的完全一致 */
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
  color: var(--text-primary);
}

.subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

/* Upload Section */
.upload-section {
  margin-bottom: 24px;
}

.upload-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tab-button {
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: var(--text-secondary);
}

.tab-button:hover {
  background: var(--bg-light);
}

.tab-button.active {
  background: var(--sports-teal);
  color: white;
  border-color: var(--sports-teal);
}

.upload-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-area:hover {
  border-color: var(--sports-teal);
  background: rgba(15, 140, 134, 0.02);
}

.upload-area svg {
  color: var(--sports-teal);
  margin-bottom: 16px;
}

.upload-area p {
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.upload-area small {
  color: var(--text-secondary);
}

.progress-bar {
  height: 4px;
  background: var(--bg-light);
  border-radius: 2px;
  margin-top: 16px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--sports-teal);
  transition: width 0.3s;
}

.upload-form {
  max-width: 600px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.required {
  color: #ef4444;
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: var(--sports-teal);
}

.textarea-large {
  min-height: 200px;
  resize: vertical;
}

/* Table Card */
.table-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-light);
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background: var(--sports-teal);
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.table-wrapper {
  overflow-x: auto;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
}

.modern-table thead {
  background: var(--bg-light);
}

.modern-table th {
  padding: 14px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.modern-table td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
}

.modern-table tr:last-child td {
  border-bottom: none;
}

.checkbox-cell {
  width: 48px;
}

.custom-checkbox {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.custom-checkbox input {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-color);
  border-radius: 4px;
  position: relative;
  transition: all 0.2s;
}

.custom-checkbox input:checked + .checkmark {
  background: var(--sports-teal);
  border-color: var(--sports-teal);
}

.custom-checkbox input:checked + .checkmark::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.doc-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-info svg {
  color: var(--text-secondary);
}

.text-gray {
  color: var(--text-secondary);
}

.text-right {
  text-align: right;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn-icon-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn-icon-text:hover {
  background: var(--bg-light);
}

.text-teal {
  color: var(--sports-teal);
}

.text-danger {
  color: #ef4444;
}

.type-tag {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.type-file {
  background: rgba(15, 140, 134, 0.1);
  color: var(--sports-teal);
}

.type-link {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.type-text {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.empty-state {
  padding: 48px;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--text-secondary);
}

/* Buttons */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--sports-teal);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary:hover {
  background: #0e7a75;
}

.btn-danger-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: white;
  color: #ef4444;
  border: 1px solid #ef4444;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn-danger-outline:hover {
  background: rgba(239, 68, 68, 0.05);
}

.is-selected {
  background: rgba(15, 140, 134, 0.02);
}
</style>