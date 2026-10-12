<template>
  <view class="order-detail-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">订单详情</text>
      <view class="nav-right"></view>
    </view>

    <view class="page-content">
      <view v-if="isRefundOrder" class="refund-status-card" :class="refundThemeClass">
        <text class="refund-status-badge">{{ refundBadgeText }}</text>
        <text class="refund-status-title">{{ refundHeaderTitle }}</text>
        <text class="refund-status-desc">{{ refundHeaderDescription }}</text>
        <view class="refund-status-meta">
          <text class="refund-meta-item">订单号：{{ orderDetail.orderNo }}</text>
          <text v-if="lastRefundQueryTimeText" class="refund-meta-item">最近刷新：{{ lastRefundQueryTimeText }}</text>
        </view>
      </view>

      <view v-else class="order-status-card">
        <text class="status-text">{{ getStatusText(orderDetail.status) }}</text>
        <text class="status-desc">{{ getStatusDesc(orderDetail.status) }}</text>
      </view>

      <view v-if="isRefundOrder" class="refund-progress-card">
        <text class="card-title">退款进度</text>

        <view class="progress-item" :class="{ active: refundApplied }">
          <view class="progress-dot"></view>
          <view class="progress-content">
            <text class="progress-title">退款申请已提交</text>
            <text class="progress-desc">{{ refundReasonDescription }}</text>
          </view>
        </view>

        <view class="progress-item" :class="{ active: reviewPassed }">
          <view class="progress-dot"></view>
          <view class="progress-content">
            <text class="progress-title">商家处理中</text>
            <text class="progress-desc">{{ refundReviewDescription }}</text>
          </view>
        </view>

        <view class="progress-item" :class="{ active: refundFinished }">
          <view class="progress-dot"></view>
          <view class="progress-content">
            <text class="progress-title">{{ refundFinishedTitle }}</text>
            <text class="progress-desc">{{ refundFinishedDescription }}</text>
          </view>
        </view>
      </view>

      <view class="order-info-card">
        <text class="card-title">订单信息</text>
        <view class="info-row">
          <text class="info-label">订单号</text>
          <text class="info-value info-value-strong">{{ orderDetail.orderNo }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间</text>
          <text class="info-value">{{ orderDetail.createTime }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">订单金额</text>
          <text class="info-value price">¥{{ orderDetail.totalAmount }}</text>
        </view>
        <view v-if="isRefundOrder && refundStatusText" class="info-row align-start">
          <text class="info-label">退款状态</text>
          <view class="refund-status-text-group">
            <text class="info-value refund-value">{{ refundStatusText }}</text>
            <text v-if="refundStatusDesc" class="refund-inline-desc">{{ refundStatusDesc }}</text>
          </view>
        </view>
      </view>

      <view class="order-items-card">
        <text class="card-title">商品信息</text>
        <view v-for="item in orderDetail.items" :key="item.id" class="order-item">
          <image :src="item.productImage || '/static/mall/default-placeholder.png'" class="product-image" />
          <view class="product-info">
            <text class="product-name">{{ item.productName }}</text>
            <view class="price-quantity">
              <text class="product-price">¥{{ item.price }}</text>
              <text class="product-quantity">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
        <view v-if="!orderDetail.items || orderDetail.items.length === 0" class="empty-items">
          <text class="empty-items-text">暂无商品信息</text>
        </view>
      </view>

      <view v-if="isRefundOrder" class="refund-tips-card">
        <text class="card-title no-divider">退款说明</text>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">提交申请后会先进入退款中，订单状态会同步更新。</text>
        </view>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">商家审核通过后，系统会继续处理退款并自动刷新结果。</text>
        </view>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">如果退款被驳回，请根据提示重新与商家沟通或再次发起申请。</text>
        </view>
      </view>
    </view>

    <view class="order-actions">
      <view v-if="orderDetail.status === 0" class="action-btn pay-btn" @click="goToPayment">
        去支付
      </view>
      <view v-if="orderDetail.status === 0" class="action-btn cancel-btn" @click="cancelOrder">
        取消订单
      </view>
      <view v-if="orderDetail.status === 2" class="action-btn confirm-btn" @click="confirmOrder">
        确认收货
      </view>
      <view v-if="orderDetail.status === 1" class="action-btn refund-btn" @click="requestRefund">
        申请退款
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder, confirmOrder, getOrderItems, queryRefundStatus } from '@/api/order.js';

const REFUND_STATUS = {
  NONE: '',
  PENDING_APPROVAL: 'REFUND_PENDING_APPROVAL',
  PROCESSING: 'REFUND_PROCESSING',
  SUCCESS: 'REFUND_SUCCESS',
  REJECTED: 'REFUND_REJECTED'
};

export default {
  data() {
    return {
      orderNo: '',
      orderDetail: {
        orderNo: '',
        totalAmount: 0,
        status: 0,
        createTime: '',
        items: []
      },
      loading: false,
      refundStatus: REFUND_STATUS.NONE,
      refundMessage: '',
      lastRefundQueryAt: ''
    };
  },
  computed: {
    isRefundOrder() {
      return this.orderDetail.status === 5 || this.orderDetail.status === 6;
    },
    refundApplied() {
      return this.isRefundOrder;
    },
    reviewPassed() {
      return [
        REFUND_STATUS.PROCESSING,
        REFUND_STATUS.SUCCESS
      ].includes(this.refundStatus);
    },
    refundFinished() {
      return [
        REFUND_STATUS.SUCCESS,
        REFUND_STATUS.REJECTED
      ].includes(this.refundStatus) || this.orderDetail.status === 6;
    },
    refundStatusText() {
      const refundStatusMap = {
        [REFUND_STATUS.PENDING_APPROVAL]: '等待商家审核',
        [REFUND_STATUS.PROCESSING]: '商家已同意，退款处理中',
        [REFUND_STATUS.SUCCESS]: '退款成功',
        [REFUND_STATUS.REJECTED]: '退款被驳回'
      };

      if (this.orderDetail.status === 6 && !this.refundStatus) {
        return '退款成功';
      }
      return refundStatusMap[this.refundStatus] || '';
    },
    refundStatusDesc() {
      if (this.refundMessage) {
        return this.refundMessage;
      }

      const refundDescMap = {
        [REFUND_STATUS.PENDING_APPROVAL]: '退款申请已提交，等待管理员审核',
        [REFUND_STATUS.PROCESSING]: '商家已同意退款，系统正在继续处理',
        [REFUND_STATUS.SUCCESS]: '退款已原路退回，请留意到账通知',
        [REFUND_STATUS.REJECTED]: '本次退款申请未通过，请查看驳回原因'
      };

      if (this.orderDetail.status === 6 && !this.refundStatus) {
        return '退款已完成，请留意支付账户的到账信息';
      }

      return refundDescMap[this.refundStatus] || '退款申请处理中';
    },
    refundBadgeText() {
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '已退款';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '已驳回';
      }
      if (this.refundStatus === REFUND_STATUS.PROCESSING) {
        return '处理中';
      }
      return '退款中';
    },
    refundHeaderTitle() {
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '退款成功';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '退款被驳回';
      }
      if (this.refundStatus === REFUND_STATUS.PROCESSING) {
        return '商家已同意，退款处理中';
      }
      return '等待商家审核';
    },
    refundHeaderDescription() {
      return this.refundStatusDesc;
    },
    refundReasonDescription() {
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '退款申请已提交，当前申请已被驳回。';
      }
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '退款申请已提交，平台已完成后续处理。';
      }
      return '退款申请已提交，正在等待商家审核。';
    },
    refundReviewDescription() {
      if (this.refundStatus === REFUND_STATUS.PENDING_APPROVAL) {
        return '当前还在等待商家审核。';
      }
      if (this.refundStatus === REFUND_STATUS.PROCESSING) {
        return '商家已同意，系统正在处理退款。';
      }
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '商家已处理完成，退款流程已经结束。';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '商家未通过本次退款申请。';
      }
      return '提交申请后，这里会展示商家处理进度。';
    },
    refundFinishedTitle() {
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '退款成功';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '退款被驳回';
      }
      return '等待最终结果';
    },
    refundFinishedDescription() {
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return this.refundMessage || '退款已完成，请留意支付账户的到账信息。';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return this.refundMessage || '本次退款申请未通过，请根据提示重新处理。';
      }
      return '系统会自动同步最新的退款结果。';
    },
    refundThemeClass() {
      if (this.orderDetail.status === 6 || this.refundStatus === REFUND_STATUS.SUCCESS) {
        return 'theme-success';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return 'theme-rejected';
      }
      return 'theme-processing';
    },
    lastRefundQueryTimeText() {
      return this.formatDateTime(this.lastRefundQueryAt);
    }
  },
  onLoad(options) {
    if (options.orderNo) {
      this.orderNo = options.orderNo;
      this.loadOrderDetail();
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
  onShow() {
    if (this.orderNo) {
      this.loadOrderDetail();
    }
  },
  methods: {
    formatDateTime(value) {
      if (!value) {
        return '';
      }

      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return value;
      }

      const yyyy = date.getFullYear();
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      const hh = String(date.getHours()).padStart(2, '0');
      const mi = String(date.getMinutes()).padStart(2, '0');
      const ss = String(date.getSeconds()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
    },
    async loadOrderDetail() {
      if (this.loading) {
        return;
      }

      this.loading = true;
      try {
        const res = await getOrderDetail(this.orderNo);
        if (res.success && res.data) {
          const orderData = res.data;
          const processedOrder = {
            ...orderData,
            status: orderData.status !== undefined ? orderData.status : 0,
            orderNo: orderData.orderNo || '',
            totalAmount: orderData.totalAmount || 0,
            createTime: orderData.createTime || new Date().toISOString()
          };

          let orderItems = [];
          try {
            const itemsRes = await getOrderItems(orderData.id);
            if (itemsRes.success && itemsRes.data) {
              orderItems = Array.isArray(itemsRes.data) ? itemsRes.data : [];
            }
          } catch (error) {
            console.error('获取订单商品失败:', error);
          }

          this.orderDetail = {
            ...processedOrder,
            items: orderItems.map((item) => ({
              ...item,
              productImage: item.productImage || '/static/mall/default-placeholder.png',
              productName: item.productName || '未知商品',
              price: item.price || 0,
              quantity: item.quantity || 1
            }))
          };

          if (processedOrder.status === 5 || processedOrder.status === 6) {
            this.loadRefundStatus();
          } else {
            this.refundStatus = REFUND_STATUS.NONE;
            this.refundMessage = '';
            this.lastRefundQueryAt = '';
          }
        }
      } catch (error) {
        console.error('加载订单详情失败:', error);
        uni.showToast({
          title: '加载订单详情失败',
          icon: 'none'
        });
      } finally {
        this.loading = false;
      }
    },
    async loadRefundStatus() {
      try {
        const res = await queryRefundStatus(this.orderNo);
        if (res.success && res.data) {
          const data = res.data;
          this.refundStatus = data.status || data.refundStatus || REFUND_STATUS.NONE;
          this.refundMessage = data.message || data.reason || data.rejectReason || '';
          this.lastRefundQueryAt = new Date().toISOString();
        } else if (this.orderDetail.status === 6) {
          this.refundStatus = REFUND_STATUS.SUCCESS;
        }
      } catch (error) {
        console.error('加载退款状态失败:', error);
        if (this.orderDetail.status === 6) {
          this.refundStatus = REFUND_STATUS.SUCCESS;
        }
      }
    },
    async cancelOrder() {
      uni.showModal({
        title: '提示',
        content: '确定要取消该订单吗？',
        success: async (modalRes) => {
          if (!modalRes.confirm) {
            return;
          }

          try {
            const res = await cancelOrder(this.orderNo, '用户取消订单');
            if (res.success) {
              uni.showToast({
                title: '取消订单成功',
                icon: 'success'
              });
              this.loadOrderDetail();
            }
          } catch (error) {
            console.error('取消订单失败:', error);
          }
        }
      });
    },
    async confirmOrder() {
      uni.showModal({
        title: '提示',
        content: '确定已经收到商品吗？',
        success: async (modalRes) => {
          if (!modalRes.confirm) {
            return;
          }

          try {
            const res = await confirmOrder(this.orderNo);
            if (res.success) {
              uni.showToast({
                title: '确认收货成功',
                icon: 'success'
              });
              this.loadOrderDetail();
            }
          } catch (error) {
            console.error('确认收货失败:', error);
          }
        }
      });
    },
    requestRefund() {
      uni.navigateTo({
        url: `/pages/mall/refund?orderNo=${this.orderNo}`
      });
    },
    goToPayment() {
      uni.navigateTo({
        url: `/pages/mall/payment?orderNo=${this.orderNo}&amount=${this.orderDetail.totalAmount}`
      });
    },
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
    getStatusDesc(status) {
      const descMap = {
        0: '请在 30 分钟内完成支付',
        1: '商家正在准备商品，请耐心等待',
        2: '商品正在配送途中',
        3: '订单已完成，感谢你的购买',
        4: '订单已取消',
        5: '退款申请处理中',
        6: '退款已完成'
      };
      return descMap[status] || '';
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
.order-detail-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f7f8fb 0%, #eef2f8 100%);
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.nav-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 26rpx;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1rpx solid rgba(25, 53, 103, 0.08);
  backdrop-filter: blur(18rpx);
}

.back-btn,
.nav-right {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
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

.page-content {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.order-status-card,
.order-info-card,
.order-items-card,
.refund-status-card,
.refund-progress-card,
.refund-tips-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  box-shadow: 0 18rpx 42rpx rgba(27, 43, 70, 0.08);
}

.order-status-card {
  padding: 32rpx 28rpx;
  background: linear-gradient(135deg, #275ee8 0%, #4d8dff 100%);
  color: #ffffff;
}

.status-text {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
}

.status-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.86);
}

.refund-status-card {
  padding: 30rpx 28rpx;
  border: 1rpx solid rgba(87, 138, 255, 0.14);
}

.refund-status-card.theme-processing {
  background:
    radial-gradient(circle at top right, rgba(77, 141, 255, 0.16) 0, rgba(77, 141, 255, 0) 36%),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.refund-status-card.theme-success {
  background:
    radial-gradient(circle at top right, rgba(39, 168, 110, 0.16) 0, rgba(39, 168, 110, 0) 36%),
    linear-gradient(180deg, #ffffff 0%, #f6fffa 100%);
}

.refund-status-card.theme-rejected {
  background:
    radial-gradient(circle at top right, rgba(232, 88, 88, 0.14) 0, rgba(232, 88, 88, 0) 36%),
    linear-gradient(180deg, #ffffff 0%, #fff8f8 100%);
}

.refund-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96rpx;
  height: 46rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(58, 109, 255, 0.1);
  color: #2d67ee;
  font-size: 22rpx;
  font-weight: 700;
}

.theme-success .refund-status-badge {
  background: rgba(39, 168, 110, 0.12);
  color: #148256;
}

.theme-rejected .refund-status-badge {
  background: rgba(232, 88, 88, 0.1);
  color: #c84242;
}

.refund-status-title {
  display: block;
  margin-top: 24rpx;
  font-size: 42rpx;
  font-weight: 800;
  color: #13233f;
}

.refund-status-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #56657d;
}

.refund-status-meta {
  margin-top: 22rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.refund-meta-item {
  font-size: 22rpx;
  color: #8090a5;
}

.refund-progress-card,
.order-info-card,
.order-items-card,
.refund-tips-card {
  padding: 28rpx;
}

.card-title {
  display: block;
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid rgba(17, 34, 68, 0.08);
  font-size: 32rpx;
  font-weight: 800;
  color: #10213d;
}

.card-title.no-divider {
  margin-bottom: 18rpx;
  padding-bottom: 0;
  border-bottom: 0;
}

.progress-item {
  position: relative;
  display: flex;
  gap: 18rpx;
  padding-bottom: 24rpx;
  opacity: 0.42;
}

.progress-item:last-child {
  padding-bottom: 0;
}

.progress-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 10rpx;
  top: 34rpx;
  width: 2rpx;
  height: calc(100% - 12rpx);
  background: #d9e0ec;
}

.progress-item.active {
  opacity: 1;
}

.progress-item.active::after {
  background: linear-gradient(180deg, #2f72f4 0%, #85aefc 100%);
}

.progress-dot {
  position: relative;
  z-index: 1;
  width: 22rpx;
  height: 22rpx;
  margin-top: 10rpx;
  border-radius: 50%;
  background: #d9e0ec;
  flex-shrink: 0;
}

.progress-item.active .progress-dot {
  background: #2f72f4;
  box-shadow: 0 0 0 10rpx rgba(47, 114, 244, 0.12);
}

.progress-content {
  flex: 1;
}

.progress-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #13233f;
}

.progress-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7b8798;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 20rpx;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-row.align-start {
  align-items: flex-start;
}

.info-label {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #78869a;
}

.info-value {
  font-size: 25rpx;
  line-height: 1.6;
  color: #23324d;
  text-align: right;
}

.info-value-strong {
  font-weight: 700;
}

.info-value.price,
.product-price {
  color: #e94d3d;
  font-weight: 800;
}

.refund-status-text-group {
  max-width: 68%;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
}

.refund-value {
  color: #2f72f4;
  font-weight: 700;
}

.refund-inline-desc {
  font-size: 22rpx;
  line-height: 1.6;
  color: #7b8798;
  text-align: right;
}

.order-item {
  display: flex;
  gap: 18rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(17, 34, 68, 0.06);
}

.order-item:last-of-type {
  border-bottom: 0;
  padding-bottom: 0;
}

.product-image {
  width: 152rpx;
  height: 152rpx;
  border-radius: 22rpx;
  background: #f2f5fb;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 28rpx;
  line-height: 1.6;
  color: #172742;
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-quantity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
}

.product-price,
.product-quantity {
  font-size: 26rpx;
}

.product-quantity {
  color: #7c8ca2;
}

.empty-items {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180rpx;
  border-radius: 20rpx;
  background: #f7f9fc;
}

.empty-items-text {
  font-size: 24rpx;
  color: #92a0b3;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-dot {
  width: 10rpx;
  height: 10rpx;
  margin-top: 14rpx;
  border-radius: 50%;
  background: #2f72f4;
  flex-shrink: 0;
}

.tip-text {
  font-size: 25rpx;
  line-height: 1.8;
  color: #5c6c82;
}

.order-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.98);
  border-top: 1rpx solid rgba(17, 34, 68, 0.08);
  backdrop-filter: blur(18rpx);
}

.action-btn {
  min-width: 176rpx;
  height: 78rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.cancel-btn {
  border: 1rpx solid #d8deea;
  color: #67768c;
  background: #ffffff;
}

.confirm-btn,
.pay-btn {
  background: linear-gradient(135deg, #286df1 0%, #3e88ff 100%);
  color: #ffffff;
  box-shadow: 0 14rpx 28rpx rgba(47, 114, 244, 0.22);
}

.refund-btn {
  border: 1rpx solid rgba(47, 114, 244, 0.24);
  color: #2f72f4;
  background: rgba(47, 114, 244, 0.06);
}
</style>
