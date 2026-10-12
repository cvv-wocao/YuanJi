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
              <image class="exercise-icon" :src="exercise.icon" mode="aspectFit"></image>
              <text class="exercise-name">{{ exercise.name }}</text>
            </view>
          </view>
        </view>

        <view class="feature-card" @click="startPoseAnalysis">
          <view class="feature-icon">
            <image class="icon" :src="getExerciseIcon(selectedExercise)" mode="aspectFit"></image>
          </view>
          <view class="feature-info">
            <text class="feature-title">{{ getExerciseTitle(selectedExercise) }}</text>
            <text class="feature-desc">{{ getExerciseDesc(selectedExercise) }}</text>
          </view>
          <view class="feature-arrow">
            <text class="arrow">→</text>
          </view>
        </view>

        <!-- AI聊天卡片 -->
        <view class="feature-card" @click="showChatModal = true">
          <view class="feature-icon">
            <image class="icon" src="../../static/mall/icons/robot.png" mode="aspectFit"></image>
          </view>
          <view class="feature-info">
            <text class="feature-title">AI运动顾问</text>
            <text class="feature-desc">智能问答与建议</text>
          </view>
          <view class="feature-arrow">
            <text class="arrow">→</text>
          </view>
        </view>

        <!-- 设备信息卡片 -->
        <view class="feature-card" @click="showDeviceModal = true">
          <view class="feature-icon">
            <image class="icon" src="../../static/mall/icons/equipment.png" mode="aspectFit"></image>
          </view>
          <view class="feature-info">
            <text class="feature-title">设备信息</text>
            <text class="feature-desc">查看树莓派传感器数据</text>
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
		  
		  <view v-if="analysisResult" class="metrics-container">
		    <!-- 跑步：显示膝关节、髋关节、躯干角度 -->
		    <template v-if="selectedExercise === 'running'">
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
			</template>
		  
		    <!-- 哑铃弯举：显示肘关节角度 -->
		    <template v-else-if="selectedExercise === 'dumbbell_curl'">
		      <view v-if="analysisResult.angle !== undefined" class="metric-item">
		        <text class="metric-label">肘关节角度</text>
		        <text class="metric-value">{{ analysisResult.angle }}°</text>
		      </view>
		    </template>
		  
		    <!-- 俯卧撑：显示肘关节角度 -->
		    <template v-else-if="selectedExercise === 'pushup'">
		      <view v-if="analysisResult.angle !== undefined" class="metric-item">
		        <text class="metric-label">肘关节角度</text>
		        <text class="metric-value">{{ analysisResult.angle }}°</text>
		      </view>
		    </template>
		  
		    <!-- 默认（兜底）显示动作角度 -->
		    <template v-else>
		      <view v-if="analysisResult.angle !== undefined" class="metric-item">
		        <text class="metric-label">动作角度</text>
		        <text class="metric-value">{{ analysisResult.angle }}°</text>
		      </view>
		    </template>
		  </view>

          <view class="suggestion-container" v-if="analysisResult.ai_realtime_feedback">
            <text class="suggestion-title">💡 AI 实时反馈</text>
            <text class="suggestion-content">{{ analysisResult.ai_realtime_feedback }}</text>
          </view>

          <!-- AI 智能分析区域 -->
          <view class="ai-analysis-container" v-if="analysisResult.ai_analysis">
            <view class="ai-header">
              <text class="ai-icon">🤖</text>
              <text class="ai-title">AI 智能分析</text>
              <!--<text class="ai-badge">星火大模型</text>-->
            </view>
            <view class="ai-content">
              <text class="ai-text">{{ analysisResult.ai_analysis }}</text>
            </view>
            <view class="ai-footer" v-if="analysisResult.analysis_time">
              <text class="ai-time">分析时间：{{ analysisResult.analysis_time }}</text>
            </view>
          </view>

          <view class="result-controls">
            <button class="control-btn" @click="toggleCamera">关闭摄像头</button>
            <button class="control-btn" @click="clearAnalysis">清除结果</button>
          </view>
        </view>

        <view v-if="showCamera" class="camera-container">
          <!-- #ifdef H5 -->
          <!-- H5 环境：使用 video 元素显示摄像头 -->
          <view class="camera-preview">
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
              <view class="analysis-metrics">
                <view v-if="analysisResult.count !== undefined" class="metric-item">
                  <text class="metric-value">✅ {{ analysisResult.count }}</text>
                </view>
                <view v-if="analysisResult?.angle !== undefined" class="metric-item">
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

            <view v-if="isAnalyzing && analysisResult" class="live-feedback-panel">
              <view class="live-row">
                <text class="live-label">评分</text>
                <text class="live-value">{{ analysisResult?.score ?? 0 }}</text>
              </view>
              <view class="live-row">
                <text class="live-label">次数</text>
                <text class="live-value">{{ analysisResult?.count ?? 0 }}</text>
              </view>
              <view class="live-row" v-if="analysisResult?.angle">
                <text class="live-label">角度</text>
                <text class="live-value">{{ analysisResult?.angle }}°</text>
              </view>
              <view class="live-row">
                <text class="live-label">AI 建议</text>
                <text class="live-feedback">{{ analysisResult?.ai_realtime_feedback || '检测中...' }}</text>
              </view>
            </view>
          </view>
          <!-- #endif -->

          <!-- #ifndef H5 -->
          <!-- 非H5环境（APP/小程序）：使用原生 camera 组件 -->
          <view class="camera-preview">
            <!-- 确保 camera 组件直接渲染，不要用 v-if 嵌套太深 -->
            <camera
              v-if="isCameraOn"
              class="native-camera"
              device-position="back"
              flash="off"
              @error="handleCameraError"
              @ready="handleCameraReady"
              style="width: 100%; height: 100%; position: absolute; top: 0; left: 0;"
            ></camera>
            <view v-if="!isCameraOn" class="camera-placeholder">
              <text class="placeholder-text">点击下方按钮打开摄像头</text>
            </view>

            <canvas
              ref="canvasElement"
              v-if="isCameraOn && showCanvas"
              class="canvas-overlay"
              canvas-id="analysisCanvas"
              :style="{ width: '100%', height: '100%', position: 'absolute', top: 0, left: 0, pointerEvents: 'none' }"
            ></canvas>

            <view v-if="isRecording" class="recording-indicator">
              <view class="recording-dot"></view>
              <text class="recording-text">录制中</text>
            </view>

            <view v-if="isAnalyzing && analysisResult" class="analysis-overlay">
              <view class="analysis-metrics">
                <view v-if="analysisResult.count !== undefined" class="metric-item">
                  <text class="metric-value">✅ {{ analysisResult.count }}</text>
                </view>
                <view v-if="analysisResult?.angle !== undefined" class="metric-item">
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

            <view v-if="isAnalyzing && analysisResult" class="live-feedback-panel">
              <view class="live-row">
                <text class="live-label">评分</text>
                <text class="live-value">{{ analysisResult?.score ?? 0 }}</text>
              </view>
              <view class="live-row">
                <text class="live-label">次数</text>
                <text class="live-value">{{ analysisResult?.count ?? 0 }}</text>
              </view>
              <view class="live-row" v-if="analysisResult?.angle">
                <text class="live-label">角度</text>
                <text class="live-value">{{ analysisResult?.angle }}°</text>
              </view>
              <view class="live-row">
                <text class="live-label">AI 建议</text>
                <text class="live-feedback">{{ analysisResult?.ai_realtime_feedback || '检测中...' }}</text>
              </view>
            </view>
          </view>
          <!-- #endif -->
          <view class="camera-controls">
            <button class="control-btn" @click="toggleCamera">
              {{ isCameraOn ? '关闭摄像头' : '打开摄像头' }}
            </button>
            <button class="control-btn" @click="optimizedStartRealtimeAnalysis">
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









    <!-- AI聊天弹窗 -->
    <view v-if="showChatModal" class="chat-modal">
      <view class="chat-container">
        <view class="chat-header">
          <text class="chat-title">AI运动顾问</text>
          <view class="chat-actions">
            <view class="thread-selector" @click="createNewThread">
              + 新对话
            </view>
            <text class="chat-close" @click="closeChatModal">✕</text>
          </view>
        </view>

        <scroll-view 
          class="chat-messages" 
          scroll-y 
          :scroll-top="chatScrollTop"
          :scroll-with-animation="true"
        >
          <view 
            v-for="(msg, index) in chatMessages" 
            :key="index" 
            class="message-item"
            :class="msg.role === 'user' ? 'user-message' : 'ai-message'"
          >
            <view class="message-content">
              <text class="message-text">{{ msg.content }}</text>
              <text class="message-time">{{ formatTime(msg.timestamp) }}</text>
            </view>
          </view>

          <!-- AI正在输入提示 -->
          <view v-if="isAiTyping" class="message-item ai-message">
            <view class="message-content">
              <text class="message-text typing-indicator">AI正在思考...</text>
            </view>
          </view>
        </scroll-view>

        <view class="chat-input-area">
          <input 
            v-model="chatInput" 
            class="chat-input" 
            placeholder="输入您的问题..."
            @confirm="sendMessage"
            confirm-type="send"
          />
          <button class="send-btn" @click="sendMessage" :disabled="!chatInput.trim() || isAiTyping">
            发送
          </button>
        </view>
      </view>
    </view>



    <!-- 线程列表弹窗 -->
    <view v-if="showThreadList" class="thread-list-overlay">
      <view class="thread-list-container">
        <view class="thread-list-header">
          <text class="thread-list-title">选择对话线程</text>
          <text class="thread-list-close" @click="showThreadList = false">✕</text>
        </view>

        <scroll-view class="thread-list" scroll-y>
          <!-- 创建新线程按钮 -->
          <view class="create-thread-btn" @click="() => { createNewThread(); showThreadList = false; }">
            <text class="create-thread-icon">+</text>
            <text class="create-thread-text">创建新线程</text>
          </view>

          <!-- 线程列表 -->
          <view v-if="loadingThreads" class="loading-content">
            <text class="loading-text">加载线程中...</text>
          </view>

          <view v-else-if="threads.length === 0" class="empty-history">
            <text class="empty-icon">📝</text>
            <text class="empty-text">暂无线程</text>
            <text class="empty-hint">点击上方按钮创建新线程</text>
          </view>

          <view 
            v-for="thread in threads" 
            :key="thread.slug" 
            class="thread-item"
            :class="{ active: thread.slug === currentThreadSlug }"
            @click="switchThread(thread.slug)"
          >
            <text class="thread-name">{{ thread.name }}</text>
            <text class="thread-time">{{ formatTime(thread.updatedAt) }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <view v-if="loading" class="loading-overlay">
      <view class="loading-content">
        <text class="loading-text">{{ loadingText }}</text>
      </view>
    </view>

    <!-- 历史记录弹窗 -->
    <SportsModalHistory
      :show-history="showHistory"
      :history-records="historyRecords"
      @close="showHistory = false"
      @show-report="handleShowReportDetail"
      @open-habit-analysis="handleOpenHabitAnalysis"
    />

    <!-- 习惯分析弹窗 -->
    <SportsModalHabit
      :show-habit-modal="showHabitModal"
      :loading-habit="loadingHabit"
      :habit-analysis-result="habitAnalysisResult"
      :habit-error="habitError"
      @close="showHabitModal = false"
    />

    <!-- 报告详情弹窗 -->
    <SportsModalReport
      :show-report="showReport"
      :loading-report="loadingReport"
      :current-report="currentReport"
      @close="showReport = false"
    />

    <!-- 设备信息弹窗 -->
    <SportsModalDevice
      :show-device-modal="showDeviceModal"
      :loading-devices="loadingDevices"
      :devices="devices"
      @close="showDeviceModal = false"
      @refresh="handleFetchDevices"
    />
  </view>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { showToast } from '@/utils/index.js';
import { useAIChat } from '@/utils/aiChat.js';
import SportsModalHistory from './components/sports-modal-history.vue';
import SportsModalHabit from './components/sports-modal-habit.vue';
import SportsModalReport from './components/sports-modal-report.vue';
import SportsModalDevice from './components/sports-modal-device.vue';
import { API_BASE_URL, getToken, fetchDevices, showHabitAnalysis, loadHistoryRecords, showReportDetail, uploadVideoFile, uploadVideoForAnalysis } from '@/utils/sports-api.js';
import { drawSkeleton, drawDebug, analyzeFrame } from '@/utils/sports-camera.js';
import { speakText, isSpeechEnabled, lastSpokenText } from '@/utils/sports-speech.js';

export default {
  components: {
    SportsModalHistory,
    SportsModalHabit,
    SportsModalReport,
    SportsModalDevice
  },
  setup() {
    // 使用AI聊天功能
    const {
      showChatModal,
      chatMessages,
      chatInput,
      isAiTyping,
      chatScrollTop,
      currentThreadSlug,
      threads,
      showThreadList,
      loadingThreads,
      closeChatModal,
      sendMessage,
      createNewThread,
      getThreads,
      switchThread
    } = useAIChat();

    const showCamera = ref(false);
    const isCameraOn = ref(false);
    const cameraVideoUrl = ref('');
    const analysisResult = ref(null);
    const loading = ref(false);
    const loadingText = ref('正在分析...');
    const videoElement = ref(null);
    const h5VideoElement = ref(null);
    const canvasElement = ref(null);
    const cameraContext = ref(null); // 原生摄像头上下文（APP/小程序用）
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
      { value: 'running', name: '跑步', icon: '../../static/mall/icons/running.png' },
      { value: 'dumbbell_curl', name: '哑铃弯举', icon: '../../static/mall/icons/dumbbell.png' },
      { value: 'pushup', name: '俯卧撑', icon: '../../static/mall/icons/pushup.png' }
    ];

	// 习惯分析相关
	const showHabitModal = ref(false);
	const loadingHabit = ref(false);
	const habitAnalysisResult = ref(null);
	const habitError = ref('');

	// 设备信息相关
	const showDeviceModal = ref(false);
	const loadingDevices = ref(false);
	const devices = ref([]);

	// 适配导入的 fetchDevices 函数
	const handleFetchDevices = async () => {
	  loadingDevices.value = true;
	  try {
	    const data = await fetchDevices();
	    if (Array.isArray(data)) {
	      devices.value = data;
	    } else {
	      devices.value = [];
	    }
	  } catch (error) {
	    console.error('获取设备列表失败:', error);
	    showToast('获取设备失败，请检查网络连接');
	    devices.value = [];
	  } finally {
	    loadingDevices.value = false;
	  }
	};

	const isDeviceOnline = (lastSeen) => {
	  if (!lastSeen) return false;
	  const now = Date.now();
	  const diff = now - lastSeen;
	  return diff < 60000;
	};

	const formatDeviceTime = (timestamp) => {
	  if (!timestamp) return '--';
	  const date = new Date(timestamp);
	  return date.toLocaleString('zh-CN', {
	    month: '2-digit',
	    day: '2-digit',
	    hour: '2-digit',
	    minute: '2-digit',
	    second: '2-digit'
	  });
	};

	

	// 适配导入的 showHabitAnalysis 函数
	const handleShowHabitAnalysis = async () => {
	  showHabitModal.value = true;
	  loadingHabit.value = true;
	  habitAnalysisResult.value = null;
	  habitError.value = '';
	  
	  try {
	    const result = await showHabitAnalysis();
	    if (result.success) {
	      habitAnalysisResult.value = result;
	    } else {
	      habitError.value = result.msg || '分析失败，请稍后重试';
	    }
	  } catch (err) {
	    console.error('习惯分析请求失败:', err);
	    habitError.value = '网络错误，请检查后端服务';
	  } finally {
	    loadingHabit.value = false;
	  }
	};

const handleOpenHabitAnalysis = () => {
  showHistory.value = false;
  handleShowHabitAnalysis();
};

    // 计算滚动视图高度
    const calculateScrollHeight = () => {
      // #ifdef H5
      try {
        if (typeof document !== 'undefined' && document.querySelector) {
          const header = document.querySelector('.app-header');
          if (header) {
            const headerHeight = header.offsetHeight;
            const windowHeight = window.innerHeight;
            scrollViewHeight.value = windowHeight - headerHeight;
          } else {
            scrollViewHeight.value = window.innerHeight - 100;
          }
        } else {
          scrollViewHeight.value = 600;
        }
      } catch (e) {
        console.error('计算滚动高度失败:', e);
        scrollViewHeight.value = 600;
      }
      // #endif
      
      // #ifndef H5
      const systemInfo = uni.getSystemInfoSync();
      const windowHeight = systemInfo.windowHeight;
      scrollViewHeight.value = windowHeight - 100;
      // #endif
    };

    // 监听窗口大小变化
    const handleResize = () => {
      calculateScrollHeight();
    };

    const onScroll = (e) => {
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

    // 处理原生摄像头准备就绪
    const handleCameraReady = (e) => {
      console.log('原生摄像头准备就绪:', e);
      
      // 获取摄像头上下文
      cameraContext.value = uni.createCameraContext();
      console.log('摄像头上下文已创建:', cameraContext.value);
      
      // 摄像头真正打开后才显示成功提示
      showToast('摄像头已打开');
      console.log('摄像头已成功打开');
    };
    
    // 处理原生摄像头错误
    const handleCameraError = (err) => {
      console.error('原生摄像头错误:', err);
      
      // 在手机上显示详细错误信息
      uni.showModal({
        title: '摄像头错误',
        content: '错误详情: ' + JSON.stringify(err),
        showCancel: false
      });
      
      const errorMsg = err?.detail?.errMsg || err?.message || String(err);
      
      if (errorMsg.includes('permission') || errorMsg.includes('denied')) {
        showToast('摄像头权限被拒绝，请在设置中允许访问摄像头');
      } else if (errorMsg.includes('not found') || errorMsg.includes('未找到')) {
        showToast('未找到可用摄像头设备');
      } else if (errorMsg.includes('binding') || errorMsg.includes('占用')) {
        showToast('摄像头被其他应用占用');
      } else {
        showToast(`摄像头异常: ${errorMsg}`);
      }
      
      isCameraOn.value = false;
      showCanvas.value = false;
      stopRealtimeAnalysis();
    };

    const selectExercise = (exercise) => {
      selectedExercise.value = exercise;
      analysisResult.value = null;
      showToast(`已切换：${exercises.find(e=>e.value===exercise)?.name}`);
    };

    const getExerciseIcon = (e) => exercises.find(x=>x.value===e)?.icon || '../../static/mall/icons/running.png';
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
		if (!isCameraOn.value) return showToast('请打开摄像头');
		  if (isAnalyzing.value) {
		    stopRealtimeAnalysis();
		    return showToast('已停止分析');
		  }
		
		  // ✅ 初始化分析结果，避免字段缺失
		  analysisResult.value = {
		    score: 0,
		    count: 0,
		    feedback: [],
		    ai_realtime_feedback: '检测中...',
		    angle: 0,
		    kneeAngle: 0,
		    hipAngle: 0,
		    trunkAngle: 0,
		    skeleton: []
		  };
		
		  showCanvas.value = true;
		  await nextTick();
		  
      loading.value = true;
      loadingText.value = '加载标准动作...';
      try {
        const token = getToken();
        const res = await fetch(`${API_BASE_URL}/analyze/standard`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        const data = await res.json();
        showToast(data.success ? '分析完成' : '分析失败');
      } catch (e) {
        showToast('后端未启动');
      } finally { loading.value = false; }
    };

    // 停止实时分析并清除定时器的公共函数
    const stopRealtimeAnalysis = () => {
      if (isAnalyzing.value) {
        isAnalyzing.value = false;
        if (analysisInterval.value) {
          clearInterval(analysisInterval.value);
          analysisInterval.value = null;
        }
        console.log('实时分析已停止');
      }
    };

    const toggleCamera = async () => {
      console.log('尝试打开/关闭摄像头...');
      
      // 先检查是否点击了"开始检测"
      if (!showCamera.value) {
        showToast('请先点击"开始检测"按钮');
        return;
      }
      
      try {
        if (isCameraOn.value) {
          // ===== 关闭摄像头 =====
          stopRealtimeAnalysis();
          
          // #ifdef H5
          if (stream.value) {
            stream.value.getTracks().forEach(t => t.stop());
            stream.value = null;
          }
          
          const cameraContainer = document.querySelector('.camera-preview');
          if (cameraContainer) {
            const videoEl = cameraContainer.querySelector('video');
            if (videoEl) {
              cameraContainer.removeChild(videoEl);
              console.log('H5视频元素已移除');
            }
          }
          // #endif
          
          // #ifndef H5
          if (cameraContext.value) {
            cameraContext.value.stopRecord();
            cameraContext.value = null;
          }
          // #endif
          
          isCameraOn.value = false;
          showCanvas.value = false;
          showToast('摄像头已关闭');
          console.log('摄像头已关闭');
        } else {
          // ===== 打开摄像头 =====
          // 先设置 showCanvas 为 true，但 isCameraOn 只在成功后设置
          showCanvas.value = true;
          await nextTick();
          console.log('DOM 已更新');
          
          await new Promise(resolve => setTimeout(resolve, 300));
          
          // #ifdef H5
          // H5 环境：使用浏览器 API
          console.log('H5请求摄像头权限...');
          
          uni.showToast({ title: '正在请求摄像头权限...', icon: 'none', duration: 2000 });
          
          // 添加环境检测：检查 WebRTC API 是否可用
          if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            console.error('当前环境不支持 WebRTC API');
            throw new Error('当前设备或浏览器不支持摄像头访问功能');
          }
          
          // 检查安全上下文（HTTPS 或 localhost）
          if (typeof window !== 'undefined' && !window.isSecureContext) {
            console.warn('警告：当前不在安全上下文环境中，摄像头访问可能受限');
          }
          
          try {
            stream.value = await navigator.mediaDevices.getUserMedia({ video: true });
            console.log('H5获取摄像头流成功');
            
            // 只有成功获取到流后才设置 isCameraOn
            isCameraOn.value = true;
          } catch (error) {
            console.error('Web API 获取摄像头失败:', error);
            // 重置状态
            showCanvas.value = false;
            throw new Error('摄像头访问被拒绝，请检查浏览器权限设置');
          }
          
          const cameraContainer = document.querySelector('.camera-preview');
          const existingVideo = cameraContainer.querySelector('video');
          if (existingVideo) {
            cameraContainer.removeChild(existingVideo);
          }
          
          const videoEl = document.createElement('video');
          videoEl.id = 'videoElement';
          videoEl.autoplay = true;
          videoEl.playsInline = true;
          videoEl.muted = true;
          videoEl.style.position = 'absolute';
          videoEl.style.width = '100%';
          videoEl.style.height = '100%';
          videoEl.style.objectFit = 'cover';
          videoEl.srcObject = stream.value;
          cameraContainer.appendChild(videoEl);
          videoElement.value = videoEl;
          
          const canvasEl = document.querySelector('.canvas-overlay');
          if (canvasEl) {
            canvasElement.value = canvasEl;
          }
          
          videoEl.onloadedmetadata = async () => {
            console.log('H5视频元数据已加载');
            await nextTick();
            if (canvasElement.value && typeof canvasElement.value.getContext === 'function') {
              const canvas = canvasElement.value;
              canvas.width = videoEl.videoWidth || 640;
              canvas.height = videoEl.videoHeight || 480;
            }
          };
          
          await new Promise(resolve => setTimeout(resolve, 1000));
          showToast('H5摄像头已打开');
          console.log('H5摄像头已成功打开');
          // #endif
          
          // #ifndef H5
          // 非H5环境（APP/小程序）：使用 uni-app 原生 camera API
          console.log('初始化原生摄像头...');
          
          uni.showToast({ title: '正在请求摄像头权限...', icon: 'none', duration: 2000 });
          
          // 先请求摄像头权限
          try {
            await new Promise((resolve, reject) => {
              uni.authorize({
                scope: 'scope.camera',
                success: () => {
                  console.log('摄像头权限已授权');
                  resolve();
                },
                fail: (err) => {
                  console.error('摄像头权限授权失败:', err);
                  reject(new Error('请在系统设置中开启摄像头权限'));
                }
              });
            });
          } catch (error) {
            // 如果权限请求失败，尝试直接继续（某些设备可能不需要手动授权）
            console.warn('权限请求失败，尝试直接打开:', error.message);
          }
          
          // 使用 uni.createSelectorQuery 获取 canvas 元素
          uni.createSelectorQuery()
            .select('.canvas-overlay')
            .node()
            .exec((res) => {
              if (res && res[0] && res[0].node) {
                canvasElement.value = res[0].node;
                console.log('原生Canvas元素已获取');
              }
            });
          
          // 设置 isCameraOn = true 来渲染 <camera> 组件
          // 摄像头真正打开的状态会在 handleCameraReady 回调中确认
          isCameraOn.value = true;
          console.log('等待摄像头组件渲染...');
          // #endif
        }
      } catch (error) {
        console.error('摄像头访问错误:', error);
        
        // 在手机上显示错误弹窗
        uni.showModal({
          title: '摄像头错误',
          content: '错误类型: ' + (error.name || '未知') + '\n错误信息: ' + (error.message || JSON.stringify(error)),
          showCancel: false
        });
        
        isCameraOn.value = false;
        showCanvas.value = false;
        stopRealtimeAnalysis();
      }
    };

    const startRecording = async () => {
      if (!stream.value) return showToast('请先开摄像头');
      
      showCountdown.value = true;
      countdown.value = 3;
      showCanvas.value = true;
      await nextTick();
      
      if (!canvasElement.value) {
        const canvasEl = document.querySelector('.canvas-overlay');
        if (canvasEl) {
          canvasElement.value = canvasEl;
        }
      }
      
      const countdownTimer = setTimeout(async () => {
        countdown.value = 2;
        setTimeout(async () => {
          countdown.value = 1;
          setTimeout(async () => {
            countdown.value = 0;
            showCountdown.value = false;
			
			analysisResult.value = {
			  score: 0,
			  count: 0,
			  feedback: [],
			  ai_realtime_feedback: '检测中...',
			  angle: 0,
			  kneeAngle: 0,
			  hipAngle: 0,
			  trunkAngle: 0,
			  skeleton: []
			};
            
            showCanvas.value = true;
            await nextTick();
            await new Promise(resolve => setTimeout(resolve, 200));
            
            if (!canvasElement.value) {
              const canvasEl = document.querySelector('.canvas-overlay');
              if (canvasEl) {
                canvasElement.value = canvasEl;
              }
            }
            
            uploadedVideoPath.value = '';
            
            // 初始化分析结果，确保面板能正确显示
            analysisResult.value = {
              score: 0,
              count: 0,
              feedback: [],
              ai_realtime_feedback: '检测中...',
              angle: 0,
              kneeAngle: 0,
              hipAngle: 0,
              trunkAngle: 0,
              skeleton: []
            };
            
            // 配置 MediaRecorder
            const options = {
              mimeType: 'video/webm;codecs=vp9',
              videoBitsPerSecond: 1000000 // 1 Mbps
            };
            
            try {
              recorder.value = new MediaRecorder(stream.value, options);
            } catch (e) {
              console.warn('使用默认编码:', e);
              recorder.value = new MediaRecorder(stream.value);
            }
            
            recordedChunks.value = [];
            const startTime = Date.now();
            
            recorder.value.ondataavailable = e => {
              if (e.data.size > 0) {
                recordedChunks.value.push(e.data);
                console.log('获取数据块:', e.data.size, 'bytes');
              }
            };
            
            recorder.value.onstop = async () => {
              const blob = new Blob(recordedChunks.value, { type: 'video/webm' });
              const url = URL.createObjectURL(blob);
              uploadedVideoPath.value = url;
              const duration = (Date.now() - startTime) / 1000;
              console.log('录制时长:', duration, '秒');
              console.log('录制文件大小:', blob.size, 'bytes');
              
              if (duration > 1) {
                await uploadVideoForAnalysis(blob);
              } else {
                showToast('录制时间太短，无法分析');
              }
              
              // 停止实时分析
              stopRealtimeAnalysis();
            };
            
            recorder.value.onerror = (e) => {
              console.error('录制错误:', e);
              isRecording.value = false;
              stopRealtimeAnalysis();
              showToast('录制失败');
            };
            
            const recordingStartTime = Date.now();
            // 开始录制，每1秒获取一个数据块
            recorder.value.start(1000);
            isRecording.value = true;
            isAnalyzing.value = true; // 确保分析面板显示
            showToast('开始录制');
            
            // 启动实时分析
            analyzeFrame();
            if (analysisInterval.value) clearInterval(analysisInterval.value);
            analysisInterval.value = setInterval(analyzeFrame, 500);
            
            const suggestionInterval = setInterval(() => {
              const elapsedTime = (Date.now() - recordingStartTime) / 1000;
              if (Math.floor(elapsedTime) % 10 === 0 && elapsedTime > 0) {
                console.log('显示分析建议');
              }
            }, 1000);
            
            const originalOnStop = recorder.value.onstop;
            recorder.value.onstop = async () => {
              clearInterval(suggestionInterval);
              await originalOnStop();
            };
          }, 1000);
        }, 1000);
      }, 1000);
    };

    const stopRecording = () => {
      if (recorder.value?.state === 'recording') {
        recorder.value.stop();
        isRecording.value = false;
        stopRealtimeAnalysis();
        showToast('已停止，正在分析…');
      }
    };

    // 上传视频前确保停止所有实时分析并关闭摄像头
    const uploadVideo = () => {
      // 停止实时分析（如果正在运行）
      stopRealtimeAnalysis();
      // 如果摄像头打开，关闭它（避免资源占用）
      if (isCameraOn.value) {
        toggleCamera(); // 异步关闭，但选择视频会等待
      }
      // 直接调用文件选择器，确保在用户激活状态下
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
      const token = getToken();
      
      // #ifdef H5
      return new Promise((resolve, reject) => {
        uni.uploadFile({
          url: `${API_BASE_URL}/analyze/video`,
          filePath: path,
          name: 'video',
          timeout: 240000,
          formData: {
            'exercise_type': selectedExercise.value
          },
          header: {
            'Authorization': `Bearer ${token}`
          },
          success: (uploadRes) => {
            try {
              const data = JSON.parse(uploadRes.data);
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
                // 检查是否是运动类型不匹配
                if (data.detected_exercise && data.detected_exercise !== data.selected_exercise) {
                  // 显示类型不匹配提示，提醒用户需要切换模块
                  uni.showModal({
                    title: '运动类型不匹配',
                    content: `检测到该视频是${data.detected_name}动作，与您当前选择的${data.selected_name}模块不匹配，请切换到${data.detected_name}模块进行分析。`,
                    confirmText: '确定',
                    showCancel: false,
                    success: (modalRes) => {
                      // 用户点击确定，不自动切换，让用户手动选择
                    }
                  });
                } else {
                  showToast(data.feedback?.[0] || '分析失败');
                }
                reject(new Error(data.feedback?.[0] || '分析失败'));
              }
            } catch (e) {
              console.error('解析返回数据失败：', e);
              showToast('分析失败');
              reject(e);
            }
          },
          fail: (err) => {
            console.error('上传失败：', err);
            showToast(`上传失败：${err.errMsg}`);
            reject(err);
          }
        });
      });
      // #endif
      
      // #ifndef H5
      return new Promise((resolve, reject) => {
        uni.uploadFile({
          url: `${API_BASE_URL}/analyze/video`,
          filePath: path,
          name: 'video',
	      timeout: 240000,
          formData: { exercise_type: selectedExercise.value },
          header: {
            'Authorization': `Bearer ${token}`
          },
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
                // 检查是否是运动类型不匹配
                if (data.detected_exercise && data.detected_exercise !== data.selected_exercise) {
                  // 显示类型不匹配提示，提醒用户需要切换模块
                  uni.showModal({
                    title: '运动类型不匹配',
                    content: `检测到该视频是${data.detected_name}动作，与您当前选择的${data.selected_name}模块不匹配，请切换到${data.detected_name}模块进行分析。`,
                    confirmText: '确定',
                    showCancel: false,
                    success: (modalRes) => {
                      // 用户点击确定，不自动切换，让用户手动选择
                    }
                  });
                } else {
                  showToast(data.feedback?.[0] || '分析失败');
                }
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
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 120000);
  try {
	const form = new FormData();
	form.append('video', blob, 'video.webm');
	form.append('exercise_type', selectedExercise.value);
	const token = getToken();
	const r = await fetch(`${API_BASE_URL}/analyze/video`, {
	  method: 'POST',
	  body: form,
	  signal: controller.signal,
	  headers: {
	    'Authorization': `Bearer ${token}`
	  }
	});
		clearTimeout(timeoutId);
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
		  showToast('分析完成');
		} else {
		  showToast(data.msg || '分析失败');
		}
	  } catch (e) {
		clearTimeout(timeoutId);
		if (e.name === 'AbortError') {
		  showToast('分析超时，请稍后重试或缩短视频时长');
		} else {
		  console.error('上传分析失败:', e);
		  showToast('服务未启动或网络错误');
		}
	  }
	};
	
    const startRealtimeAnalysis = async () => {
	  if (window.speechSynthesis && !window._speechActivated) {
	    window._speechActivated = true;
	    const tempUtterance = new SpeechSynthesisUtterance(' ');
	    tempUtterance.lang = 'zh-CN';
	    window.speechSynthesis.speak(tempUtterance);
	    setTimeout(() => window.speechSynthesis.cancel(), 100);
	  }
      if (!isCameraOn.value) return showToast('请打开摄像头');
      if (isAnalyzing.value) {
        stopRealtimeAnalysis();
        return showToast('已停止分析');
      }
      
      showCanvas.value = true;
      await nextTick();
      if (!canvasElement.value) {
        const canvasEl = document.querySelector('canvas.canvas-overlay');
        if (canvasEl) {
          canvasElement.value = canvasEl;
        }
      }
      
      isAnalyzing.value = true;
      analyzeFrame();
      if (analysisInterval.value) clearInterval(analysisInterval.value);
      analysisInterval.value = setInterval(analyzeFrame, 800);
      showToast('开始实时分析');
    };

    const analyzeFrame = async () => {
      try {
        // 摄像头未打开时不执行分析，且不输出错误日志（避免刷屏）
        if (!isCameraOn.value) {
          return;
        }
        
        if (!videoElement.value) {
          const videoEl = document.querySelector('#videoElement');
          if (videoEl) {
            videoElement.value = videoEl;
          } else {
            return;
          }
        }
        
        showCanvas.value = true;
        await nextTick();
        await new Promise(resolve => setTimeout(resolve, 50));
        
        let canvasEl = canvasElement.value;
        if (!canvasEl) {
          canvasEl = document.querySelector('canvas.canvas-overlay') || document.querySelector('canvas.uni-canvas-canvas') || document.querySelector('canvas');
          if (canvasEl && canvasEl.tagName === 'CANVAS') {
            canvasElement.value = canvasEl;
          } else {
            return;
          }
        }
        
        if (canvasEl.tagName !== 'CANVAS') return;
        if (typeof canvasEl.getContext !== 'function') return;
        
        const ctx = canvasEl.getContext('2d');
        if (!ctx) return;
        
        if (videoElement.value.videoWidth && videoElement.value.videoHeight) {
          canvasEl.width = videoElement.value.videoWidth;
          canvasEl.height = videoElement.value.videoHeight;
        }
        
        ctx.clearRect(0,0,canvasEl.width,canvasEl.height);
        ctx.drawImage(videoElement.value,0,0,canvasEl.width,canvasEl.height);
        
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
              const token = getToken();
              const r = await fetch(`${API_BASE_URL}/analyze/frame`, { 
                method: 'POST', 
                body: f,
                headers: {
                  'Authorization': `Bearer ${token}`
                }
              });
              const j = await r.json();
              if (j.success) {
				const newFeedback = j.ai_realtime_feedback || '分析中...';
				if (!uploadedVideoPath.value && 
				    newFeedback !== analysisResult.value?.ai_realtime_feedback && 
				    newFeedback !== '分析中...' && 
				    newFeedback !== '检测中...') {
				    speakText(newFeedback).catch(err => console.log('语音播报失败:', err));
				}
				  
                analysisResult.value = {
                  score: j.score || 0,
                  count: j.count || 0,
                  feedback: j.feedback || [],
                  ai_realtime_feedback: j.ai_realtime_feedback || '分析中...',
                  angle: j.angle || 0,
                  kneeAngle: j.kneeAngle || 0,
                  hipAngle: j.hipAngle || 0,
                  trunkAngle: j.trunkAngle || 0,
                  skeleton: j.skeleton || [],
                  ai_analysis: analysisResult.value?.ai_analysis || ''
                };
                const currentCanvas = canvasElement.value;
                if (currentCanvas && typeof currentCanvas.getContext === 'function') {
                  const ctx = currentCanvas.getContext('2d');
                  if (ctx) {
                    drawSkeleton(ctx, analysisResult.value.skeleton, canvasEl.width, canvasEl.height);
                    drawDebug(ctx, analysisResult.value);
                  }
                }
              }
            } catch(e) {
              console.error('帧分析失败:', e);
            }
          });
        } catch(e) {
          console.error('帧捕获失败:', e);
        }
      } catch (error) {
        // 静默处理，避免刷屏
      }
    };

    const drawSkeleton = (ctx, skeleton, canvasWidth, canvasHeight) => {
      if (!skeleton || skeleton.length === 0) return;
      
      const connections = [
        [0, 1], [1, 2], [2, 3], [3, 4],
        [0, 5], [5, 6], [6, 7], [7, 8],
        [0, 9], [9, 10], [10, 11], [11, 12],
        [0, 13], [13, 14], [14, 15], [15, 16],
        [0, 17], [17, 18], [18, 19], [19, 20],
        [20, 21], [21, 22], [22, 23], [23, 24]
      ];
      
      ctx.strokeStyle = '#00FFFF';
      ctx.lineWidth = 3;
      
      connections.forEach(([start, end]) => {
        if (start < skeleton.length && end < skeleton.length) {
          const startPoint = skeleton[start];
          const endPoint = skeleton[end];
          if (startPoint && endPoint) {
            ctx.beginPath();
            ctx.moveTo(startPoint[0] * canvasWidth, startPoint[1] * canvasHeight);
            ctx.lineTo(endPoint[0] * canvasWidth, endPoint[1] * canvasHeight);
            ctx.stroke();
          }
        }
      });
      
      ctx.fillStyle = '#FF00FF';
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 2;
      
      skeleton.forEach(point => {
        if (point) {
          const x = point[0] * canvasWidth;
          const y = point[1] * canvasHeight;
          ctx.beginPath();
          ctx.arc(x, y, 5, 0, Math.PI * 2);
          ctx.fill();
          ctx.stroke();
        }
      });
    };

    const drawDebug = (ctx, res) => {
      if (!res) return;
      ctx.fillStyle = 'rgba(0,0,0,0.6)';
      ctx.fillRect(10,10,180,90);
      ctx.fillStyle = '#fff';
      ctx.font = '16px Arial';
      if (res.count !== undefined) ctx.fillText(`计数: ${res.count}`,20,30);
      if (res.angle !== undefined) ctx.fillText(`角度: ${res.angle}°`,20,55);
      if (res.feedback) ctx.fillText(`${res.feedback[0]}`,20,80);
      if (res.ai_realtime_feedback) ctx.fillText(`${res.ai_realtime_feedback}`,20,80);
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

    // 适配导入的 loadHistoryRecords 函数
    const handleLoadHistoryRecords = async () => {
      try {
        const data = await loadHistoryRecords();
        if (data.success) {
          historyRecords.value = data.records;
        }
      } catch (e) {
        console.error('加载历史记录失败:', e);
      }
    };

    // 适配导入的 showReportDetail 函数
    const handleShowReportDetail = async (record) => {
      showHistory.value = false;
      showReport.value = true;
      loadingReport.value = true;
      
      try {
        const report = await showReportDetail(record, getExerciseName);
        currentReport.value = report || {
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
      handleLoadHistoryRecords();
    };

    // 优化的帧分析函数，基于项目5的实现
    const optimizedAnalyzeFrame = async () => {
      try {
        console.log('开始分析帧（优化版）');
        
        // 检查摄像头是否打开
        if (!isCameraOn.value) {
          console.error('摄像头未打开');
          return;
        }
        
        // 尝试获取视频元素（如果 videoElement.value 不存在）
        if (!videoElement.value) {
          const videoEl = document.querySelector('#videoElement');
          if (videoEl) {
            videoElement.value = videoEl;
            console.log('从 DOM 中获取视频元素:', videoEl);
          } else {
            console.error('视频元素未找到');
            return;
          }
        }
        
        // 确保 canvas 显示
        showCanvas.value = true;
        console.log('设置 showCanvas.value = true');
        
        // 检查摄像头状态
        console.log('摄像头状态:', isCameraOn.value);
        console.log('Canvas 显示状态:', showCanvas.value);
        
        // 等待 DOM 更新
        await nextTick();
        console.log('DOM 已更新');
        
        // 增加等待时间，确保 canvas 元素完全渲染
        await new Promise(resolve => setTimeout(resolve, 500));
        console.log('等待 500ms 后尝试获取 canvas 元素');
        
        // 尝试多次获取 canvas 元素
        let canvasEl = null;
        for (let i = 0; i < 3; i++) {
          // 尝试使用不同的选择器获取 canvas 元素
          canvasEl = document.querySelector('canvas.canvas-overlay') || document.querySelector('canvas.uni-canvas-canvas') || document.querySelector('canvas');
          console.log(`尝试获取 canvas 元素 (${i+1}/3):`, canvasEl);
          
          if (canvasEl) {
            break;
          }
          
          // 等待一段时间后再次尝试
          await new Promise(resolve => setTimeout(resolve, 300));
        }
        
        // 检查 canvas 元素是否存在
        if (!canvasEl) {
          console.error('无法找到 canvas 元素，分析帧失败');
          console.error('当前 DOM 中所有 canvas 元素:', document.querySelectorAll('canvas'));
          return;
        }
        
        // 检查是否是真正的 canvas 元素
        if (typeof canvasEl !== 'object' || canvasEl === null) {
          console.error('canvasEl 不是有效的对象:', canvasEl);
          return;
        }
        
        if (typeof canvasEl.tagName !== 'string') {
          console.error('canvasEl 没有有效的 tagName 属性:', canvasEl);
          return;
        }
        
        if (canvasEl.tagName !== 'CANVAS') {
          console.error('获取到的元素不是 canvas 元素:', canvasEl.tagName);
          return;
        }
        
        // 更新 ref，确保下次使用时能直接获取到
        canvasElement.value = canvasEl;
        
        // 检查 canvas 元素是否有效
        if (typeof canvasEl.getContext !== 'function') {
          console.error('canvas 元素没有 getContext 方法');
          return;
        }
        
        const ctx = canvasEl.getContext('2d');
        if (!ctx) {
          console.error('无法获取 canvas 2D 上下文');
          return;
        }
        
        // 确保 canvas 尺寸正确
        if (videoElement.value.videoWidth && videoElement.value.videoHeight) {
          canvasEl.width = videoElement.value.videoWidth;
          canvasEl.height = videoElement.value.videoHeight;
          console.log('Canvas 尺寸已设置:', canvasEl.width, 'x', canvasEl.height);
        }
        
        ctx.clearRect(0,0,canvasEl.width,canvasEl.height);
        ctx.drawImage(videoElement.value,0,0,canvasEl.width,canvasEl.height);
        
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
              const token = getToken();
              const r = await fetch(`${API_BASE_URL}/analyze/frame`, { 
                method: 'POST', 
                body: f,
                headers: {
                  'Authorization': `Bearer ${token}`
                }
              });
              const j = await r.json();
              console.log('后端返回数据:', j);
              if (j.success) {
                const newFeedback = j.ai_realtime_feedback || '分析中...';
                if (!uploadedVideoPath.value && 
                    newFeedback !== analysisResult.value?.ai_realtime_feedback && 
                    newFeedback !== '分析中...' && 
                    newFeedback !== '检测中...') {
                    speakText(newFeedback).catch(err => console.log('语音播报失败:', err));
                }
                
                analysisResult.value = {
                  score: j.score || 0,
                  count: j.count || 0,
                  feedback: j.feedback || [],
                  ai_realtime_feedback: j.ai_realtime_feedback || '分析中...',
                  angle: j.angle || 0,
                  kneeAngle: j.kneeAngle || 0,
                  hipAngle: j.hipAngle || 0,
                  trunkAngle: j.trunkAngle || 0,
                  skeleton: j.skeleton || [],
                  ai_analysis: analysisResult.value?.ai_analysis || ''
                };
                console.log('分析结果:', analysisResult.value);
                // 确保 canvas 元素仍然存在且有效
                const currentCanvas = canvasElement.value;
                if (currentCanvas && typeof currentCanvas.getContext === 'function') {
                  const ctx = currentCanvas.getContext('2d');
                  if (ctx) {
                    drawSkeleton(ctx, analysisResult.value.skeleton, canvasEl.width, canvasEl.height);
                    drawDebug(ctx, analysisResult.value);
                  }
                }
              }
            } catch(e) {
              console.error('帧分析失败:', e);
            }
          });
        } catch(e) {
          console.error('帧捕获失败:', e);
        }
      } catch (error) {
        console.error('分析帧时发生错误:', error);
      }
    };

    // 优化的实时分析启动函数
    const optimizedStartRealtimeAnalysis = async () => {
      // 首次激活语音播报（如果尚未激活）
      if (window.speechSynthesis && !window._speechActivated) {
        window._speechActivated = true;
        // 静默播报一个空字符或极短字符，激活 API
        const tempUtterance = new SpeechSynthesisUtterance(' ');
        tempUtterance.lang = 'zh-CN';
        window.speechSynthesis.speak(tempUtterance);
        // 立即取消，避免发出声音
        setTimeout(() => window.speechSynthesis.cancel(), 100);
      }
      if (!isCameraOn.value) return showToast('请打开摄像头');
      if (isAnalyzing.value) {
        isAnalyzing.value = false;
        clearInterval(analysisInterval.value);
        return showToast('已停止分析');
      }
      
      // 确保 canvas 显示
      showCanvas.value = true;
      console.log('设置 showCanvas.value = true');
      
      // 等待 DOM 更新
      await nextTick();
      console.log('DOM 已更新');
      
      // 尝试获取 canvas 元素
      if (!canvasElement.value) {
        const canvasEl = document.querySelector('canvas.canvas-overlay');
        if (canvasEl) {
          canvasElement.value = canvasEl;
          console.log('从 DOM 中获取 canvas 元素:', canvasEl);
        }
      }
      
      isAnalyzing.value = true;
      optimizedAnalyzeFrame();
      if (analysisInterval.value) clearInterval(analysisInterval.value);
      analysisInterval.value = setInterval(optimizedAnalyzeFrame, 800);
      showToast('开始实时分析');
    };

    onMounted(() => {
      calculateScrollHeight();
      window.addEventListener('resize', handleResize);
    });

    onUnmounted(() => {
      // 组件卸载时彻底清理
      if (analysisInterval.value) clearInterval(analysisInterval.value);
      if (stream.value) {
        stream.value.getTracks().forEach(t => t.stop());
      }
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
      handleCameraError,
      handleCameraReady,
      startRecording,
      stopRecording,
      uploadVideo,
      uploadVideoFile,
      uploadVideoForAnalysis,
      startRealtimeAnalysis,
      analyzeFrame,
      optimizedStartRealtimeAnalysis,
      optimizedAnalyzeFrame,
      drawSkeleton,
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
      handleLoadHistoryRecords,
      handleShowReportDetail,
	  showHabitModal,
	  loadingHabit,
	  habitAnalysisResult,
	  habitError,
	  handleShowHabitAnalysis,
	  handleOpenHabitAnalysis,
	  // 设备信息相关
	  showDeviceModal,
	  loadingDevices,
	  devices,
	  handleFetchDevices,
	  isDeviceOnline,
	  formatDeviceTime,
      getExerciseName,
      getScoreClass,
      formatTime,
      handleShowHistory,
      onPageScroll,
      onScroll,
      showChatModal,
      chatMessages,
      chatInput,
      isAiTyping,
      chatScrollTop,
      currentThreadSlug,
      threads,
      showThreadList,
      loadingThreads,
      closeChatModal,
      sendMessage,
      createNewThread,
      getThreads,
      switchThread
    };
  }
};
</script>

