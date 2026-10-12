<template>
  <view class="mall-page">
    <view class="topbar">
      <view class="top-copy">
        <text class="eyebrow">YUANJI MALL</text>
        <text class="title">远疾商城</text>
        <text class="subtitle">运动器材、服饰与恢复好物，收成一页更清爽的商品流。</text>
      </view>
      <view class="cart-btn" @tap="goToCart">
        <text class="cart-text">购物袋</text>
        <text v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</text>
      </view>
    </view>

    <view class="search-wrap">
      <view class="search-bar">
        <text class="search-icon">搜</text>
        <input
          v-model.trim="searchKeyword"
          class="search-input"
          type="text"
          confirm-type="search"
          placeholder="搜索器材、服饰、补剂"
          @confirm="handleSearch"
        />
        <text v-if="searchKeyword" class="search-clear" @tap="clearSearch">清空</text>
      </view>
      <view class="search-submit" @tap="handleSearch">搜索</view>
    </view>

    <scroll-view class="content" scroll-y @scrolltolower="loadMoreProducts">
      <view v-if="categories.length" class="section first-section">
        <view class="section-head">
          <view>
            <text class="section-tag">分类</text>
            <text class="section-title">按品类浏览</text>
          </view>
        </view>
        <scroll-view class="category-scroll" scroll-x show-scrollbar="false">
          <view class="category-row">
            <view
              v-for="(category, index) in categories"
              :key="category.id"
              class="category-card"
              @tap="goToCategory(category.id, category.name)"
            >
              <view class="category-icon" :class="accentClass(index)">
                <image :src="category.icon" class="category-image" mode="aspectFit" />
              </view>
              <text class="category-name">{{ category.name }}</text>
              <text class="category-desc">{{ categoryHint(category.name) }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view v-if="hotProducts.length" class="section">
        <view class="section-head">
          <view>
            <text class="section-tag">精选</text>
            <text class="section-title">热销商品</text>
          </view>
          <text class="section-link" @tap="goToHotProducts">更多</text>
        </view>
        <scroll-view class="featured-scroll" scroll-x show-scrollbar="false">
          <view class="featured-row">
            <view
              v-for="product in hotProducts"
              :key="product.id"
              class="featured-card"
              @tap="goToDetail(product.id)"
            >
              <image :src="getProductImage(product)" class="featured-image" mode="aspectFill" />
              <view class="featured-body">
                <text class="product-name clamp-2">{{ product.name }}</text>
                <text class="product-note">{{ salesText(product) }}</text>
                <view class="product-foot">
                  <text class="product-price">¥{{ formatPrice(product.price) }}</text>
                  <text class="product-link">查看</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="section">
        <view class="section-head">
          <view>
            <text class="section-tag">商品</text>
            <text class="section-title">全部商品</text>
          </view>
        </view>
        <view class="product-grid">
          <view
            v-for="product in products"
            :key="product.id"
            class="product-card"
            @tap="goToDetail(product.id)"
          >
            <image :src="getProductImage(product)" class="product-image" mode="aspectFill" />
            <view class="product-body">
              <text class="product-name clamp-2">{{ product.name }}</text>
              <text class="product-note">{{ productSubtitle(product) }}</text>
              <view class="product-foot">
                <text class="product-price">¥{{ formatPrice(product.price) }}</text>
                <view class="product-cta">详情</view>
              </view>
            </view>
          </view>
        </view>
        <view v-if="loading" class="status">商品加载中...</view>
        <view v-if="noMore && products.length" class="status">已经到底了</view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { get } from '@/utils/request.js';

export default {
  data() {
    return {
      cartCount: 0,
      categories: [],
      products: [],
      hotProducts: [],
      searchKeyword: '',
      page: 1,
      pageSize: 10,
      loading: false,
      noMore: false
    };
  },
  onLoad() {
    this.loadCategories();
    this.loadProducts();
    this.loadHotProducts();
    this.loadCartCount();
  },
  onShow() {
    this.loadCartCount();
  },
  onReachBottom() {
    if (!this.loading && !this.noMore) this.loadMoreProducts();
  },
  methods: {
    formatPrice(price) {
      const value = Number(price);
      return Number.isFinite(value) ? value.toFixed(2) : '0.00';
    },
    getProductImage(product) {
      return product.thumbnail || product.image || '/static/mall/default-placeholder.png';
    },
    salesText(product) {
      const sales = Number(product.sales || 0);
      return sales > 0 ? `已售 ${sales}` : '刚刚上架';
    },
    productSubtitle(product) {
      if (product.brief) return product.brief;
      if (product.categoryName) return `${product.categoryName} · 适合训练与日常使用`;
      return '精选材质与舒适体验，适合训练和日常场景';
    },
    accentClass(index) {
      const list = ['accent-blue', 'accent-sand', 'accent-green'];
      return list[index % list.length];
    },
    categoryHint(name) {
      if (!name) return '快速筛选';
      if (name.includes('器材')) return '训练核心装备';
      if (name.includes('服')) return '运动与日常穿搭';
      if (name.includes('补')) return '补给与恢复支持';
      if (name.includes('拉伸') || name.includes('恢复')) return '放松与舒缓工具';
      return '快速筛选';
    },
    clearSearch() {
      this.searchKeyword = '';
      this.loadProducts();
    },
    loadCartCount() {
      const cartData = uni.getStorageSync('cartItems');
      if (!cartData) {
        this.cartCount = 0;
        return;
      }
      const cartItems = JSON.parse(cartData);
      this.cartCount = cartItems.reduce((total, item) => total + item.quantity, 0);
    },
    async loadCategories() {
      try {
        const res = await get('/product/categories');
        if (res.success) {
          this.categories = (res.data || []).map((category) => {
            const name = category.name || '';
            if (name.includes('器材')) return { ...category, icon: category.icon || '/static/mall/default-placeholder.png' };
          if (name.includes('服')) return { ...category, icon: category.icon || '/static/mall/sports-shorts.png' };
          if (name.includes('拉伸') || name.includes('恢复')) {
            return { ...category, icon: category.icon || '/static/mall/nutrition-supplement.png' };
          }
          return { ...category, icon: category.icon || '/static/mall/default-placeholder.png' };
          });
        }
      } catch (error) {
        console.error('获取分类失败:', error);
      }
    },
    async loadProducts() {
      if (this.loading) return;
      this.loading = true;
      this.page = 1;
      try {
        const res = await get('/product/list', { page: this.page, pageSize: this.pageSize });
        if (res.success) {
          this.products = res.data || [];
          this.noMore = this.products.length < this.pageSize;
        }
      } catch (error) {
        console.error('获取商品列表失败:', error);
      } finally {
        this.loading = false;
      }
    },
    async loadMoreProducts() {
      if (this.loading || this.noMore) return;
      this.loading = true;
      this.page += 1;
      try {
        const res = await get('/product/list', { page: this.page, pageSize: this.pageSize });
        if (res.success) {
          const list = res.data || [];
          if (list.length) {
            this.products = [...this.products, ...list];
            this.noMore = list.length < this.pageSize;
          } else {
            this.noMore = true;
          }
        }
      } catch (error) {
        console.error('加载更多商品失败:', error);
        this.page -= 1;
      } finally {
        this.loading = false;
      }
    },
    async loadHotProducts() {
      try {
        const res = await get('/product/hot', { limit: 6 });
        if (res.success) this.hotProducts = res.data || [];
      } catch (error) {
        console.error('获取热销商品失败:', error);
      }
    },
    handleSearch() {
      if (!this.searchKeyword) {
        this.loadProducts();
        return;
      }
      this.searchProducts();
    },
    async searchProducts() {
      if (this.loading) return;
      this.loading = true;
      this.page = 1;
      try {
        const res = await get('/product/list', {
          keyword: this.searchKeyword.trim(),
          page: this.page,
          pageSize: this.pageSize
        });
        if (res.success) {
          this.products = res.data || [];
          this.noMore = this.products.length < this.pageSize;
        }
      } catch (error) {
        console.error('搜索商品失败:', error);
      } finally {
        this.loading = false;
      }
    },
    goToHotProducts() {
      uni.navigateTo({ url: '/pages/mall/list?type=hot&title=热销商品' });
    },
    goToCart() {
      uni.navigateTo({ url: '/pages/mall/cart' });
    },
    goToDetail(id) {
      uni.navigateTo({ url: `/pages/mall/detail?id=${id}` });
    },
    goToCategory(id, name) {
      uni.navigateTo({ url: `/pages/mall/category?id=${id}&name=${name}` });
    }
  }
};
</script>

<style lang="scss" scoped>
.mall-page {
  height: 100vh;
  overflow: hidden;
  background: #f6f4ef;
}

.topbar {
  padding: calc(30rpx + env(safe-area-inset-top)) 28rpx 18rpx;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
}

.top-copy {
  flex: 1;
}

.eyebrow {
  display: block;
  font-size: 20rpx;
  letter-spacing: 4rpx;
  font-weight: 800;
  color: rgba(27, 39, 64, 0.48);
}

.title {
  display: block;
  margin-top: 12rpx;
  font-size: 44rpx;
  line-height: 1.2;
  font-weight: 800;
  color: #1b2740;
}

.subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.65;
  color: #6f788b;
}

