<template>
  <div class="mobile-container sports-theme-mobile">
    <div class="login-wrapper">
      <!-- 顶部 Logo 和标语 -->
      <div class="login-header">
        <div class="logo-box">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14H12L11 22L21 10H12L13 2Z"/></svg>
        </div>
        <h1>登录您的账号</h1>
        <p class="slogan">记录运动，分享健康生活</p>
      </div>

      <!-- 登录表单 -->
      <div class="login-form-container">
        <form @submit.prevent="handleLogin" class="modern-mobile-form">
          <div class="form-group">
            <label>手机号码</label>
            <div class="input-with-icon">
              <span class="icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
              </span>
              <input
                v-model="form.number"
                type="text"
                placeholder="请输入注册手机号"
                required
              />
            </div>
          </div>
          
          <div class="form-group">
            <label>密码</label>
            <div class="input-with-icon">
              <span class="icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              </span>
              <input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                required
              />
            </div>
          </div>
          
          <div class="form-actions">
            <button type="submit" class="btn-primary-mobile" :disabled="loading">
              {{ loading ? '登录中...' : '立即登录' }}
            </button>
          </div>
        </form>
      </div>

      <!-- 底部链接 -->
      <div class="login-footer">
        <p class="register-hint">还没有账号？ <a @click="goToRegister" class="link-primary">立即注册</a></p>
        <div class="divider">
          <span>或</span>
        </div>
        <a @click="goToAdminLogin" class="link-secondary">切换至管理员登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '@/api/user';

const router = useRouter();

const form = reactive({
  number: '',
  password: ''
});

const loading = ref(false);

const handleLogin = async () => {
  if (!form.number || !form.password) {
    alert('请输入手机号和密码');
    return;
  }

  loading.value = true;
  
  try {
    const result = await login(form.number, form.password);
    
    if (result.success) {
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('token', result.data?.token || result.token);
      localStorage.setItem('userId', result.data?.id || result.userId);
      localStorage.setItem('userName', result.data?.name || result.userName || '用户');
      
      router.push('/home');
    } else {
      alert(result.message || '登录失败，请检查账号和密码');
    }
  } catch (error) {
    alert(error.message || '登录失败，请检查账号和密码');
    console.error('登录错误:', error);
  } finally {
    loading.value = false;
  }
};

const goToRegister = () => {
  router.push('/register');
};

const goToAdminLogin = () => {
  router.push('/admin/login');
};
</script>

<style scoped>
/* Mobile Sports Theme Variables */
.sports-theme-mobile {
  --sports-primary: #FF6B35; /* 活力橙 */
  --sports-primary-dark: #E85A2A;
  --sports-secondary: #00B4D8; /* 动感蓝绿 */
  --bg-color: #F8FAFC;
  --surface-color: #FFFFFF;
  --text-main: #0F172A;
  --text-muted: #64748B;
  --border-color: #E2E8F0;
  font-family: 'Inter', -apple-system, sans-serif;
  background-color: var(--bg-color);
  min-height: 100vh;
}

.login-wrapper {
  padding: 40px 24px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  box-sizing: border-box;
}

.login-header {
  margin-top: 40px;
  margin-bottom: 48px;
  text-align: center;
}

.logo-box {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--sports-primary), var(--sports-secondary));
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: white;
  box-shadow: 0 12px 24px rgba(255, 107, 53, 0.25);
  transform: rotate(-5deg);
}

.logo-box svg {
  transform: rotate(5deg);
}

.login-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.slogan {
  font-size: 15px;
  color: var(--text-muted);
  margin: 0;
}

.modern-mobile-form .form-group {
  margin-bottom: 24px;
}

.modern-mobile-form label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8px;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .icon {
  position: absolute;
  left: 16px;
  color: #94A3B8;
  display: flex;
}

.input-with-icon input {
  width: 100%;
  padding: 16px 16px 16px 48px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: var(--surface-color);
  font-size: 16px;
  color: var(--text-main);
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.input-with-icon input:focus {
  outline: none;
  border-color: var(--sports-primary);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.1);
}

.input-with-icon input::placeholder {
  color: #94A3B8;
}

.form-actions {
  margin-top: 40px;
}

.btn-primary-mobile {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: 16px;
  background: var(--sports-primary);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.3);
}

.btn-primary-mobile:active {
  transform: scale(0.98);
  box-shadow: 0 4px 10px rgba(255, 107, 53, 0.2);
}

.btn-primary-mobile:disabled {
  background: #CBD5E1;
  box-shadow: none;
  transform: none;
}

.login-footer {
  margin-top: auto;
  text-align: center;
  padding-top: 40px;
}

.register-hint {
  color: var(--text-muted);
  font-size: 14px;
}

.link-primary {
  color: var(--sports-primary);
  font-weight: 600;
  cursor: pointer;
}

.divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
  color: #CBD5E1;
}

.divider::before, .divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-color);
}

.divider span {
  padding: 0 16px;
  font-size: 13px;
}

.link-secondary {
  color: #94A3B8;
  font-size: 13px;
  text-decoration: underline;
  cursor: pointer;
}
</style>