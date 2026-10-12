<template>
  <view class="order-list-container">
    <!-- 顶部导航栏 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">我的订单</text>
      <view class="nav-right"></view>
    </view>

    <!-- 标签栏 -->
    <view class="tab-bar">
      <scroll-view 
        class="tab-scroll" 
        scroll-x 
        :show-scrollbar="false"
      >
        <view class="tab-items">
          <view 
            v-for="tab in tabs" 
            :key="tab.value" 
            class="tab-item" 
            :class="{ active: currentTab === tab.value }"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 订单列表 - 使用scroll-view实现滚动 -->
    <scroll-view 
      class="order-scroll" 
      scroll-y 
      :style="{ height: scrollHeight }"
      @scrolltolower="loadMoreOrders"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="order-list">
        <view class="order-item" v-for="order in orderList" :key="order.id" @click="viewOrderDetail(order.orderNo)">
          <view class="order-header">
            <text class="order-no">订单号：{{ order.orderNo }}</text>
            <text class="order-status" :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</text>
          </view>

          <view class="order-items">
            <view class="order-product" v-if="order.items && order.items.length > 0">
              <image :src="order.items[0].productImage || '/static/mall/default-placeholder.png'" class="product-image" mode="aspectFill" />
              <view class="product-info">
                <text class="product-name">{{ order.items[0].productName }}{{ order.hasMoreItems ? '等' : '' }}</text>
                <view class="price-quantity">
                  <text class="product-price">¥{{ order.items[0].price }}</text>
                  <text class="product-quantity">x{{ order.items[0].quantity }}</text>
                </view>
              </view>
            </view>
            <view v-if="!order.items || order.items.length === 0" class="empty-items">
              <text class="empty-items-text">暂无商品信息</text>
            </view>
          </view>

          <view class="order-footer">
            <text class="order-time">{{ order.createTime }}</text>
            <text class="order-total">合计：¥{{ order.totalAmount }}</text>
          </view>

          <view class="order-actions">
            <view
              v-if="order.status === 0"
              class="action-btn pay-btn"
              @click.stop="payOrder(order.orderNo)"
            >
              去付款
            </view>
            <view 
              v-if="order.status === 0" 
              class="action-btn cancel-btn" 
              @click.stop="cancelOrder(order.orderNo)"
            >
              取消订单
            </view>
            <view 
              v-if="order.status === 2" 
              class="action-btn confirm-btn" 
              @click.stop="confirmOrder(order.orderNo)"
            >
              确认收货
            </view>
            <view 
              v-if="order.status === 1" 
              class="action-btn refund-btn" 
              @click.stop="requestRefund(order.orderNo)"
            >
              申请退款
            </view>
            <view
              v-if="order.status === 3 && !order.hasComment"
              class="action-btn comment-btn"
              @click.stop="goToComment(order.orderNo)"
            >
              去评价
            </view>
            <view
              v-if="order.status === 3 && order.hasComment"
              class="action-btn viewed-btn"
              @click.stop="viewComment(order.orderNo)"
            >
              查看评价
            </view>
          </view>
        </view>

        <!-- 加载更多状态 -->
        <view class="load-more" v-if="orderList.length > 0">
          <text v-if="loading">加载中...</text>
          <text v-else-if="hasMore">上拉加载更多</text>
          <text v-else>没有更多了</text>
        </view>

        <!-- 空状态 -->
        <view v-if="!loading && orderList.length === 0" class="empty-state">
          <text class="empty-icon">📦</text>
          <text class="empty-text">暂无订单</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { get, post } from '@/utils/request.js';
import { getOrderList, cancelOrder, confirmOrder, requestRefund, getOrderItems } from '@/api/order.js';

