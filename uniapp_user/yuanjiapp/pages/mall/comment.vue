
<template>
  <view class="comment-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">{{ viewOnly ? '查看评价' : '发表评价' }}</text>
      <view class="nav-right"></view>
    </view>

    <view class="order-info" v-if="orderDetail.orderNo">
      <view class="info-row">
        <text class="info-label">订单号</text>
        <text class="info-value">{{ orderDetail.orderNo }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ orderDetail.createTime }}</text>
      </view>
    </view>

    <view class="product-list" v-if="orderDetail.items && orderDetail.items.length > 0">
      <view class="product-item" v-for="(item, index) in orderDetail.items" :key="index">
        <image :src="item.productImage || '/static/mall/default-placeholder.png'" class="product-image" />
        <view class="product-info">
          <text class="product-name">{{ item.productName }}</text>
          <text class="product-spec">规格：{{ item.specification || '默认' }}</text>
          <text class="product-price">¥{{ item.price }}</text>
        </view>
      </view>
    </view>

    <view class="comment-form" v-if="!viewOnly">
      <view class="rating-section">
        <text class="section-title">商品评分</text>
        <view class="star-rating">
          <text 
            v-for="star in 5" 
            :key="star" 
            class="star" 
            :class="{ active: star <= productRating }"
            @click="setProductRating(star)"
          >
            ★
          </text>
        </view>
      </view>

      <view class="rating-section">
        <text class="section-title">物流评分</text>
        <view class="star-rating">
          <text 
            v-for="star in 5" 
            :key="star" 
            class="star" 
            :class="{ active: star <= logisticsRating }"
            @click="setLogisticsRating(star)"
          >
            ★
          </text>
        </view>
      </view>

      <view class="rating-section">
        <text class="section-title">服务评分</text>
        <view class="star-rating">
          <text 
            v-for="star in 5" 
            :key="star" 
            class="star" 
            :class="{ active: star <= serviceRating }"
            @click="setServiceRating(star)"
          >
            ★
          </text>
        </view>
      </view>

      <view class="comment-section">
        <text class="section-title">评价内容</text>
        <textarea 
          class="comment-input" 
          v-model="comment" 
          placeholder="请输入您的评价内容" 
          maxlength="500"
        />
        <text class="char-count">{{ comment.length }}/500</text>
      </view>

      <view class="image-section">
        <text class="section-title">添加图片</text>
        <view class="image-list">
          <view class="image-item" v-for="(img, index) in imageList" :key="index">
            <image :src="img" class="upload-image" />
            <text class="delete-btn" @click="deleteImage(index)">×</text>
          </view>
          <view class="image-item add-image" v-if="imageList.length < 9" @click="chooseImage">
            <text class="add-icon">+</text>
          </view>
        </view>
      </view>
    </view>

    <view class="comment-detail" v-else-if="existingComment">
      <view class="rating-display">
        <view class="rating-item">
          <text class="rating-label">商品评分：</text>
          <view class="star-display">
            <text class="star" v-for="star in 5" :key="'product-' + star" :class="{ active: star <= existingComment.productRating }">★</text>
          </view>
        </view>
        <view class="rating-item">
          <text class="rating-label">物流评分：</text>
          <view class="star-display">
            <text class="star" v-for="star in 5" :key="'logistics-' + star" :class="{ active: star <= existingComment.logisticsRating }">★</text>
          </view>
        </view>
        <view class="rating-item">
          <text class="rating-label">服务评分：</text>
          <view class="star-display">
            <text class="star" v-for="star in 5" :key="'service-' + star" :class="{ active: star <= existingComment.serviceRating }">★</text>
          </view>
        </view>
      </view>

      <view class="comment-content">
        <text class="section-title">评价内容</text>
        <text class="comment-text">{{ existingComment.content }}</text>
      </view>

      <view class="comment-images" v-if="existingComment.images && existingComment.images.length > 0">
        <text class="section-title">评价图片</text>
        <view class="image-list">
          <view class="image-item" v-for="(img, index) in existingComment.images" :key="index">
            <image :src="img" class="upload-image" @click="previewImage(index)" />
          </view>
        </view>
      </view>

      <view class="comment-time">
        <text>评价时间：{{ existingComment.createTime }}</text>
      </view>
    </view>

    <view class="submit-bar" v-if="!viewOnly">
      <view class="submit-btn" @click="submitComment">提交评价</view>
    </view>
  </view>
</template>

<script>
import { get, post } from '@/utils/request.js';
import { getOrderDetail } from '@/api/order.js';
import { upload } from '@/utils/request.js';