<style lang="scss" scoped>

$primary-gradient: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
$secondary-gradient: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
$accent-color: #2fcaa352;
$success-color: #4caf50;
$warning-color: #ff9800;
$error-color: #f44336;
$shadow-sm: 0 2rpx 8rpx rgba(0,0,0,0.05);
$shadow-md: 0 4rpx 12rpx rgba(0,0,0,0.08);
$shadow-lg: 0 6rpx 20rpx rgba(0,0,0,0.1);

:root {
  --bg-primary: #f8fafc;
  --bg-card: #ffffff;
  --text-primary: #c9cf17ff;
  --text-secondary: #0bc7ca;
  --bg-metric: #f5f5f5;
  --border-light: #e5e5e5;
  --accent-color: #1a1a1a;
  --success-color: #4caf50;
  --warning-color: #ff9800;
  --error-color: #f44336;
}
/* 禁用暗色模式，使用浅色主题 */
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #f8fafc;
    --bg-card: #ffffff;
    --text-primary: #cdcf39;
    --text-secondary: #ffaa7f;
    --bg-metric: #f5f5f5;
    --border-light: #e5e5e5;
    --accent-color: #79e133;
    --success-color: #4caf50;
    --warning-color: #ff9800;
    --error-color: #f44336;
  }
}

