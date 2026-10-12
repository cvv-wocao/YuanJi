<template>
  <view class="order-page">
    <view class="nav-bar">
      <view class="nav-btn" @tap="goBack">
        <image src="/static/mall/back-button.png" class="nav-icon" />
      </view>
      <view class="nav-copy">
        <text class="nav-title">确认订单</text>
        <text class="nav-subtitle">检查地址、商品和结算信息</text>
      </view>
      <view class="nav-btn ghost-btn"></view>
    </view>

    <scroll-view class="order-scroll" scroll-y>
      <view class="panel address-panel" @tap="chooseAddress">
        <view class="panel-icon">址</view>
        <view class="panel-content" v-if="address.receiverName">
          <text class="panel-title">{{ address.receiverName }} {{ address.receiverPhone }}</text>
          <text class="panel-desc">{{ address.address }}</text>
        </view>
        <view class="panel-content" v-else>
          <text class="panel-title">请选择收货地址</text>
          <text class="panel-desc">确认配送信息后再提交订单</text>
        </view>
        <text class="panel-arrow">›</text>
      </view>

      <view class="panel order-panel">
        <view class="section-head">
          <text class="section-title">商品清单</text>
          <text class="section-subtitle">{{ orderItems.length }} 件商品</text>
        </view>
        <view class="order-item" v-for="item in orderItems" :key="item.id || item.cartId">
          <image :src="item.productImage" class="product-image" mode="aspectFill" />
          <view class="product-content">
            <text class="product-name">{{ item.productName }}</text>
            <text class="product-specification" v-if="item.specification">{{ item.specification }}</text>
            <view class="product-foot">
              <text class="product-price">¥{{ formatPrice(item.price) }}</text>
              <text class="product-quantity">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="panel coupon-panel" @tap="chooseCoupon">
        <view class="section-head inline-head">
          <view>
            <text class="section-title">优惠券</text>
            <text class="section-subtitle">选择可用优惠</text>
          </view>
          <text class="panel-arrow">›</text>
        </view>
        <text class="coupon-value">{{ selectedCoupon ? '已选择优惠券' : '暂未使用优惠券' }}</text>
      </view>

      <view class="panel remark-panel">
        <view class="section-head">
          <text class="section-title">订单备注</text>
          <text class="section-subtitle">可选</text>
        </view>
        <input
          type="text"
          class="remark-input"
          v-model="remark"
          placeholder="给商家留一句话，比如配送时间偏好"
        />
      </view>

      <view class="panel summary-panel">
        <view class="summary-row">
          <text class="summary-label">商品金额</text>
          <text class="summary-value">¥{{ formatPrice(goodsAmount) }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">运费</text>
          <text class="summary-value">¥{{ formatPrice(shippingFee) }}</text>
        </view>
        <view class="summary-row total-row">
          <text class="summary-total-label">合计</text>
          <text class="summary-total-price">¥{{ formatPrice(totalAmount) }}</text>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <view class="bottom-bar">
      <view class="total-copy">
        <text class="total-label">应付金额</text>
        <text class="total-price">¥{{ formatPrice(totalAmount) }}</text>
      </view>
      <view class="submit-btn" @tap="submitOrder">{{ loading ? '提交中...' : '提交订单' }}</view>
    </view>
  </view>
</template>

<script>
import { get } from '@/utils/request.js';
import { createOrder } from '@/api/order.js';

export default {
  data() {
    return {
      address: {},
      orderItems: [],
      shippingFee: 0,
      selectedCoupon: null,
      remark: '',
      loading: false
    };
  },
  computed: {
    goodsAmount() {
      return this.orderItems.reduce((total, item) => total + Number(item.price || 0) * Number(item.quantity || 0), 0);
    },
    totalAmount() {
      return this.goodsAmount + Number(this.shippingFee || 0);
    }
  },
  onLoad() {
    this.loadOrderData();
  },
  methods: {
    formatPrice(price) {
      const value = Number(price);
      return Number.isFinite(value) ? value.toFixed(2) : '0.00';
    },
    async loadOrderData() {
      try {
        const res = await get('/cart/list');
        if (res.success) {
          const cartData = res.data || [];
          this.orderItems = await Promise.all(
            cartData.map(async (item) => {
              try {
                const productRes = await get(`/product/detail/${item.productId}`);
                if (productRes.success && productRes.data) {
                  const product = productRes.data;
                  let images = [];
                  if (typeof product.images === 'string') {
                    try {
                      images = JSON.parse(product.images);
                    } catch (e) {
                      console.error('解析商品图片失败:', e);
                    }
                  } else if (Array.isArray(product.images)) {
                    images = product.images;
                  }
                  return {
                    ...item,
                    productImage: images[0] || '/static/mall/default-placeholder.png',
                    productName: product.name,
                    price: product.price
                  };
                }
              } catch (error) {
                console.error(`获取商品 ${item.productId} 详情失败:`, error);
              }
              return item;
            })
          );
        }
      } catch (error) {
        console.error('加载订单数据失败:', error);
        uni.showToast({ title: '加载订单数据失败', icon: 'none' });
      }
    },
    goBack() {
      uni.navigateBack();
    },
    chooseAddress() {
      uni.navigateTo({ url: '/pages/mall/address' });
    },
    chooseCoupon() {
      uni.navigateTo({ url: '/pages/mall/coupon' });
    },
    async submitOrder() {
      if (!this.address || !this.address.id) {
        uni.showToast({ title: '请选择收货地址', icon: 'none' });
        return;
      }
      if (this.orderItems.length === 0) {
        uni.showToast({ title: '购物车为空', icon: 'none' });
        return;
      }
      if (this.loading) return;

      const token = uni.getStorageSync('token');
      if (!token) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 1500);
        return;
      }

      this.loading = true;
      try {
        const orderData = {
          totalAmount: this.totalAmount,
          addressId: this.address.id,
          remark: this.remark || '',
          couponId: this.selectedCoupon ? this.selectedCoupon.id : null,
          items: this.orderItems.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price
          }))
        };
        const res = await createOrder(orderData);

        if (res.success && res.data) {
          const orderNo = res.data.orderNo || res.data.id;
          uni.showToast({ title: '订单创建成功', icon: 'success' });
          setTimeout(() => {
            uni.navigateTo({
              url: `/pages/mall/payment?orderNo=${orderNo}&amount=${this.totalAmount}`
            });
          }, 1200);
        } else {
          uni.showToast({ title: res.message || '创建订单失败', icon: 'none' });
        }
      } catch (error) {
        console.error('提交订单失败:', error);
        uni.showToast({ title: error.message || '提交订单失败', icon: 'none' });
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.order-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f8fc 0%, #eef3f9 100%);
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.nav-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: calc(22rpx + env(safe-area-inset-top)) 28rpx 18rpx;
  background: rgba(246, 248, 252, 0.94);
  backdrop-filter: blur(16rpx);
}

