<template>
  <view class="profile-page">
    <scroll-view class="profile-scroll" scroll-y enable-back-to-top>
      <view class="hero-banner">
        <image class="hero-bg" src="/static/backgrounds/user.jpg" mode="aspectFill" />
        <view class="hero-mask"></view>

        <view class="hero-top" :style="{ paddingTop: statusBarHeight + 'px' }">
          <view class="hero-actions">
            <view class="hero-round menu-round" @click="openDrawer">
              <text>退</text>
            </view>
          </view>
        </view>

        <view class="hero-user">
          <view class="avatar-shell" @click="openEditModal">
            <image v-if="userInfo.avatar" :src="userInfo.avatar" mode="aspectFill" class="hero-avatar" />
            <view v-else class="hero-avatar avatar-fallback">
              <text>{{ getUserInitial(userInfo.name) }}</text>
            </view>
          </view>

          <view class="hero-meta">
            <view class="name-row">
              <text class="hero-name">{{ userInfo.name || '活力用户' }}</text>
            </view>
            <text class="hero-number">账号：{{ userInfo.number || '暂未设置' }}</text>
          </view>
        </view>
      </view>

      <view class="content-panel">
        <view class="summary-card">
          <view class="stats-row">
            <view class="stat-box">
              <text class="stat-value">{{ totalLikes }}</text>
              <text class="stat-label">获赞</text>
            </view>
            <view class="stat-box">
              <text class="stat-value">{{ totalPosts }}</text>
              <text class="stat-label">帖子</text>
            </view>
            <view class="stat-box" @click="goToFollowing">
              <text class="stat-value">{{ followStats.followingCount || 0 }}</text>
              <text class="stat-label">关注</text>
            </view>
            <view class="stat-box" @click="goToFollowers">
              <text class="stat-value">{{ followStats.followerCount || 0 }}</text>
              <text class="stat-label">粉丝</text>
            </view>
            <view class="edit-home-btn" @click="openEditModal">
              <text>编辑主页</text>
            </view>
          </view>

          <view class="mood-line">
            <text>{{ profileMood }}</text>
          </view>

          <view class="tag-row">
            <view v-for="tag in profileTags" :key="tag" class="tag-pill">
              <text>{{ tag }}</text>
            </view>
          </view>

          <view class="menu-grid">
            <view v-for="item in quickMenus" :key="item.key" class="menu-item" @click="handleQuickMenu(item.key)">
              <view class="menu-icon" :class="item.themeClass">
                <view v-if="item.key === 'orders'" class="icon-order">
                  <view class="icon-order-line line-top"></view>
                  <view class="icon-order-line line-mid"></view>
                  <view class="icon-order-line line-bottom"></view>
                </view>
                <view v-else-if="item.key === 'cart'" class="icon-cart">
                  <view class="icon-cart-handle"></view>
                  <view class="icon-cart-body"></view>
                  <view class="icon-cart-wheel wheel-left"></view>
                  <view class="icon-cart-wheel wheel-right"></view>
                </view>
                <view v-else-if="item.key === 'posts'" class="icon-post">
                  <view class="icon-post-head"></view>
                  <view class="icon-post-line line-1"></view>
                  <view class="icon-post-line line-2"></view>
                  <view class="icon-post-line line-3"></view>
                </view>
                <view v-else-if="item.key === 'coupon'" class="icon-coupon">
                  <view class="icon-coupon-core"></view>
                  <view class="icon-coupon-cut cut-left"></view>
                  <view class="icon-coupon-cut cut-right"></view>
                  <view class="icon-coupon-dash"></view>
                </view>
                <view v-else-if="item.key === 'address'" class="icon-address">
                  <view class="icon-address-pin"></view>
                  <view class="icon-address-hole"></view>
                  <view class="icon-address-base"></view>
                </view>
              </view>
              <text class="menu-label">{{ item.label }}</text>
            </view>
          </view>
        </view>

        <view class="tab-bar">
          <view v-for="tab in tabs" :key="tab.key" class="tab-item" :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key">
            <text>{{ tab.label }}</text>
          </view>
        </view>

        <view class="works-section">
          <view v-if="displayPosts.length" class="works-grid">
            <view v-for="post in displayPosts" :key="post.id" class="work-card" @click="goToPostDetail(post.id)">
              <view class="work-cover">
                <image v-if="post.cover" :src="post.cover" mode="widthFix" class="work-cover-image" />
                <image v-else-if="post.images && post.images.length" :src="post.images[0]" mode="widthFix"
                  class="work-cover-image" />
                <view v-else class="cover-fallback">
                  <text>{{ getCoverLabel(post) }}</text>
                </view>
              </view>

              <view class="work-body">
                <text class="work-title">{{ post.content || '未填写文案' }}</text>
                <text class="work-subtitle">{{ formatDate(post.createTime) }} · {{ post.likeCount || 0 }} 人互动</text>
              </view>

              <button class="delete-trigger" @click.stop="handleDeletePost(post.id)">删除</button>
            </view>
          </view>

          <view v-else class="empty-state">
            <view class="empty-cover">
              <text>{{ activeTabLabel }}</text>
            </view>
            <text class="empty-title">{{ emptyStateTitle }}</text>
            <text class="empty-desc">{{ emptyStateDesc }}</text>
          </view>

          <button v-if="showPublishButton" class="publish-btn" @click="goToHome">
            <text class="publish-icon">相机</text>
            <text>去发布</text>
          </button>
        </view>
      </view>
    </scroll-view>

    <view v-if="showDrawer" class="drawer-mask" @click="closeDrawer">
      <view class="drawer-panel" @click.stop>
        <view class="drawer-head">
          <text class="drawer-title">快捷功能</text>
          <view class="drawer-close" @click="closeDrawer">×</view>
        </view>

        <view class="drawer-user">
          <view class="drawer-avatar">
            <image v-if="userInfo.avatar" :src="userInfo.avatar" mode="aspectFill" class="drawer-avatar-image" />
            <view v-else class="drawer-avatar-image avatar-fallback">
              <text>{{ getUserInitial(userInfo.name) }}</text>
            </view>
          </view>
          <view class="drawer-user-copy">
            <text class="drawer-user-name">{{ userInfo.name || '活力用户' }}</text>
            <text class="drawer-user-number">账号 {{ userInfo.number || '未设置' }}</text>
          </view>
        </view>

        <view class="drawer-menu">
          <view v-for="item in drawerMenus" :key="item.key" class="drawer-item" @click="handleDrawerAction(item.key)">
            <view class="drawer-item-main">
              <view class="drawer-item-icon" :class="item.themeClass">{{ item.icon }}</view>
              <view class="drawer-item-copy">
                <text class="drawer-item-title">{{ item.label }}</text>
                <text class="drawer-item-desc">{{ item.desc }}</text>
              </view>
            </view>
            <text class="drawer-item-arrow">›</text>
          </view>
        </view>

        <view class="drawer-footer">
          <button class="drawer-logout" @click="handleLogout">退出登录</button>
        </view>
      </view>
    </view>

    <view v-if="showEditModal" class="modal-mask" @click="closeEditModal">
      <view class="modal-panel" @click.stop>
        <view class="modal-head">
          <view>
            <text class="modal-title">编辑资料</text>
            <text class="modal-subtitle">更新头像、昵称和基础信息</text>
          </view>
          <view class="modal-close" @click="closeEditModal">
            <text>关闭</text>
          </view>
        </view>

        <scroll-view class="modal-scroll" scroll-y>
          <view class="avatar-editor" @click="chooseAvatar">
            <image v-if="avatarPreview" :src="avatarPreview" mode="aspectFill" class="edit-avatar" />
            <view v-else class="edit-avatar avatar-fallback">
              <text>{{ getUserInitial(editForm.name) }}</text>
            </view>
            <text class="avatar-editor-tip">更换头像</text>
          </view>

          <view class="form-list">
            <view class="form-item">
              <text class="form-label">昵称</text>
              <input v-model="editForm.name" class="form-input" placeholder="输入昵称" />
            </view>

            <view class="form-item">
              <text class="form-label">手机号</text>
              <input v-model="editForm.number" class="form-input" type="number" placeholder="输入手机号" />
            </view>

            <view class="form-item">
              <text class="form-label">邮箱</text>
              <input v-model="editForm.email" class="form-input" type="text" placeholder="输入邮箱" />
            </view>

            <view class="form-row">
              <view class="form-item half">
                <text class="form-label">年龄</text>
                <input v-model="editForm.age" class="form-input" type="number" placeholder="例如 21" />
              </view>

              <view class="form-item half">
                <text class="form-label">性别</text>
                <picker :range="sexOptions" @change="onSexChange">
                  <view class="picker-input">{{ editForm.sex || '请选择' }}</view>
                </picker>
              </view>
            </view>

            <view class="form-item">
              <text class="form-label">密码</text>
              <input v-model="editForm.password" class="form-input" password placeholder="如需修改可重新填写" />
            </view>
          </view>
        </scroll-view>

        <view class="modal-foot">
          <button class="ghost-btn modal-btn" @click="closeEditModal">取消</button>
          <button class="save-btn modal-btn" :disabled="saving" @click="saveProfile">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getUserInfo, updateUserWithAvatar } from '@/api/user.js';
