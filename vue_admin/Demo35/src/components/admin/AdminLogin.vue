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
            v-model="form.username"
            type="text"
            placeholder="请输入管理员用户名"
            required
          />
        </div>
        
        <div class="form-item">
          <label>密码</label>
          <input
            v-model="form.password"
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
      </form>
      
      <div class="back-link">
        <a @click="goToUserLogin">← 返回用户登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { adminLogin } from '@/api/admin';

const router = useRouter();

const form = reactive({
  username: '',
  password: ''
});

const loading = ref(false);

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码');
    return;
  }

  loading.value = true;
  
  try {
    const result = await adminLogin(form.username, form.password);
    
    if (result.success) {
      // 保存管理员登录状态
      localStorage.setItem('adminLoggedIn', 'true');
      localStorage.setItem('adminToken', result.token);
      localStorage.setItem('adminUsername', form.username);
      
      alert('登录成功！');
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
  background: linear-gradient(135deg, #0B132B 0%, #1C2541 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.admin-login-container::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 180, 216, 0.2), transparent);
}

.admin-login-container::after {
  content: '';
  position: absolute;
  bottom: -150px;
  left: -50px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.15), transparent);
}

.login-box {
  width: 420px;
  padding: 48px 40px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 2;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h2 {
  margin: 0 0 12px 0;
  color: #0B132B;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.login-header p {
  margin: 0;
  color: #8D99AE;
  font-size: 15px;
}

.form-item {
  margin-bottom: 24px;
}

.form-item label {
  display: block;
  margin-bottom: 10px;
  color: #2B2D42;
  font-size: 14px;
  font-weight: 600;
}

.form-item input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 15px;
  box-sizing: border-box;
  transition: all 0.3s;
  background: #F8FAFC;
}

.form-item input:focus {
  outline: none;
  border-color: #00B4D8;
  background: white;
  box-shadow: 0 0 0 3px rgba(0, 180, 216, 0.1);
}

.form-item button {
  width: 100%;
  padding: 14px;
  margin-top: 8px;
  background: #00B4D8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 180, 216, 0.3);
}

.form-item button:hover:not(:disabled) {
  background: #0077B6;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 180, 216, 0.4);
}

.form-item button:disabled {
  background: #94A3B8;
  box-shadow: none;
  transform: none;
  cursor: not-allowed;
}

.back-link {
  text-align: center;
  margin-top: 30px;
}

.back-link a {
  color: #8D99AE;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s;
}

.back-link a:hover {
  color: #00B4D8;
}
</style>
