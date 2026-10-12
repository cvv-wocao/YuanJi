<template>
  <view class="product-list-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">{{ title }}</text>
      <view class="nav-right"></view>
    </view>

    <view class="filter-bar">
      <view class="filter-item" :class="{ active: sortBy === 'default' }" @click="sortBy = 'default'; sortProducts()">
        <text>综合</text>
      </view>
      <view class="filter-item" :class="{ active: sortBy === 'price' }" @click="toggleSort">
        <text>价格</text>
        <image :src="sortBy === 'price' ? (priceAsc ? 'https://via.placeholder.com/12x12?text=↑' : 'https://via.placeholder.com/12x12?text=↓') : 'https://via.placeholder.com/12x12?text=≡'" class="sort-icon" />
      </view>
    </view>

    <view class="product-list">
      <view class="product-item" v-for="product in products" :key="product.id" @click="goToDetail(product.id)">
        <image :src="product.thumbnail" class="product-image" />
        <text class="product-name">{{ product.name }}</text>
        <text class="product-price">¥{{ product.price }}</text>
        <text class="product-sales">已售{{ product.sales }}件</text>
      </view>
    </view>

    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script>
import { get } from '@/utils/request.js';

export default {
  data() {
    return {
      products: [],
      sortBy: 'default',
      priceAsc: true,
      loading: false,
      page: 1,
      pageSize: 10,
      categoryId: '',
      keyword: '',
      type: '',
      title: '商品列表'
    };
  },
  onLoad(options) {
    this.categoryId = options.categoryId || '';
    this.keyword = options.keyword || '';
    this.type = options.type || '';
    this.title = options.title || '商品列表';
    console.log('页面参数:', options);
    this.loadProducts();
  },
  methods: {
    goBack() {
      uni.navigateBack({ delta: 1 });
    },
    async loadProducts() {
      if (this.loading) return;

      this.loading = true;
      this.page = 1;

      try {
        let res;
        // 根据type参数调用不同的API
        if (this.type === 'hot') {
          // 获取热销商品
          res = await get('/product/hot', { limit: 20 });
        } else if (this.type === 'new') {
          // 获取新品商品
          res = await get('/product/new', { limit: 20 });
        } else if (this.type === 'recommend') {
          // 获取推荐商品
          res = await get('/product/recommend', { limit: 20 });
        } else if (this.categoryId) {
          // 根据分类ID获取商品
          res = await get('/product/list', {
            categoryId: this.categoryId,
            page: this.page,
            pageSize: this.pageSize
          });
        } else if (this.keyword) {
          // 根据关键词搜索商品
          res = await get('/product/list', {
            keyword: this.keyword,
            page: this.page,
            pageSize: this.pageSize
          });
        } else {
          // 获取所有商品
          res = await get('/product/list', {
            page: this.page,
            pageSize: this.pageSize
          });
        }

        if (res.success) {
          this.products = res.data || [];
          console.log('获取到的商品数据:', this.products);
        } else {
          uni.showToast({
            title: res.message || '获取商品列表失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('获取商品列表失败:', error);
        uni.showToast({
          title: '获取商品列表失败',
          icon: 'none'
        });
      } finally {
        this.loading = false;
      }
    },
    goToDetail(id) {
      uni.navigateTo({
        url: `/pages/mall/detail?id=${id}`
      });
    },
    toggleSort() {
      if (this.sortBy === 'price') {
        this.priceAsc = !this.priceAsc;
      } else {
        this.sortBy = 'price';
        this.priceAsc = true;
      }
      this.sortProducts();
    },
    sortProducts() {
      if (this.sortBy === 'price') {
        this.products.sort((a, b) => {
          return this.priceAsc ? a.price - b.price : b.price - a.price;
        });
      } else if (this.sortBy === 'default') {
        // 综合排序，按销量降序
        this.products.sort((a, b) => b.sales - a.sales);
      }
    }
  }
};
</script>

<style scoped>
.product-list-container {
  padding-bottom: calc(60px + env(safe-area-inset-bottom));
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 15px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
}

.back-icon {
  width: 24px;
  height: 24px;
}

.nav-title {
  font-size: 16px;
  font-weight: bold;
}

.nav-right {
  width: 44px;
}

.filter-bar {
  display: flex;
  background-color: white;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 44px;
  z-index: 99;
}

.filter-item {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  color: #666;
  position: relative;
}

.filter-item.active {
  color: #1890ff;
}

.sort-icon {
  width: 12px;
  height: 12px;
  margin-left: 5px;
}

.product-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 10px;
  box-sizing: border-box;
}

.product-item {
  width: calc(50% - 5px);
  background-color: white;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.product-image {
  width: 100%;
  height: 150px;
  border-radius: 8px;
  margin-bottom: 10px;
}

.product-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  font-size: 16px;
  font-weight: bold;
  color: #ff4d4f;
  margin-bottom: 5px;
}

.product-sales {
  font-size: 12px;
  color: #999;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>