import { getMyFollowStats } from '@/api/follow.js';
import { deletePost, getMyPosts } from '@/api/post.js';

const createEmptyUser = () => ({
  id: '',
  name: '',
  number: '',
  email: '',
  age: '',
  sex: '',
  avatar: ''
});

export default {
  data() {
    return {
      statusBarHeight: 20,
      userInfo: createEmptyUser(),
      followStats: {
        followingCount: 0,
        followerCount: 0
      },
      userPosts: [],
      totalPosts: 0,
      totalLikes: 0,
      showDrawer: false,
      showEditModal: false,
      editForm: {
        name: '',
        number: '',
        email: '',
        age: '',
        sex: '',
        password: ''
      },
      avatarFile: '',
      avatarPreview: '',
      saving: false,
      sexOptions: ['男', '女', '保密'],
      activeTab: 'posts',
      tabs: [
        { key: 'posts', label: '帖子' },
        { key: 'favorites', label: '收藏' },
        { key: 'likes', label: '喜欢' }
      ],
      quickMenus: [
        { key: 'orders', label: '我的订单', themeClass: 'theme-violet' },
        { key: 'cart', label: '购物车', themeClass: 'theme-teal' },
        { key: 'coupon', label: '优惠券', themeClass: 'theme-orange' },
        { key: 'address', label: '地址管理', themeClass: 'theme-pink' }
      ],
      drawerMenus: [
        { key: 'edit', label: '编辑主页', desc: '修改头像和基础资料', icon: '编', themeClass: 'theme-violet' },
        { key: 'orders', label: '我的订单', desc: '查看订单和售后进度', icon: '单', themeClass: 'theme-blue' },
        { key: 'cart', label: '购物车', desc: '继续整理待结算商品', icon: '车', themeClass: 'theme-teal' },
        { key: 'coupon', label: '优惠券', desc: '查看当前可用优惠', icon: '券', themeClass: 'theme-orange' },
        { key: 'address', label: '地址管理', desc: '维护常用收货地址', icon: '址', themeClass: 'theme-pink' },
        { key: 'home', label: '回到首页', desc: '返回发现页继续浏览', icon: '回', themeClass: 'theme-blue' }
      ]
    };
  },
  computed: {
    profileMood() {
      return this.userInfo.email || '保持更新，让你的主页更有内容感。';
    },
    profileTags() {
      const tags = [];
      if (this.userInfo.sex) tags.push(this.userInfo.sex);
      if (this.userInfo.age) tags.push(`${this.userInfo.age} 岁`);
      if (this.totalPosts) tags.push(`${this.totalPosts} 个帖子`);
      if (!tags.length) {
        tags.push('添加性别等标签');
      }
      return tags;
    },
    activeTabLabel() {
      const current = this.tabs.find((item) => item.key === this.activeTab);
      return current ? current.label : '帖子';
    },
    emptyStateTitle() {
      if (this.activeTab === 'posts') {
        return '还没有帖子';
      }
      return `${this.activeTabLabel}功能待开发`;
    },
    emptyStateDesc() {
      if (this.activeTab === 'posts') {
        return '去首页发布第一条帖子，让个人主页更完整。';
      }
      return `当前${this.activeTabLabel}模块暂未接入接口，后续开发完成后会在这里展示。`;
    },
    displayPosts() {
      if (this.activeTab === 'posts') {
        return this.userPosts;
      }
      return [];
    },
    showPublishButton() {
      return this.activeTab === 'posts' && !this.displayPosts.length;
    }
  },
  onLoad() {
    this.initPageMetrics();
    this.loadUserData();
  },
  onShow() {
    this.fetchFollowStats();
    this.fetchUserPosts();
  },
  methods: {
    initPageMetrics() {
      const systemInfo = uni.getSystemInfoSync();
      this.statusBarHeight = systemInfo.statusBarHeight || 20;
    },
    async loadUserData() {
      try {
        const result = await getUserInfo();
        if (result.success && result.data) {
          this.userInfo = {
            ...createEmptyUser(),
            ...result.data
          };
          uni.setStorageSync('userName', this.userInfo.name || '');
          uni.setStorageSync('userId', this.userInfo.id || '');
          uni.setStorageSync('userInfo', this.userInfo);
        }
      } catch (error) {
        console.error('loadUserData error:', error);
      } finally {
        await Promise.allSettled([this.fetchFollowStats(), this.fetchUserPosts()]);
      }
    },
    async fetchFollowStats() {
      try {
        const result = await getMyFollowStats();
        if (result.success && result.data) {
          this.followStats = {
            followingCount: result.data.followingCount || 0,
            followerCount: result.data.followerCount || result.data.followersCount || 0
          };
        }
      } catch (error) {
        console.error('fetchFollowStats error:', error);
      }
    },
    async fetchUserPosts() {
      try {
        const result = await getMyPosts();
        if (result.success) {
          const data = result.data || {};
          const posts = Array.isArray(data.posts) ? data.posts : Array.isArray(data) ? data : [];
          this.userPosts = posts;
          this.totalPosts = typeof data.totalPosts === 'number' ? data.totalPosts : posts.length;
          this.totalLikes = typeof data.totalLikes === 'number'
            ? data.totalLikes
            : posts.reduce((sum, item) => sum + (item.likeCount || 0), 0);
        }
      } catch (error) {
        console.error('fetchUserPosts error:', error);
      }
    },
    getUserInitial(name) {
      return (name || 'U').slice(0, 1).toUpperCase();
    },
    getCoverLabel(post) {
      if (post.videos && post.videos.length) return '视频';
      if (post.images && post.images.length) return '图片';
      return '动态';
    },
    formatDate(value) {
      if (!value) return '刚刚';
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return '刚刚';
      const month = `${date.getMonth() + 1}`.padStart(2, '0');
      const day = `${date.getDate()}`.padStart(2, '0');
      return `${month}-${day}`;
    },
    openDrawer() {
      this.showDrawer = true;
    },
    closeDrawer() {
      this.showDrawer = false;
    },
    handleDrawerAction(key) {
      this.closeDrawer();
      const actions = {
        edit: this.openEditModal,
        orders: this.goToOrders,
        cart: this.goToCart,
        coupon: this.goToCoupons,
        address: this.goToAddress,
        home: this.goToHome
      };
      if (actions[key]) {
        actions[key]();
      }
    },
    openEditModal() {
      this.closeDrawer();
      this.editForm = {
        name: this.userInfo.name || '',
        number: this.userInfo.number || '',
        email: this.userInfo.email || '',
        age: this.userInfo.age || '',
        sex: this.userInfo.sex || '',
        password: ''
      };
      this.avatarFile = '';
      this.avatarPreview = this.userInfo.avatar || '';
      this.showEditModal = true;
    },
    closeEditModal() {
      this.showEditModal = false;
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          const file = res.tempFilePaths && res.tempFilePaths[0] ? res.tempFilePaths[0] : '';
          if (file) {
            this.avatarFile = file;
            this.avatarPreview = file;
          }
        }
      });
    },
    onSexChange(event) {
      const index = Number(event.detail.value || 0);
      this.editForm.sex = this.sexOptions[index] || '';
    },
    async saveProfile() {
      if (this.saving) return;
      this.saving = true;

      try {
        const result = await updateUserWithAvatar(this.editForm, this.avatarFile);
        if (result.success) {
          uni.showToast({
            title: '资料已更新',
            icon: 'success'
          });
          this.closeEditModal();
          await this.loadUserData();
        }
      } catch (error) {
        console.error('saveProfile error:', error);
        uni.showToast({
          title: '保存失败',
          icon: 'none'
        });
      } finally {
        this.saving = false;
      }
    },
    handleLogout() {
      this.closeDrawer();
      uni.showModal({
        title: '退出登录',
        content: '确认退出当前账号吗？',
        success: (res) => {
          if (!res.confirm) return;
          uni.removeStorageSync('token');
          uni.removeStorageSync('isLoggedIn');
          uni.removeStorageSync('userInfo');
          uni.reLaunch({
            url: '/pages/login/login'
          });
        }
      });
    },
    async handleDeletePost(postId) {
      uni.showModal({
        title: '删除动态',
        content: '删除后无法恢复，确定继续吗？',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            const result = await deletePost(postId);
            if (result.success) {
              this.userPosts = this.userPosts.filter((item) => item.id !== postId);
              this.totalPosts = this.userPosts.length;
              this.totalLikes = this.userPosts.reduce((sum, item) => sum + (item.likeCount || 0), 0);
              uni.showToast({
                title: '已删除',
                icon: 'success'
              });
            }
          } catch (error) {
            console.error('handleDeletePost error:', error);
            uni.showToast({
              title: '删除失败',
              icon: 'none'
            });
          }
        }
      });
    },
    handleQuickMenu(key) {
      const actions = {
        orders: this.goToOrders,
        cart: this.goToCart,
        coupon: this.goToCoupons,
        address: this.goToAddress
      };
      if (actions[key]) {
        actions[key]();
      }
    },
    goToFollowing() {
      uni.navigateTo({
        url: '/pages/follow/follow?type=following'
      });
    },
    goToFollowers() {
      uni.navigateTo({
        url: '/pages/follow/follow?type=followers'
      });
    },
    goToOrders() {
      uni.navigateTo({
        url: '/pages/mall/order-list'
      });
    },
    goToCart() {
      uni.navigateTo({
        url: '/pages/mall/cart'
      });
    },
    goToCoupons() {
      uni.navigateTo({
        url: '/pages/mall/coupon'
      });
    },
    goToAddress() {
      uni.navigateTo({
        url: '/pages/mall/address'
      });
    },
    goToHome() {
      uni.switchTab({
        url: '/pages/home/home'
      });
    },
    goToPostDetail(postId) {
      uni.navigateTo({
        url: `/pages/post/post?id=${postId}`
      });
    }
  }
};
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f4f6fb;
}