export default {
  data() {
    return {
      orderNo: '',
      viewOnly: false,
      orderDetail: {
        orderNo: '',
        createTime: '',
        items: []
      },
      productRating: 0,
      logisticsRating: 0,
      serviceRating: 0,
      comment: '',
      imageList: [],
      existingComment: null
    };
  },
  onLoad(options) {
    if (options.orderNo) {
      this.orderNo = options.orderNo;
      this.viewOnly = options.viewOnly === 'true';
      this.loadOrderDetail();

      if (this.viewOnly) {
        this.loadComment();
      }
    } else {
      uni.showToast({
        title: '订单号不存在',
        icon: 'none'
      });
      setTimeout(() => {
        uni.navigateBack();
      }, 1500);
    }
  },
  methods: {
    // 加载订单详情
    async loadOrderDetail() {
      try {
        const res = await getOrderDetail(this.orderNo);
        if (res.success && res.data) {
          // 为每个订单项添加商品图片信息
          const itemsWithImages = await Promise.all(res.data.items.map(async (item) => {
            try {
              const productRes = await get(`/product/detail/${item.productId}`);
              if (productRes.success && productRes.data) {
                const product = productRes.data;
                // 解析图片JSON字符串为数组
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
                  productName: product.name
                };
              }
            } catch (error) {
              console.error(`获取商品 ${item.productId} 详情失败:`, error);
            }
            return item;
          }));

          this.orderDetail = {
            ...res.data,
            items: itemsWithImages
          };
        }
      } catch (error) {
        console.error('加载订单详情失败:', error);
        uni.showToast({
          title: '加载订单详情失败',
          icon: 'none'
        });
      }
    },

    // 加载评论
    async loadComment() {
      try {
        const res = await get(`/comment/order/${this.orderNo}`);
        if (res.success && res.data) {
          this.existingComment = res.data;
        }
      } catch (error) {
        console.error('加载评论失败:', error);
      }
    },

    // 设置商品评分
    setProductRating(star) {
      this.productRating = star;
    },

    // 设置物流评分
    setLogisticsRating(star) {
      this.logisticsRating = star;
    },

    // 设置服务评分
    setServiceRating(star) {
      this.serviceRating = star;
    },

    // 选择图片
    chooseImage() {
      const that = this;
      uni.chooseImage({
        count: 9 - this.imageList.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: function(res) {
          // 上传图片
          res.tempFilePaths.forEach(filePath => {
            that.uploadImage(filePath);
          });
        }
      });
    },

    // 上传图片
    async uploadImage(filePath) {
      try {
        const res = await upload('/file/upload', filePath);
        if (res.success && res.data) {
          this.imageList.push(res.data.url);
        }
      } catch (error) {
        console.error('上传图片失败:', error);
        uni.showToast({
          title: '上传图片失败',
          icon: 'none'
        });
      }
    },

    // 删除图片
    deleteImage(index) {
      this.imageList.splice(index, 1);
    },

    // 预览图片
    previewImage(index) {
      const images = this.existingComment.images || [];
      uni.previewImage({
        current: index,
        urls: images
      });
    },

    // 提交评价
    async submitComment() {
      if (this.productRating === 0) {
        uni.showToast({
          title: '请对商品进行评分',
          icon: 'none'
        });
        return;
      }

      if (this.comment.trim() === '') {
        uni.showToast({
          title: '请输入评价内容',
          icon: 'none'
        });
        return;
      }

      try {
        const commentData = {
          orderNo: this.orderNo,
          productRating: this.productRating,
          logisticsRating: this.logisticsRating,
          serviceRating: this.serviceRating,
          content: this.comment,
          images: this.imageList
        };

        const res = await post('/comment/add', commentData);
        if (res.success) {
          uni.showToast({
            title: '评价成功',
            icon: 'success'
          });
          setTimeout(() => {
            uni.navigateBack();
          }, 1500);
        }
      } catch (error) {
        console.error('提交评价失败:', error);
      }
    },

    // 返回上一页
    goBack() {
      uni.navigateBack({
        delta: 1
      });
    }
  }
};
</script>

<style scoped>
.comment-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: calc(112px + env(safe-area-inset-bottom));
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

.order-info {
  background-color: white;
  padding: 15px;
  margin-bottom: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  color: #333;
}

.product-list {
  background-color: white;
  padding: 15px;
  margin-bottom: 10px;
}

.product-item {
  display: flex;
  margin-bottom: 15px;
}

.product-item:last-child {
  margin-bottom: 0;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  margin-right: 10px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-spec {
  font-size: 12px;
  color: #999;
}

.product-price {
  font-size: 14px;
  font-weight: bold;
  color: #ff4d4f;
}

.comment-form, .comment-detail {
  background-color: white;
  padding: 15px;
  margin-bottom: 10px;
}

.rating-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 10px;
}

.star-rating {
  display: flex;
  gap: 5px;
}

.star {
  font-size: 30px;
  color: #ddd;
}

.star.active {
  color: #ff9800;
}

.rating-display {
  margin-bottom: 15px;
}

.rating-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.rating-label {
  font-size: 14px;
  color: #666;
  margin-right: 10px;
}

.star-display {
  display: flex;
  gap: 3px;
}

.comment-section {
  margin-bottom: 20px;
}

.comment-input {
  width: 100%;
  min-height: 100px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background-color: #f9f9f9;
}

.char-count {
  font-size: 12px;
  color: #999;
  text-align: right;
  display: block;
  margin-top: 5px;
}

.image-section, .comment-images {
  margin-bottom: 20px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 80px;
  height: 80px;
}

.upload-image {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

.add-image {
  border: 1px dashed #ddd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-icon {
  font-size: 30px;
  color: #999;
}

.delete-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.comment-content {
  margin-bottom: 15px;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.comment-time {
  font-size: 12px;
  color: #999;
  text-align: center;
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  padding: 10px 15px calc(16px + env(safe-area-inset-bottom));
  border-top: 1px solid #f0f0f0;
}

.submit-btn {
  width: 100%;
  height: 44px;
  line-height: 44px;
  text-align: center;
  background-color: #ff9800;
  color: white;
  border-radius: 22px;
  font-size: 16px;
  font-weight: bold;
}
</style>
