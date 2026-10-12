<template>
  <div class="mobile-container">
    <div class="register-content">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="app-logo">📝</div>
        <h1 class="app-name">注册账号</h1>
        <p class="app-slogan">加入我们，分享精彩生活</p>
      </div>

      <!-- 注册表单 -->
      <div class="form-section">
        <form @submit.prevent="handleRegister">
          <div class="input-group">
            <!-- 姓名 -->
            <div class="input-item">
              <span class="input-icon">👤</span>
              <input
                v-model="form.name"
                type="text"
                placeholder="请输入姓名"
                required
              />
            </div>

            <!-- 电话号码 -->
            <div class="input-item">
              <span class="input-icon">📱</span>
              <input
                v-model="form.number"
                type="text"
                placeholder="请输入电话号码"
                maxlength="11"
                required
              />
            </div>

            <!-- 密码 -->
            <div class="input-item">
              <span class="input-icon">🔒</span>
              <input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                required
              />
            </div>

            <!-- 确认密码 -->
            <div class="input-item">
              <span class="input-icon">🔐</span>
              <input
                v-model="form.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                required
              />
            </div>

            <!-- 邮箱 -->
            <div class="input-item">
              <span class="input-icon">📧</span>
              <input
                v-model="form.email"
                type="email"
                placeholder="请输入邮箱（可选）"
              />
            </div>

            <!-- 年龄和性别 -->
            <div class="form-row">
              <div class="input-item half">
                <span class="input-icon">🎂</span>
                <input
                  v-model="form.age"
                  type="number"
                  min="1"
                  max="150"
                  placeholder="年龄"
                />
              </div>
              <div class="input-item half">
                <span class="input-icon">⚧</span>
                <select v-model="form.sex">
                  <option value="">性别</option>
                  <option value="男">男</option>
                  <option value="女">女</option>
                </select>
              </div>
            </div>

            <!-- 头像上传 -->
            <div class="avatar-section">
              <label class="avatar-label">上传头像（可选）</label>
              <div class="avatar-upload">
                <input ref="fileInput" type="file" accept="image/*" @change="onFileChange" hidden />
                <div class="avatar-box" @click="fileInput?.click()">
                  <img v-if="previewUrl" :src="previewUrl" />
                  <div v-else class="avatar-placeholder">
                    <span>📷</span>
                    <p>点击上传</p>
                  </div>
                </div>
                <button v-if="avatarFile" type="button" class="btn-clear" @click.stop="clearAvatar">清除</button>
              </div>
            </div>
          </div>

          <button class="register-btn" type="submit" :disabled="loading">
            <span v-if="!loading">立即注册</span>
            <span v-else>注册中...</span>
          </button>
        </form>

        <div class="action-links">
          <span>已有账号？</span>
          <a href="javascript:void(0)" class="link-login" @click="router.push('/login')">立即登录</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { registerWithAvatar } from '@/api/user';

const router = useRouter();
const fileInput = ref(null);
const avatarFile = ref(null);
const previewUrl = ref('');
const loading = ref(false);

const form = reactive({
  name: '',
  number: '',
  password: '',
  confirmPassword: '',
  email: '',
  age: '',
  sex: ''
});

// 选择文件
const onFileChange = (e) => {
  const file = e.target.files[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    alert('图片不能超过 5MB');
    return;
  }

  avatarFile.value = file;
  previewUrl.value = URL.createObjectURL(file);
};

// 清除头像
const clearAvatar = () => {
  avatarFile.value = null;
  previewUrl.value = '';
  if (fileInput.value) fileInput.value.value = '';
};

