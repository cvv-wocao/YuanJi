<template>
  <view class="coupon-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">我的优惠券</text>
      <view class="nav-right"></view>
    </view>
    
    <view class="coupon-tabs">
      <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">
        <text>可用优惠券</text>
      </view>
      <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">
        <text>已使用</text>
      </view>
      <view class="tab-item" :class="{ active: currentTab === 2 }" @click="currentTab = 2">
        <text>已过期</text>
      </view>
    </view>
    
    <view class="coupon-list">
      <view class="coupon-item" v-for="coupon in filteredCoupons" :key="coupon.id">
        <view class="coupon-left">
          <text class="coupon-amount">¥{{ coupon.amount }}</text>
          <text class="coupon-condition">满{{ coupon.minAmount }}可用</text>
        </view>
        <view class="coupon-right">
          <text class="coupon-name">{{ coupon.name }}</text>
          <text class="coupon-time">有效期至 {{ coupon.expireTime }}</text>
          <view class="coupon-btn" :class="{ disabled: currentTab !== 0 }" @click="useCoupon(coupon)">
            <text>{{ currentTab === 0 ? '立即使用' : (currentTab === 1 ? '已使用' : '已过期') }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <view class="empty-coupon" v-if="filteredCoupons.length === 0">
      <image src="https://picsum.photos/80/80?text=🎫" class="empty-icon" />
      <text class="empty-text">暂无优惠券</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 0,
      coupons: [
        {
          id: 1,
          name: '满100减10',
          amount: 10,
          minAmount: 100,
          expireTime: '2024-12-31',
          status: 0
        },
        {
          id: 2,
          name: '满200减30',
          amount: 30,
          minAmount: 200,
          expireTime: '2024-12-31',
          status: 0
        },
        {
          id: 3,
          name: '满50减5',
          amount: 5,
          minAmount: 50,
          expireTime: '2024-10-01',
          status: 1
        }
      ]
    };
  },
  computed: {
    filteredCoupons() {
      return this.coupons.filter(coupon => coupon.status === this.currentTab);
    }
  },
  methods: {
    goBack() {
      uni.navigateBack({
        delta: 1
      });
    },
    useCoupon(coupon) {
      if (this.currentTab === 0) {
        uni.navigateBack({
          delta: 1
        });
      }
    }
  }
};
</script>

<style scoped>
.coupon-container {
  padding-bottom: calc(60px + env(safe-area-inset-bottom));
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: white;
  padding: 10px 15px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  width: 24px;
  height: 24px;
}

.nav-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.nav-right {
  width: 40px;
}

.coupon-tabs {
  display: flex;
  background-color: white;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 44px;
  z-index: 99;
}

.tab-item {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #666;
  padding: 10px 0;
}

.tab-item.active {
  color: #1890ff;
  border-bottom: 2px solid #1890ff;
}

.coupon-list {
  padding: 15px;
}

.coupon-item {
  display: flex;
  background-color: white;
  border-radius: 8px;
  margin-bottom: 15px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.coupon-left {
  width: 100px;
  background-color: #ff4d4f;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 15px;
}

.coupon-amount {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

.coupon-condition {
  font-size: 12px;
  color: white;
  margin-top: 5px;
}

.coupon-right {
  flex: 1;
  padding: 15px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.coupon-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.coupon-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.coupon-btn {
  width: 80px;
  height: 30px;
  line-height: 30px;
  text-align: center;
  background-color: #ff4d4f;
  color: white;
  border-radius: 15px;
  font-size: 12px;
  margin-top: 10px;
}

.coupon-btn.disabled {
  background-color: #ccc;
}

.empty-coupon {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

.empty-text {
  font-size: 14px;
  color: #999;
}
</style>
