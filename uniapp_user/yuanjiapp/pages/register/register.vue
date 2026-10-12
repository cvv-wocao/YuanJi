<template>
  <view class="register-page">
    <image class="page-bg" src="/static/backgrounds/user.jpg" mode="aspectFill" />
    <view class="bg-mask"></view>

    <view class="register-shell">
      <view class="brand-block">
        <view class="brand-mark">
          <text class="brand-mark-text">YJ</text>
        </view>
        <text class="brand-name">创建你的远疾账号</text>
        <text class="brand-copy">完善基础信息后，就可以开始记录运动、管理订单和连接日常健康生活。</text>
      </view>

      <view class="register-card">
        <view class="card-head">
          <text class="card-title">立即注册</text>
          <text class="card-subtitle">信息只需填写一次，后续都可以在个人中心继续完善。</text>
        </view>

        <view class="avatar-panel">
          <view class="avatar-box" @tap="chooseAvatar">
            <image v-if="previewUrl" :src="previewUrl" class="avatar-image" mode="aspectFill" />
            <view v-else class="avatar-placeholder">
              <text class="avatar-placeholder-mark">+</text>
              <text class="avatar-placeholder-text">上传头像</text>
            </view>
          </view>
          <view class="avatar-copy">
            <text class="avatar-title">个人头像</text>
            <text class="avatar-subtitle">选填，支持相册或拍照上传</text>
            <text v-if="previewUrl" class="avatar-clear" @tap="clearAvatar">移除头像</text>
          </view>
        </view>

        <view class="field-list">
          <view class="field-item">
            <text class="field-label">姓名</text>
            <input
              v-model.trim="form.name"
              class="field-input"
              type="text"
              maxlength="20"
              placeholder="请输入真实姓名或昵称"
            />
          </view>

          <view class="field-item">
            <text class="field-label">手机号</text>
            <input
              v-model.trim="form.number"
              class="field-input"
              type="number"
              maxlength="11"
              placeholder="请输入 11 位手机号"
            />
          </view>

          <view class="field-item">
            <view class="field-top">
              <text class="field-label">密码</text>
              <text class="field-action" @tap="togglePassword('passwordHidden')">
                {{ passwordHidden ? '显示' : '隐藏' }}
              </text>
            </view>
            <input
              v-model="form.password"
              class="field-input"
              :password="passwordHidden"
              maxlength="20"
              placeholder="6-20 位，建议字母和数字组合"
            />
          </view>

          <view class="field-item">
            <view class="field-top">
              <text class="field-label">确认密码</text>
              <text class="field-action" @tap="togglePassword('confirmPasswordHidden')">
                {{ confirmPasswordHidden ? '显示' : '隐藏' }}
              </text>
            </view>
            <input
              v-model="form.confirmPassword"
              class="field-input"
              :password="confirmPasswordHidden"
              maxlength="20"
              placeholder="请再次输入密码"
            />
          </view>

          <view class="field-item">
            <text class="field-label">邮箱</text>
            <input
              v-model.trim="form.email"
              class="field-input"
              type="text"
              placeholder="请输入邮箱，可选"
            />
          </view>

          <view class="field-grid">
            <view class="field-item compact-item">
              <text class="field-label">年龄</text>
              <input
                v-model.trim="form.age"
                class="field-input"
                type="number"
                maxlength="3"
                placeholder="选填"
              />
            </view>

            <view class="field-item compact-item">
              <text class="field-label">性别</text>
              <picker @change="onSexChange" :value="sexIndex" :range="sexOptions">
                <view class="picker-shell">
                  <text class="picker-text" :class="{ 'picker-placeholder': !form.sex }">
                    {{ form.sex || '请选择' }}
                  </text>
                  <text class="picker-arrow">⌄</text>
                </view>
              </picker>
            </view>
          </view>
        </view>

        <button class="register-btn" :disabled="loading" @tap="handleRegister">
          <text>{{ loading ? '注册中...' : '创建账号' }}</text>
        </button>

        <view class="agreement-copy">
          <text>注册即表示你同意平台基础服务规则，并接受必要的账号信息校验。</text>
        </view>

        <view class="card-footer">
          <text class="footer-text">已经有账号？</text>
          <text class="footer-link" @tap="goToLogin">返回登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { post, upload } from '@/utils/request.js';

