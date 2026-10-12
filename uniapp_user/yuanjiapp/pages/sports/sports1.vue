<template>
  <view class="mobile-container">
    <view class="bg-decoration"></view>

    <view class="app-header">
      <text class="header-title">运动</text>
    </view>

    <!-- 修改：给 scroll-view 设置固定高度 -->
    <scroll-view 
      class="app-content" 
      scroll-y 
      :style="{ height: scrollViewHeight + 'px' }"
      @scroll="onScroll"
    >
      <view class="content-wrapper">
        <view class="exercise-selector">
          <text class="selector-title">选择运动类型</text>
          <view class="exercise-options">
            <view 
              v-for="exercise in exercises" 
              :key="exercise.value"
              class="exercise-option"
              :class="{ active: selectedExercise === exercise.value }"
              @click="selectExercise(exercise.value)"
            >
              <text class="exercise-icon">{{ exercise.icon }}</text>
              <text class="exercise-name">{{ exercise.name }}</text>
            </view>
          </view>
        </view>

        <view class="feature-card" @click="startPoseAnalysis">
          <view class="feature-icon">
            <text class="icon">{{ getExerciseIcon(selectedExercise) }}</text>
          </view>
          <view class="feature-info">
            <text class="feature-title">{{ getExerciseTitle(selectedExercise) }}</text>
            <text class="feature-desc">{{ getExerciseDesc(selectedExercise) }}</text>
          </view>
          <view class="feature-arrow">
            <text class="arrow">→</text>
          </view>
        </view>

        <view v-if="uploadedVideoPath && !videoErrorMsg" class="uploaded-video-container">
          <text class="result-title">上传的视频</text>
          <view class="video-preview">
            <!-- #ifdef H5 -->
            <video 
              ref="h5VideoElement"
              :src="uploadedVideoPath" 
              controls 
              class="uploaded-video"
              style="width: 100%; height: 100%;"
              @error="videoError"
              @loadedmetadata="videoLoaded"
            ></video>
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <video 
              :src="uploadedVideoPath" 
              controls 
              class="uploaded-video"
              poster="" 
              autoplay="false"
              initial-time="0"
              :duration="videoDuration"
              muted="false"
              loop="false"
              show-fullscreen-btn="true"
              show-play-btn="true"
              show-center-play-btn="true"
              enable-progress-gesture="true"
              object-fit="contain"
              @error="videoError"
              @loadedmetadata="videoLoaded"
            ></video>
            <!-- #endif -->
          </view>
          <view class="video-controls">
            <button class="control-btn" @click="resetUpload">重新上传</button>
            <button class="control-btn" @click="clearVideo">清除视频</button>
          </view>
        </view>

        <view v-if="uploadedVideoPath && videoErrorMsg" class="uploaded-video-container">
          <text class="result-title">上传的视频</text>
          <view class="video-preview error">
            <text class="error-text">视频加载失败</text>
            <text class="error-detail" v-if="debug">{{ videoErrorMsg }}</text>
          </view>
          <view class="video-controls">
            <button class="control-btn" @click="resetUpload">重新上传</button>
            <button class="control-btn" @click="clearVideo">清除视频</button>
          </view>
        </view>

        <view v-if="analysisResult" class="result-card">
          <text class="result-title">分析结果</text>
          
          <view class="score-container">
            <text class="score">{{ analysisResult.score }}</text>
            <text class="score-label">动作评分</text>
          </view>

          <view v-if="analysisResult.count > 0" class="count-container">
            <text class="count-num">{{ analysisResult.count }}</text>
            <text class="count-label">完成次数</text>
          </view>

          <view class="metrics-container">
            <view v-if="analysisResult.kneeAngle !== undefined" class="metric-item">
              <text class="metric-label">膝关节角度</text>
              <text class="metric-value">{{ analysisResult.kneeAngle }}°</text>
            </view>
            <view v-if="analysisResult.hipAngle !== undefined" class="metric-item">
              <text class="metric-label">髋关节角度</text>
              <text class="metric-value">{{ analysisResult.hipAngle }}°</text>
            </view>
            <view v-if="analysisResult.trunkAngle !== undefined" class="metric-item">
              <text class="metric-label">躯干角度</text>
              <text class="metric-value">{{ analysisResult.trunkAngle }}°</text>
            </view>
            <view v-if="analysisResult.angle !== undefined" class="metric-item">
              <text class="metric-label">动作角度</text>
              <text class="metric-value">{{ analysisResult.angle }}°</text>
            </view>
          </view>

          <view class="suggestion-container" v-if="analysisResult.feedback && analysisResult.feedback.length">
            <text class="suggestion-title">💡 实时反馈</text>
            <text class="suggestion-content">{{ analysisResult.feedback.join('，') }}</text>
          </view>

          <!-- AI 智能分析区域 -->
          <view class="ai-analysis-container" v-if="analysisResult.ai_analysis">
            <view class="ai-header">
              <text class="ai-icon">🤖</text>
              <text class="ai-title">AI 智能分析</text>
              <text class="ai-badge">星火大模型</text>
            </view>
            <view class="ai-content">
              <text class="ai-text">{{ analysisResult.ai_analysis }}</text>
            </view>
            <view class="ai-footer" v-if="analysisResult.analysis_time">
              <text class="ai-time">分析时间：{{ analysisResult.analysis_time }}</text>
            </view>
          </view>

          <view class="result-controls">
            <button class="control-btn" @click="resetAnalysis">重新分析</button>
            <button class="control-btn" @click="clearAnalysis">清除结果</button>
          </view>
        </view>

        <view v-if="showCamera" class="camera-container">
          <view class="camera-preview">
            <!-- 视频元素将通过 JavaScript 动态创建 -->
            <canvas 
              ref="canvasElement"
              v-if="isCameraOn && showCanvas"
              class="canvas-overlay"
            ></canvas>
            <view v-else class="camera-placeholder">
              <text class="placeholder-text">摄像头预览</text>
            </view>
            
            <view v-if="isRecording" class="recording-indicator">
              <view class="recording-dot"></view>
              <text class="recording-text">录制中</text>
            </view>
            
            <view v-if="isAnalyzing && analysisResult" class="analysis-overlay">
              <view class="analysis-score">
                <text class="score-value">{{ analysisResult.score }}</text>
                <text class="score-label">分</text>
              </view>
              <view class="analysis-metrics">
                <view v-if="analysisResult.count !== undefined" class="metric-item">
                  <text class="metric-value">✅ {{ analysisResult.count }}</text>
                </view>
                <view v-if="analysisResult.angle !== undefined" class="metric-item">
                  <text class="metric-value">{{ analysisResult.angle }}°</text>
                </view>
                <view v-if="analysisResult.kneeAngle !== undefined" class="metric-item">
                  <text class="metric-value">膝 {{ analysisResult.kneeAngle }}°</text>
                </view>
              </view>
            </view>
            
            <view v-if="showCountdown" class="countdown-overlay">
              <text class="countdown-text">{{ countdown }}</text>
            </view>
          </view>
          <view class="camera-controls">
            <button class="control-btn" @click="toggleCamera">
              {{ isCameraOn ? '关闭摄像头' : '打开摄像头' }}
            </button>
            <button class="control-btn" @click="startRealtimeAnalysis">
              {{ isAnalyzing ? '停止分析' : '开始分析' }}
            </button>
            <button class="control-btn" @click="startRecording">开始录制</button>
            <button class="control-btn" @click="stopRecording">停止录制</button>
          </view>
        </view>

        <view class="action-buttons">
          <button class="action-btn primary" @click="showCamera = true">开始检测</button>
          <button class="action-btn secondary" @click="uploadVideo">上传视频分析</button>
          <button class="action-btn secondary" @click="handleShowHistory">历史记录</button>
        </view>
      </view>
    </scroll-view>

    <!-- 历史记录弹窗 -->
    <view v-if="showHistory" class="history-overlay" @click="showHistory = false">
      <view class="history-content" @click.stop>
        <view class="history-header">
          <view class="history-title-wrapper">
            <text class="history-icon">📊</text>
            <text class="history-title">运动历史</text>
          </view>
          <view class="close-btn-wrapper" @click="showHistory = false">
            <text class="close-btn">✕</text>
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
            @click="showReportDetail(record)"
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
          <button class="close-history-btn" @click="showHistory = false">
            <text class="btn-text">关闭</text>
          </button>
        </view>
      </view>
    </view>

    <!-- 报告详情弹窗 -->
    <view v-if="showReport" class="report-overlay" @click="showReport = false">
      <view class="report-content" @click.stop>
        <view class="report-header">
          <text class="report-title">分析报告</text>
          <view class="close-btn-wrapper" @click="showReport = false">
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
            
            <view class="report-feedback" v-if="currentReport.feedback.length">
              <text class="section-title">💡 分析反馈</text>
              <view class="feedback-list">
                <text v-for="(fb, index) in currentReport.feedback" :key="index" class="feedback-item">• {{ fb }}</text>
              </view>
            </view>
            
            <view class="report-suggestions" v-if="currentReport.suggestions.length">
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
          <button class="close-report-btn" @click="showReport = false">
            <text class="btn-text">关闭</text>
          </button>
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading-overlay">
      <view class="loading-content">
        <text class="loading-text">{{ loadingText }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { showToast } from '@/utils/index.js';

export default {
  setup() {
    const showCamera = ref(false);
    const isCameraOn = ref(false);
    const cameraVideoUrl = ref('');
    const analysisResult = ref(null);
    const loading = ref(false);
    const loadingText = ref('正在分析...');
    const videoElement = ref(null);
    const h5VideoElement = ref(null);
    const canvasElement = ref(null);
    const stream = ref(null);
    const recorder = ref(null);
    const recordedChunks = ref([]);
    const showCanvas = ref(false);
    const isAnalyzing = ref(false);
    const analysisInterval = ref(null);
    const uploadedVideoPath = ref('');
    const debug = ref(true);
    const videoDuration = ref(0);
    const videoErrorMsg = ref('');
    const isRecording = ref(false);
    const scrollViewHeight = ref(0);

    // 历史记录和报告相关状态
    const showHistory = ref(false);
    const showReport = ref(false);
    const historyRecords = ref([]);
    const currentReport = ref(null);
    const loadingReport = ref(false);

    const selectedExercise = ref('running');
    const exercises = [
      { value: 'running', name: '跑步', icon: '🏃' },
      { value: 'dumbbell_curl', name: '哑铃弯举', icon: '💪' },
      { value: 'pushup', name: '俯卧撑', icon: '👊' }
    ];

    const API_BASE_URL = 'http://localhost:5002/api';

    // 计算滚动视图高度
    const calculateScrollHeight = () => {
      // #ifdef H5
      const header = document.querySelector('.app-header');
      if (header) {
        const headerHeight = header.offsetHeight;
        const windowHeight = window.innerHeight;
        scrollViewHeight.value = windowHeight - headerHeight;
      } else {
        scrollViewHeight.value = window.innerHeight - 100;
      }
      // #endif
      
      // #ifndef H5
      // 在小程序中使用系统信息
      const systemInfo = uni.getSystemInfoSync();
      const windowHeight = systemInfo.windowHeight;
      // 假设 header 高度为 100px（可以根据实际情况调整）
      scrollViewHeight.value = windowHeight - 100;
      // #endif
    };

    // 监听窗口大小变化
    const handleResize = () => {
      calculateScrollHeight();
    };

    const onScroll = (e) => {
      // 滚动事件，可以在这里处理一些滚动相关的逻辑
      console.log('滚动位置:', e.detail.scrollTop);
    };

    const onVideoLoaded = () => {
      if (videoElement.value && canvasElement.value) {
        const canvas = canvasElement.value;
        const video = videoElement.value;
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        showCanvas.value = true;
      }
    };

    const handleVideoPlay = () => console.log('视频播放');
    const handleVideoError = (e) => { 
      console.error('视频错误:', e);
      console.error('错误详情:', {
        message: e.message,
        error: e.error,
        target: e.target ? '视频元素' : '未知',
        readyState: videoElement.value ? videoElement.value.readyState : 'N/A',
        isCameraOn: isCameraOn.value
      });
      if (isCameraOn.value) {
        const errorMessage = e.message || '摄像头异常';
        showToast(`摄像头错误: ${errorMessage}`);
      }
    };

    const selectExercise = (exercise) => {
      selectedExercise.value = exercise;
      analysisResult.value = null;
      showToast(`已切换：${exercises.find(e=>e.value===exercise)?.name}`);
    };

    const getExerciseIcon = (e) => exercises.find(x=>x.value===e)?.icon || '🏃';
    const getExerciseTitle = (e) => ({
      running:'跑步姿势分析',
      dumbbell_curl:'哑铃弯举分析',
      pushup:'俯卧撑分析'
    }[e] || '运动分析');

    const getExerciseDesc = (e) => ({
      running:'实时检测姿势与评分',
      dumbbell_curl:'精准计数 + 姿势标准评估',
      pushup:'标准动作检测 + 智能计数'
    }[e] || '专业运动分析');

    const countdown = ref(3);
    const showCountdown = ref(false);

    const startPoseAnalysis = async () => {
      loading.value = true;
      loadingText.value = '加载标准动作...';
      try {
        const res = await fetch(`${API_BASE_URL}/analyze/standard`);
        const data = await res.json();
        showToast(data.success ? '分析完成' : '分析失败');
      } catch (e) {
        showToast('后端未启动');
      } finally { loading.value = false; }
    };

    const toggleCamera = async () => {
      try {
        console.log('尝试打开摄像头...');
        if (!isCameraOn.value) {
          isCameraOn.value = true;
          console.log('设置 isCameraOn = true');
          
          await nextTick();
          console.log('DOM 已更新');
          
          await new Promise(resolve => setTimeout(resolve, 200));
          console.log('视频元素应该已渲染');
          
          if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            console.error('浏览器不支持摄像头访问');
            throw new Error('浏览器不支持摄像头访问');
          }
          
          console.log('navigator.mediaDevices 可用');
          
          console.log('请求摄像头权限...');
          stream.value = await navigator.mediaDevices.getUserMedia({ 
            video: true
          });
          console.log('获取摄像头流成功');
          console.log('流信息:', {
            active: stream.value.active,
            tracks: stream.value.getTracks().length
          });
          
          try {
            console.log('尝试动态创建视频元素');
            
            const cameraContainer = document.querySelector('.camera-preview');
            if (!cameraContainer) {
              console.error('摄像头容器未找到');
              throw new Error('摄像头容器未找到');
            }
            console.log('摄像头容器已找到:', cameraContainer);
            
            cameraContainer.innerHTML = '';
            
            const videoEl = document.createElement('video');
            videoEl.id = 'videoElement';
            videoEl.autoplay = true;
            videoEl.playsInline = true;
            videoEl.muted = true;
            videoEl.style.position = 'absolute';
            videoEl.style.width = '100%';
            videoEl.style.height = '100%';
            videoEl.style.objectFit = 'cover';
            
            console.log('使用标准 srcObject 方法设置流');
            videoEl.srcObject = stream.value;
            console.log('srcObject 设置成功');
            
            cameraContainer.appendChild(videoEl);
            console.log('视频元素已添加到容器');
            
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            showToast('摄像头已打开');
            console.log('摄像头已成功打开');
          } catch (error) {
            console.error('动态创建视频元素失败:', error);
            throw error;
          }
        } else {
          if (stream.value) {
            stream.value.getTracks().forEach(t => t.stop());
            stream.value = null;
          }
          
          try {
            const cameraContainer = document.querySelector('.camera-preview');
            if (cameraContainer) {
              const videoEl = cameraContainer.querySelector('video');
              if (videoEl) {
                cameraContainer.removeChild(videoEl);
                console.log('视频元素已移除');
              }
            }
          } catch (e) {
            console.error('清理视频元素失败:', e);
          }
          
          isCameraOn.value = false;
          showToast('摄像头已关闭');
          console.log('摄像头已关闭');
        }
      } catch (e) {
        console.error('摄像头访问错误:', e);
        showToast(`权限或设备异常: ${e.message}`);
        isCameraOn.value = false;
        
        try {
          const cameraContainer = document.querySelector('.camera-preview');
          if (cameraContainer) {
            const videoEl = cameraContainer.querySelector('video');
            if (videoEl) {
              cameraContainer.removeChild(videoEl);
              console.log('视频元素已移除');
            }
          }
        } catch (cleanupError) {
          console.error('清理视频元素失败:', cleanupError);
        }
      }
    };

    const startRecording = async () => {
      if (!stream.value) return showToast('请先开摄像头');
      
      showCountdown.value = true;
      countdown.value = 3;
      
      const countdownInterval = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
          clearInterval(countdownInterval);
          showCountdown.value = false;
          
          uploadedVideoPath.value = '';
          recorder.value = new MediaRecorder(stream.value);
          recordedChunks.value = [];
          recorder.value.ondataavailable = e => e.data.size && recordedChunks.value.push(e.data);
          recorder.value.onstop = async () => {
            const blob = new Blob(recordedChunks.value, { type: 'video/webm' });
            const url = URL.createObjectURL(blob);
            uploadedVideoPath.value = url;
            await uploadVideoForAnalysis(blob);
          };
          recorder.value.start();
          isRecording.value = true;
          showToast('开始录制');
        }
      }, 1000);
    };

    const stopRecording = () => {
      if (recorder.value?.state === 'recording') {
        recorder.value.stop();
        isRecording.value = false;
        showToast('已停止，正在分析…');
      }
    };

    const uploadVideo = () => {
      uni.chooseVideo({
        sourceType: ['album'],
        maxDuration: 60,
        success: async (res) => {
          loading.value = true;
          try {
            await uploadVideoFile(res.tempFilePath);
          } catch (err) {
            showToast("分析失败");
          } finally {
            loading.value = false;
          }
        },
        fail: () => showToast('选择失败')
      });
    };

    const uploadVideoFile = async (path) => {
      uploadedVideoPath.value = path;
      videoErrorMsg.value = '';
      
      // #ifdef H5
      try {
        const response = await fetch(path);
        const blob = await response.blob();
        const form = new FormData();
        form.append('video', blob, 'video.mp4');
        form.append('exercise_type', selectedExercise.value);
        
        const r = await fetch(`${API_BASE_URL}/analyze/video`, { 
          method: 'POST', 
          body: form,
          credentials: 'include'
        });
        const data = await r.json();
        console.log('后端返回数据：', data);
        
        if (data.success) {
          analysisResult.value = {
            score: data.score || 0,
            count: data.count || 0,
            feedback: data.feedback || [],
            kneeAngle: data.kneeAngle ?? 0,
            hipAngle: data.hipAngle ?? 0,
            trunkAngle: data.trunkAngle ?? 0,
            angle: data.angle ?? 0,
            ai_analysis: data.ai_analysis || '',
            analysis_time: data.analysis_time || ''
          };
          showToast('分析完成 ✅');
          return analysisResult.value;
        } else {
          console.log('后端返回失败：', data.feedback);
          showToast(data.feedback?.[0] || '分析失败');
          throw new Error(data.feedback?.[0] || '分析失败');
        }
      } catch (e) {
        console.error('上传失败：', e);
        showToast(`上传失败：${e.message}`);
        throw e;
      }
      // #endif
      
      // #ifndef H5
      return new Promise((resolve, reject) => {
        uni.uploadFile({
          url: `${API_BASE_URL}/analyze/video`,
          filePath: path,
          name: 'video',
          formData: { exercise_type: selectedExercise.value },
          success: (res) => {
            try {
              const data = JSON.parse(res.data);
              console.log('后端返回数据：', data);
              if (data.success) {
                analysisResult.value = {
                  score: data.score || 0,
                  count: data.count || 0,
                  feedback: data.feedback || [],
                  kneeAngle: data.kneeAngle ?? 0,
                  hipAngle: data.hipAngle ?? 0,
                  trunkAngle: data.trunkAngle ?? 0,
                  angle: data.angle ?? 0,
                  ai_analysis: data.ai_analysis || '',
                  analysis_time: data.analysis_time || ''
                };
                showToast('分析完成 ✅');
                resolve(analysisResult.value);
              } else {
                console.log('后端返回失败：', data.feedback);
                showToast(data.feedback?.[0] || '分析失败');
                reject();
              }
            } catch (e) {
              console.error('解析失败：', e);
              reject();
            }
          },
          fail: (err) => {
            console.error('上传失败：', err);
            showToast(`上传失败：${JSON.stringify(err)}`);
            reject(err);
          }
        });
      });
      // #endif
    };

    const uploadVideoForAnalysis = async (blob) => {
      try {
        const form = new FormData();
        form.append('video', blob, 'video.webm');
        form.append('exercise_type', selectedExercise.value);
        const r = await fetch(`${API_BASE_URL}/analyze/video`, { method: 'POST', body: form });
        const data = await r.json();
        if (data.success) {
          analysisResult.value = {
            score: data.score || 0,
            count: data.count || 0,
            feedback: data.feedback || [],
            kneeAngle: data.kneeAngle ?? 0,
            hipAngle: data.hipAngle ?? 0,
            trunkAngle: data.trunkAngle ?? 0,
            angle: data.angle ?? 0,
            ai_analysis: data.ai_analysis || '',
            analysis_time: data.analysis_time || ''
          };
        }
        showToast('分析完成');
      } catch (e) {
        showToast('服务未启动');
      }
    };

    const startRealtimeAnalysis = () => {
      if (!isCameraOn.value) return showToast('请打开摄像头');
      if (isAnalyzing.value) {
        isAnalyzing.value = false;
        clearInterval(analysisInterval.value);
        return showToast('已停止分析');
      }
      isAnalyzing.value = true;
      analyzeFrame();
      analysisInterval.value = setInterval(analyzeFrame, 800);
      showToast('开始实时分析');
    };

    const analyzeFrame = async () => {
      if (!videoElement.value || !canvasElement.value) return;
      const c = canvasElement.value;
      const ctx = c.getContext('2d');
      ctx.clearRect(0,0,c.width,c.height);
      ctx.drawImage(videoElement.value,0,0,c.width,c.height);
      
      try {
        const temp = document.createElement('canvas');
        temp.width = videoElement.value.videoWidth || 640;
        temp.height = videoElement.value.videoHeight || 480;
        temp.getContext('2d').drawImage(videoElement.value,0,0,temp.width,temp.height);
        
        temp.toBlob(async (b) => {
          try {
            const f = new FormData();
            f.append('image', b, 'frame.jpg');
            f.append('exercise_type', selectedExercise.value);
            const r = await fetch(`${API_BASE_URL}/analyze/frame`, { method: 'POST', body: f });
            const j = await r.json();
            if (j.success) {
              analysisResult.value = {
                score: j.score || 0,
                count: j.count || 0,
                feedback: j.feedback || [],
                angle: j.angle || 0,
                kneeAngle: j.kneeAngle || 0,
                hipAngle: j.hipAngle || 0,
                trunkAngle: j.trunkAngle || 0,
                ai_analysis: analysisResult.value?.ai_analysis || ''
              };
              drawDebug(ctx, analysisResult.value);
            }
          } catch(e) {
            console.error('帧分析失败:', e);
          }
        });
      } catch(e) {
        console.error('帧捕获失败:', e);
      }
    };

    const drawDebug = (ctx, res) => {
      if (!res) return;
      ctx.fillStyle = 'rgba(0,0,0,0.6)';
      ctx.fillRect(10,10,180,110);
      ctx.fillStyle = '#fff';
      ctx.font = '16px Arial';
      ctx.fillText(`评分: ${res.score}`,20,30);
      if (res.count !== undefined) ctx.fillText(`计数: ${res.count}`,20,55);
      if (res.angle !== undefined) ctx.fillText(`角度: ${res.angle}°`,20,80);
      if (res.feedback) ctx.fillText(`${res.feedback[0]}`,20,105);
    };

    const resetUpload = () => { uploadedVideoPath.value = ''; analysisResult.value = null; uploadVideo(); };
    const clearVideo = () => { uploadedVideoPath.value = ''; analysisResult.value = null; showToast('已清除'); };
    const resetAnalysis = async () => {
      if (!uploadedVideoPath.value) return showToast('无视频');
      loading.value = true;
      try {
        await uploadVideoFile(uploadedVideoPath.value);
      } finally { 
        loading.value = false;
      }
    };
    const clearAnalysis = () => { 
      analysisResult.value = null; 
      showToast('已清除'); 
    };

    const videoError = (e) => { videoErrorMsg.value = '视频加载失败'; showToast('视频异常'); };
    const videoLoaded = (e) => { videoDuration.value = e.detail.duration; videoErrorMsg.value = ''; };

    // 历史记录和报告相关方法
    const loadHistoryRecords = async () => {
      try {
        const token = uni.getStorageSync('token');
        const res = await fetch(`${API_BASE_URL}/records`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        const data = await res.json();
        if (data.success) {
          historyRecords.value = data.records;
        }
      } catch (e) {
        console.error('加载历史记录失败:', e);
      }
    };

    const showReportDetail = async (record) => {
      showHistory.value = false;
      showReport.value = true;
      loadingReport.value = true;
      
      try {
        currentReport.value = {
          id: record.id,
          record_id: record.id,
          score: record.score,
          count: record.count,
          feedback: record.feedback || [],
          exercise_type: record.exercise_type,
          record_created_at: record.created_at,
          knee_angle: record.knee_angle,
          hip_angle: record.hip_angle,
          trunk_angle: record.trunk_angle,
          suggestions: [
            { type: '基础', content: '保持良好的运动习惯' },
            { type: '姿势', content: '注意动作规范性' }
          ]
        };
      } catch (e) {
        console.error('加载报告失败:', e);
        showToast('加载报告失败');
      } finally {
        loadingReport.value = false;
      }
    };

    const getExerciseName = (type) => {
      const exercise = exercises.find(e => e.value === type);
      return exercise ? exercise.name : type;
    };

    const getScoreClass = (score) => {
      if (score >= 80) return 'score-excellent';
      if (score >= 60) return 'score-good';
      if (score >= 40) return 'score-average';
      return 'score-poor';
    };

    const formatTime = (timeStr) => {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    const handleShowHistory = () => {
      showHistory.value = true;
      loadHistoryRecords();
    };

    onMounted(() => {
      calculateScrollHeight();
      window.addEventListener('resize', handleResize);
    });

    onUnmounted(() => {
      stream.value?.getTracks().forEach(t=>t.stop());
      clearInterval(analysisInterval.value);
      window.removeEventListener('resize', handleResize);
    });

    const onPageScroll = () => {
      return;
    };

    return {
      showCamera,
      isCameraOn,
      analysisResult,
      loading,
      loadingText,
      videoElement,
      h5VideoElement,
      canvasElement,
      showCanvas,
      isAnalyzing,
      uploadedVideoPath,
      debug,
      videoDuration,
      videoErrorMsg,
      isRecording,
      countdown,
      showCountdown,
      selectedExercise,
      exercises,
      scrollViewHeight,
      onVideoLoaded,
      handleVideoPlay,
      handleVideoError,
      selectExercise,
      getExerciseIcon,
      getExerciseTitle,
      getExerciseDesc,
      startPoseAnalysis,
      toggleCamera,
      startRecording,
      stopRecording,
      uploadVideo,
      uploadVideoFile,
      uploadVideoForAnalysis,
      startRealtimeAnalysis,
      analyzeFrame,
      drawDebug,
      resetUpload,
      clearVideo,
      resetAnalysis,
      clearAnalysis,
      videoError,
      videoLoaded,
      showHistory,
      showReport,
      historyRecords,
      currentReport,
      loadingReport,
      loadHistoryRecords,
      showReportDetail,
      getExerciseName,
      getScoreClass,
      formatTime,
      handleShowHistory,
      onPageScroll,
      onScroll
    };
  }
};
</script>

