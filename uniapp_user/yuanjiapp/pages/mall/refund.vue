<template>
  <view class="refund-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">申请退款</text>
      <view class="nav-right"></view>
    </view>

    <view class="refund-content">
      <view class="status-card" :class="statusThemeClass">
        <text class="status-badge">{{ refundStatusText }}</text>
        <text class="status-title">{{ refundStatusTitle }}</text>
        <text class="status-desc">{{ refundStatusDescription }}</text>
        <view class="status-meta">
          <text class="meta-item">订单号：{{ orderNo }}</text>
          <text v-if="lastQueryTimeText" class="meta-item">最近刷新：{{ lastQueryTimeText }}</text>
        </view>
      </view>

      <view v-if="showForm" class="refund-form">
        <view class="form-item">
          <text class="label">订单号</text>
          <text class="value">{{ orderNo }}</text>
        </view>

        <view class="form-item">
          <text class="label">退款原因</text>
          <picker @change="onReasonChange" :value="reasonIndex" :range="reasons">
            <view class="picker">
              {{ reasons[reasonIndex] || '请选择退款原因' }}
              <text class="arrow">></text>
            </view>
          </picker>
        </view>

        <view class="form-item textarea-item">
          <text class="label">详细说明</text>
          <textarea
            v-model="description"
            class="textarea"
            placeholder="请输入退款详细说明（选填）"
            :maxlength="200"
          />
          <text class="char-count">{{ description.length }}/200</text>
        </view>
      </view>

      <view v-else class="progress-card">
        <text class="card-title">退款进度</text>
        <view class="progress-item" :class="{ active: refundApplied }">
          <view class="progress-dot"></view>
          <view class="progress-content">
            <text class="progress-title">退款申请已提交</text>
            <text class="progress-desc">{{ refundReasonText || '用户已提交退款申请，等待商家处理。' }}</text>
          </view>
        </view>
        <view class="progress-item" :class="{ active: reviewPassed }">
          <view class="progress-dot"></view>
          <view class="progress-content">
            <text class="progress-title">商家处理中</text>
            <text class="progress-desc">{{ processingDescription }}</text>
          </view>
        </view>
        <view class="progress-item" :class="{ active: isFinished }">
          <view class="progress-dot"></view>
          <view class="progress-content">
            <text class="progress-title">{{ finishedTitle }}</text>
            <text class="progress-desc">{{ finishedDescription }}</text>
          </view>
        </view>
      </view>

      <view class="tips-card">
        <text class="tips-title">退款说明</text>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">提交申请后会先进入退款中，订单状态更新为退款中。</text>
        </view>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">商家审核通过后，系统会继续处理退款并自动同步结果。</text>
        </view>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">如果退款被驳回，请根据提示重新与商家沟通或再次发起申请。</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view
        v-if="showForm"
        class="submit-btn"
        :class="{ disabled: submitting }"
        @click="submitRefund"
      >
        {{ submitting ? '提交中...' : '提交申请' }}
      </view>
      <view
        v-else-if="!isFinished"
        class="submit-btn secondary-btn"
        :class="{ disabled: pollingRefund }"
        @click="manualQueryRefundStatus"
      >
        {{ pollingRefund ? '查询中...' : '刷新退款状态' }}
      </view>
      <view v-else class="submit-btn" @click="goBack">
        返回订单
      </view>
    </view>
  </view>
</template>

<script>
import { queryRefundStatus, requestRefund } from '@/api/order.js';

const REFUND_STATUS = {
  NONE: '',
  PENDING_APPROVAL: 'REFUND_PENDING_APPROVAL',
  PROCESSING: 'REFUND_PROCESSING',
  SUCCESS: 'REFUND_SUCCESS',
  REJECTED: 'REFUND_REJECTED'
};

const QUERY_INTERVAL = 3000;