.mobile-container {
  background: linear-gradient(180deg, #f8fafc 0%, #e8eeff 100%);
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
  background: linear-gradient(135deg, #e0e0e0 0%, #b0b0b0 50%, #d0d0d0 100%);
  opacity: 0.06;
  pointer-events: none;
  border-radius: 0 0 80rpx 80rpx;
}

.app-header {
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 80rpx 30rpx 24rpx;
  border-bottom: none;
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
  box-shadow: 0 4rpx 30rpx rgba(0,0,0,0.05);
}

.header-title {
  font-size: 44rpx;
  font-weight: 800;
  color: #118cde;
  letter-spacing: 2rpx;
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
  border-radius: 32rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.06);
}

.selector-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
}

.selector-title::before {
  content: '';
  width: 6rpx;
  height: 28rpx;
  background: linear-gradient(135deg, #B3E5FC 0%, #C8E6C9 100%);
  border-radius: 3rpx;
  margin-right: 12rpx;
}

.exercise-options {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}

.exercise-option {
  flex: 1;
  min-width: 150rpx;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8rpx;
  padding: 24rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  transition: all 0.3s ease;
  color: #333;
}

.exercise-option.active {
  background: linear-gradient(135deg, #B3E5FC 0%, #C8E6C9 100%);
  color: #388E3C;
  box-shadow: 0 4rpx 12rpx rgba(179,229,252,0.3);
  transform: translateY(-2rpx);
}

.exercise-icon {
  width: 48rpx;
  height: 48rpx;
  transition: transform 0.3s ease;
}

.exercise-option.active .exercise-icon {
  transform: scale(1.1);
}

.exercise-name {
  font-size: 24rpx;
  font-weight: 600;
}

.feature-card {
  background: var(--bg-card);
  border-radius: 32rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.06);
  border: 1rpx solid rgba(0,0,0,0.03);
  transition: all 0.3s ease;
}

.feature-card:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
}

