<template>
  <div class="admin-login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>管理员登录</h2>
        <p>后台管理系统</p>
      </div>
      <form @submit.prevent="handleLogin">
        <div class="form-item">
          <label>用户名</label>
          <input
            v-model="loginForm.username"
            type="text"
            placeholder="请输入管理员用户名"
            required
          />
        </div>
        <div class="form-item">
          <label>密码</label>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>
        <div class="form-item">
          <button type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>
        <div class="form-item link-item">
          <a @click="goToUserLogin">返回用户登录</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { adminLogin } from '@/api/admin';

const router = useRouter();

const loginForm = reactive({
  username: '',
  password: ''
});

const loading = ref(false);

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    alert('请输入用户名和密码');
    return;
  }

  loading.value = true;
  
  try {
    const result = await adminLogin(loginForm.username, loginForm.password);
    
    if (result.success) {
      // 保存管理员登录状态
      localStorage.setItem('isAdminLoggedIn', 'true');
      localStorage.setItem('adminUsername', loginForm.username);
      localStorage.setItem('adminToken', result.token);
      
      // 跳转到管理后台首页
      router.push('/admin/dashboard');
    }
  } catch (error) {
    alert(error.message || '登录失败，请检查用户名和密码');
    console.error('登录错误:', error);
  } finally {
    loading.value = false;
  }
};

const goToUserLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.admin-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 24px;
}

.login-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.form-item input {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  transition: all 0.3s;
}

.form-item input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.form-item button {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.form-item button:hover:not(:disabled) {
  background: #5568d3;
}

.form-item button:disabled {
  background: #a5b4fc;
  cursor: not-allowed;
}

.link-item {
  text-align: center;
  font-size: 14px;
}

.link-item a {
  color: #667eea;
  cursor: pointer;
  text-decoration: none;
}

.link-item a:hover {
  text-decoration: underline;
}
</style>