export default {
  data() {
    return {
      orderNo: '',
      reasonIndex: -1,
      reasons: [
        '不想要了',
        '商品信息描述不符',
        '商品质量问题',
        '发货太慢',
        '其他原因'
      ],
      description: '',
      submitting: false,
      pollingRefund: false,
      refundStatus: REFUND_STATUS.NONE,
      refundMessage: '',
      refundReasonText: '',
      lastQueryAt: '',
      pollingTimer: null
    };
  },
  computed: {
    showForm() {
      return this.refundStatus === REFUND_STATUS.NONE;
    },
    refundApplied() {
      return this.refundStatus !== REFUND_STATUS.NONE;
    },
    reviewPassed() {
      return this.refundStatus === REFUND_STATUS.PROCESSING || this.refundStatus === REFUND_STATUS.SUCCESS;
    },
    isFinished() {
      return this.refundStatus === REFUND_STATUS.SUCCESS || this.refundStatus === REFUND_STATUS.REJECTED;
    },
    refundStatusText() {
      const map = {
        [REFUND_STATUS.NONE]: '待提交',
        [REFUND_STATUS.PENDING_APPROVAL]: '退款中',
        [REFUND_STATUS.PROCESSING]: '退款处理中',
        [REFUND_STATUS.SUCCESS]: '退款成功',
        [REFUND_STATUS.REJECTED]: '已驳回'
      };
      return map[this.refundStatus] || '退款中';
    },
    refundStatusTitle() {
      const map = {
        [REFUND_STATUS.NONE]: '填写退款申请',
        [REFUND_STATUS.PENDING_APPROVAL]: '等待商家审核',
        [REFUND_STATUS.PROCESSING]: '商家已同意，退款处理中',
        [REFUND_STATUS.SUCCESS]: '退款成功',
        [REFUND_STATUS.REJECTED]: '退款被驳回'
      };
      return map[this.refundStatus] || '退款申请处理中';
    },
    refundStatusDescription() {
      if (this.refundMessage) {
        return this.refundMessage;
      }
      const map = {
        [REFUND_STATUS.NONE]: '提交退款申请后，系统会自动跟踪退款状态。',
        [REFUND_STATUS.PENDING_APPROVAL]: '退款申请已提交，请等待商家审核处理。',
        [REFUND_STATUS.PROCESSING]: '商家已同意退款，平台正在处理退款流程。',
        [REFUND_STATUS.SUCCESS]: '退款已经原路退回，请注意查收到账信息。',
        [REFUND_STATUS.REJECTED]: '商家暂未通过退款申请，请查看原因后再处理。'
      };
      return map[this.refundStatus] || '退款申请处理中';
    },
    processingDescription() {
      if (this.refundStatus === REFUND_STATUS.PENDING_APPROVAL) {
        return '当前还在等待商家审核。';
      }
      if (this.refundStatus === REFUND_STATUS.PROCESSING) {
        return '商家已同意，系统正在处理退款。';
      }
      if (this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '退款流程已完成。';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '商家未通过审核，退款流程已结束。';
      }
      return '提交申请后会在这里展示最新进度。';
    },
    finishedTitle() {
      if (this.refundStatus === REFUND_STATUS.SUCCESS) {
        return '退款成功';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return '退款被驳回';
      }
      return '等待最终结果';
    },
    finishedDescription() {
      if (this.refundStatus === REFUND_STATUS.SUCCESS) {
        return this.refundMessage || '退款已完成，请留意支付账户的到账信息。';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return this.refundMessage || '本次退款申请未通过，请根据提示重新处理。';
      }
      return '系统会自动更新最终结果。';
    },
    statusThemeClass() {
      if (this.refundStatus === REFUND_STATUS.SUCCESS) {
        return 'theme-success';
      }
      if (this.refundStatus === REFUND_STATUS.REJECTED) {
        return 'theme-rejected';
      }
      if (this.refundStatus === REFUND_STATUS.NONE) {
        return 'theme-default';
      }
      return 'theme-processing';
    },
    lastQueryTimeText() {
      if (!this.lastQueryAt) {
        return '';
      }
      const date = new Date(this.lastQueryAt);
      if (Number.isNaN(date.getTime())) {
        return this.lastQueryAt;
      }
      const yyyy = date.getFullYear();
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      const hh = String(date.getHours()).padStart(2, '0');
      const mi = String(date.getMinutes()).padStart(2, '0');
      const ss = String(date.getSeconds()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
    }
  },
  onLoad(options) {
    if (options.orderNo) {
      this.orderNo = options.orderNo;
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
  onUnload() {
    this.clearRefundPolling();
  },
  methods: {
    goBack() {
      this.clearRefundPolling();
      uni.navigateBack({
        delta: 1
      });
    },
    onReasonChange(e) {
      this.reasonIndex = Number(e.detail.value);
    },
    normalizeRefundStatus(data) {
      if (!data) {
        return REFUND_STATUS.NONE;
      }
      if (typeof data === 'string') {
        return data;
      }
      if (typeof data === 'object') {
        return data.status || data.refundStatus || data.dataStatus || REFUND_STATUS.NONE;
      }
      return REFUND_STATUS.NONE;
    },
    normalizeRefundMessage(data) {
      if (!data || typeof data !== 'object') {
        return '';
      }
      return data.message || data.reason || data.rejectReason || '';
    },
    updateRefundState(data) {
      const status = this.normalizeRefundStatus(data);
      const message = this.normalizeRefundMessage(data);

      if (!Object.values(REFUND_STATUS).includes(status) || status === REFUND_STATUS.NONE) {
        return false;
      }

      this.refundStatus = status;
      this.refundMessage = message;
      this.lastQueryAt = new Date().toISOString();
      return true;
    },
    async submitRefund() {
      if (this.reasonIndex === -1) {
        uni.showToast({
          title: '请选择退款原因',
          icon: 'none'
        });
        return;
      }

      if (this.submitting) {
        return;
      }

      this.submitting = true;
      try {
        const baseReason = this.reasons[this.reasonIndex];
        const reason = this.description ? `${baseReason}：${this.description}` : baseReason;
        const res = await requestRefund(this.orderNo, reason);

        if (res.success) {
          this.refundReasonText = reason;
          this.refundStatus = REFUND_STATUS.PENDING_APPROVAL;
          this.refundMessage = '退款申请已提交，请等待商家审核';
          this.lastQueryAt = new Date().toISOString();
          uni.showToast({
            title: '退款申请已提交',
            icon: 'success'
          });
          this.startRefundPolling();
        } else {
          uni.showToast({
            title: res.message || '退款申请失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('提交退款申请失败:', error);
        uni.showToast({
          title: error?.message || '退款申请失败',
          icon: 'none'
        });
      } finally {
        this.submitting = false;
      }
    },
    async queryRefundStatusOnce(showToast = false) {
      if (!this.orderNo || this.pollingRefund) {
        return;
      }

      this.pollingRefund = true;
      try {
        const res = await queryRefundStatus(this.orderNo);
        if (res.success && typeof res.data !== 'undefined') {
          const loaded = this.updateRefundState(res.data);
          if (!loaded && showToast) {
            uni.showToast({
              title: res.message || '暂无退款进度',
              icon: 'none'
            });
          }

          if (this.isFinished) {
            this.clearRefundPolling();
          } else if (showToast) {
            uni.showToast({
              title: this.refundStatusDescription,
              icon: 'none'
            });
          }
        } else if (showToast) {
          uni.showToast({
            title: res.message || '查询退款状态失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('查询退款状态失败:', error);
        if (showToast) {
          uni.showToast({
            title: error?.message || '查询退款状态失败',
            icon: 'none'
          });
        }
      } finally {
        this.pollingRefund = false;
      }
    },
    startRefundPolling() {
      this.clearRefundPolling();
      this.queryRefundStatusOnce(false);
      this.pollingTimer = setInterval(() => {
        this.queryRefundStatusOnce(false);
      }, QUERY_INTERVAL);
    },
    clearRefundPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
    },
    manualQueryRefundStatus() {
      this.queryRefundStatusOnce(true);
    }
  }
};
</script>

<style scoped>
.refund-container {
  min-height: 100vh;
  background-color: #f5f7fb;
  padding-bottom: calc(114px + env(safe-area-inset-bottom));
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  padding: 10px 15px;
  border-bottom: 1px solid #eef1f5;
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
  font-weight: 700;
  color: #333;
}

.nav-right {
  width: 40px;
}

.refund-content {
  padding: 14px 15px 24px;
}

.status-card,
.refund-form,
.progress-card,
.tips-card {
  background-color: #fff;
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 14px;
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.05);
}

.status-card.theme-default {
  border: 1px solid #e6ebf2;
}

.status-card.theme-processing {
  border: 1px solid #d6e6ff;
  background: linear-gradient(180deg, #ffffff 0%, #f6faff 100%);
}

.status-card.theme-success {
  border: 1px solid #d9f0dc;
  background: linear-gradient(180deg, #ffffff 0%, #f6fcf7 100%);
}

.status-card.theme-rejected {
  border: 1px solid #ffe0da;
  background: linear-gradient(180deg, #ffffff 0%, #fff7f5 100%);
}

.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 999px;
  background-color: #eef3ff;
  color: #3c6cff;
  font-size: 12px;
  margin-bottom: 12px;
}

.theme-success .status-badge {
  background-color: #e8f7eb;
  color: #2b8a3e;
}

.theme-rejected .status-badge {
  background-color: #ffede8;
  color: #d94841;
}

.status-title {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #1f2a37;
  margin-bottom: 8px;
}

.status-desc {
  display: block;
  font-size: 14px;
  line-height: 1.7;
  color: #5f6b7a;
}

.status-meta {
  margin-top: 12px;
}

.meta-item {
  display: block;
  font-size: 12px;
  color: #8a94a6;
  line-height: 1.8;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f0f2f5;
}

.form-item:last-child {
  border-bottom: none;
}

.label {
  width: 100px;
  font-size: 14px;
  color: #333;
}

.value {
  flex: 1;
  font-size: 14px;
  color: #666;
  word-break: break-all;
}

.picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
}

.arrow {
  color: #999;
  margin-left: 10px;
}

.textarea-item {
  flex-direction: column;
  align-items: flex-start;
}

.textarea {
  width: 100%;
  min-height: 100px;
  margin-top: 10px;
  padding: 10px;
  box-sizing: border-box;
  background-color: #f9fafc;
  border-radius: 10px;
  font-size: 14px;
  color: #333;
}

.char-count {
  align-self: flex-end;
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.card-title,
.tips-title {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #1f2a37;
  margin-bottom: 14px;
}

.progress-item {
  display: flex;
  align-items: flex-start;
  opacity: 0.55;
  margin-bottom: 16px;
}

.progress-item:last-child {
  margin-bottom: 0;
}

.progress-item.active {
  opacity: 1;
}

.progress-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #cfd7e6;
  margin-right: 12px;
  margin-top: 4px;
  flex-shrink: 0;
}

.progress-item.active .progress-dot {
  background-color: #1677ff;
}

.theme-success .progress-dot,
.progress-item.active:last-child .progress-dot {
  background-color: #2b8a3e;
}

.theme-rejected .progress-item.active:last-child .progress-dot {
  background-color: #d94841;
}

.progress-content {
  flex: 1;
}

.progress-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2a37;
  margin-bottom: 6px;
}

.progress-desc {
  display: block;
  font-size: 13px;
  line-height: 1.6;
  color: #6b7788;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #1677ff;
  margin-top: 8px;
  margin-right: 10px;
  flex-shrink: 0;
}

.tip-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.7;
  color: #5f6b7a;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.96);
  border-top: 1px solid #eef1f5;
  padding: 12px 15px calc(18px + env(safe-area-inset-bottom));
}

.submit-btn {
  width: 100%;
  padding: 13px 0;
  background-color: #ff4d4f;
  color: #fff;
  text-align: center;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
}

.secondary-btn {
  background-color: #1677ff;
}

.disabled {
  opacity: 0.6;
}
</style>
