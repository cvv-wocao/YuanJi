<template>
  <view class="cart-page">
    <view class="nav-bar">
      <view class="nav-btn" @tap="goBack">
        <image src="/static/mall/back-button.png" class="nav-icon" />
      </view>
      <view class="nav-copy">
        <text class="nav-title">购物车</text>
        <text class="nav-subtitle">整理准备结算的商品</text>
      </view>
      <view class="nav-action" @tap="clearCart" v-if="cartItems.length > 0">
        <text class="nav-action-text">清空</text>
      </view>
      <view v-else class="nav-action ghost-action"></view>
    </view>

    <scroll-view class="cart-scroll" scroll-y>
      <view v-if="cartItems.length > 0" class="summary-strip">
        <view class="summary-card">
          <text class="summary-label">已选商品</text>
          <text class="summary-value">{{ cartItems.length }}</text>
        </view>
        <view class="summary-card">
          <text class="summary-label">预计总额</text>
          <text class="summary-value">¥{{ totalPrice }}</text>
        </view>
      </view>

      <view v-if="cartItems.length > 0" class="cart-list">
        <view class="cart-card" v-for="item in cartItems" :key="item.id || item.cartId">
          <view class="cart-main">
            <image
              :src="item.productImage || item.thumbnail || item.image || '/static/mall/default-placeholder.png'"
              class="product-image"
              mode="aspectFill"
            />
            <view class="product-content">
              <text class="product-name">{{ item.productName || item.name || item.product_name }}</text>
              <text class="product-specification" v-if="item.specification">{{ item.specification }}</text>
              <view class="product-meta">
                <text class="product-price">¥{{ formatPrice(item.price || item.productPrice || item.product_price) }}</text>
                <view class="quantity-control">
                  <view class="quantity-btn" @tap="decreaseQuantity(item.id || item.cartId)">-</view>
                  <text class="quantity">{{ item.quantity }}</text>
                  <view class="quantity-btn active-btn" @tap="increaseQuantity(item.id || item.cartId)">+</view>
                </view>
              </view>
            </view>
          </view>
          <view class="card-footer">
            <text class="line-total">小计 ¥{{ lineTotal(item) }}</text>
            <view class="delete-btn" @tap="deleteItem(item.id || item.cartId)">删除</view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state">
        <view class="empty-mark">袋</view>
        <text class="empty-title">购物车还是空的</text>
        <text class="empty-copy">去商城挑点器材、服饰或者恢复好物吧。</text>
        <view class="go-shopping-btn" @tap="goShopping">去逛商城</view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <view class="bottom-bar" v-if="cartItems.length > 0">
      <view class="total-copy">
        <text class="total-label">合计</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
      <view class="checkout-btn" @tap="checkout">去结算</view>
    </view>
  </view>
</template>

<script>
import { get, postForm } from '@/utils/request.js';

