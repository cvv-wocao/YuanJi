<template>
  <view v-if="showReport" class="report-overlay" @click="$emit('close')">
    <view class="report-content" @click.stop>
      <view class="report-header">
        <text class="report-title">分析报告</text>
        <view class="close-btn-wrapper" @click="$emit('close')">
          <text class="close-btn">×</text>
        </view>
      </view>
      <scroll-view class="report-body" scroll-y>
        <view v-if="loadingReport" class="loading-report">
          <text class="loading-text">加载中...</text>
        </view>
        <view v-else-if="currentReport" class="report-details">
          <view class="report-meta">
            <text class="report-exercise">{{ getExerciseName(currentReport.exercise_type) }}</text>
            <text class="report-date">{{ formatTime(currentReport.record_created_at) }}</text>
          </view>
          
          <view class="report-score">
            <text class="score-number">{{ currentReport.score }}</text>
            <text class="score-label">动作评分</text>
          </view>
          
          <view class="report-stats">
            <view class="stat-item">
              <text class="stat-value">{{ currentReport.count }}</text>
              <text class="stat-label">完成次数</text>
            </view>
            <view class="stat-item" v-if="currentReport.knee_angle">
              <text class="stat-value">{{ currentReport.knee_angle }}°</text>
              <text class="stat-label">膝关节角度</text>
            </view>
            <view class="stat-item" v-if="currentReport.hip_angle">
              <text class="stat-value">{{ currentReport.hip_angle }}°</text>
              <text class="stat-label">髋关节角度</text>
            </view>
            <view class="stat-item" v-if="currentReport.trunk_angle">
              <text class="stat-value">{{ currentReport.trunk_angle }}°</text>
              <text class="stat-label">躯干角度</text>
            </view>
          </view>
          
          <view class="report-feedback" v-if="currentReport.feedback && currentReport.feedback.length">
            <text class="section-title">💡 分析反馈</text>
            <view class="feedback-list">
              <text v-for="(fb, index) in currentReport.feedback" :key="index" class="feedback-item">• {{ fb }}</text>
            </view>
          </view>
          
          <view class="report-suggestions" v-if="currentReport.suggestions && currentReport.suggestions.length">
            <text class="section-title">📋 运动建议</text>
            <view class="suggestion-list">
              <view v-for="(suggestion, index) in currentReport.suggestions" :key="index" class="suggestion-item">
                <text class="suggestion-type">{{ suggestion.type }}</text>
                <text class="suggestion-content">{{ suggestion.content }}</text>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="report-footer">
        <button class="close-report-btn" @click="$emit('close')">
          <text class="btn-text">关闭</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'SportsModalReport',
  props: {
    showReport: {
      type: Boolean,
      default: false
    },
    loadingReport: {
      type: Boolean,
      default: false
    },
    currentReport: {
      type: Object,
      default: null
    }
  },
  emits: ['close'],
  methods: {
    getExerciseName(type) {
      const exercises = [
        { value: 'running', name: '跑步' },
        { value: 'dumbbell_curl', name: '哑铃弯举' },
        { value: 'pushup', name: '俯卧撑' }
      ];
      const exercise = exercises.find(e => e.value === type);
      return exercise ? exercise.name : type;
    },
    formatTime(timeStr) {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
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
  width: 85%;
  max-height: 75vh;
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

.report-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.report-exercise {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
}

.report-date {
  font-size: 22rpx;
  color: var(--text-secondary);
}

.report-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx;
  background: var(--bg-metric);
  border-radius: 28rpx;
  margin-bottom: 32rpx;
}

.score-number {
  font-size: 80rpx;
  font-weight: 800;
  background: linear-gradient(135deg, #0f8c86 0%, #40b0a5 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 8rpx;
}

.score-label {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.report-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.stat-item {
  background: var(--bg-metric);
  padding: 20rpx;
  border-radius: 20rpx;
  text-align: center;
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4rpx;
}

.stat-label {
  font-size: 20rpx;
  color: var(--text-secondary);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16rpx;
}

.report-feedback {
  margin-bottom: 32rpx;
}

.feedback-list {
  background: #f8fafc;
  padding: 24rpx;
  border-radius: 20rpx;
}

.feedback-item {
  display: block;
  font-size: 24rpx;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 8rpx;
}

.report-suggestions {
  margin-bottom: 16rpx;
}

.suggestion-list {
  background: #f0f9f8;
  padding: 24rpx;
  border-radius: 20rpx;
}

.suggestion-item {
  margin-bottom: 16rpx;
  padding-bottom: 16rpx;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.suggestion-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.suggestion-type {
  display: block;
  font-size: 20rpx;
  font-weight: 600;
  color: #0f8c86;
  margin-bottom: 8rpx;
}

.suggestion-content {
  display: block;
  font-size: 24rpx;
  color: var(--text-secondary);
  line-height: 1.5;
}

.report-footer {
  padding: 24rpx 32rpx;
  border-top: 2rpx solid var(--border-light);
  display: flex;
  justify-content: center;
}

.close-report-btn {
  padding: 16rpx 48rpx;
  background: linear-gradient(135deg, #0f8c86 0%, #40b0a5 100%);
  border: none;
  border-radius: 48rpx;
  color: white;
  font-size: 28rpx;
  font-weight: 600;
}
</style>