export default {
  data() {
    return {
      currentTab: -1, // -1: 全部, 0: 待付款, 1: 待发货, 2: 待收货, 3: 已完成, 4: 已取消, 5: 退款中, 6: 已退款
      tabs: [
        { label: '全部', value: -1 },
        { label: '待付款', value: 0 },
        { label: '待发货', value: 1 },
        { label: '待收货', value: 2 },
        { label: '已完成', value: 3 },
        { label: '已取消', value: 4 },
        { label: '退款中', value: 5 },
        { label: '已退款', value: 6 }
      ],
      orderList: [],
      loading: false,
      refreshing: false,
      page: 1,
      pageSize: 10,
      hasMore: true,
      scrollHeight: '0px'
    };
  },
  onLoad() {
    this.calcScrollHeight();
    this.loadOrderList();
  },
  onShow() {
    if (this.orderList.length > 0) {
      this.page = 1;
      this.hasMore = true;
      this.loadOrderList();
    }
  },
  onReady() {
    this.calcScrollHeight();
  },
  methods: {
    // 计算滚动区域高度
    calcScrollHeight() {
      try {
        const systemInfo = uni.getSystemInfoSync();
        const windowHeight = systemInfo.windowHeight;
        
        // 获取导航栏和标签栏高度
        const query = uni.createSelectorQuery().in(this);
        query.select('.nav-bar').boundingClientRect();
        query.select('.tab-bar').boundingClientRect();
        
        query.exec((res) => {
          let navHeight = 60; // 默认导航栏高度
          let tabHeight = 50; // 默认标签栏高度
          
          if (res[0] && res[0].height) {
            navHeight = res[0].height;
          }
          if (res[1] && res[1].height) {
            tabHeight = res[1].height;
          }
          
          // 计算滚动区域高度
          const scrollHeight = windowHeight - navHeight - tabHeight;
          this.scrollHeight = scrollHeight + 'px';
          
          console.log('滚动区域高度:', this.scrollHeight);
        });
      } catch (error) {
        console.error('计算滚动高度失败:', error);
        this.scrollHeight = 'calc(100vh - 220rpx)';
      }
    },
    
    // 切换标签
    switchTab(tabValue) {
      if (this.currentTab === tabValue) return;
      
      this.currentTab = tabValue;
      this.page = 1;
      this.hasMore = true;
      this.orderList = [];
      this.loadOrderList();
    },
    
    // 下拉刷新
    onRefresh() {
      this.refreshing = true;
      this.page = 1;
      this.hasMore = true;
      this.loadOrderList().finally(() => {
        this.refreshing = false;
      });
    },
    
    // 加载订单列表
    async loadOrderList() {
      if (this.loading) return;

      this.loading = true;
      try {
        const params = {
          page: this.page,
          pageSize: this.pageSize
        };

        // 如果不是全部，则添加状态过滤
        if (this.currentTab !== -1) {
          params.status = this.currentTab;
        }

        const res = await getOrderList(params);
        console.log('订单列表接口返回数据:', res);
        
        if (res.success) {
          const orders = res.data || [];
          console.log('解析后的订单数据:', orders);

          // 为每个订单项添加商品图片信息
          const newOrders = await Promise.all(orders.map(async (order) => {
            // 确保订单有基本字段
            const processedOrder = {
              ...order,
              status: order.status !== undefined ? order.status : 1,
              orderNo: order.orderNo || '',
              totalAmount: order.totalAmount || 0,
              createTime: order.createTime || new Date().toISOString()
            };

            // 获取订单商品
            let orderItems = [];
            try {
              const itemsRes = await getOrderItems(order.id);
              console.log('订单商品接口返回数据:', itemsRes);
              if (itemsRes.success && itemsRes.data) {
                orderItems = Array.isArray(itemsRes.data) ? itemsRes.data : [];
              }
            } catch (error) {
              console.error('获取订单商品失败:', error);
            }

            // 只显示第一个商品的信息
            let displayItem = null;
            if (orderItems.length > 0) {
              const firstItem = orderItems[0];
              displayItem = {
                ...firstItem,
                productImage: firstItem.productImage || '/static/mall/default-placeholder.png',
                productName: firstItem.productName || '未知商品',
                price: firstItem.price || 0,
                quantity: firstItem.quantity || 1
              };
            }

            return {
              ...processedOrder,
              items: displayItem ? [displayItem] : [],
              hasMoreItems: orderItems.length > 1
            };
          }));

          if (this.page === 1) {
            this.orderList = newOrders;
          } else {
            this.orderList = [...this.orderList, ...newOrders];
          }
          
          // 判断是否还有更多数据
          this.hasMore = orders.length === this.pageSize;
        } else {
          uni.showToast({
            title: res.message || '加载订单列表失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('加载订单列表失败:', error);
        uni.showToast({
          title: '加载订单列表失败',
          icon: 'none'
        });
      } finally {
        this.loading = false;
      }
    },

    // 加载更多订单
    loadMoreOrders() {
      if (this.hasMore && !this.loading) {
        this.page++;
        this.loadOrderList();
      }
    },

    // 查看订单详情
    viewOrderDetail(orderNo) {
      uni.navigateTo({
        url: `/pages/mall/order-detail?orderNo=${orderNo}`
      });
    },

    // 取消订单
    async cancelOrder(orderNo) {
      uni.showModal({
        title: '提示',
        content: '确定要取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await cancelOrder(orderNo, '用户取消订单');
              if (result.success) {
                uni.showToast({
                  title: '取消订单成功',
                  icon: 'success'
                });
                // 重新加载订单列表
                this.page = 1;
                this.hasMore = true;
                this.loadOrderList();
              }
            } catch (error) {
              console.error('取消订单失败:', error);
              uni.showToast({
                title: '取消订单失败',
                icon: 'none'
              });
            }
          }
        }
      });
    },

    // 确认收货
    async confirmOrder(orderNo) {
      uni.showModal({
        title: '提示',
        content: '确定已收到商品吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await confirmOrder(orderNo);
              if (result.success) {
                uni.showToast({
                  title: '确认收货成功',
                  icon: 'success'
                });
                // 重新加载订单列表
                this.page = 1;
                this.hasMore = true;
                this.loadOrderList();
              }
            } catch (error) {
              console.error('确认收货失败:', error);
              uni.showToast({
                title: '确认收货失败',
                icon: 'none'
              });
            }
          }
        }
      });
    },

    // 申请退款
    requestRefund(orderNo) {
      uni.navigateTo({
        url: `/pages/mall/refund?orderNo=${orderNo}`
      });
    },
    
    // 去评价
    goToComment(orderNo) {
      uni.navigateTo({
        url: `/pages/mall/comment?orderNo=${orderNo}`
      });
    },
    
    // 查看评价
    viewComment(orderNo) {
      uni.navigateTo({
        url: `/pages/mall/comment?orderNo=${orderNo}&viewOnly=true`
      });
    },

    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        0: '待付款',
        1: '待发货',
        2: '待收货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款'
      };
      return statusMap[status] || '未知状态';
    },

    // 获取状态样式类
    getStatusClass(status) {
      const classMap = {
        0: 'status-pending',
        1: 'status-shipping',
        2: 'status-receiving',
        3: 'status-completed',
        4: 'status-cancelled',
        5: 'status-refunding',
        6: 'status-refunded'
      };
      return classMap[status] || '';
    },

    // 返回上一页
    goBack() {
      uni.navigateBack({
        delta: 1
      });
    },

    // 去付款
    payOrder(orderNo) {
      // 获取订单信息
      const order = this.orderList.find(o => o.orderNo === orderNo);
      if (order) {
        // 跳转到支付页面
        uni.navigateTo({
          url: `/pages/mall/payment?orderNo=${orderNo}&amount=${order.totalAmount}`
        });
      } else {
        uni.showToast({
          title: '订单信息不存在',
          icon: 'none'
        });
      }
    }
  }
};
</script>

