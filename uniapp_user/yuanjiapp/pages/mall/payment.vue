<template>
  <view class="payment-container">
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <image src="/static/mall/back-button.png" class="back-icon" />
      </view>
      <text class="nav-title">支付订单</text>
      <view class="nav-right"></view>
    </view>

    <view class="payment-content">
      <view class="hero-card">
        <view class="hero-header">
          <view>
            <text class="hero-caption">订单待支付</text>
            <text class="hero-order-no">订单编号：{{ orderNo }}</text>
          </view>
          <text class="status-pill" :class="statusThemeClass">{{ statusBadgeText }}</text>
        </view>
        <text class="hero-amount-label">支付金额</text>
        <text class="hero-amount">¥{{ orderAmount }}</text>
        <view class="hero-footer">
          <view class="hero-info-box">
            <text class="hero-info-label">剩余支付时间</text>
            <text class="hero-info-value countdown-value">{{ countdownText }}</text>
          </view>
          <view class="hero-info-box hero-channel-box">
            <text class="channel-tag">支付宝</text>
            <text class="hero-info-value">扫码支付</text>
          </view>
        </view>
      </view>

      <view v-if="qrCodeUrl" class="qr-card">
        <view class="section-header">
          <view>
            <text class="section-title">支付宝扫一扫</text>
            <text class="section-subtitle">请使用支付宝扫描二维码完成付款</text>
          </view>
          <text class="section-mark">当面付</text>
        </view>

        <view class="qr-frame">
          <!-- #ifdef H5 -->
          <image :src="qrCodeUrl" class="qr-code" mode="aspectFit" />
          <!-- #endif -->

          <!-- #ifndef H5 -->
          <!-- 非H5环境：显示生成的二维码图片 -->
          <image v-if="qrCodeUrl" :src="qrCodeUrl" class="qr-code" mode="aspectFit" />
          <view v-else class="qr-placeholder">正在生成二维码...</view>
          <!-- #endif -->
        </view>

        <text class="qr-code-tip">付款完成后页面会自动刷新支付结果，请勿重复支付。</text>

        <view class="action-row" v-if="paymentLink">
          <view class="ghost-btn" @click="copyPaymentLink">复制付款链接</view>
          <view
            class="primary-btn"
            :class="{ 'is-disabled': statusLoading }"
            @click="manualCheckStatus"
          >
            {{ statusLoading ? '正在确认...' : '我已完成支付' }}
          </view>
        </view>
      </view>

      <view v-else-if="loading" class="placeholder-card">
        <text class="placeholder-title">正在生成付款二维码</text>
        <text class="placeholder-desc">请稍候，系统正在为你创建支付宝支付订单。</text>
      </view>

      <view v-else class="placeholder-card error-state">
        <text class="placeholder-title">{{ qrCodeError || '二维码生成失败' }}</text>
        <text class="placeholder-desc">你可以返回订单页重新发起支付，或复制付款链接继续测试。</text>
        <view class="action-row" v-if="paymentLink">
          <view class="ghost-btn" @click="copyPaymentLink">复制付款链接</view>
        </view>
      </view>

      <view class="result-card" :class="statusThemeClass">
        <view class="result-header">
          <view>
            <text class="result-title">{{ statusTitle }}</text>
            <text class="result-subtitle">{{ statusHintText }}</text>
          </view>
          <text class="result-trade-status">{{ tradeStatusText }}</text>
        </view>
        <text class="result-message">{{ statusMessage }}</text>
      </view>

      <view class="detail-card">
        <text class="detail-title">支付详情</text>

        <view class="detail-row">
          <text class="detail-label">支付方式</text>
          <text class="detail-value">{{ paymentMethodText }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">支付流水状态</text>
          <text class="detail-value">{{ paymentFlowText }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">订单状态</text>
          <text class="detail-value">{{ orderStatusText }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">交易状态</text>
          <text class="detail-value">{{ tradeStatusText }}</text>
        </view>
        <view v-if="paymentQuery.tradeNo" class="detail-row align-top">
          <text class="detail-label">支付宝交易号</text>
          <text class="detail-value multiline-value">{{ paymentQuery.tradeNo }}</text>
        </view>
        <view v-if="formattedPaymentTime" class="detail-row">
          <text class="detail-label">支付时间</text>
          <text class="detail-value">{{ formattedPaymentTime }}</text>
        </view>
        <view v-if="lastQueryTimeText" class="detail-row">
          <text class="detail-label">最近刷新</text>
          <text class="detail-value">{{ lastQueryTimeText }}</text>
        </view>
      </view>

      <view class="tips-card">
        <text class="tips-title">支付说明</text>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">请使用本人支付宝扫码完成付款，避免重复支付。</text>
        </view>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">支付成功后订单会自动更新为待发货，无需手动刷新订单状态。</text>
        </view>
        <view class="tip-item">
          <text class="tip-dot"></text>
          <text class="tip-text">如果已经付款但页面还未变化，可以点击“我已完成支付”立即确认结果。</text>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="bottom-btn" @click="cancelPayment">
        {{ paymentStatus === 0 ? '取消支付' : '返回订单' }}
      </view>
    </view>
  </view>
</template>

<script>
import { createAlipayOrder, queryAlipayStatus } from '@/api/order.js';

const DEFAULT_COUNTDOWN_SECONDS = 30 * 60;

// 简化的 QRCode 生成器（纯 JavaScript 实现，兼容手机端）
const QRCode = {
  // 生成二维码数据矩阵
  generateMatrix(text) {
    // 使用简化的矩阵生成算法
    const size = Math.max(21, Math.ceil(text.length / 2) + 21);
    const matrix = Array(size).fill(null).map(() => Array(size).fill(0));
    
    // 添加定位图案（左上角）
    this.addFinderPattern(matrix, 0, 0);
    this.addFinderPattern(matrix, size - 7, 0);
    this.addFinderPattern(matrix, 0, size - 7);
    
    // 添加时序图案
    for (let i = 8; i < size - 8; i++) {
      matrix[6][i] = i % 2 === 0 ? 1 : 0;
      matrix[i][6] = i % 2 === 0 ? 1 : 0;
    }
    
    // 添加对齐图案
    const alignPos = this.getAlignPosition(size);
    alignPos.forEach(x => {
      alignPos.forEach(y => {
        if (!(x < 9 && y < 9) && !(x < 9 && y > size - 10) && !(x > size - 10 && y < 9)) {
          this.addAlignmentPattern(matrix, x, y);
        }
      });
    });
    
    // 填充数据
    const dataBits = this.encodeData(text);
    this.fillData(matrix, dataBits, size);
    
    return matrix;
  },
  
  addFinderPattern(matrix, row, col) {
    for (let r = 0; r < 7; r++) {
      for (let c = 0; c < 7; c++) {
        if (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
          matrix[row + r][col + c] = 1;
        } else {
          matrix[row + r][col + c] = 0;
        }
      }
    }
    // 添加分隔符
    for (let i = 0; i < 8; i++) {
      if (row - 1 >= 0) matrix[row - 1][col + i] = 0;
      if (row + 7 < matrix.length) matrix[row + 7][col + i] = 0;
      if (col - 1 >= 0) matrix[row + i][col - 1] = 0;
      if (col + 7 < matrix.length) matrix[row + i][col + 7] = 0;
    }
  },
  
  addAlignmentPattern(matrix, row, col) {
    for (let r = -2; r <= 2; r++) {
      for (let c = -2; c <= 2; c++) {
        if (r === -2 || r === 2 || c === -2 || c === 2 || (r === 0 && c === 0)) {
          matrix[row + r][col + c] = 1;
        } else {
          matrix[row + r][col + c] = 0;
        }
      }
    }
  },
  
  getAlignPosition(size) {
    if (size < 25) return [6];
    if (size < 45) return [6, size - 7];
    if (size < 65) return [6, size - 13, size - 7];
    return [6, size - 21, size - 13, size - 7];
  },
  
  encodeData(text) {
    const bits = [];
    text.split('').forEach(char => {
      bits.push(char.charCodeAt(0));
    });
    return bits;
  },
  
  fillData(matrix, dataBits, size) {
    let bitIndex = 0;
    let upward = true;
    
    for (let col = size - 1; col > 0; col -= 2) {
      if (col === 6) col = 5;
      
      const rows = upward ? Array.from({length: size}, (_, i) => size - 1 - i) : Array.from({length: size}, (_, i) => i);
      
      for (let row of rows) {
        for (let c = 0; c < 2; c++) {
          const currentCol = col - c;
          if (matrix[row][currentCol] === 0 && bitIndex < dataBits.length * 8) {
            const byteIndex = Math.floor(bitIndex / 8);
            const bitOffset = 7 - (bitIndex % 8);
            matrix[row][currentCol] = (dataBits[byteIndex] >> bitOffset) & 1 ? 1 : 0;
            bitIndex++;
          }
        }
      }
      upward = !upward;
    }
  },
  
  // 生成 canvas 二维码图片
  toCanvas(canvas, text, options = {}) {
    const matrix = this.generateMatrix(text);
    const size = matrix.length;
    const pixelSize = (options.width || 250) / size;
    
    canvas.width = options.width || 250;
    canvas.height = options.height || 250;
    
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = options.color?.dark || '#111111';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    ctx.fillStyle = options.color?.light || '#FFFFFF';
    
    for (let row = 0; row < size; row++) {
      for (let col = 0; col < size; col++) {
        if (matrix[row][col] === 0) {
          ctx.fillRect(col * pixelSize, row * pixelSize, pixelSize, pixelSize);
        }
      }
    }
    
    return canvas;
  },
  
  // 生成 DataURL
  toDataURL(text, options = {}) {
    const canvas = document.createElement('canvas');
    this.toCanvas(canvas, text, options);
    return canvas.toDataURL('image/png');
  }
};

const createDefaultPaymentQuery = () => ({
  orderNo: '',
  paid: false,
  finished: false,
  orderStatus: 0,
  paymentStatus: 0,
  tradeStatus: 'WAIT_BUYER_PAY',
  tradeNo: '',
  paymentMethod: 'alipay',
  paymentTime: '',
  message: '等待付款'
});

export default {
  data() {
    return {
      orderNo: '',
      orderAmount: 0,
      qrCodeUrl: '',
      paymentLink: '',
      qrCodeError: '',
      paymentStatus: 0,
      paymentQuery: createDefaultPaymentQuery(),
      pollingTimer: null,
      timeoutTimer: null,
      redirectTimer: null,
      countdownTimer: null,
      loading: false,
      statusLoading: false,
      finalStateHandled: false,
      lastQueryAt: '',
      remainingSeconds: DEFAULT_COUNTDOWN_SECONDS
    };
  },
  computed: {
    statusBadgeText() {
      if (this.paymentStatus === 1 || this.paymentQuery.paid) {
        return '已支付';
      }
      if (this.paymentStatus === 2 || this.paymentQuery.finished) {
        return '已关闭';
      }
      return this.statusLoading ? '确认中' : '待付款';
    },
    statusHintText() {
      if (this.paymentStatus === 1 || this.paymentQuery.paid) {
        return '款项已确认到账，订单即将进入发货流程';
      }
      if (this.paymentStatus === 2 || this.paymentQuery.finished) {
        return '本次支付流程已结束，如需购买请重新发起支付';
      }
      return '请在倒计时结束前完成付款，系统会自动同步支付结果';
    },
    statusTitle() {
      if (this.paymentStatus === 1 || this.paymentQuery.paid) {
        return '支付成功';
      }
      if (this.paymentStatus === 2 || this.paymentQuery.finished) {
        return '交易已关闭';
      }
      return '等待付款';
    },
    statusMessage() {
      if (this.paymentQuery.message) {
        return this.paymentQuery.message;
      }
      if (this.paymentStatus === 1 || this.paymentQuery.paid) {
        return '付款完成后订单状态会自动更新。';
      }
      if (this.paymentStatus === 2 || this.paymentQuery.finished) {
        return '本次支付未成功完成。';
      }
      return '请使用支付宝扫码完成付款。';
    },
    statusThemeClass() {
      if (this.paymentStatus === 1 || this.paymentQuery.paid) {
        return 'theme-success';
      }
      if (this.paymentStatus === 2 || this.paymentQuery.finished) {
        return 'theme-closed';
      }
      return 'theme-waiting';
    },
    paymentFlowText() {
      const statusMap = {
        0: '待支付',
        1: '支付成功',
        2: '支付关闭'
      };
      return statusMap[this.paymentQuery.paymentStatus] || '未知';
    },
    orderStatusText() {
      const statusMap = {
        0: '待付款',
        1: '待发货',
        2: '待收货',
        3: '已完成',
        4: '已取消',
        5: '退款中',
        6: '已退款'
      };
      return statusMap[this.paymentQuery.orderStatus] || '未知状态';
    },
    paymentMethodText() {
      const method = (this.paymentQuery.paymentMethod || 'alipay').toLowerCase();
      return method === 'alipay' ? '支付宝' : this.paymentQuery.paymentMethod || '支付宝';
    },
    tradeStatusText() {
      const statusMap = {
        WAIT_BUYER_PAY: '等待买家付款',
        TRADE_SUCCESS: '支付成功',
        TRADE_FINISHED: '交易完成',
        TRADE_CLOSED: '交易关闭',
        TRADE_NOT_EXIST: '支付单不存在'
      };
      return statusMap[this.paymentQuery.tradeStatus] || this.paymentQuery.tradeStatus || '等待买家付款';
    },
    formattedPaymentTime() {
      return this.formatDateTime(this.paymentQuery.paymentTime);
    },
    lastQueryTimeText() {
      return this.formatDateTime(this.lastQueryAt);
    },
    countdownText() {
      const minutes = String(Math.floor(this.remainingSeconds / 60)).padStart(2, '0');
      const seconds = String(this.remainingSeconds % 60).padStart(2, '0');
      return `${minutes}:${seconds}`;
    }
  },
  onLoad(options) {
    console.log('支付页面接收到的参数:', options);
    if (options.orderNo) {
      this.orderNo = options.orderNo;
      this.orderAmount = options.amount || 0;
      this.paymentQuery.orderNo = options.orderNo;
      this.initPayment();
    } else {
      uni.showToast({ title: '订单号不存在', icon: 'none' });
      setTimeout(() => uni.navigateBack(), 1500);
    }
  },
  onUnload() {
    this.clearPolling();
  },
  methods: {
    isRequestSuccess(res) {
      if (!res) {
        return false;
      }
      if (typeof res.success === 'boolean') {
        return res.success;
      }
      if (typeof res.code !== 'undefined') {
        return Number(res.code) === 200;
      }
      return true;
    },
    isImageSource(value) {
      if (!value || typeof value !== 'string') {
        return false;
      }
      return /^data:image\//.test(value) || /\.(png|jpe?g|gif|webp|svg)(\?.*)?$/i.test(value);
    },
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
    normalizeQueryData(data) {
      const base = createDefaultPaymentQuery();

      if (data && typeof data === 'object' && !Array.isArray(data)) {
        const numericOrderStatus = Number(data.orderStatus);
        const numericPaymentStatus = Number(data.paymentStatus);
        const tradeNotExist = data.tradeStatus === 'TRADE_NOT_EXIST';
        const paid =
          typeof data.paid === 'boolean'
            ? data.paid
            : numericPaymentStatus === 1 || ['TRADE_SUCCESS', 'TRADE_FINISHED'].includes(data.tradeStatus);
        const finished =
          typeof data.finished === 'boolean'
            ? data.finished
            : paid || numericPaymentStatus === 2 || data.tradeStatus === 'TRADE_CLOSED' || tradeNotExist;
        const tradeStatus = data.tradeStatus || (paid ? 'TRADE_SUCCESS' : finished ? 'TRADE_CLOSED' : base.tradeStatus);
        const orderStatus = Number.isNaN(numericOrderStatus)
          ? paid
            ? 1
            : base.orderStatus
          : numericOrderStatus;
        const paymentStatus = Number.isNaN(numericPaymentStatus)
          ? paid
            ? 1
            : finished
              ? 2
              : base.paymentStatus
          : numericPaymentStatus;
        const message = data.message || (paid
          ? '付款已完成'
          : tradeNotExist
            ? '支付单不存在，请重新发起支付'
            : finished
              ? '本次支付已结束'
              : '等待付款');

        return {
          ...base,
          ...data,
          orderNo: data.orderNo || this.orderNo,
          paid,
          finished,
          orderStatus,
          paymentStatus,
          tradeStatus,
          message
        };
      }

      const numericStatus = parseInt(data, 10);
      if (numericStatus === 1) {
        return {
          ...base,
          orderNo: this.orderNo,
          paid: true,
          finished: true,
          orderStatus: 1,
          paymentStatus: 1,
          tradeStatus: 'TRADE_SUCCESS',
          message: '付款已完成'
        };
      }

      if (numericStatus === 2) {
        return {
          ...base,
          orderNo: this.orderNo,
          paid: false,
          finished: true,
          orderStatus: 0,
          paymentStatus: 2,
          tradeStatus: 'TRADE_CLOSED',
          message: '本次支付已关闭'
        };
      }

      return {
        ...base,
        orderNo: this.orderNo,
        paid: false,
        finished: false,
        orderStatus: 0,
        paymentStatus: 0,
        tradeStatus: 'WAIT_BUYER_PAY',
        message: '等待付款'
      };
    },
    // H5 环境：使用浏览器原生 Canvas 生成二维码
    renderQrCodeH5(content) {
      const canvas = document.createElement('canvas');
      QRCode.toCanvas(canvas, content, {
        width: 250,
        margin: 1,
        color: {
          dark: '#111111',
          light: '#FFFFFF'
        }
      });
      this.qrCodeUrl = canvas.toDataURL('image/png');
      console.log('H5二维码生成成功');
    },

    // APP/小程序环境：使用 uni canvas 生成二维码
    renderQrCodeApp(content) {
      try {
        console.log('开始APP二维码生成...');
        const size = 250;

        // 创建离屏canvas进行绘制
        const offscreenCanvas = uni.createOffscreenCanvas({
          width: size,
          height: size,
          type: '2d'
        });

        if (offscreenCanvas) {
          const ctx = offscreenCanvas.getContext('2d');
          const matrix = QRCode.generateMatrix(content);
          const matrixSize = matrix.length;
          const pixelSize = size / matrixSize;

          // 绘制白色背景
          ctx.fillStyle = '#FFFFFF';
          ctx.fillRect(0, 0, size, size);

          // 绘制黑色方块
          ctx.fillStyle = '#111111';
          for (let row = 0; row < matrixSize; row++) {
            for (let col = 0; col < matrixSize; col++) {
              if (matrix[row][col] === 1) {
                ctx.fillRect(col * pixelSize, row * pixelSize, pixelSize, pixelSize);
              }
            }
          }

          // 获取图片数据
          const imageData = offscreenCanvas.toDataURL('image/png');
          console.log('APP离屏Canvas二维码生成成功');
          this.qrCodeUrl = imageData;
        } else {
          throw new Error('无法创建离屏canvas');
        }
      } catch (e) {
        console.error('APP二维码生成失败:', e);
        this.qrCodeError = '二维码生成失败，请使用支付链接';
      }
    },

    async renderQrCode(content) {
      if (!content) {
        throw new Error('支付链接为空');
      }

      if (this.isImageSource(content)) {
        this.qrCodeUrl = content;
        return;
      }

      try {
        // #ifdef H5
        this.renderQrCodeH5(content);
        // #endif

        // #ifndef H5
        this.renderQrCodeApp(content);
        // #endif

      } catch (error) {
        console.error('生成二维码失败:', error);
        this.qrCodeError = '二维码生成失败，请使用支付链接付款';
      }
    },
    
    // 在 canvas 上绘制二维码（简化版）
    drawQRCode(ctx, text, size) {
      // 生成二维码矩阵
      const matrix = QRCode.generateMatrix(text);
      const matrixSize = matrix.length;
      const pixelSize = size / matrixSize;
      
      // 绘制白色背景
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(0, 0, size, size);
      
      // 绘制黑色方块
      ctx.fillStyle = '#111111';
      for (let row = 0; row < matrixSize; row++) {
        for (let col = 0; col < matrixSize; col++) {
          if (matrix[row][col] === 1) {
            ctx.fillRect(col * pixelSize, row * pixelSize, pixelSize, pixelSize);
          }
        }
      }
    },
    startCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer);
      }

      this.countdownTimer = setInterval(() => {
        if (this.remainingSeconds > 0) {
          this.remainingSeconds -= 1;
        }
      }, 1000);
    },
    stopCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer);
        this.countdownTimer = null;
      }
    },
    expirePayment() {
      this.paymentStatus = 2;
      this.paymentQuery = {
        ...this.paymentQuery,
        finished: true,
        paid: false,
        paymentStatus: 2,
        tradeStatus: this.paymentQuery.tradeStatus === 'WAIT_BUYER_PAY' ? 'TRADE_CLOSED' : this.paymentQuery.tradeStatus,
        message: '支付超时，请返回订单重新发起支付'
      };
      this.clearPolling();
      uni.showToast({ title: '支付已超时', icon: 'none' });
    },
    async initPayment() {
      if (this.loading) return;
      if (!this.orderNo) {
        uni.showToast({ title: '订单号不存在', icon: 'none' });
        setTimeout(() => uni.navigateBack(), 1500);
        return;
      }

      this.loading = true;
      this.finalStateHandled = false;
      this.remainingSeconds = DEFAULT_COUNTDOWN_SECONDS;
      this.qrCodeUrl = '';
      this.paymentLink = '';
      this.qrCodeError = '';
      this.paymentStatus = 0;
      this.paymentQuery = {
        ...createDefaultPaymentQuery(),
        orderNo: this.orderNo,
        message: '请使用支付宝扫码完成付款'
      };

      try {
        console.log('准备创建支付宝订单，订单号:', this.orderNo);
        const res = await createAlipayOrder({ orderNo: this.orderNo });
        console.log('创建支付宝订单响应:', res);

        if (this.isRequestSuccess(res) && res.data) {
          this.paymentLink = res.data;
          await this.renderQrCode(res.data);
          this.startPolling();
        } else {
          const message = res?.message || '创建支付订单失败';
          this.qrCodeError = message;
          this.paymentQuery.message = message;
          uni.showToast({ title: message, icon: 'none', duration: 3000 });
        }
      } catch (error) {
        console.error('创建支付订单失败:', error);
        this.qrCodeError = error?.message || '创建支付订单失败';
        this.paymentQuery.message = this.qrCodeError;
        uni.showToast({ title: this.qrCodeError, icon: 'none' });
      } finally {
        this.loading = false;
      }
    },
    async checkPaymentStatus(showToast = false) {
      if (!this.orderNo || this.statusLoading) {
        return;
      }

      this.statusLoading = true;
      try {
        const res = await queryAlipayStatus(this.orderNo);
        console.log('查询支付状态响应:', res);

        if (this.isRequestSuccess(res) && typeof res.data !== 'undefined') {
          const queryData = this.normalizeQueryData(res.data);
          this.paymentQuery = queryData;
          this.lastQueryAt = new Date().toISOString();
          this.handlePaymentResult(queryData, showToast);
        } else if (showToast) {
          uni.showToast({ title: res?.message || '查询支付状态失败', icon: 'none' });
        }
      } catch (error) {
        console.error('查询支付状态失败:', error);
        if (showToast) {
          uni.showToast({ title: error?.message || '查询支付状态失败', icon: 'none' });
        }
      } finally {
        this.statusLoading = false;
      }
    },
    handlePaymentResult(result, showToast = false) {
      if (result.paid) {
        this.paymentStatus = 1;
        this.stopCountdown();
        this.remainingSeconds = 0;
        if (!this.finalStateHandled) {
          this.finalStateHandled = true;
          this.clearPolling();
          uni.showToast({ title: result.message || '支付成功', icon: 'success' });
          this.redirectTimer = setTimeout(() => {
            uni.redirectTo({ url: '/pages/mall/order-list?status=1' });
          }, 1800);
        }
        return;
      }

      if (result.finished) {
        this.paymentStatus = 2;
        this.stopCountdown();
        if (!this.finalStateHandled) {
          this.finalStateHandled = true;
          this.clearPolling();
          uni.showToast({ title: result.message || '支付已关闭', icon: 'none' });
        } else if (showToast) {
          uni.showToast({ title: result.message || '支付已结束', icon: 'none' });
        }
        return;
      }

      this.paymentStatus = 0;
      if (showToast) {
        uni.showToast({ title: result.message || '尚未完成付款', icon: 'none' });
      }
    },
    startPolling() {
      this.clearPolling();
      this.startCountdown();
      console.log('开始轮询支付状态，订单号:', this.orderNo);

      this.checkPaymentStatus(false);
      this.pollingTimer = setInterval(() => {
        this.checkPaymentStatus(false);
      }, 3000);

      this.timeoutTimer = setTimeout(() => {
        if (this.paymentStatus === 0) {
          this.expirePayment();
        }
      }, DEFAULT_COUNTDOWN_SECONDS * 1000);
    },
    clearPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
        console.log('已清除轮询定时器');
      }

      if (this.timeoutTimer) {
        clearTimeout(this.timeoutTimer);
        this.timeoutTimer = null;
      }

      if (this.redirectTimer) {
        clearTimeout(this.redirectTimer);
        this.redirectTimer = null;
      }

      this.stopCountdown();
    },
    manualCheckStatus() {
      if (this.statusLoading) {
        return;
      }
      this.checkPaymentStatus(true);
    },
    copyPaymentLink() {
      if (!this.paymentLink) {
        uni.showToast({ title: '暂无付款链接', icon: 'none' });
        return;
      }

      uni.setClipboardData({
        data: this.paymentLink,
        success: () => {
          uni.showToast({ title: '付款链接已复制', icon: 'success' });
        }
      });
    },
    cancelPayment() {
      if (this.paymentStatus !== 0) {
        this.clearPolling();
        uni.navigateBack();
        return;
      }

      uni.showModal({
        title: '提示',
        content: '确定要取消本次支付吗？',
        success: (res) => {
          if (res.confirm) {
            this.clearPolling();
            uni.navigateBack();
          }
        }
      });
    },
    goBack() {
      if (this.paymentStatus !== 0) {
        this.clearPolling();
        uni.navigateBack();
        return;
      }

      uni.showModal({
        title: '提示',
        content: '离开支付页后订单仍会保留，确定现在返回吗？',
        success: (res) => {
          if (res.confirm) {
            this.clearPolling();
            uni.navigateBack();
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.payment-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4f7fb 0%, #eef2f6 100%);
  padding-bottom: calc(112px + env(safe-area-inset-bottom));
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeff5;
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
  color: #1f2a37;
}

.nav-right {
  width: 40px;
}

.payment-content {
  padding: 16px 15px 24px;
}

.hero-card,
.qr-card,
.placeholder-card,
.result-card,
.detail-card,
.tips-card {
  background-color: #fff;
  border-radius: 18px;
  box-shadow: 0 12px 28px rgba(22, 32, 56, 0.06);
  margin-bottom: 16px;
}

.hero-card {
  padding: 18px;
  background: linear-gradient(135deg, #18263d 0%, #243a5f 100%);
  color: #fff;
}

.hero-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.hero-caption {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 6px;
}

.hero-order-no {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}

.status-pill {
  min-width: 66px;
  padding: 6px 12px;
  text-align: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.theme-waiting {
  background-color: rgba(255, 255, 255, 0.14);
  color: #fdfefe;
}

.status-pill.theme-success {
  background-color: rgba(65, 196, 120, 0.16);
  color: #94f0b0;
}

.status-pill.theme-closed {
  background-color: rgba(255, 187, 80, 0.14);
  color: #ffd699;
}

.hero-amount-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
  margin-bottom: 8px;
}

.hero-amount {
  display: block;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 18px;
}

.hero-footer {
  display: flex;
  justify-content: space-between;
}

.hero-info-box {
  width: 48%;
  background-color: rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px;
  box-sizing: border-box;
}

.hero-info-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}

.hero-info-value {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.countdown-value {
  letter-spacing: 1px;
}

.hero-channel-box {
  text-align: right;
}

.channel-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background-color: #1677ff;
  color: #fff;
  font-size: 12px;
  margin-bottom: 8px;
}

.qr-card,
.placeholder-card,
.result-card,
.detail-card,
.tips-card {
  padding: 18px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1f2a37;
  margin-bottom: 6px;
}

.section-subtitle {
  display: block;
  font-size: 13px;
  color: #768396;
  line-height: 1.5;
}

.section-mark {
  padding: 5px 10px;
  border-radius: 999px;
  background-color: #f1f6ff;
  color: #1677ff;
  font-size: 12px;
}

.qr-frame {
  width: 270px;
  height: 270px;
  margin: 0 auto 14px;
  border-radius: 20px;
  padding: 10px;
  background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%);
  border: 1px solid #dbe7ff;
  box-sizing: border-box;
}

.qr-code {
  width: 250px;
  height: 250px;
  border-radius: 16px;
  background-color: #fff;
}

.qr-code-tip {
  display: block;
  text-align: center;
  font-size: 13px;
  line-height: 1.6;
  color: #6b7788;
  margin-bottom: 16px;
}

.action-row {
  display: flex;
  justify-content: space-between;
}

.ghost-btn,
.primary-btn,
.bottom-btn {
  border-radius: 999px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
}

.ghost-btn,
.primary-btn {
  width: 48%;
  padding: 12px 0;
}

.ghost-btn {
  border: 1px solid #d6e3f7;
  color: #27538b;
  background-color: #f8fbff;
}

.primary-btn {
  background: linear-gradient(135deg, #1677ff 0%, #3c99ff 100%);
  color: #fff;
  box-shadow: 0 10px 22px rgba(22, 119, 255, 0.22);
}

.is-disabled {
  opacity: 0.65;
}

.placeholder-card {
  text-align: center;
}

.placeholder-title {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #1f2a37;
  margin-bottom: 8px;
}

.placeholder-desc {
  display: block;
  font-size: 14px;
  line-height: 1.6;
  color: #7b8796;
}

.error-state .placeholder-title {
  color: #d94841;
}

.result-card.theme-waiting {
  border: 1px solid #dce7f7;
}

.result-card.theme-success {
  border: 1px solid #d8efd9;
  background: linear-gradient(180deg, #ffffff 0%, #f7fcf7 100%);
}

.result-card.theme-closed {
  border: 1px solid #ffe5d1;
  background: linear-gradient(180deg, #ffffff 0%, #fff9f3 100%);
}

.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 10px;
}

.result-title {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1f2a37;
  margin-bottom: 6px;
}

.result-subtitle {
  display: block;
  font-size: 13px;
  line-height: 1.5;
  color: #7a8797;
}

.result-trade-status {
  max-width: 110px;
  text-align: right;
  font-size: 12px;
  color: #506072;
  line-height: 1.5;
}

.result-message {
  display: block;
  font-size: 15px;
  line-height: 1.7;
  color: #2a3545;
}

.detail-title,
.tips-title {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #1f2a37;
  margin-bottom: 14px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #edf1f6;
}

.detail-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.align-top {
  align-items: flex-start;
}

.detail-label {
  font-size: 14px;
  color: #7a8797;
}

.detail-value {
  max-width: 62%;
  text-align: right;
  font-size: 14px;
  color: #1f2a37;
}

.multiline-value {
  word-break: break-all;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
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
  color: #5f6c7c;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px 15px calc(18px + env(safe-area-inset-bottom));
  background-color: rgba(255, 255, 255, 0.96);
  border-top: 1px solid #ebeff5;
}

.bottom-btn {
  width: 100%;
  padding: 13px 0;
  background-color: #fff;
  color: #445366;
  border: 1px solid #d9e2ee;
}
</style>