.feature-icon {
  width: 96rpx;
  height: 96rpx;
  background: linear-gradient(135deg, rgba(102,126,234,0.15) 0%, rgba(118,75,162,0.15) 100%);
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.feature-card:active .feature-icon {
  transform: scale(1.05);
}

.feature-icon .icon {
  width: 60rpx;
  height: 60rpx;
}

.feature-info {
  flex: 1;
  margin-left: 24rpx;
}

.feature-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6rpx;
}

.feature-desc {
  font-size: 24rpx;
  color: var(--text-secondary);
  line-height: 1.4;
}

.result-card {
  background: var(--bg-card);
  border-radius: 32rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.06);
  animation: slideInUp 0.4s ease-out;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
}

.result-title::before {
  content: '';
  width: 6rpx;
  height: 28rpx;
  background: #1a1a1a;
  border-radius: 3rpx;
  margin-right: 12rpx;
}

.score-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx;
  border-radius: 32rpx;
  background: #f5f5f5;
  margin-bottom: 24rpx;
}

.score {
  font-size: 72rpx;
  font-weight: 900;
  color: #1a1a1a;
  line-height: 1;
  margin-bottom: 8rpx;
}

.score-label {
  font-size: 26rpx;
  color: var(--text-secondary);
  font-weight: 500;
}

