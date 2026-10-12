<template>
  <view class="tab-bar">
    <view
      v-for="(item, index) in list"
      :key="item.pagePath"
      class="tab-item"
      :class="{ active: current === index }"
      @click="switchTab(item, index)"
    >
      <view class="tab-icon" :class="item.themeClass">
        <text>{{ item.shortLabel }}</text>
      </view>
      <text class="tab-text">{{ item.text }}</text>
      <view v-if="item.hasBadge" class="tab-badge"></view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      current: 0,
      list: [
        {
          pagePath: '/pages/home/home',
          text: '首页',
          shortLabel: '首',
          themeClass: 'theme-home',
          hasBadge: false
        },
        {
          pagePath: '/pages/mall/index',
          text: '商城',
          shortLabel: '购',
          themeClass: 'theme-mall',
          hasBadge: false
        },
        {
          pagePath: '/pages/sports/sports',
          text: '运动',
          shortLabel: '动',
          themeClass: 'theme-sports',
          hasBadge: false
        },
        {
          pagePath: '/pages/message/message',
          text: '消息',
          shortLabel: '聊',
          themeClass: 'theme-message',
          hasBadge: true
        },
        {
          pagePath: '/pages/profile/profile',
          text: '我的',
          shortLabel: '我',
          themeClass: 'theme-profile',
          hasBadge: false
        }
      ]
    };
  },
  created() {
    this.syncCurrentTab();
  },
  methods: {
    syncCurrentTab() {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const route = currentPage?.route ? `/${currentPage.route}` : '';
      const targetIndex = this.list.findIndex((item) => item.pagePath === route);
      this.current = targetIndex === -1 ? 0 : targetIndex;
    },
    switchTab(item, index) {
      if (this.current === index) return;
      uni.switchTab({
        url: item.pagePath
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.tab-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(18rpx + env(safe-area-inset-bottom));
  z-index: 999;
  height: 110rpx;
  padding: 12rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18rpx);
  box-shadow: 0 20rpx 42rpx rgba(49, 68, 100, 0.14);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8rpx;
}

.tab-item {
  position: relative;
  border-radius: 999rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.tab-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 18rpx;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: scale(0.92);
  opacity: 0.72;
  transition: all 0.2s ease;
}

.tab-text {
  font-size: 20rpx;
  color: #7b8798;
  font-weight: 600;
  transition: all 0.2s ease;
}

.tab-item.active {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1) 0%, rgba(20, 184, 166, 0.08) 100%);
}

.tab-item.active .tab-icon {
  transform: scale(1);
  opacity: 1;
}

.tab-item.active .tab-text {
  color: #18243c;
}

.tab-badge {
  position: absolute;
  top: 16rpx;
  right: 24rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 4rpx rgba(255, 255, 255, 0.95);
}

.theme-home {
  background: linear-gradient(135deg, #4f46e5 0%, #818cf8 100%);
}

.theme-mall {
  background: linear-gradient(135deg, #ea580c 0%, #fb923c 100%);
}

.theme-sports {
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
}

.theme-message {
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
}

.theme-profile {
  background: linear-gradient(135deg, #be185d 0%, #fb7185 100%);
}
</style>
