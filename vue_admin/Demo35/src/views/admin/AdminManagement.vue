<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>管理员管理</h2>
        <p class="subtitle">管理后台操作人员及权限等级</p>
      </div>
      <div class="header-right">
        <button class="btn-primary" @click="showAddModal = true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          新增管理员
        </button>
      </div>
    </div>

    <!-- 数据展示区 -->
    <div class="table-card">
      <div class="table-wrapper">
        <table class="modern-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>管理员信息</th>
              <th>联系方式</th>
              <th>权限等级</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="admin in admins" :key="admin.id">
              <td class="text-gray">#{{ admin.id }}</td>
              <td>
                <div class="user-profile-cell">
                  <img v-if="admin.avatar" :src="admin.avatar" class="avatar-img" />
                  <div v-else class="avatar-placeholder bg-orange-light">
                    {{ admin.username?.charAt(0) || 'A' }}
                  </div>
                  <div class="user-info">
                    <span class="user-name">{{ admin.username }}</span>
                  </div>
                </div>
              </td>
              <td>
                <div class="contact-info">
                  <span class="contact-item"><svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg> {{ admin.phone || '未设置' }}</span>
                  <span class="contact-item text-gray"><svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg> {{ admin.email || '未绑定' }}</span>
                </div>
              </td>
              <td>
                <span class="level-badge" :class="getLevelClass(admin.level)">
                  <svg v-if="admin.level === 3" viewBox="0 0 24 24" width="12" height="12" fill="currentColor" stroke="none"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                  {{ getLevelText(admin.level) }}
                </span>
              </td>
              <td class="text-right">
                <div class="action-buttons">
                  <button class="btn-icon-text text-teal" @click="editAdmin(admin)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                    编辑
                  </button>
                  <button class="btn-icon-text text-danger" @click="deleteAdminConfirm(admin.id)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="admins.length === 0">
              <td colspan="5" class="empty-state">
                <div class="empty-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#CBD5E1" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                  <p>暂无管理员数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增管理员弹窗 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
      <div class="modal-card">
        <div class="modal-header">
          <h4>新增管理员</h4>
          <button class="btn-close" @click="closeAddModal">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveNewAdmin" class="modern-form">
            <div class="form-row">
              <div class="form-group">
                <label>用户名 <span class="required">*</span></label>
                <input v-model="addForm.username" type="text" class="form-control" required placeholder="登录账号" />
              </div>
              <div class="form-group">
                <label>密码 <span class="required">*</span></label>
                <input v-model="addForm.password" type="password" class="form-control" required placeholder="登录密码" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>电子邮箱</label>
                <input v-model="addForm.email" type="email" class="form-control" placeholder="常用邮箱" />
              </div>
              <div class="form-group">
                <label>联系电话</label>
                <input v-model="addForm.phone" type="text" class="form-control" placeholder="手机号" />
              </div>
            </div>
            <div class="form-group">
              <label>管理等级</label>
              <div class="custom-select-wrapper">
                <select v-model="addForm.level" class="form-control custom-select">
                  <option :value="1">1 - 普通管理员 (基础权限)</option>
                  <option :value="2">2 - 高级管理员 (内容管理)</option>
                  <option :value="3">3 - 超级管理员 (最高权限)</option>
                </select>
              </div>
            </div>
            <div class="form-group">
              <label>管理员头像</label>
              <div class="avatar-upload-box">
                <input type="file" accept="image/*" @change="onAvatarChange" id="avatar-upload" class="hidden-input" />
                <label for="avatar-upload" class="upload-trigger" v-if="!avatarPreview">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                  <span>点击上传头像</span>
                </label>
                <div class="preview-container" v-else>
                  <img :src="avatarPreview" class="avatar-preview-img" />
                  <button type="button" class="btn-remove-img" @click="removeAvatar">更换</button>
                </div>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-default" @click="closeAddModal">取消</button>
          <button type="button" class="btn-primary" @click="saveNewAdmin">确认新增</button>
        </div>
      </div>
    </div>

    <!-- 编辑管理员弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-card">
        <div class="modal-header">
          <h4>编辑管理员信息</h4>
          <button class="btn-close" @click="closeEditModal">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveEditAdmin" class="modern-form">
            <div class="form-group">
              <label>用户名 <span class="required">*</span></label>
              <input v-model="editForm.username" type="text" class="form-control" required />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>电子邮箱</label>
                <input v-model="editForm.email" type="email" class="form-control" />
              </div>
              <div class="form-group">
                <label>联系电话</label>
                <input v-model="editForm.phone" type="text" class="form-control" />
              </div>
            </div>
            <div class="form-group">
              <label>管理等级</label>
              <div class="custom-select-wrapper">
                <select v-model="editForm.level" class="form-control custom-select">
                  <option :value="1">1 - 普通管理员 (基础权限)</option>
                  <option :value="2">2 - 高级管理员 (内容管理)</option>
                  <option :value="3">3 - 超级管理员 (最高权限)</option>
                </select>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-default" @click="closeEditModal">取消</button>
          <button type="button" class="btn-primary" @click="saveEditAdmin">保存修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAllAdmins, createAdmin, updateAdmin, deleteAdmin } from '@/api/admin';

