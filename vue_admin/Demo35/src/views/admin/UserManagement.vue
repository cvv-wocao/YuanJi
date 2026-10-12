<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>用户管理</h2>
        <p class="subtitle">管理平台所有注册用户及基础信息</p>
      </div>
      <div class="header-right">
        <button class="btn-primary" @click="loadUsers">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
          刷新数据
        </button>
      </div>
    </div>

    <!-- 数据展示区 -->
    <div class="table-card">
      <div class="table-toolbar" v-if="selectedIds.length > 0">
        <div class="batch-info">
          <span class="badge">{{ selectedIds.length }}</span> 项已选择
        </div>
        <div class="batch-actions">
          <button class="btn-danger-outline" @click="batchDeleteConfirm">
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
                  <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
                  <span class="checkmark"></span>
                </label>
              </th>
              <th>用户ID</th>
              <th>用户资料</th>
              <th>联系方式</th>
              <th>基本信息</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id" :class="{'is-selected': selectedIds.includes(user.id)}">
              <td class="checkbox-cell">
                <label class="custom-checkbox">
                  <input type="checkbox" v-model="selectedIds" :value="user.id" />
                  <span class="checkmark"></span>
                </label>
              </td>
              <td class="text-gray">#{{ user.id }}</td>
              <td>
                <div class="user-profile-cell">
                  <img v-if="user.avatar" :src="user.avatar" class="avatar-img" />
                  <div v-else class="avatar-placeholder bg-teal-light">
                    {{ user.name?.charAt(0) || 'U' }}
                  </div>
                  <div class="user-info">
                    <span class="user-name">{{ user.name }}</span>
                  </div>
                </div>
              </td>
              <td>
                <div class="contact-info">
                  <span class="contact-item"><svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg> {{ user.number }}</span>
                  <span class="contact-item text-gray"><svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg> {{ user.email || '未绑定' }}</span>
                </div>
              </td>
              <td>
                <div class="basic-info-tags">
                  <span class="info-tag" v-if="user.age">{{ user.age }}岁</span>
                  <span class="info-tag" :class="user.sex === '女' ? 'tag-pink' : 'tag-blue'" v-if="user.sex">{{ user.sex }}</span>
                </div>
              </td>
              <td class="text-right">
                <div class="action-buttons">
                  <button class="btn-icon-text text-teal" @click="editUser(user)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                    编辑
                  </button>
                  <button class="btn-icon-text text-danger" @click="deleteUserConfirm(user.id)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="6" class="empty-state">
                <div class="empty-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#CBD5E1" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                  <p>暂无用户数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 编辑用户弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-card">
        <div class="modal-header">
          <h4>编辑用户信息</h4>
          <button class="btn-close" @click="closeEditModal">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveUser" class="modern-form">
            <div class="form-row">
              <div class="form-group">
                <label>姓名 <span class="required">*</span></label>
                <input v-model="editForm.name" type="text" class="form-control" required placeholder="请输入真实姓名" />
              </div>
              <div class="form-group">
                <label>电话号码 <span class="required">*</span></label>
                <input v-model="editForm.number" type="text" class="form-control" required placeholder="请输入手机号" />
              </div>
            </div>
            <div class="form-group">
              <label>电子邮箱</label>
              <input v-model="editForm.email" type="email" class="form-control" placeholder="请输入邮箱地址" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>年龄</label>
                <input v-model="editForm.age" type="number" class="form-control" placeholder="年龄" />
              </div>
              <div class="form-group">
                <label>性别</label>
                <div class="custom-select-wrapper">
                  <select v-model="editForm.sex" class="form-control custom-select">
                    <option value="">请选择性别</option>
                    <option value="男">男</option>
                    <option value="女">女</option>
                  </select>
                </div>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-default" @click="closeEditModal">取消</button>
          <button type="button" class="btn-primary" @click="saveUser">保存修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAllUsers, updateUser, deleteUser, batchDeleteUsers } from '@/api/admin';

const users = ref([]);
const selectedIds = ref([]);
const selectAll = ref(false);
const showEditModal = ref(false);

const editForm = ref({
  id: null,
  name: '',
  number: '',
  email: '',
  age: '',
  sex: ''
});

onMounted(() => {
  loadUsers();
});

const loadUsers = async () => {
  try {
    const result = await getAllUsers();
    if (result.success) {
      users.value = result.data || [];
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
  }
};

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedIds.value = users.value.map(u => u.id);
  } else {
    selectedIds.value = [];
  }
};

const editUser = (user) => {
  editForm.value = { ...user };
  showEditModal.value = true;
};

const closeEditModal = () => {
  showEditModal.value = false;
};

const saveUser = async () => {
  try {
    const result = await updateUser(editForm.value.id, editForm.value);
    if (result.success) {
      closeEditModal();
      loadUsers();
    }
  } catch (error) {
    console.error('更新失败:', error);
  }
};

const deleteUserConfirm = async (id) => {
  if (!confirm('确定要删除该用户吗？此操作不可恢复。')) return;
  
  try {
    const result = await deleteUser(id);
    if (result.success) {
      loadUsers();
    }
  } catch (error) {
    console.error('删除失败:', error);
  }
};

const batchDeleteConfirm = async () => {
  if (!confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？`)) return;
  
  try {
    const result = await batchDeleteUsers(selectedIds.value);
    if (result.success) {
      selectedIds.value = [];
      selectAll.value = false;
      loadUsers();
    }
  } catch (error) {
    console.error('批量删除失败:', error);
  }
};
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

.btn-default {
  background: white;
  color: var(--text-dark);
  border: 1px solid var(--border-color);
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.btn-default:hover {
  background: #F8FAFC;
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

.text-teal { color: var(--sports-teal-dark); }
.text-teal:hover { background: #F0FDF4; color: var(--sports-teal); }

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

.text-gray {
  color: var(--text-gray) !important;
}

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

/* Table Cell Specifics */
.user-profile-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #F1F5F9;
}

.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  color: var(--sports-teal-dark);
}

.bg-teal-light {
  background: var(--sports-teal-light);
}

.user-name {
  font-weight: 600;
  color: var(--text-dark);
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.basic-info-tags {
  display: flex;
  gap: 8px;
}

.info-tag {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: #F1F5F9;
  color: #475569;
}

.tag-blue { background: #E0F2FE; color: #0369A1; }
.tag-pink { background: #FCE7F3; color: #BE185D; }

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(11, 19, 43, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  background: white;
  width: 100%;
  max-width: 500px;
  border-radius: var(--radius-md);
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
  overflow: hidden;
  animation: modalFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-dark);
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-gray);
  cursor: pointer;
  line-height: 1;
  padding: 0;
  transition: var(--transition);
}

.btn-close:hover {
  color: #EF4444;
}

.modal-body {
  padding: 24px;
}

.modern-form .form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-dark);
}

.required {
  color: #EF4444;
}

.form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  font-size: 14px;
  transition: var(--transition);
  font-family: inherit;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: var(--sports-teal);
  box-shadow: 0 0 0 3px rgba(0, 180, 216, 0.1);
}

.custom-select-wrapper {
  position: relative;
}

.custom-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%238D99AE' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
}

.modal-footer {
  padding: 16px 24px;
  background: #F8FAFC;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>