// 提交注册
const handleRegister = async () => {
  // 验证
  if (!form.name || !form.number || !form.password) {
    alert('请填写必填项');
    return;
  }
  if (form.password !== form.confirmPassword) {
    alert('两次密码不一致');
    return;
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    alert('邮箱格式错误');
    return;
  }

  loading.value = true;

  try {
    // 构造 FormData
    const fd = new FormData();
    fd.append('name', form.name);
    fd.append('number', form.number);
    fd.append('password', form.password);
    if (form.email) fd.append('email', form.email);
    if (form.age) fd.append('age', form.age);
    if (form.sex) fd.append('sex', form.sex);
    if (avatarFile.value) fd.append('avatar', avatarFile.value);

    const res = await registerWithAvatar(fd);
    if (res.success) {
      alert('注册成功！');
      router.push('/login');
    }
  } catch (err) {
    alert(err.message || '注册失败');
  } finally {
    loading.value = false;
  }
};
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
  background-color: var(--bg-color);
  min-height: 100vh;
}

.mobile-container {
  display: flex;
  flex-direction: column;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  min-height: 100vh;
}

.register-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 40px 24px;
}

/* Logo区域 */
.logo-section {
  text-align: center;
  padding: 10px 0 30px;
}

.app-logo {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, var(--sports-primary), var(--sports-secondary));
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  margin: 0 auto 16px;
  color: white;
  box-shadow: 0 12px 24px rgba(255, 107, 53, 0.25);
  transform: rotate(-5deg);
}

.app-name {
  font-size: 26px;
  margin: 0 0 8px 0;
  font-weight: 800;
  color: var(--text-main);
  letter-spacing: -0.5px;
}

.app-slogan {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

/* 表单区域 */
.form-section {
  background: var(--surface-color);
  border-radius: 24px;
  padding: 24px;
  margin-top: auto;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0,0,0,0.02);
}

.input-group {
  margin-bottom: 24px;
}

.input-item {
  display: flex;
  align-items: center;
  background: #F8FAFC;
  border-radius: 16px;
  padding: 0 16px;
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
  transition: all 0.3s;
}

.input-item:focus-within {
  border-color: var(--sports-primary);
  background: white;
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.1);
}

.input-icon {
  font-size: 18px;
  margin-right: 12px;
  color: #94A3B8;
  display: flex;
}

.input-item input,
.input-item select {
  flex: 1;
  border: none;
  background: transparent;
  padding: 16px 0;
  font-size: 15px;
  outline: none;
  color: var(--text-main);
}

.input-item input::placeholder {
  color: #94A3B8;
}

.input-item select {
  cursor: pointer;
  color: var(--text-main);
}

.form-row {
  display: flex;
  gap: 12px;
}

.input-item.half {
  flex: 1;
  margin-bottom: 16px;
}

/* 头像上传 */
.avatar-section {
  margin-top: 12px;
  margin-bottom: 24px;
}

.avatar-label {
  display: block;
  font-size: 14px;
  color: var(--text-main);
  font-weight: 600;
  margin-bottom: 12px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-box {
  width: 80px;
  height: 80px;
  border: 2px dashed #CBD5E1;
  border-radius: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
  background: #F8FAFC;
}

.avatar-box:hover {
  border-color: var(--sports-primary);
  background: #FFF7ED;
}

.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  text-align: center;
  color: #94A3B8;
}

.avatar-placeholder span {
  font-size: 24px;
  display: block;
  margin-bottom: 4px;
}

.avatar-placeholder p {
  margin: 0;
  font-size: 12px;
  font-weight: 500;
}

.btn-clear {
  padding: 8px 16px;
  background: #FEE2E2;
  color: #EF4444;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-clear:hover {
  background: #FCA5A5;
}

.register-btn {
  width: 100%;
  padding: 16px;
  background: var(--sports-primary);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.3);
}

.register-btn:active {
  transform: scale(0.98);
  box-shadow: 0 4px 10px rgba(255, 107, 53, 0.2);
}

.register-btn:disabled {
  background: #CBD5E1;
  box-shadow: none;
  transform: none;
  cursor: not-allowed;
}

.action-links {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.link-login {
  color: var(--sports-primary);
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}
</style>
