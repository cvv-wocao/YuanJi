<template>
  <div class="bigdata-screen">
    <div class="screen-header">
      <h2>数据大屏</h2>
      <div class="header-actions">
        <button class="btn-refresh" @click="refreshFrame">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path d="M17.65 6.35A7.958 7.958 0 0 0 12 4a8 8 0 0 0-8 8 8 8 0 0 0 8 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18a6 6 0 0 1-6-6 6 6 0 0 1 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35Z" fill="currentColor"/>
          </svg>
          刷新
        </button>
        <button class="btn-fullscreen" @click="toggleFullscreen">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path d="M7 14H5v5h5v-2H7v-3Zm-2-4h2V7h3V5H5v5Zm12 7h-3v2h5v-5h-2v3ZM14 5v2h3v3h2V5h-5Z" fill="currentColor"/>
          </svg>
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </button>
      </div>
    </div>
    <div class="screen-container" :class="{ fullscreen: isFullscreen }">
      <iframe
        ref="screenFrame"
        :src="screenUrl"
        frameborder="0"
        allowfullscreen
        @load="onFrameLoad"
      ></iframe>
      <div v-if="loading" class="loading-overlay">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';

export default {
  name: 'BigDataScreen',
  setup() {
    const screenUrl = ref('http://localhost:8080/dashboard/bigscreen');
    const screenFrame = ref(null);
    const loading = ref(true);
    const isFullscreen = ref(false);

    const refreshFrame = () => {
      loading.value = true;
      if (screenFrame.value) {
        screenFrame.value.src = screenUrl.value + '?t=' + Date.now();
      }
    };

    const onFrameLoad = () => {
      loading.value = false;
    };

    const toggleFullscreen = () => {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen();
        isFullscreen.value = true;
      } else {
        document.exitFullscreen();
        isFullscreen.value = false;
      }
    };

    const handleEsc = () => {
      if (!document.fullscreenElement) {
        isFullscreen.value = false;
      }
    };

    onMounted(() => {
      document.addEventListener('fullscreenchange', handleEsc);
    });

    onUnmounted(() => {
      document.removeEventListener('fullscreenchange', handleEsc);
    });

    return {
      screenUrl,
      screenFrame,
      loading,
      isFullscreen,
      refreshFrame,
      onFrameLoad,
      toggleFullscreen
    };
  }
};
</script>

<style scoped>
.bigdata-screen {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  background: #f5f7fa;
  padding: 20px;
}

.screen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.screen-header h2 {
  font-size: 20px;
  color: #1a1a2e;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-refresh,
.btn-fullscreen {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-refresh {
  background: #f0f4f8;
  color: #5a6785;
}

.btn-refresh:hover {
  background: #e8ecf2;
  color: #1a1a2e;
}

.btn-fullscreen {
  background: linear-gradient(135deg, var(--sports-teal) 0%, #40b0a5 100%);
  color: white;
}

.btn-fullscreen:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.screen-container {
  flex: 1;
  position: relative;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.screen-container.fullscreen {
  border-radius: 0;
}

.screen-container iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f0f4f8;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-overlay p {
  margin-top: 12px;
  color: #5a6785;
  font-size: 14px;
}
</style>