.count-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 24rpx;
  padding: 20rpx;
  background: #f5f5f5;
  border-radius: 24rpx;
}

.count-num {
  font-size: 56rpx;
  font-weight: 800;
  color: #1a1a1a;
}

.count-label {
  font-size: 26rpx;
  color: var(--text-secondary);
}

.metrics-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.metric-item {
  background: linear-gradient(135deg, rgba(102,126,234,0.08) 0%, rgba(118,75,162,0.08) 100%);
  padding: 24rpx 20rpx;
  border-radius: 24rpx;
  text-align: center;
  transition: all 0.3s ease;
}

.metric-item:active {
  transform: scale(0.97);
}

.metric-label {
  font-size: 24rpx;
  color: var(--text-secondary);
  margin-bottom: 8rpx;
  display: block;
}

.metric-value {
  font-size: 36rpx;
  font-weight: 800;
  color: #1a1a1a;
  display: block;
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
  background: #f5f5f5;
  border-radius: 16rpx;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
}

.ai-icon {
  font-size: 40rpx;
}

.ai-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
}

.ai-badge {
  font-size: 20rpx;
  background: #1a1a1a;
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
  font-size: 30rpx;
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
  position: relative;
  width: 100%;
  height: 500rpx;
  background: #111;
  border-radius: 28rpx;
  overflow: hidden;
  position: relative;
}