.profile-scroll {
  min-height: 100vh;
}

.hero-banner {
  position: relative;
  height: 520rpx;
  overflow: hidden;
}

.hero-bg,
.hero-mask {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-mask {
  background:
    linear-gradient(180deg, rgba(19, 72, 132, 0.22) 0%, rgba(20, 105, 182, 0.46) 58%, rgba(20, 105, 182, 0.78) 100%),
    radial-gradient(circle at left top, rgba(255, 255, 255, 0.26), transparent 32%),
    radial-gradient(circle at right top, rgba(164, 243, 255, 0.2), transparent 30%);
}

.hero-top {
  position: relative;
  z-index: 2;
  padding-left: 32rpx;
  padding-right: 32rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.hero-actions {
  display: flex;
  gap: 18rpx;
}

.hero-round {
  width: 74rpx;
  height: 74rpx;
  border-radius: 50%;
  background: rgba(56, 88, 124, 0.52);
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10rpx);
}

.menu-round {
  color: transparent;
  font-size: 0;
  position: relative;
}

.menu-round::before {
  content: '';
  width: 28rpx;
  height: 4rpx;
  border-radius: 999rpx;
  background: #ffffff;
  box-shadow: 0 -10rpx 0 #ffffff, 0 10rpx 0 #ffffff;
}

.hero-user {
  position: absolute;
  left: 32rpx;
  right: 32rpx;
  bottom: 50rpx;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  gap: 28rpx;
}

.avatar-shell {
  position: relative;
  width: 172rpx;
  height: 172rpx;
  border-radius: 50%;
  border: 8rpx solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 18rpx 36rpx rgba(0, 0, 0, 0.16);
  overflow: visible;
  flex-shrink: 0;
}

.hero-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #18243c 0%, #1f7dd8 100%);
  color: #ffffff;
  font-size: 56rpx;
  font-weight: 800;
}

