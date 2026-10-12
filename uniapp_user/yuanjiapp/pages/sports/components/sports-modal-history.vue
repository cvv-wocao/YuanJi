<template>
  <view v-if="showHistory" class="history-overlay" @click="$emit('close')">
    <view class="history-content" @click.stop>
      <view class="history-header">
        <view class="history-title-wrapper">
          <text class="history-icon">📊</text>
          <text class="history-title">运动历史</text>
        </view>
        <view class="header-actions">
          <view class="habit-btn" @click="$emit('openHabitAnalysis')">
            <text class="habit-btn-text">📈 习惯分析</text>
          </view>
          <view class="close-btn-wrapper" @click="$emit('close')">
            <text class="close-btn">✕</text>
          </view>
        </view>
      </view>
      <scroll-view class="history-list" scroll-y>
        <view v-if="historyRecords.length === 0" class="empty-history">
          <text class="empty-icon">📝</text>
          <text class="empty-text">暂无运动记录</text>
          <text class="empty-hint">开始运动并分析，记录将显示在这里</text>
        </view>
        <view 
          v-for="(record, index) in historyRecords" 
          :key="record.id"
          class="history-item"
          :class="{'history-item-top3': index < 3}"
          @click="$emit('showReport', record)"
        >
          <view class="history-rank" v-if="index < 3">
            <text class="rank-badge" :class="`rank-${index + 1}`">{{ index + 1 }}</text>
          </view>
          <view class="history-item-main">
            <view class="history-item-header">
              <text class="exercise-name">{{ getExerciseName(record.exercise_type) }}</text>
              <view class="score-badge" :class="getScoreClass(record.score)">
                <text class="score-text">{{ record.score }}分</text>
              </view>
            </view>
            <view class="history-item-info">
              <text class="record-time">{{ formatTime(record.created_at) }}</text>
              <text class="record-count">{{ record.count }}次动作</text>
            </view>
          </view>
          <view class="history-item-arrow">
            <text class="arrow-icon">›</text>
          </view>
        </view>
      </scroll-view>
      <view class="history-footer">
        <view class="history-stats">
          <text class="stats-text">共 {{ historyRecords.length }} 条记录</text>
        </view>
        <button class="close-history-btn" @click="$emit('close')">
          <text class="btn-text">关闭</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'SportsModalHistory',
  props: {
    showHistory: {
      type: Boolean,
      default: false
    },
    historyRecords: {
      type: Array,
      default: () => []
    }
  },
  emits: ['close', 'showReport', 'openHabitAnalysis'],
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
    getScoreClass(score) {
      if (score >= 80) return 'score-excellent';
      if (score >= 60) return 'score-good';
      if (score >= 40) return 'score-average';
      return 'score-poor';
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
.history-overlay {
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

.history-content {
  background: #ffffff;
  border-radius: 32rpx;
  width: 85%;
  max-width: 600rpx;
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

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 24rpx;
  border-bottom: 2rpx solid var(--border-light);
}

.history-title-wrapper {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.history-icon {
  font-size: 40rpx;
}

.history-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: 20rpx;
  align-items: center;
}

.habit-btn {
  background: linear-gradient(135deg, #d8f3ef 0%, #b2dfdb 100%);
  padding: 12rpx 24rpx;
  border-radius: 8rpx;
  box-shadow: 0 4rpx 12rpx rgba(15,140,134,0.15);
  transition: all 0.3s ease;
}

.habit-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(15,140,134,0.1);
}

.habit-btn-text {
  color: #0f8c86;
  font-size: 26rpx;
  font-weight: 600;
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

.history-list {
  flex: 1;
  max-height: 500rpx;
  padding: 16rpx 24rpx;
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

.history-item {
  padding: 24rpx;
  margin-bottom: 16rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #d8f3ef 0%, #e8f5e9 100%);
  display: flex;
  align-items: center;
  gap: 20rpx;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1rpx solid #d8f3ef;
  box-shadow: 0 4rpx 12rpx rgba(15,140,134,0.08);
}

.history-item:active {
  transform: scale(0.98);
  box-shadow: 0 2rpx 8rpx rgba(15,140,134,0.06);
}

.history-item-top3 {
  background: linear-gradient(135deg, #b2dfdb 0%, #c8e6c9 100%);
  box-shadow: 0 4rpx 12rpx rgba(15,140,134,0.12);
  border: 1rpx solid #b2dfdb;
}

.history-rank {
  flex-shrink: 0;
}

.rank-badge {
  display: inline-block;
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 24rpx;
  font-weight: 700;
}

.rank-1 {
  background: linear-gradient(135deg, #FFD700 0%, #FFB347 100%);
  color: #fff;
  box-shadow: 0 2rpx 8rpx rgba(255,215,0,0.3);
}

.rank-2 {
  background: linear-gradient(135deg, #C0C0C0 0%, #A0A0A0 100%);
  color: #fff;
  box-shadow: 0 2rpx 8rpx rgba(192,192,192,0.3);
}

.rank-3 {
  background: linear-gradient(135deg, #CD7F32 0%, #B87333 100%);
  color: #fff;
  box-shadow: 0 2rpx 8rpx rgba(205,127,50,0.3);
}

.history-item-main {
  flex: 1;
}

.history-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.exercise-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #388E3C;
}

.score-badge {
  padding: 6rpx 16rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.score-excellent {
  background: rgba(56, 142, 60, 0.1);
  color: #388E3C;
}

.score-good {
  background: rgba(78, 205, 196, 0.1);
  color: #4ecdc4;
}

.score-average {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.score-poor {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.history-item-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.record-time {
  font-size: 22rpx;
  color: var(--text-secondary);
}

.record-count {
  font-size: 22rpx;
  color: var(--text-secondary);
}

.history-item-arrow {
  flex-shrink: 0;
}

.arrow-icon {
  font-size: 36rpx;
  color: #388E3C;
}

.history-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  border-top: 2rpx solid var(--border-light);
}

.history-stats {
  flex: 1;
}

.stats-text {
  font-size: 26rpx;
  color: var(--text-secondary);
}

.close-history-btn {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #d8f3ef 0%, #b2dfdb 100%);
  border: none;
  border-radius: 8rpx;
  box-shadow: 0 4rpx 12rpx rgba(15,140,134,0.15);
  transition: all 0.3s ease;
}

.close-history-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(15,140,134,0.1);
}

.btn-text {
  color: #0f8c86;
  font-size: 26rpx;
  font-weight: 600;
}
</style>