.cart-btn {
  position: relative;
  min-width: 108rpx;
  height: 82rpx;
  padding: 0 22rpx;
  border-radius: 24rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 28rpx rgba(30, 38, 51, 0.06);
}

.cart-text {
  font-size: 24rpx;
  font-weight: 700;
  color: #1b2740;
}

.cart-badge {
  position: absolute;
  top: -8rpx;
  right: -6rpx;
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: #de6f4f;
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid #f6f4ef;
  box-sizing: border-box;
}

.search-wrap {
  padding: 0 28rpx 8rpx;
  display: flex;
  gap: 12rpx;
}

.search-bar {
  flex: 1;
  height: 82rpx;
  padding: 0 20rpx;
  border-radius: 24rpx;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 12rpx;
  box-shadow: 0 12rpx 28rpx rgba(30, 38, 51, 0.06);
}

.search-icon {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: rgba(27, 39, 64, 0.08);
  color: #1b2740;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-input {
  flex: 1;
  font-size: 27rpx;
  color: #1b2740;
}

.search-clear {
  font-size: 22rpx;
  color: #8d95a4;
}

.search-submit {
  width: 116rpx;
  height: 82rpx;
  border-radius: 24rpx;
  background: #1b2740;
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content {
  height: calc(100vh - 214rpx - env(safe-area-inset-top));
  padding-bottom: calc(44rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.section {
  margin-top: 22rpx;
  padding: 0 28rpx;
}

.first-section {
  margin-top: 14rpx;
}

.section-head {
  margin-bottom: 18rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18rpx;
}

.section-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(27, 39, 64, 0.08);
  color: rgba(27, 39, 64, 0.64);
  font-size: 20rpx;
  font-weight: 700;
}

.section-title {
  display: block;
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 800;
  color: #1b2740;
}

.section-link {
  font-size: 22rpx;
  font-weight: 700;
  color: #667085;
}

.category-scroll,
.featured-scroll {
  white-space: nowrap;
}

.category-row,
.featured-row {
  display: inline-flex;
  gap: 16rpx;
  padding-right: 28rpx;
}

.category-card,
.featured-card,
.product-card {
  background: #fff;
  box-shadow: 0 12rpx 28rpx rgba(30, 38, 51, 0.06);
}

.category-card {
  width: 208rpx;
  padding: 20rpx;
  border-radius: 28rpx;
}

.category-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.accent-blue {
  background: #e5edfb;
}

.accent-sand {
  background: #f6e8d8;
}

.accent-green {
  background: #dff1ea;
}

.category-image {
  width: 56rpx;
  height: 56rpx;
}

.category-name {
  display: block;
  margin-top: 16rpx;
  font-size: 27rpx;
  font-weight: 800;
  color: #1b2740;
}

.category-desc,
.product-note {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.62;
  color: #717b8f;
}

.featured-card {
  width: 304rpx;
  border-radius: 28rpx;
  overflow: hidden;
  vertical-align: top;
}

.featured-image,
.product-image {
  width: 100%;
  height: 230rpx;
  display: block;
}

.featured-body,
.product-body {
  padding: 18rpx;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.product-card {
  border-radius: 28rpx;
  overflow: hidden;
}

.product-name {
  display: block;
  font-size: 26rpx;
  line-height: 1.45;
  color: #1b2740;
}

.clamp-2 {
  min-height: 74rpx;
}

.product-foot {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.product-price {
  font-size: 30rpx;
  font-weight: 800;
  color: #de6f4f;
}

.product-link {
  font-size: 22rpx;
  color: #667085;
}

.product-cta {
  min-width: 88rpx;
  height: 46rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #1b2740;
  color: #fff;
  font-size: 21rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status {
  padding: 30rpx 0 64rpx;
  text-align: center;
  font-size: 24rpx;
  color: #8d95a4;
}
</style>