.hero-meta {
  padding-bottom: 10rpx;
  color: #ffffff;
}

.name-row {
  display: flex;
  align-items: center;
}

.hero-name {
  font-size: 72rpx;
  line-height: 1;
  font-weight: 800;
}

.hero-number {
  display: block;
  margin-top: 18rpx;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.82);
}

.content-panel {
  position: relative;
  margin-top: -28rpx;
  min-height: calc(100vh - 420rpx);
  background: #ffffff;
  border-radius: 40rpx 40rpx 0 0;
  box-shadow: 0 -10rpx 30rpx rgba(19, 72, 132, 0.08);
  padding: 24rpx 0 calc(112rpx + env(safe-area-inset-bottom));
}

.summary-card,
.works-section {
  padding-left: 32rpx;
  padding-right: 32rpx;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) 260rpx;
  gap: 10rpx;
  align-items: center;
}

.stat-box {
  min-height: 104rpx;
  border-radius: 18rpx;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rpx 0;
}

.stat-value {
  font-size: 32rpx;
  color: #18243c;
  font-weight: 800;
}

.stat-label {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #67748a;
}

.edit-home-btn {
  height: 78rpx;
  width: 100%;
  min-width: 0;
  margin: 0;
  border-radius: 12rpx;
  background: linear-gradient(180deg, #f8fafd 0%, #eef2f7 100%);
  color: #172033;
  font-size: 28rpx;
  font-weight: 600;
  border: 1rpx solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  line-height: 1;
  padding: 0;
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.9);
  align-self: center;
  justify-self: stretch;
}

