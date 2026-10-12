<template>
  <view class="product-detail-container">
    <view class="page-glow glow-top"></view>
    <view class="page-glow glow-bottom"></view>

    <view class="nav-bar">
      <view class="nav-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">商品详情</text>
      <view class="nav-btn" @click="addToFavorite">
        <text class="nav-icon">{{ isFavorite ? '藏' : '星' }}</text>
      </view>
    </view>

    <scroll-view class="detail-scroll" scroll-y>
      <view class="gallery-card">
        <swiper
          class="product-swiper"
          indicator-dots
          indicator-color="rgba(255,255,255,0.38)"
          indicator-active-color="#ffffff"
          autoplay
          circular
        >
          <swiper-item v-for="(image, index) in product.images" :key="index">
            <image :src="image" class="product-image" mode="aspectFill" />
          </swiper-item>
        </swiper>
        <view class="gallery-badge">精选单品</view>
      </view>

      <view class="price-card">
        <view class="price-row">
          <view>
            <text class="current-price">¥{{ product.price }}</text>
            <text v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice }}</text>
          </view>
          <text class="sales-tag">已售 {{ product.sales || 0 }}</text>
        </view>
        <text class="product-name">{{ product.name }}</text>
        <text class="product-brief">适合日常训练、拉伸恢复和轻量运动场景，兼顾耐用性与舒适感。</text>

        <view class="feature-row">
          <view class="feature-chip">官方精选</view>
          <view class="feature-chip">品质保障</view>
          <view class="feature-chip">支持售后</view>
        </view>
      </view>

      <view class="service-card">
        <view class="section-header">
          <text class="section-title">购买权益</text>
        </view>
        <view class="service-grid">
          <view class="service-item">
            <text class="service-mark">快</text>
            <text class="service-name">极速发货</text>
            <text class="service-desc">支付成功后尽快安排出库</text>
          </view>
          <view class="service-item">
            <text class="service-mark">真</text>
            <text class="service-name">商品保障</text>
            <text class="service-desc">展示信息与商品实物一致</text>
          </view>
          <view class="service-item">
            <text class="service-mark">稳</text>
            <text class="service-name">售后无忧</text>
            <text class="service-desc">支持订单查看与退款跟踪</text>
          </view>
        </view>
      </view>

      <view class="description-card">
        <view class="section-header">
          <text class="section-title">商品介绍</text>
        </view>
        <rich-text class="description-content" :nodes="product.description || '暂无商品介绍'" />
      </view>

      <view class="review-card">
        <view class="section-header">
          <text class="section-title">用户评价</text>
          <text class="review-count">{{ product.reviews.length }} 条</text>
        </view>

        <view v-if="product.reviews.length > 0">
          <view v-for="review in product.reviews" :key="review.id" class="review-item">
            <view class="review-head">
              <view class="review-avatar">{{ (review.userName || '用').charAt(0) }}</view>
              <view class="review-meta">
                <text class="review-user">{{ review.userName }}</text>
                <text class="review-time">{{ review.createTime }}</text>
              </view>
            </view>
            <text class="review-content">{{ review.content }}</text>
          </view>
        </view>

        <view v-else class="empty-review">
          <text class="empty-review-text">暂时还没有评价，欢迎成为第一位下单用户。</text>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <view class="bottom-bar">
      <view class="side-actions">
        <view class="icon-item" @click="goToCart">
          <text class="side-icon">袋</text>
          <text class="icon-text">购物车</text>
        </view>
        <view class="icon-item" @click="addToFavorite">
          <text class="side-icon">{{ isFavorite ? '藏' : '星' }}</text>
          <text class="icon-text">{{ isFavorite ? '已收藏' : '收藏' }}</text>
        </view>
      </view>
      <view class="action-group">
        <view class="btn add-to-cart" @click="addToCart">加入购物车</view>
        <view class="btn buy-now" @click="buyNow">立即购买</view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, postForm } from '@/utils/request.js';