.nav-btn,
.ghost-btn {
  width: 82rpx;
  height: 82rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.88);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 14rpx 28rpx rgba(18, 35, 63, 0.06);
  flex-shrink: 0;
}

.ghost-btn {
  background: transparent;
  box-shadow: none;
}

.nav-icon {
  width: 32rpx;
  height: 32rpx;
}

.nav-copy {
  flex: 1;
  min-width: 0;
}

.nav-title {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
  color: #162643;
}

.nav-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #77859a;
}

.order-scroll {
  height: calc(100vh - 126rpx - env(safe-area-inset-top));
}

.panel,
.bottom-bar {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16rpx 34rpx rgba(18, 35, 63, 0.06);
}

.panel {
  margin: 18rpx 28rpx 0;
  padding: 22rpx;
  border-radius: 30rpx;
}

.address-panel {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.panel-icon {
  width: 84rpx;
  height: 84rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #dbe7fa 0%, #edf3ff 100%);
  color: #315f92;
  font-size: 30rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.panel-content {
  flex: 1;
  min-width: 0;
}

.panel-title {
  display: block;
  font-size: 28rpx;
  line-height: 1.45;
  font-weight: 800;
  color: #162643;
}

.panel-desc,
.section-subtitle,
.coupon-value {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.64;
  color: #7a8799;
}

.panel-arrow {
  font-size: 32rpx;
  color: #98a2b3;
  flex-shrink: 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.inline-head {
  align-items: flex-start;
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #162643;
}

.order-item {
  margin-top: 18rpx;
  display: flex;
  gap: 18rpx;
}

.product-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 22rpx;
  background: #eef3f9;
  flex-shrink: 0;
}

.product-content {
  flex: 1;
  min-width: 0;
}

.product-name {
  display: block;
  font-size: 28rpx;
  line-height: 1.46;
  font-weight: 700;
  color: #162643;
}

.product-specification {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #8692a3;
}

.product-foot {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-price,
.summary-total-price,
.total-price {
  font-size: 32rpx;
  font-weight: 800;
  color: #e35f46;
}

.product-quantity {
  font-size: 24rpx;
  color: #6f7d92;
}

.remark-input {
  margin-top: 16rpx;
  width: 100%;
  min-height: 84rpx;
  padding: 0 20rpx;
  border-radius: 22rpx;
  background: #f4f7fb;
  font-size: 24rpx;
  color: #162643;
  box-sizing: border-box;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 10rpx 0;
}

.summary-label,
.summary-value {
  font-size: 24rpx;
  color: #6f7d92;
}

.total-row {
  margin-top: 10rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(18, 35, 63, 0.06);
}

.summary-total-label {
  font-size: 26rpx;
  font-weight: 700;
  color: #162643;
}

.bottom-space {
  height: 36rpx;
}

.bottom-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  padding: 20rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.total-copy {
  min-width: 0;
}

.total-label {
  display: block;
  font-size: 22rpx;
  color: #7a8799;
}

.submit-btn {
  min-width: 236rpx;
  height: 86rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #18304d 0%, #315f92 100%);
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
