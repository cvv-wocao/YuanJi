<template>
  <view class="category-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">{{ categoryName }}</text>
      <view class="nav-right"></view>
    </view>
    
    <view class="product-list">
      <view class="product-item" v-for="product in products" :key="product.id" @click="goToDetail(product.id)">
        <image :src="product.thumbnail" class="product-image" />
        <text class="product-name">{{ product.name }}</text>
        <view class="product-footer">
          <text class="product-price">¥{{ product.price }}</text>
          <view class="favorite-btn" @click.stop="toggleFavorite(product)">
            <text :class="['favorite-icon', favorites.includes(product.id) ? 'active' : '']">
              {{ favorites.includes(product.id) ? '♥' : '♡' }}
            </text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 加载状态 -->
    <view class="loading-state" v-if="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 没有更多数据 -->
    <view class="no-more" v-if="!hasMore && products.length > 0">
      <text class="no-more-text">没有更多了</text>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="products.length === 0 && !loading">
      <image src="https://picsum.photos/100/100?text=📦" class="empty-icon" />
      <text class="empty-text">暂无商品</text>
    </view>
  </view>
</template>

<script>
import { get, post } from '@/utils/request'

export default {
  data() {
    return {
      categoryId: null,
      categoryName: '',
      products: [],
      page: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      hasMore: true,
      favorites: [] // 收藏的商品ID列表
    };
  },
  onLoad(options) {
    console.log('分类页面接收到的参数:', options);
    this.categoryId = options.id || options.categoryId || '';
    this.categoryName = options.name || options.categoryName || '商品分类';
    console.log('分类ID:', this.categoryId, '分类名称:', this.categoryName);
    this.loadProducts();
    this.loadFavorites();
  },
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.page++;
      this.loadProducts();
    }
  },
  onPullDownRefresh() {
    this.page = 1;
    this.products = [];
    this.hasMore = true;
    this.loadProducts().then(() => {
      uni.stopPullDownRefresh();
    });
  },
  methods: {
    // 获取商品列表
    async loadProducts() {
      if (this.loading) return;
      this.loading = true;
      
      try {
        console.log('请求商品列表，分类ID:', this.categoryId);
        const response = await get('/product/list', {
          categoryId: this.categoryId,
          page: this.page,
          pageSize: this.pageSize
        });
        
        console.log('商品列表响应:', response);

        if (response.success) {
          // 后端直接返回商品列表，不是包含list和total的对象
          const list = response.data || [];
          if (this.page === 1) {
            this.products = list;
          } else {
            this.products = [...this.products, ...list];
          }
          this.hasMore = list.length < this.pageSize;
          console.log('加载商品成功，当前商品数量:', this.products.length);
        } else {
          uni.showToast({
            title: response.message || '获取商品失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('加载商品失败:', error);
        uni.showToast({
          title: '加载商品失败，请稍后重试',
          icon: 'none'
        });
      } finally {
        this.loading = false;
      }
    },
    
    // 获取收藏列表
    async loadFavorites() {
      try {
        const response = await get('/product/favorite/list');
        if (response.code === 200) {
          this.favorites = response.data.map(item => item.productId);
        }
      } catch (error) {
        console.error('加载收藏失败:', error);
      }
    },
    
    // 添加收藏
    async addFavorite(productId) {
      try {
        const response = await post('/product/favorite/add', {
          productId
        });
        
        if (response.code === 200) {
          this.favorites.push(productId);
          uni.showToast({
            title: '收藏成功',
            icon: 'success'
          });
        }
      } catch (error) {
        console.error('收藏失败:', error);
      }
    },
    
    // 取消收藏
    async removeFavorite(productId) {
      try {
        const response = await post('/product/favorite/remove', {
          productId
        });
        
        if (response.code === 200) {
          const index = this.favorites.indexOf(productId);
          if (index > -1) {
            this.favorites.splice(index, 1);
          }
          uni.showToast({
            title: '已取消收藏',
            icon: 'success'
          });
        }
      } catch (error) {
        console.error('取消收藏失败:', error);
      }
    },
    
    // 切换收藏状态
    toggleFavorite(product) {
      if (this.favorites.includes(product.id)) {
        this.removeFavorite(product.id);
      } else {
        this.addFavorite(product.id);
      }
    },
    
    goToDetail(id) {
      uni.navigateTo({
        url: `/pages/mall/detail?id=${id}`
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
.category-container {
  min-height: 100vh;
  background-color: #f8f8f8;
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

.product-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 10px;
}

.product-item {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  padding: 10px;
}

.product-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.product-name {
  display: block;
  font-size: 14px;
  color: #333;
  margin: 8px 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.product-price {
  display: block;
  font-size: 16px;
  font-weight: bold;
  color: #ff4d4f;
}

.favorite-btn {
  padding: 4px;
}

.favorite-icon {
  font-size: 20px;
  color: #999;
}

.favorite-icon.active {
  color: #ff4d4f;
}

.loading-state,
.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.loading-text,
.no-more-text {
  font-size: 14px;
  color: #999;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 100px;
  height: 100px;
  margin-bottom: 20px;
}

.empty-text {
  font-size: 14px;
  color: #999;
}
</style>
