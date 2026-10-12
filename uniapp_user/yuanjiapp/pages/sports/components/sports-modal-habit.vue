<template>
  <view v-if="showHabitModal" class="report-overlay" @click="$emit('close')">
    <view class="report-content" style="width: 90%; max-height: 80vh;" @click.stop>
      <view class="report-header">
        <text class="report-title">📈 运动习惯分析</text>
        <view class="close-btn-wrapper" @click="$emit('close')">
          <text class="close-btn">✕</text>
        </view>
      </view>
      <scroll-view class="report-body" scroll-y>
        <view v-if="loadingHabit" class="loading-report">
          <text class="loading-text">正在分析您的运动习惯...</text>
        </view>
        <view v-else-if="habitAnalysisResult" class="habit-analysis">
          <view class="habit-stats">
            <text class="stat-title">📊 统计摘要</text>
            <view class="stat-row">总运动次数：<text class="stat-value">{{ habitAnalysisResult.habit_data.total_records }}</text> 次</view>
            <view class="stat-row">最常锻炼：<text class="stat-value">{{ getExerciseName(habitAnalysisResult.habit_data.most_common_exercise) }}</text></view>
            <view class="stat-row">平均得分：<text class="stat-value">{{ habitAnalysisResult.habit_data.overall_avg_score }}</text> / 100</view>
            <view class="stat-row">各类型得分：</view>
            <view class="score-list">
              <view v-for="(score, type) in habitAnalysisResult.habit_data.avg_scores_by_type" :key="type" class="score-item">
                <text>{{ getExerciseName(type) }}：{{ score }}分</text>
              </view>
            </view>
          </view>
          <view class="habit-feedback" v-if="habitAnalysisResult.habit_data.top_feedback.length">
            <text class="stat-title">⚠️ 常见问题</text>
            <view v-for="fb in habitAnalysisResult.habit_data.top_feedback" :key="fb.text" class="feedback-item">
              • {{ fb.text }} （出现 {{ fb.count }} 次）
            </view>
          </view>
          <view class="habit-suggestion">
            <text class="stat-title">💡 AI 建议</text>
            <view class="suggestion-content">
              <text class="ai-text">{{ habitAnalysisResult.ai_suggestion }}</text>
            </view>
          </view>
        </view>
        <view v-else-if="habitError" class="error-msg">
          <text>{{ habitError }}</text>
        </view>
      </scroll-view>
      <view class="report-footer">
        <button class="close-report-btn" @click="$emit('close')">关闭</button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'SportsModalHabit',
  props: {
    showHabitModal: {
      type: Boolean,
      default: false
    },
    loadingHabit: {
      type: Boolean,
      default: false
    },
    habitAnalysisResult: {
      type: Object,
      default: null
    },
    habitError: {
      type: String,
      default: ''
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

.habit-analysis {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.habit-stats {
  background: linear-gradient(135deg, #E1F5FE 0%, #E8F5E9 100%);
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(225,245,254,0.15);
  border: 1rpx solid #E1F5FE;
}

.stat-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #388E3C;
  margin-bottom: 20rpx;
  display: block;
}

.stat-row {
  font-size: 26rpx;
  color: var(--text-secondary);
  margin-bottom: 16rpx;
  line-height: 1.6;
}

.stat-value {
  color: #388E3C;
  font-weight: 600;
}

.score-list {
  margin-top: 12rpx;
  padding-left: 20rpx;
}

.score-item {
  font-size: 24rpx;
  color: var(--text-secondary);
  margin-bottom: 8rpx;
}

.habit-feedback {
  background: linear-gradient(135deg, #FFF9E6 0%, #FFF3CD 100%);
  border-radius: 20rpx;
  padding: 24rpx;
  border-left: 6rpx solid #f59e0b;
  box-shadow: 0 4rpx 12rpx rgba(245,158,11,0.15);
  border: 1rpx solid #FFE082;
}

.feedback-item {
  font-size: 24rpx;
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 8rpx;
}

.habit-suggestion {
  background: linear-gradient(135deg, #B3E5FC 0%, #C8E6C9 100%);
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(179,229,252,0.3);
  border: 1rpx solid #B3E5FC;
}

.suggestion-content {
  margin-top: 16rpx;
}

.ai-text {
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.error-msg {
  padding: 40rpx;
  text-align: center;
  font-size: 28rpx;
  color: #ef4444;
}

.report-footer {
  padding: 24rpx 32rpx;
  border-top: 2rpx solid var(--border-light);
  display: flex;
  justify-content: center;
}

.close-report-btn {
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

.close-report-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(179,229,252,0.2);
}
</style>
