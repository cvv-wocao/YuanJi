<template>
  <div class="user-manage">
    <div class="page-header">
      <h1>用户管理</h1>
      <div class="search-box">
        <input 
          v-model="searchKey" 
          type="text" 
          placeholder="搜索用户姓名或电话"
          @keyup.enter="handleSearch"
        />
        <button @click="handleSearch">搜索</button>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th width="60">
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll">
            </th>
            <th>ID</th>
            <th>头像</th>
            <th>姓名</th>
            <th>电话号码</th>
            <th>邮箱</th>
            <th>性别</th>
            <th>年龄</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in userList" :key="user.id">
            <td>
              <input type="checkbox" v-model="selectedIds" :value="user.id">
            </td>
            <td>{{ user.id }}</td>
            <td>
              <img v-if="user.avatar" :src="user.avatar" class="avatar-img" />
              <div v-else class="avatar-default">{{ user.name?.charAt(0) }}</div>
            </td>
            <td>{{ user.name }}</td>
            <td>{{ user.number }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>{{ user.sex || '-' }}</td>
            <td>{{ user.age || '-' }}</td>
            <td>
              <button class="btn-edit" @click="editUser(user)">编辑</button>
              <button class="btn-delete" @click="deleteUser(user.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="batch-actions" v-if="selectedIds.length > 0">
        <span>已选择 {{ selectedIds.length }} 项</span>
        <button class="btn-batch-delete" @click="batchDelete">批量删除</button>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div class="modal" v-if="showEditModal">
      <div class="modal-content">
        <h3>编辑用户</h3>
        <div class="form-item">
          <label>姓名</label>
          <input v-model="editForm.name" type="text" />
        </div>
        <div class="form-item">
          <label>电话号码</label>
          <input v-model="editForm.number" type="text" disabled />
        </div>
        <div class="form-item">
          <label>邮箱</label>
          <input v-model="editForm.email" type="email" />
        </div>
        <div class="form-item">
          <label>性别</label>
          <select v-model="editForm.sex">
            <option value="">请选择</option>
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
        </div>
        <div class="form-item">
          <label>年龄</label>
          <input v-model="editForm.age" type="number" />
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showEditModal = false">取消</button>
          <button class="btn-save" @click="saveUser">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAllUsers, updateUser, deleteUser, batchDeleteUsers } from '@/api/admin';

const userList = ref([]);
const searchKey = ref('');
const selectedIds = ref([]);
const selectAll = ref(false);
const showEditModal = ref(false);
const editForm = ref({});

const fetchUsers = async () => {
  try {
    const result = await getAllUsers();
    if (result.success && result.data) {
      userList.value = result.data;
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
    alert('获取用户列表失败');
  }
};

onMounted(fetchUsers);

const handleSearch = () => {
  if (!searchKey.value) {
    fetchUsers();
    return;
  }
  const key = searchKey.value.toLowerCase();
  userList.value = userList.value.filter(u => 
    u.name?.toLowerCase().includes(key) || 
    u.number?.includes(key)
  );
};

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedIds.value = userList.value.map(u => u.id);
  } else {
    selectedIds.value = [];
  }
};

const editUser = (user) => {
  editForm.value = { ...user };
  showEditModal.value = true;
};

const saveUser = async () => {
  try {
    const result = await updateUser(editForm.value.id, editForm.value);
    if (result.success) {
      alert('保存成功');
      showEditModal.value = false;
      fetchUsers();
    }
  } catch (error) {
    alert('保存失败: ' + error.message);
  }
};

const deleteUser = async (id) => {
  if (!confirm('确定要删除该用户吗？')) return;
  try {
    const result = await deleteUser(id);
    if (result.success) {
      alert('删除成功');
      fetchUsers();
    }
  } catch (error) {
    alert('删除失败: ' + error.message);
  }
};

const batchDelete = async () => {
  if (!confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？`)) return;
  try {
    const result = await batchDeleteUsers(selectedIds.value);
    if (result.success) {
      alert('批量删除成功');
      selectedIds.value = [];
      selectAll.value = false;
      fetchUsers();
    }
  } catch (error) {
    alert('批量删除失败: ' + error.message);
  }
};
</script>

<style scoped>
.user-manage {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: #333;
}

.search-box {
  display: flex;
  gap: 10px;
}

.search-box input {
  width: 250px;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.search-box button {
  padding: 8px 20px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.table-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  background: #fafafa;
  color: #666;
  font-weight: 500;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-default {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-weight: bold;
}

.btn-edit,
.btn-delete {
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 8px;
}

.btn-edit {
  background: #1890ff;
  color: white;
}

.btn-delete {
  background: #ff4d4f;
  color: white;
}

.batch-actions {
  margin-top: 16px;
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-batch-delete {
  padding: 6px 16px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 弹窗 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 8px;
  width: 400px;
}

.modal-content h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.modal-content .form-item {
  margin-bottom: 16px;
}

.modal-content label {
  display: block;
  margin-bottom: 6px;
  color: #666;
  font-size: 14px;
}

.modal-content input,
.modal-content select {
  width: 100%;
  padding: 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-cancel {
  padding: 8px 20px;
  background: #f0f0f0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-save {
  padding: 8px 20px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