export default {
  data() {
    return {
      productId: null,
      product: {
        id: 0,
        name: '',
        price: 0,
        originalPrice: 0,
        sales: 0,
        images: [],
        description: '',
        reviews: []
      },
      isFavorite: false
    };
  },
  onLoad(options) {
    this.productId = parseInt(options.id, 10) || 1;
    this.loadProductDetail();
  },
  methods: {
    async loadProductDetail() {
      try {
        const res = await get(`/product/detail/${this.productId}`);
        if (res.success && res.data) {
          this.product = res.data;
          if (typeof this.product.images === 'string') {
            this.product.images = JSON.parse(this.product.images);
          }
          if (!Array.isArray(this.product.images) || this.product.images.length === 0) {
            this.product.images = ['/static/mall/default-placeholder.png'];
          }
          if (!Array.isArray(this.product.reviews)) {
            this.product.reviews = [];
          }
        }
      } catch (error) {
        console.error('获取商品详情失败:', error);
        uni.showToast({
          title: '获取商品详情失败',
          icon: 'none'
        });
      }
    },
    async addToCart() {
      try {
        const res = await postForm('/cart/add', {
          productId: this.product.id,
          quantity: 1
        });
        if (res.success) {
          uni.showToast({
            title: '已加入购物车',
            icon: 'success'
          });
        }
      } catch (error) {
        console.error('添加购物车失败:', error);
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
      }
    },
    addToCartLocal() {
      let cartItems = uni.getStorageSync('cartItems');
      if (cartItems) {
        cartItems = JSON.parse(cartItems);
      } else {
        cartItems = [];
      }

      const existingItem = cartItems.find((item) => item.productId === this.product.id);
      if (existingItem) {
        existingItem.quantity++;
      } else {
        cartItems.push({
          id: Date.now(),
          productId: this.product.id,
          productName: this.product.name,
          productImage: this.product.images[0] || '',
          price: this.product.price,
          quantity: 1
        });
      }

      uni.setStorageSync('cartItems', JSON.stringify(cartItems));
      uni.showToast({
        title: '已加入购物车',
        icon: 'success'
      });
    },
    buyNow() {
      uni.navigateTo({
        url: '/pages/mall/order'
      });
    },
    addToFavorite() {
      this.isFavorite = !this.isFavorite;
      uni.showToast({
        title: this.isFavorite ? '收藏成功' : '取消收藏',
        icon: 'success'
      });
    },
    goToCart() {
      uni.navigateTo({
        url: '/pages/mall/cart'
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
.product-detail-container {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(55, 122, 255, 0.14) 0, rgba(55, 122, 255, 0) 32%),
    linear-gradient(180deg, #f7f9fd 0%, #eef3f8 100%);
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}

.page-glow {
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  filter: blur(60rpx);
  opacity: 0.45;
  pointer-events: none;
}

.glow-top {
  top: 0;
  right: -100rpx;
  background: rgba(61, 122, 255, 0.2);
}

.glow-bottom {
  top: 520rpx;
  left: -120rpx;
  background: rgba(42, 188, 160, 0.15);
}

.nav-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(20rpx + env(safe-area-inset-top)) 24rpx 18rpx;
  background: rgba(247, 249, 253, 0.86);
  backdrop-filter: blur(18rpx);
}

.nav-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.86);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 14rpx 28rpx rgba(19, 35, 63, 0.06);
}

.back-icon {
  width: 34rpx;
  height: 34rpx;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 800;
  color: #13233f;
}

.nav-icon {
  font-size: 24rpx;
  font-weight: 700;
  color: #3d6fec;
}

.detail-scroll {
  height: calc(100vh - 110rpx - env(safe-area-inset-top));
}

.gallery-card,
.price-card,
.service-card,
.description-card,
.review-card {
  position: relative;
  z-index: 1;
  margin: 0 24rpx 20rpx;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 34rpx;
  overflow: hidden;
  box-shadow: 0 18rpx 38rpx rgba(19, 35, 63, 0.06);
}

.gallery-card {
  margin-top: 4rpx;
}

.product-swiper,
.product-image {
  width: 100%;
  height: 660rpx;
}

.gallery-badge {
  position: absolute;
  left: 24rpx;
  bottom: 24rpx;
  height: 48rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(13, 27, 51, 0.62);
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
}

.price-card,
.service-card,
.description-card,
.review-card {
  padding: 28rpx;
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.current-price {
  font-size: 50rpx;
  line-height: 1;
  font-weight: 800;
  color: #ed543e;
}

.original-price {
  margin-left: 12rpx;
  font-size: 24rpx;
  color: #97a3b4;
  text-decoration: line-through;
}

.sales-tag {
  flex-shrink: 0;
  height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(61, 111, 236, 0.1);
  color: #3d6fec;
  font-size: 21rpx;
  display: flex;
  align-items: center;
}

.product-name {
  display: block;
  margin-top: 22rpx;
  font-size: 38rpx;
  line-height: 1.35;
  font-weight: 800;
  color: #152644;
}

.product-brief {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: #68778f;
}

.feature-row {
  margin-top: 24rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.feature-chip {
  height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #f3f6fb;
  color: #54657e;
  font-size: 21rpx;
  display: flex;
  align-items: center;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #13233f;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.service-item {
  min-width: 0;
  padding: 22rpx 18rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #f7faff 0%, #eef4fb 100%);
}

.service-mark {
  display: inline-flex;
  width: 42rpx;
  height: 42rpx;
  border-radius: 12rpx;
  background: rgba(61, 111, 236, 0.12);
  color: #3d6fec;
  font-size: 22rpx;
  font-weight: 800;
  align-items: center;
  justify-content: center;
}

.service-name {
  display: block;
  margin-top: 16rpx;
  font-size: 25rpx;
  font-weight: 700;
  color: #1e304c;
}

.service-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 21rpx;
  line-height: 1.7;
  color: #7a8798;
}

.description-content {
  font-size: 25rpx;
  line-height: 1.8;
  color: #5e6e85;
}

.review-count {
  font-size: 22rpx;
  color: #7f8ea3;
}

.review-item {
  padding: 22rpx 0;
  border-top: 1rpx solid rgba(19, 35, 63, 0.06);
}

.review-item:first-of-type {
  padding-top: 0;
  border-top: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.review-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #3d6fec 0%, #29b6a5 100%);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-meta {
  flex: 1;
  min-width: 0;
}

.review-user {
  display: block;
  font-size: 25rpx;
  font-weight: 700;
  color: #182945;
}

.review-time {
  display: block;
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #8a98ad;
}

.review-content {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: #607089;
}

.empty-review {
  padding: 26rpx;
  border-radius: 24rpx;
  background: #f6f8fb;
}

.empty-review-text {
  font-size: 24rpx;
  line-height: 1.7;
  color: #7f8ea3;
}

.bottom-space {
  height: 36rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(19, 35, 63, 0.06);
  backdrop-filter: blur(18rpx);
}

.side-actions {
  display: flex;
  gap: 12rpx;
}

.icon-item {
  width: 108rpx;
  height: 92rpx;
  border-radius: 26rpx;
  background: #f4f7fb;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.side-icon {
  font-size: 26rpx;
  font-weight: 800;
  color: #3d6fec;
}

.icon-text {
  font-size: 20rpx;
  color: #596b84;
}

.action-group {
  flex: 1;
  display: flex;
  gap: 12rpx;
}

.btn {
  flex: 1;
  height: 92rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.add-to-cart {
  background: #edf3ff;
  color: #3569e7;
}

.buy-now {
  background: linear-gradient(135deg, #f06a45 0%, #f58d3d 100%);
  color: #ffffff;
  box-shadow: 0 18rpx 30rpx rgba(240, 106, 69, 0.22);
}
</style>