<style lang="scss" scoped>
$primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
$shadow-sm: 0 4rpx 20rpx rgba(0,0,0,0.05);
$shadow-md: 0 8rpx 24rpx rgba(0,0,0,0.08);

:root {
  --bg-primary: #ffffff;
  --bg-card: #ffffff;
  --text-primary: #1f2937;
  --text-secondary: #4b5563;
  --bg-metric: #f8f9fc;
  --border-light: #e5e7eb;
}
/* 禁用暗色模式，强制使用白色背景 */
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #ffffff;
    --bg-card: #ffffff;
    --text-primary: #1f2937;
    --text-secondary: #4b5563;
    --bg-metric: #f8f9fc;
    --border-light: #e5e7eb;
  }
}

.mobile-container {
  background: var(--bg-primary);
  min-height: 100vh;
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 500rpx;
  background: radial-gradient(circle at 20% 30%, rgba(102,126,234,0.08), transparent);
  pointer-events: none;
}

.app-header {
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(20px);
  padding: 80rpx 30rpx 20rpx;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
}

.header-title {
  font-size: 40rpx;
  font-weight: 700;
  background: $primary-gradient;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.app-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.content-wrapper {
  padding: 20rpx 30rpx 40rpx;
  padding-bottom: calc(40rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

.exercise-selector {
  background: var(--bg-card);
  border-radius: 28rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.selector-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20rpx;
}

.exercise-options {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}

.exercise-option {
  flex: 1;
  min-width: 150rpx;
  background: #f3f4f6;
  border-radius: 48rpx;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.exercise-option.active {
  background: $primary-gradient;
  color: white;
}

.exercise-icon {
  font-size: 40rpx;
}

.exercise-name {
  font-size: 24rpx;
  font-weight: 500;
}

.feature-card {
  background: var(--bg-card);
  border-radius: 28rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
}

.feature-icon {
  width: 88rpx;
  height: 88rpx;
  background: rgba(102,126,234,0.1);
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-info {
  flex: 1;
  margin-left: 20rpx;
}

.feature-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text-primary);
}

.feature-desc {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.result-card {
  background: var(--bg-card);
  border-radius: 28rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.result-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20rpx;
}

.score-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx;
  border-radius: 28rpx;
  background: var(--bg-metric);
  margin-bottom: 24rpx;
}

.score {
  font-size: 80rpx;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.count-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 16rpx;
  background: var(--bg-metric);
  border-radius: 20rpx;
}

.count-num {
  font-size: 48rpx;
  font-weight: 700;
  color: #667eea;
}

.count-label {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.metrics-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.metric-item {
  background: var(--bg-metric);
  padding: 20rpx;
  border-radius: 20rpx;
  text-align: center;
}

.metric-label {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.metric-value {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.suggestion-container {
  background: #fff9e6;
  padding: 24rpx;
  border-radius: 20rpx;
  border-left: 6rpx solid #f59e0b;
  margin-bottom: 24rpx;
}

.suggestion-title {
  font-size: 26rpx;
  color: #f59e0b;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.suggestion-content {
  font-size: 24rpx;
  color: var(--text-secondary);
  line-height: 1.5;
}

/* AI 分析区域样式 */
.ai-analysis-container {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-radius: 28rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  border: 2rpx solid rgba(102, 126, 234, 0.2);
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid rgba(102, 126, 234, 0.2);
}

.ai-icon {
  font-size: 40rpx;
}

.ai-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #667eea;
  flex: 1;
}

.ai-badge {
  font-size: 20rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4rpx 12rpx;
  border-radius: 24rpx;
  font-weight: 500;
}

.ai-content {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 20rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  max-height: 400rpx;
  overflow-y: auto;
}

.ai-text {
  font-size: 26rpx;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12rpx;
}

.ai-time {
  font-size: 20rpx;
  color: var(--text-secondary);
  opacity: 0.7;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.camera-container {
  background: var(--bg-card);
  border-radius: 28rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.camera-preview {
  width: 100%;
  height: 500rpx;
  background: #111;
  border-radius: 28rpx;
  overflow: hidden;
  position: relative;
}

.video-player {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.canvas-overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.analysis-overlay {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  right: 20rpx;
  display: flex;
  justify-content: space-between;
}

.analysis-score {
  background: rgba(0,0,0,0.6);
  color: white;
  padding: 12rpx 20rpx;
  border-radius: 40rpx;
}

.score-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #ffd966;
}

.analysis-metrics {
  display: flex;
  gap: 12rpx;
}

.analysis-metrics .metric-item {
  background: rgba(0,0,0,0.6);
  color: white;
  padding: 8rpx 16rpx;
  border-radius: 32rpx;
}

.recording-indicator {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 12rpx 20rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.recording-dot {
  width: 16rpx;
  height: 16rpx;
  background: red;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

.countdown-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.countdown-text {
  font-size: 200rpx;
  font-weight: 700;
  color: white;
  text-shadow: 0 0 20rpx rgba(255,255,255,0.5);
  animation: countdown-pulse 1s infinite;
}

.camera-controls {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 20rpx;
}

.uploaded-video-container {
  background: var(--bg-card);
  border-radius: 28rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.video-preview {
  width: 100%;
  height: 400rpx;
  background: #000;
  border-radius: 20rpx;
  position: relative;
}

.uploaded-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-controls {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.action-buttons {
  display: flex;
  gap: 16rpx;
  margin-top: 16rpx;
}

.action-btn {
  flex: 1;
  padding: 28rpx 0;
  border-radius: 56rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.action-btn.primary {
  background: $primary-gradient;
  color: white;
}

.action-btn.secondary {
  background: #f3f4f6;
  color: #444;
}

.control-btn {
  flex: 1;
  padding: 20rpx;
  border-radius: 48rpx;
  background: #f3f4f6;
  border: none;
  font-size: 26rpx;
}

.result-controls {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.loading-content {
  background: rgba(255,255,255,0.9);
  border-radius: 48rpx;
  padding: 40rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.loading-content::before {
  content: "";
  width: 40rpx;
  height: 40rpx;
  border: 4rpx solid #eee;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  font-size: 28rpx;
}

@keyframes pulse {
  0%,100%{opacity:1;}50%{opacity:0.4;}
}

@keyframes spin {
  to{transform:rotate(360deg);}
}

@keyframes countdown-pulse {
  0%{transform:scale(1); opacity:1;}
  50%{transform:scale(1.2); opacity:0.8;}
  100%{transform:scale(1); opacity:1;}
}

/* 历史记录弹窗样式 */
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
  border: 2rpx solid var(--border-light);
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
  padding: 32rpx 32rpx 24rpx;
  border-bottom: 2rpx solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
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
  background: var(--bg-metric);
  display: flex;
  align-items: center;
  gap: 20rpx;
  cursor: pointer;
  transition: all 0.2s;
  border: 2rpx solid transparent;
}

.history-item-top3 {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border: 2rpx solid rgba(102, 126, 234, 0.15);
}

.history-rank {
  flex-shrink: 0;
}

.rank-badge {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
  color: white;
}

.rank-1 {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
}

.rank-2 {
  background: linear-gradient(135deg, #C0C0C0 0%, #A0A0A0 100%);
}

.rank-3 {
  background: linear-gradient(135deg, #CD7F32 0%, #B87333 100%);
}

.history-item-main {
  flex: 1;
  min-width: 0;
}

.history-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.history-item-main .exercise-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text-primary);
}

.score-badge {
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  font-weight: 600;
}

.score-excellent {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  color: white;
}

.score-good {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.score-average {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  color: white;
}

.score-poor {
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  color: white;
}

.score-text {
  font-size: 24rpx;
  font-weight: 600;
}

.history-item-info {
  display: flex;
  gap: 24rpx;
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
  font-size: 40rpx;
  color: var(--text-secondary);
  opacity: 0.5;
}

.history-footer {
  padding: 24rpx 32rpx;
  border-top: 2rpx solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
}

.history-stats {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.close-history-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 16rpx 40rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  font-weight: 600;
  cursor: pointer;
}

/* 报告详情弹窗样式 */
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
  max-width: 600rpx;
  max-height: 75vh;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
  border: 2rpx solid var(--border-light);
  display: flex;
  flex-direction: column;
}

.report-header {
  padding: 32rpx 32rpx 24rpx;
  border-bottom: 2rpx solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  flex-shrink: 0;
}

.report-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text-primary);
}

.report-body {
  flex: 1;
  overflow-y: auto;
  padding: 24rpx 32rpx;
}

.loading-report {
  padding: 64rpx 0;
  text-align: center;
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
  font-size: 20rpx;
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
  background: $primary-gradient;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 8rpx;
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
  background: #f0f9ff;
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
  color: #0284c7;
  margin-bottom: 4rpx;
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
  background: #ffffff;
  flex-shrink: 0;
}

.close-report-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 16rpx 40rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  font-weight: 600;
  cursor: pointer;
}
</style>