const admins = ref([]);
const showAddModal = ref(false);
const showEditModal = ref(false);

const addForm = ref({
  username: '',
  password: '',
  email: '',
  phone: '',
  level: 1
});

const editForm = ref({
  id: null,
  username: '',
  email: '',
  phone: '',
  level: 1
});

const avatarFile = ref(null);
const avatarPreview = ref('');

onMounted(() => {
  loadAdmins();
});

const loadAdmins = async () => {
  try {
    const result = await getAllAdmins();
    if (result.success) {
      admins.value = result.data || [];
    }
  } catch (error) {
    console.error('获取管理员列表失败:', error);
  }
};

const getLevelText = (level) => {
  const levels = { 1: '普通管理员', 2: '高级管理员', 3: '超级管理员' };
  return levels[level] || '未知级别';
};

const getLevelClass = (level) => {
  const classes = { 1: 'level-normal', 2: 'level-advanced', 3: 'level-super' };
  return classes[level] || 'level-normal';
};

const onAvatarChange = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  avatarFile.value = file;
  avatarPreview.value = URL.createObjectURL(file);
};

const removeAvatar = () => {
  avatarFile.value = null;
  avatarPreview.value = '';
};

const saveNewAdmin = async () => {
  try {
    const formData = new FormData();
    formData.append('username', addForm.value.username);
    formData.append('password', addForm.value.password);
    if (addForm.value.email) formData.append('email', addForm.value.email);
    if (addForm.value.phone) formData.append('phone', addForm.value.phone);
    formData.append('level', addForm.value.level);
    if (avatarFile.value) formData.append('avatar', avatarFile.value);

    const result = await createAdmin(formData);
    if (result.success) {
      closeAddModal();
      loadAdmins();
    }
  } catch (error) {
    console.error('创建失败:', error);
  }
};

const editAdmin = (admin) => {
  editForm.value = { ...admin };
  showEditModal.value = true;
};

const saveEditAdmin = async () => {
  try {
    const result = await updateAdmin(editForm.value.id, editForm.value);
    if (result.success) {
      closeEditModal();
      loadAdmins();
    }
  } catch (error) {
    console.error('更新失败:', error);
  }
};

const deleteAdminConfirm = async (id) => {
  if (!confirm('确定要删除该管理员吗？此操作不可逆。')) return;
  
  try {
    const result = await deleteAdmin(id);
    if (result.success) {
      loadAdmins();
    }
  } catch (error) {
    console.error('删除失败:', error);
  }
};

const closeAddModal = () => {
  showAddModal.value = false;
  addForm.value = { username: '', password: '', email: '', phone: '', level: 1 };
  avatarFile.value = null;
  avatarPreview.value = '';
};

const closeEditModal = () => {
  showEditModal.value = false;
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

.text-right {
  text-align: right;
}

.text-gray {
  color: var(--text-gray) !important;
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
  color: white;
}

.bg-orange-light {
  background: var(--sports-orange-light);
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

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.level-normal { background: #F1F5F9; color: #475569; }
.level-advanced { background: #E0F2FE; color: #0369A1; }
.level-super { 
  background: linear-gradient(135deg, #FEF08A, #F59E0B); 
  color: #78350F;
  box-shadow: 0 2px 4px rgba(245, 158, 11, 0.2);
}

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

/* Avatar Upload */
.avatar-upload-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hidden-input {
  display: none;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100px;
  height: 100px;
  border: 2px dashed #CBD5E1;
  border-radius: 12px;
  color: var(--text-gray);
  cursor: pointer;
  transition: var(--transition);
}

.upload-trigger:hover {
  border-color: var(--sports-teal);
  color: var(--sports-teal);
  background: #F0FDF4;
}

.upload-trigger span {
  font-size: 12px;
  font-weight: 500;
}

.preview-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.avatar-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.btn-remove-img {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: rgba(0,0,0,0.6);
  color: white;
  border: none;
  padding: 4px;
  font-size: 12px;
  cursor: pointer;
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