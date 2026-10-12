<template>
  <view v-if="showDeviceModal" class="report-overlay" @click="$emit('close')">
    <view class="report-content" style="width: 90%; max-height: 80vh;" @click.stop>
      <view class="report-header">
        <text class="report-title">📡 设备信息</text>
        <view class="close-btn-wrapper" @click="$emit('close')">
          <text class="close-btn">✕</text>
        </view>
      </view>
      <scroll-view class="report-body" scroll-y>
        <view v-if="loadingDevices" class="loading-report">
          <text class="loading-text">正在获取设备数据...</text>
        </view>
        <view v-else-if="devices.length === 0" class="empty-history">
          <text class="empty-icon">📝</text>
          <text class="empty-text">暂无设备</text>
          <text class="empty-hint">请确保树莓派设备已连接</text>
        </view>
        <view v-else class="device-list-container">
          <view 
            v-for="device in devices" 
            :key="device.deviceId" 
            class="device-item"
            :class="{ 'device-item-online': isDeviceOnline(device.lastSeen) }"
          >
            <view class="device-header">
              <text class="device-id">{{ device.deviceId }}</text>
              <view class="device-status" :class="isDeviceOnline(device.lastSeen) ? 'status-online' : 'status-offline'">
                <text>{{ isDeviceOnline(device.lastSeen) ? '在线' : '离线' }}</text>
              </view>
            </view>
            <view class="device-info">
              <view class="info-row">
                <text class="info-label">📍 位置</text>
                <text class="info-value">{{ device.location || '未设置' }}</text>
              </view>
              <view class="info-row">
                <text class="info-label">🌡️ 温度</text>
                <text class="info-value">{{ device.temperature ?? '--' }} °C</text>
              </view>
              <view class="info-row">
                <text class="info-label">💧 湿度</text>
                <text class="info-value">{{ device.humidity ?? '--' }} %</text>
              </view>
              <view class="info-row">
                <text class="info-label">🕐 最后在线</text>
                <text class="info-value">{{ formatDeviceTime(device.lastSeen) }}</text>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="report-footer">
        <button class="refresh-device-btn" @click="$emit('refresh')">
          <text class="btn-text">🔄 刷新</text>
        </button>
        <button class="close-report-btn" @click="$emit('close')">关闭</button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'SportsModalDevice',
  props: {
    showDeviceModal: {
      type: Boolean,
      default: false
    },
    loadingDevices: {
      type: Boolean,
      default: false
    },
    devices: {
      type: Array,
      default: () => []
    }
  },
  emits: ['close', 'refresh'],
  methods: {
    isDeviceOnline(lastSeen) {
      if (!lastSeen) return false;
      const now = Date.now();
      const diff = now - lastSeen;
      return diff < 60000;
    },
    formatDeviceTime(timestamp) {
      if (!timestamp) return '--';
      const date = new Date(timestamp);
      return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.report-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.report-content {
  background: #ffffff;
  border-radius: 32rpx;
  width: 90%;
  max-height: 80vh;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(50rpx) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 2rpx solid var(--border-light);
}

.report-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.close-btn-wrapper {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

.close-btn {
  font-size: 32rpx;
  color: var(--text-secondary);
  font-weight: 500;
}

.report-body {
  flex: 1;
  padding: 24rpx;
  max-height: 55vh;
}

.loading-report {
  padding: 60rpx 40rpx;
  text-align: center;
}

.loading-text {
  font-size: 28rpx;
  color: var(--text-secondary);
}

.empty-history {
  padding: 80rpx 40rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
  opacity: 0.6;
}

.empty-text {
  font-size: 32rpx;
  color: var(--text-primary);
  font-weight: 600;
  margin-bottom: 16rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: var(--text-secondary);
  line-height: 1.5;
}

.device-list-container {
  padding: 10rpx 0;
}

.device-item {
  background: linear-gradient(135deg, #E1F5FE 0%, #E8F5E9 100%);
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(225,245,254,0.15);
  border: 1rpx solid #E1F5FE;
}

.device-item-online {
  background: linear-gradient(135deg, #B3E5FC 0%, #C8E6C9 100%);
  box-shadow: 0 4rpx 12rpx rgba(179,229,252,0.3);
  border: 1rpx solid #B3E5FC;
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.device-id {
  font-size: 28rpx;
  font-weight: 600;
  color: #388E3C;
}

.device-status {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.status-online {
  background: rgba(56, 142, 60, 0.1);
  color: #388E3C;
}

.status-offline {
  background: rgba(156, 163, 175, 0.1);
  color: #9ca3af;
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid rgba(46, 125, 50, 0.1);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 26rpx;
  color: var(--text-secondary);
}

.info-value {
  font-size: 26rpx;
  font-weight: 500;
  color: #388E3C;
}

.report-footer {
  padding: 24rpx 32rpx;
  border-top: 2rpx solid var(--border-light);
  display: flex;
  gap: 20rpx;
  justify-content: center;
}

.refresh-device-btn {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #B3E5FC 0%, #C8E6C9 100%);
  border: none;
  border-radius: 8rpx;
  color: #388E3C;
  font-size: 26rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(179,229,252,0.3);
  transition: all 0.3s ease;
}

.refresh-device-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(179,229,252,0.2);
}

.close-report-btn {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #E1F5FE 0%, #E8F5E9 100%);
  border: 1rpx solid #E1F5FE;
  border-radius: 8rpx;
  color: #388E3C;
  font-size: 26rpx;
  font-weight: 600;
  box-shadow: 0 2rpx 8rpx rgba(225,245,254,0.15);
  transition: all 0.3s ease;
}

.close-report-btn:active {
  background: linear-gradient(135deg, #E1F5FE 0%, #E8F5E9 100%);
  transform: translateY(1rpx);
  box-shadow: 0 1rpx 4rpx rgba(225,245,254,0.15);
}
</style>