<style scoped>
/* ========== 容器 ========== */
.order-list-container {
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

/* ========== 导航栏 ========== */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: white;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  z-index: 100;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.back-icon {
  width: 24px;
  height: 24px;
}

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.nav-right {
  width: 40px;
}

/* ========== 标签栏 ========== */
.tab-bar {
  background-color: white;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  overflow: hidden;
}

.tab-scroll {
  width: 100%;
  white-space: nowrap;
}

.tab-items {
  display: inline-flex;
  padding: 0 10px;
}

.tab-item {
  display: inline-block;
  padding: 12px 16px;
  font-size: 14px;
  color: #666;
  position: relative;
  white-space: nowrap;
  transition: all 0.3s;
  cursor: pointer;
}

.tab-item.active {
  color: #ff4d4f;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background-color: #ff4d4f;
  border-radius: 2px;
}

/* ========== 滚动区域 ========== */
.order-scroll {
  flex: 1;
  width: 100%;
}

/* ========== 订单列表 ========== */
.order-list {
  padding: 12px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
}

.order-item {
  background-color: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  cursor: pointer;
}

.order-item:active {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.order-no {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.order-status {
  font-size: 14px;
  font-weight: 600;
}

.status-pending {
  color: #ff9800;
}

.status-shipping {
  color: #2196f3;
}

.status-receiving {
  color: #ff9800;
}

.status-completed {
  color: #4caf50;
}

.status-cancelled {
  color: #9e9e9e;
}

.status-refunding {
  color: #ff5722;
}

.status-refunded {
  color: #1f9d6a;
}

.order-items {
  margin-bottom: 12px;
}

.order-product {
  display: flex;
  gap: 12px;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f5f5f5;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.product-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.price-quantity {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.product-price {
  font-size: 16px;
  font-weight: bold;
  color: #ff4d4f;
}

.product-quantity {
  font-size: 14px;
  color: #999;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.order-time {
  font-size: 12px;
  color: #999;
}

.order-total {
  font-size: 16px;
  color: #333;
  font-weight: bold;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.action-btn {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  text-align: center;
  transition: all 0.3s;
  cursor: pointer;
}

.action-btn:active {
  transform: scale(0.96);
}

.cancel-btn {
  border: 1px solid #ddd;
  color: #666;
  background-color: white;
}

.pay-btn {
  background-color: #ff4d4f;
  color: white;
  border: none;
}

.confirm-btn {
  background-color: #ff4d4f;
  color: white;
  border: none;
}

.refund-btn {
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  background-color: white;
}

.comment-btn {
  background-color: #ff9800;
  color: white;
  border: none;
}

.viewed-btn {
  border: 1px solid #ddd;
  color: #999;
  background-color: white;
}

/* ========== 加载更多 ========== */
.load-more {
  text-align: center;
  padding: 20px 0 30px;
  color: #999;
  font-size: 14px;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 14px;
  color: #999;
}

.empty-items {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.empty-items-text {
  font-size: 14px;
  color: #999;
}

/* ========== 响应式适配 ========== */
@media (max-width: 375px) {
  .order-item {
    padding: 12px;
  }
  
  .product-image {
    width: 70px;
    height: 70px;
  }
  
  .action-btn {
    padding: 5px 12px;
    font-size: 12px;
  }
}
</style>
