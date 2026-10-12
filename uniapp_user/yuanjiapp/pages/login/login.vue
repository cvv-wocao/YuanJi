<template>
  <view class="login-page">
    <image class="page-bg" src="/static/backgrounds/user.jpg" mode="aspectFill" />
    <view class="bg-mask"></view>

    <view class="login-shell">
      <view class="brand-block">
        <image class="brand-logo" src="/static/logo.jpg" mode="aspectFill" />
        <text class="brand-name">远疾</text>
        <text class="brand-copy">连接运动、健康与日常管理</text>
      </view>

      <view class="login-card">
        <view class="card-head">
          <text class="card-title">欢迎回来</text>
          <text class="card-subtitle">输入手机号和密码继续使用</text>
        </view>

        <view class="field-list">
          <view class="field-item">
            <text class="field-label">手机号</text>
            <input
              v-model="loginForm.number"
              class="field-input"
              type="number"
              maxlength="11"
              placeholder="请输入电话号码"
            />
          </view>

          <view class="field-item">
            <view class="field-top">
              <text class="field-label">密码</text>
              <text class="field-action" @tap="togglePassword">
                {{ passwordHidden ? '显示' : '隐藏' }}
              </text>
            </view>
            <input
              v-model="loginForm.password"
              class="field-input"
              :password="passwordHidden"
              placeholder="请输入密码"
            />
          </view>
        </view>

        <button class="login-btn" :disabled="loading" @tap="handleLogin">
          <text>{{ loading ? '登录中...' : '登录' }}</text>
        </button>

        <view class="card-footer">
          <text class="footer-link" @tap="goToRegister">立即注册</text>
          <text class="footer-divider">|</text>
          <text class="footer-link footer-link-muted" @tap="goToAdminLogin">管理员入口</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { login } from '@/api/user.js';

export default {
  data() {
    return {
      loginForm: {
        number: '',
        password: ''
      },
      loading: false,
      passwordHidden: true
    };
  },
  methods: {
    togglePassword() {
      this.passwordHidden = !this.passwordHidden;
    },
    async handleLogin() {
      this.loading = true;

      try {
        const result = await login(this.loginForm.number, this.loginForm.password);

        if (result.success) {
          uni.setStorageSync('isLoggedIn', 'true');
          uni.setStorageSync('userNumber', this.loginForm.number);
          uni.setStorageSync('token', result.token);
          uni.setStorageSync('userName', this.loginForm.number);
          uni.setStorageSync('userId', 'mock-user-id');

          uni.showToast({
            title: '登录成功',
            icon: 'success'
          });

          setTimeout(() => {
            uni.reLaunch({
              url: '/pages/home/home'
            });
          }, 1200);
        }
      } catch (error) {
        uni.showToast({
          title: error.message || '登录失败，请检查账号和密码',
          icon: 'none'
        });
      } finally {
        this.loading = false;
      }
    },
    goToRegister() {
      uni.navigateTo({
        url: '/pages/register/register'
      });
    },
    goToAdminLogin() {
      uni.showToast({
        title: '管理员功能暂未开放',
        icon: 'none'
      });
    }
  }
};
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #0b1220;
}

.page-bg,
.bg-mask {
  position: absolute;
  inset: 0;
}

.page-bg {
  width: 100%;
  height: 100%;
}

.bg-mask {
  background: linear-gradient(180deg, rgba(8, 14, 26, 0.35) 0%, rgba(8, 14, 26, 0.68) 55%, rgba(8, 14, 26, 0.82) 100%);
}

.login-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 120rpx 36rpx 48rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-block {
  margin-bottom: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  color: #ffffff;
}

.brand-logo {
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 16rpx 36rpx rgba(0, 0, 0, 0.22);
}

.brand-name {
  font-size: 54rpx;
  font-weight: 800;
  line-height: 1.1;
}

.brand-copy {
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.82);
}

.login-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 32rpx;
  padding: 36rpx 28rpx calc(36rpx + env(safe-area-inset-bottom));
  backdrop-filter: blur(18rpx);
  box-shadow: 0 20rpx 40rpx rgba(11, 18, 32, 0.16);
}

.card-head {
  margin-bottom: 28rpx;
}

.card-title {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  color: #18243c;
}

.card-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #6b7687;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.field-item {
  padding: 22rpx 22rpx 18rpx;
  border-radius: 24rpx;
  background: #f6f8fb;
}

.field-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-label {
  display: block;
  font-size: 22rpx;
  color: #5e6a7b;
  font-weight: 700;
}

.field-action {
  font-size: 22rpx;
  color: #4f46e5;
  font-weight: 700;
}

.field-input {
  width: 100%;
  min-height: 54rpx;
  margin-top: 14rpx;
  font-size: 30rpx;
  color: #18243c;
}

.login-btn {
  margin-top: 28rpx;
  width: 100%;
  height: 92rpx;
  border-radius: 24rpx;
  background: #18243c;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 800;
}

.login-btn:disabled {
  opacity: 0.68;
}

.card-footer {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
}

.footer-link {
  font-size: 22rpx;
  color: #4f46e5;
  font-weight: 700;
}

.footer-link-muted {
  color: #7a8596;
}

.footer-divider {
  font-size: 20rpx;
  color: #b0b8c4;
}
</style>