export default {
  data() {
    return {
      form: {
        name: '',
        number: '',
        password: '',
        confirmPassword: '',
        email: '',
        age: '',
        sex: ''
      },
      sexOptions: ['男', '女'],
      sexIndex: -1,
      previewUrl: '',
      loading: false,
      passwordHidden: true,
      confirmPasswordHidden: true
    };
  },
  methods: {
    togglePassword(key) {
      this[key] = !this[key];
    },
    onSexChange(e) {
      const index = Number(e.detail.value);
      this.sexIndex = index;
      this.form.sex = this.sexOptions[index];
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const tempFilePaths = res.tempFilePaths;
          this.previewUrl = tempFilePaths[0];
        },
        fail: (err) => {
          console.error('选择图片失败:', err);
          uni.showToast({
            title: '选择图片失败',
            icon: 'none'
          });
        }
      });
    },
    clearAvatar() {
      this.previewUrl = '';
    },
    validateForm() {
      // if (!this.form.name || !this.form.number || !this.form.password) {
      //   return '请填写姓名、手机号和密码';
      // }
      // if (!/^1\d{10}$/.test(this.form.number)) {
      //   return '请输入正确的手机号';
      // }
      // if (this.form.password.length < 6) {
      //   return '密码至少需要 6 位';
      // }
      // if (this.form.password !== this.form.confirmPassword) {
      //   return '两次密码输入不一致';
      // }
      // if (this.form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
      //   return '邮箱格式不正确';
      // }
      // if (this.form.age) {
      //   const age = Number(this.form.age);
      //   if (!Number.isInteger(age) || age < 1 || age > 150) {
      //     return '请输入正确的年龄';
      //   }
      // }
      return '';
    },
    async handleRegister() {
      const errorMessage = this.validateForm();
      if (errorMessage) {
        uni.showToast({
          title: errorMessage,
          icon: 'none'
        });
        return;
      }

      this.loading = true;

      try {
        const formData = {
          name: this.form.name,
          number: parseInt(this.form.number, 10),
          password: this.form.password
        };

        if (this.form.email) formData.email = this.form.email;
        if (this.form.age) formData.age = parseInt(this.form.age, 10);
        if (this.form.sex) formData.sex = this.form.sex;

        let result;
        if (this.previewUrl) {
          result = await this.uploadRegister(this.previewUrl, formData);
        } else {
          result = await this.register(formData);
        }

        if (result.success) {
          uni.showToast({
            title: result.message || '注册成功',
            icon: 'success'
          });

          setTimeout(() => {
            uni.navigateBack();
          }, 1200);
        }
      } catch (err) {
        uni.showToast({
          title: err.message || '注册失败',
          icon: 'none'
        });
      } finally {
        this.loading = false;
      }
    },
    async register(data) {
      return post('/user/register', data);
    },
    async uploadRegister(filePath, formData) {
      return upload('/user/register', filePath, formData, 'avatar');
    },
    goToLogin() {
      uni.navigateBack();
    }
  }
};
</script>

<style scoped>
.register-page {
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
  background:
    radial-gradient(circle at top right, rgba(87, 157, 255, 0.28) 0, rgba(87, 157, 255, 0) 34%),
    linear-gradient(180deg, rgba(6, 12, 24, 0.42) 0%, rgba(7, 14, 29, 0.78) 56%, rgba(7, 14, 29, 0.92) 100%);
}

.register-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 92rpx 30rpx 48rpx;
}

.brand-block {
  margin-bottom: 30rpx;
  color: #fff;
}

.brand-mark {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 18rpx 40rpx rgba(0, 0, 0, 0.18);
  margin-bottom: 26rpx;
}

.brand-mark-text {
  font-size: 34rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.brand-name {
  display: block;
  font-size: 52rpx;
  font-weight: 800;
  line-height: 1.15;
}

.brand-copy {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.8);
  max-width: 660rpx;
}

.register-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 34rpx;
  padding: 34rpx 28rpx calc(34rpx + env(safe-area-inset-bottom));
  backdrop-filter: blur(18rpx);
  box-shadow: 0 22rpx 44rpx rgba(11, 18, 32, 0.18);
}

.card-head {
  margin-bottom: 24rpx;
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
  line-height: 1.6;
  color: #6b7687;
}

.avatar-panel {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 22rpx;
  margin-bottom: 20rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #eef4ff 0%, #f9fbff 100%);
}

.avatar-box {
  width: 120rpx;
  height: 120rpx;
  border-radius: 30rpx;
  overflow: hidden;
  background: #dfe8f7;
  flex-shrink: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #506072;
}

.avatar-placeholder-mark {
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1;
}

.avatar-placeholder-text {
  margin-top: 8rpx;
  font-size: 20rpx;
}

.avatar-copy {
  flex: 1;
}

.avatar-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #18243c;
}

.avatar-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #6c7889;
}

.avatar-clear {
  display: inline-block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #d94841;
  font-weight: 700;
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

.field-grid {
  display: flex;
  gap: 18rpx;
}

.compact-item {
  flex: 1;
}

.picker-shell {
  min-height: 54rpx;
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-text {
  font-size: 30rpx;
  color: #18243c;
}

.picker-placeholder {
  color: #9aa5b5;
}

.picker-arrow {
  font-size: 24rpx;
  color: #8090a3;
}

.register-btn {
  margin-top: 28rpx;
  width: 100%;
  height: 92rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #18243c 0%, #2b426e 100%);
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
}

.register-btn:disabled {
  opacity: 0.68;
}

.agreement-copy {
  margin-top: 22rpx;
  text-align: center;
  font-size: 20rpx;
  line-height: 1.7;
  color: #7a8596;
}

.card-footer {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
}

.footer-text {
  font-size: 22rpx;
  color: #7a8596;
}

.footer-link {
  font-size: 22rpx;
  color: #4f46e5;
  font-weight: 700;
}
</style>