.live-feedback-panel {
  position: absolute;
  bottom: 20rpx;
  left: 20rpx;
  right: 20rpx;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(12px);
  border-radius: 28rpx;
  padding: 20rpx 28rpx;
  z-index: 20;
  pointer-events: none;
  border-left: 8rpx solid #1a1a1a;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.3);
}

.live-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.live-row:last-child {
  margin-bottom: 0;
}

.live-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.live-value {
  font-size: 36rpx;
  font-weight: bold;
  color: #ffd966;
}

.live-feedback {
  font-size: 26rpx;
  color: #ffffff;
  flex: 1;
  text-align: right;
  font-weight: 500;
  word-break: break-word;
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
  margin-top: 20rpx;
}

.action-btn {
  flex: 1;
  padding: 28rpx 0;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
  transition: all 0.3s ease;
}
 
.action-btn.primary {
  background: linear-gradient(135deg, #B3E5FC 0%, #C8E6C9 100%);
  color: #388E3C;
  box-shadow: 0 4rpx 16rpx rgba(179,229,252,0.3);
}

.action-btn.primary:active {
  transform: translateY(2rpx);
  box-shadow: 0 2rpx 8rpx rgba(179,229,252,0.2);
}

.action-btn.secondary {
  background: linear-gradient(135deg, #E1F5FE 0%, #E8F5E9 100%);
  color: #388E3C;
  border: 1rpx solid #E1F5FE;
  box-shadow: 0 2rpx 8rpx rgba(225,245,254,0.15);
}

.action-btn.secondary:active {
  background: linear-gradient(135deg, #E1F5FE 0%, #E8F5E9 100%);
  transform: translateY(1rpx);
  box-shadow: 0 1rpx 4rpx rgba(225,245,254,0.15);
}

.control-btn {
  flex: 1;
  padding: 22rpx 16rpx;
  border-radius: 8rpx;
  background: #f5f5f5;
  border: none;
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
  transition: all 0.3s ease;
}

.control-btn:active {
  background: #e8e8e8;
  transform: translateY(2rpx);
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
  border: 4rpx solid #e0e0e0;
  border-top-color: #1a1a1a;
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

// AI聊天弹窗样式
.chat-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.chat-container {
  width: 100%;
  height: 80vh;
  background: #ffffff;
  border-radius: 40rpx 40rpx 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 30rpx;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  flex-shrink: 0;
}

.chat-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

.chat-actions {
  display: flex;
  gap: 20rpx;
  align-items: center;
}

.thread-selector {
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
  background: #f3f4f6;
  font-size: 24rpx;
  color: #4b5563;
  cursor: pointer;
}

.chat-close {
  font-size: 40rpx;
  color: #9ca3af;
  padding: 10rpx;
}

.thread-list-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1001;
}

.thread-list-container {
  width: 100%;
  height: 60vh;
  background: #ffffff;
  border-radius: 40rpx 40rpx 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.thread-list-header {
  padding: 30rpx;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  flex-shrink: 0;
}

.thread-list-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2937;
}

.thread-list-close {
  font-size: 40rpx;
  color: #9ca3af;
  padding: 10rpx;
}

.thread-list {
  flex: 1;
  overflow-y: auto;
  padding: 20rpx;
  background: #f9fafb;
}

.thread-item {
  padding: 24rpx;
  background: #ffffff;
  border-radius: 20rpx;
  margin-bottom: 16rpx;
  cursor: pointer;
  border: 2rpx solid #e0e0e0;
  transition: all 0.2s;
}

.thread-item:hover {
  border-color: #1a1a1a;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}

.thread-item.active {
  border-color: #1a1a1a;
  background: rgba(0, 0, 0, 0.05);
}

.thread-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8rpx;
}

.thread-time {
  font-size: 20rpx;
  color: #9ca3af;
}

.create-thread-btn {
  padding: 24rpx;
  background: #f3f4f6;
  border-radius: 20rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  cursor: pointer;
  transition: all 0.2s;
}

.create-thread-btn:hover {
  background: #e5e7eb;
}

.create-thread-icon {
  font-size: 32rpx;
  color: #1a1a1a;
}

.create-thread-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #4b5563;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20rpx;
  background: #f9fafb;
}

.message-item {
  margin-bottom: 20rpx;
  display: flex;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 20rpx 24rpx;
  border-radius: 24rpx;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1a1a1a;
  color: #ffffff;
  border-bottom-right-radius: 8rpx;
}

.ai-message .message-content {
  background: #ffffff;
  color: #1f2937;
  border-bottom-left-radius: 8rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.message-text {
  font-size: 28rpx;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 20rpx;
  opacity: 0.7;
  margin-top: 8rpx;
  display: block;
  text-align: right;
}

.typing-indicator {
  color: #9ca3af;
  font-style: italic;
}

.chat-input-area {
  padding: 20rpx;
  background: #ffffff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  height: 80rpx;
  background: #f3f4f6;
  border-radius: 8rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  color: #1f2937;
}

.send-btn {
  padding: 0 32rpx;
  height: 80rpx;
  background: #1a1a1a;
  color: #ffffff;
  border: none;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn[disabled] {
  opacity: 0.5;
}

/* 聊天输入区域 */
.chat-input-area {
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  display: flex;
  gap: 20rpx;
  align-items: flex-end;
  border-top: 2rpx solid var(--border-light);
}

.chat-input {
  flex: 1;
  background: var(--bg-metric);
  border-radius: 8rpx;
  padding: 20rpx 28rpx;
  font-size: 28rpx;
  max-height: 200rpx;
}

.send-btn {
  width: 80rpx;
  height: 80rpx;
  background: #1a1a1a;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32rpx;
  flex-shrink: 0;
}
</style>