.edit-home-btn text {
  display: block;
  width: 100%;
  text-align: center;
}

.edit-home-btn::after,
.delete-trigger::after,
.ghost-btn::after,
.save-btn::after {
  border: 0;
}

.mood-line {
  margin-top: 26rpx;
  min-height: 38rpx;
  font-size: 32rpx;
  color: #172033;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 18rpx;
}

.tag-pill {
  min-height: 52rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  background: #f5f6f9;
  color: #6b7280;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-grid {
  margin-top: 42rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.menu-icon {
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 22rpx rgba(17, 24, 39, 0.12);
}

.icon-order,
.icon-cart,
.icon-post,
.icon-coupon,
.icon-address {
  position: relative;
  width: 40rpx;
  height: 40rpx;
}

.icon-order-line {
  position: absolute;
  left: 6rpx;
  right: 6rpx;
  height: 4rpx;
  border-radius: 999rpx;
  background: #ffffff;
}

.icon-order .line-top {
  top: 8rpx;
}

.icon-order .line-mid {
  top: 18rpx;
  right: 12rpx;
}

.icon-order .line-bottom {
  top: 28rpx;
  right: 18rpx;
}

.icon-cart-handle {
  position: absolute;
  left: 4rpx;
  top: 7rpx;
  width: 12rpx;
  height: 4rpx;
  border-radius: 999rpx;
  background: #ffffff;
  transform: rotate(-18deg);
  transform-origin: left center;
}

.icon-cart-body {
  position: absolute;
  left: 10rpx;
  top: 12rpx;
  width: 22rpx;
  height: 14rpx;
  border: 3rpx solid #ffffff;
  border-top: 0;
  border-radius: 0 0 8rpx 8rpx;
}

.icon-cart-wheel {
  position: absolute;
  bottom: 3rpx;
  width: 7rpx;
  height: 7rpx;
  border-radius: 50%;
  background: #ffffff;
}

.icon-cart .wheel-left {
  left: 12rpx;
}

.icon-cart .wheel-right {
  right: 8rpx;
}

.icon-post-head {
  position: absolute;
  left: 7rpx;
  top: 6rpx;
  width: 26rpx;
  height: 28rpx;
  border: 3rpx solid #ffffff;
  border-radius: 8rpx;
}

.icon-post-line {
  position: absolute;
  left: 13rpx;
  right: 13rpx;
  height: 3rpx;
  border-radius: 999rpx;
  background: #ffffff;
}

.icon-post .line-1 {
  top: 14rpx;
}

.icon-post .line-2 {
  top: 21rpx;
}

.icon-post .line-3 {
  top: 28rpx;
  right: 17rpx;
}

.icon-coupon-core {
  position: absolute;
  left: 6rpx;
  top: 11rpx;
  width: 28rpx;
  height: 18rpx;
  border-radius: 8rpx;
  background: #ffffff;
}

.icon-coupon-cut {
  position: absolute;
  top: 17rpx;
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: currentColor;
}

.icon-coupon .cut-left {
  left: 2rpx;
}

.icon-coupon .cut-right {
  right: 2rpx;
}

.icon-coupon-dash {
  position: absolute;
  left: 19rpx;
  top: 14rpx;
  width: 3rpx;
  height: 12rpx;
  border-radius: 999rpx;
  background: currentColor;
}

.icon-address-pin {
  position: absolute;
  left: 10rpx;
  top: 4rpx;
  width: 20rpx;
  height: 26rpx;
  border-radius: 50% 50% 50% 50% / 46% 46% 58% 58%;
  background: #ffffff;
}

.icon-address-hole {
  position: absolute;
  left: 17rpx;
  top: 11rpx;
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: currentColor;
}

.icon-address-base {
  position: absolute;
  left: 18rpx;
  bottom: 2rpx;
  width: 4rpx;
  height: 12rpx;
  border-radius: 999rpx;
  background: #ffffff;
}

.theme-violet {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  color: #ffffff;
}

.theme-teal {
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  color: #ffffff;
}

.theme-blue {
  background: linear-gradient(135deg, #0f6fd8 0%, #60a5fa 100%);
  color: #ffffff;
}

.theme-orange {
  background: linear-gradient(135deg, #f97316 0%, #f59e0b 100%);
  color: #ffffff;
}

.theme-pink {
  background: linear-gradient(135deg, #ec4899 0%, #fb7185 100%);
  color: #ffffff;
}

.menu-label {
  font-size: 22rpx;
  color: #172033;
  text-align: center;
  line-height: 1.3;
}

.tab-bar {
  margin: 30rpx 32rpx 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  border: none;
  border-bottom: 1rpx solid #edf0f5;
}

.tab-item {
  position: relative;
  height: 72rpx;
  border-radius: 0;
  color: #8b95a6;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.tab-item.active {
  color: #172033;
  font-weight: 700;
  background: transparent;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 38rpx;
  right: 38rpx;
  bottom: 0;
  height: 5rpx;
  border-radius: 999rpx;
  background: #172033;
}

.works-section {
  padding-top: 30rpx;
}

.works-grid {
  column-count: 2;
  column-gap: 20rpx;
}

.work-card {
  position: relative;
  display: inline-block;
  width: 100%;
  margin-bottom: 20rpx;
  break-inside: avoid;
}

.work-cover {
  width: 100%;
  border-radius: 22rpx;
  overflow: hidden;
  background: #f3f6fa;
}

.work-cover-image {
  width: 100%;
  display: block;
  border-radius: 22rpx;
}

.cover-fallback {
  width: 100%;
  min-height: 260rpx;
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  color: #1d4ed8;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.work-body {
  padding: 18rpx 8rpx 0;
}

.work-title {
  display: block;
  font-size: 28rpx;
  line-height: 1.4;
  color: #172033;
  font-weight: 700;
}

.work-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.delete-trigger {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  min-width: 84rpx;
  height: 48rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.66);
  color: #ffffff;
  font-size: 20rpx;
  border: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  padding: 50rpx 0 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-cover {
  width: 260rpx;
  height: 260rpx;
  border-radius: 22rpx;
  background: #f4f6fb;
  color: #7b8798;
  font-size: 34rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  margin-top: 28rpx;
  font-size: 40rpx;
  color: #172033;
  font-weight: 700;
}

.empty-desc {
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #97a3b3;
  text-align: center;
}

.publish-btn {
  margin: 44rpx auto 0;
  width: 360rpx;
  height: 98rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #ff2f67 0%, #ff2d55 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 800;
  border: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  box-shadow: 0 20rpx 30rpx rgba(255, 45, 85, 0.2);
}

.publish-btn::after {
  border: 0;
}

.publish-icon {
  font-size: 24rpx;
}

.works-section {
  padding-bottom: calc(28rpx + env(safe-area-inset-bottom));
}

.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  z-index: 29;
  display: flex;
  justify-content: flex-end;
  animation: drawerMaskFadeIn 0.22s ease;
}

.drawer-panel {
  width: 560rpx;
  height: 100%;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: -24rpx 0 48rpx rgba(15, 23, 42, 0.12);
  padding: calc(34rpx + env(safe-area-inset-top)) 26rpx calc(128rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: drawerPanelSlideIn 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes drawerMaskFadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes drawerPanelSlideIn {
  from {
    opacity: 0.7;
    transform: translateX(100%);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.drawer-title {
  font-size: 34rpx;
  font-weight: 800;
  color: #18243c;
}

.drawer-close {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  background: #f4f7fb;
  color: #607086;
  font-size: 34rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drawer-user {
  margin-top: 26rpx;
  padding: 22rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #eff5ff 0%, #f6fbff 100%);
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.drawer-avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.drawer-avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.drawer-user-copy {
  flex: 1;
  min-width: 0;
}

.drawer-user-name {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: #172033;
}

.drawer-user-number {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7b8798;
}

.drawer-menu {
  margin-top: 26rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-bottom: 10rpx;
}

.drawer-item {
  padding: 18rpx;
  border-radius: 24rpx;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.drawer-item-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.drawer-item-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.drawer-item-copy {
  min-width: 0;
}

.drawer-item-title {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  color: #172033;
}

.drawer-item-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #8b95a6;
  line-height: 1.5;
}

.drawer-item-arrow {
  flex-shrink: 0;
  font-size: 30rpx;
  color: #9aa5b5;
}

.drawer-footer {
  margin-top: 18rpx;
  padding-top: 22rpx;
  flex-shrink: 0;
}

.drawer-logout {
  width: 100%;
  height: 88rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #172033 0%, #365889 100%);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 800;
  border: 0;
}

.drawer-logout::after {
  border: 0;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: calc(28rpx + env(safe-area-inset-top)) 24rpx calc(28rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.modal-panel {
  width: 100%;
  max-width: 690rpx;
  max-height: calc(100vh - 180rpx - env(safe-area-inset-top) - env(safe-area-inset-bottom));
  border-radius: 28rpx;
  background: #ffffff;
  padding: 22rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24rpx 48rpx rgba(15, 23, 42, 0.16);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}

.modal-title {
  font-size: 32rpx;
  color: #18243c;
  font-weight: 800;
}

.modal-subtitle {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #7b8798;
}

.modal-close {
  min-width: 84rpx;
  height: 48rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: #f0f4fc;
  color: #344256;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-scroll {
  flex: 1;
  min-height: 0;
  margin-top: 18rpx;
}

.avatar-editor {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 18rpx;
}

.edit-avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
}

.avatar-editor-tip {
  font-size: 22rpx;
  color: #4f46e5;
  font-weight: 700;
}

.form-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
}

.form-item {
  padding: 18rpx;
  border-radius: 18rpx;
  background: #f4f7fd;
}

.form-label {
  display: block;
  margin-bottom: 8rpx;
  font-size: 20rpx;
  color: #67748a;
}

.form-input,
.picker-input {
  min-height: 42rpx;
  font-size: 26rpx;
  color: #18243c;
}

.modal-foot {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin-top: 16rpx;
  padding-top: 2rpx;
  flex-shrink: 0;
}

.ghost-btn,
.save-btn {
  width: 100%;
  height: 74rpx;
  border-radius: 18rpx;
  border: 0;
  font-size: 26rpx;
  font-weight: 700;
}

.ghost-btn {
  background: #f1f5f9;
  color: #344256;
}

.save-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  color: #ffffff;
}

.save-btn[disabled] {
  opacity: 0.72;
}
</style>