export default {
  data() {
    return {
      cartItems: [],
      loading: false
    };
  },
  computed: {
    totalPrice() {
      return this.cartItems.reduce((total, item) => total + Number(item.price || 0) * Number(item.quantity || 0), 0).toFixed(2);
    }
  },
  onLoad() {
    this.loadCart();
  },
  onShow() {
    this.loadCart();
  },
  methods: {
    formatPrice(price) {
      const value = Number(price);
      return Number.isFinite(value) ? value.toFixed(2) : '0.00';
    },
    lineTotal(item) {
      return (Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2);
    },
    async loadCart() {
      if (this.loading) return;
      this.loading = true;

      try {
        const res = await get('/cart/list');
        if (res.success) {
          const cartData = res.data || [];
          this.cartItems = await Promise.all(
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
        console.error('加载购物车失败:', error);
        if (error.message && error.message.includes('未登录')) {
          uni.showModal({
            title: '提示',
            content: '请先登录',
            showCancel: false,
            success: () => {
              uni.navigateTo({
                url: '/pages/login/login'
              });
            }
          });
        }
      } finally {
        this.loading = false;
      }
    },
    async increaseQuantity(cartId) {
      const item = this.cartItems.find((cartItem) => (cartItem.id || cartItem.cartId) === cartId);
      if (!item) return;
      try {
        const res = await postForm('/cart/update', {
          cartId,
          quantity: item.quantity + 1
        });
        if (res.success) {
          item.quantity += 1;
        }
      } catch (error) {
        console.error('更新购物车失败:', error);
      }
    },
    async decreaseQuantity(cartId) {
      const item = this.cartItems.find((cartItem) => (cartItem.id || cartItem.cartId) === cartId);
      if (!item || item.quantity <= 1) return;
      try {
        const res = await postForm('/cart/update', {
          cartId,
          quantity: item.quantity - 1
        });
        if (res.success) {
          item.quantity -= 1;
        }
      } catch (error) {
        console.error('更新购物车失败:', error);
      }
    },
    async deleteItem(cartId) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这件商品吗？',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            const result = await postForm('/cart/delete', { cartId });
            if (result.success) {
              this.cartItems = this.cartItems.filter((item) => (item.id || item.cartId) !== cartId);
              uni.showToast({
                title: '删除成功',
                icon: 'success'
              });
            }
          } catch (error) {
            console.error('删除购物车商品失败:', error);
          }
        }
      });
    },
    async clearCart() {
      uni.showModal({
        title: '确认清空',
        content: '确定要清空购物车吗？',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            const result = await postForm('/cart/clear', {});
            if (result.success) {
              this.cartItems = [];
              uni.showToast({
                title: '购物车已清空',
                icon: 'success'
              });
            }
          } catch (error) {
            console.error('清空购物车失败:', error);
          }
        }
      });
    },
    goShopping() {
      uni.switchTab({
        url: '/pages/mall/index'
      });
    },
    checkout() {
      if (this.cartItems.length === 0) {
        uni.showToast({
          title: '购物车为空',
          icon: 'none'
        });
        return;
      }
      uni.navigateTo({
        url: '/pages/mall/order'
      });
    },
    goBack() {
      uni.navigateBack({
        delta: 1
      });
    }
  }
};
</script>

<style scoped>
.cart-page {
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
.nav-action,
.ghost-action {
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

.ghost-action {
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

.nav-action-text {
  font-size: 24rpx;
  font-weight: 700;
  color: #4f5f7b;
}

.cart-scroll {
  height: calc(100vh - 126rpx - env(safe-area-inset-top));
}

.summary-strip {
  padding: 6rpx 28rpx 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.summary-card,
.cart-card,
.empty-state,
.bottom-bar {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16rpx 34rpx rgba(18, 35, 63, 0.06);
}

.summary-card {
  padding: 22rpx;
  border-radius: 28rpx;
}

.summary-label {
  display: block;
  font-size: 22rpx;
  color: #7a8799;
}

.summary-value {
  display: block;
  margin-top: 12rpx;
  font-size: 34rpx;
  font-weight: 800;
  color: #162643;
}

.cart-list {
  padding: 18rpx 28rpx 0;
}

.cart-card {
  padding: 20rpx;
  border-radius: 30rpx;
  margin-bottom: 16rpx;
}

.cart-main {
  display: flex;
  gap: 18rpx;
}

.product-image {
  width: 176rpx;
  height: 176rpx;
  border-radius: 24rpx;
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
  color: #162643;
  font-weight: 700;
}

.product-specification {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #8692a3;
}

.product-meta {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.product-price,
.total-price {
  font-size: 32rpx;
  font-weight: 800;
  color: #e35f46;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.quantity-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: #eef3f9;
  color: #4f5f7b;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.active-btn {
  background: linear-gradient(135deg, #18304d 0%, #315f92 100%);
  color: #fff;
}

.quantity {
  min-width: 34rpx;
  text-align: center;
  font-size: 25rpx;
  font-weight: 700;
  color: #162643;
}

.card-footer {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid rgba(18, 35, 63, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.line-total {
  font-size: 22rpx;
  color: #7a8799;
}

.delete-btn {
  min-width: 92rpx;
  height: 44rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: rgba(227, 95, 70, 0.1);
  color: #de6f4f;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  margin: 28rpx;
  padding: 84rpx 36rpx;
  border-radius: 34rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-mark {
  width: 120rpx;
  height: 120rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #dbe7fa 0%, #edf3ff 100%);
  color: #315f92;
  font-size: 42rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  display: block;
  margin-top: 28rpx;
  font-size: 32rpx;
  font-weight: 800;
  color: #162643;
}

.empty-copy {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7a8799;
}

.go-shopping-btn {
  margin-top: 28rpx;
  min-width: 220rpx;
  height: 78rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #18304d 0%, #315f92 100%);
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
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

.checkout-btn {
  min-width: 220rpx;
  height: 86rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #e35f46 0%, #f29449 100%);
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
