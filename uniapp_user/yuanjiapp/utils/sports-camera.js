const toggleCamera = async (isCameraOn, showCanvas, stream, videoElement, canvasElement) => {
  try {
    console.log('尝试打开/关闭摄像头...');
    if (isCameraOn.value) {
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
      showCanvas.value = false;
      console.log('摄像头已关闭');
      return { success: true, isCameraOn: false };
    } else {
      isCameraOn.value = true;
      showCanvas.value = true;
      
      await new Promise(resolve => setTimeout(resolve, 300));
      
      const canvasEl = document.querySelector('.canvas-overlay');
      if (canvasEl) {
        canvasElement.value = canvasEl;
        console.log('Canvas 元素已获取:', canvasEl);
      } else {
        console.error('Canvas 元素未找到');
      }
      
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        console.error('浏览器不支持摄像头访问');
        throw new Error('浏览器不支持摄像头访问');
      }
      
      console.log('请求摄像头权限...');
      const mediaStream = await navigator.mediaDevices.getUserMedia({ video: true });
      console.log('获取摄像头流成功');
      
      try {
        const cameraContainer = document.querySelector('.camera-preview');
        if (!cameraContainer) {
          console.error('摄像头容器未找到');
          throw new Error('摄像头容器未找到');
        }
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
        videoEl.srcObject = mediaStream;
        cameraContainer.appendChild(videoEl);
        videoElement.value = videoEl;
        
        videoEl.onloadedmetadata = async () => {
          console.log('视频元数据已加载');
          if (canvasElement.value && typeof canvasElement.value.getContext === 'function') {
            const canvas = canvasElement.value;
            canvas.width = videoEl.videoWidth || 640;
            canvas.height = videoEl.videoHeight || 480;
            console.log('Canvas 尺寸已设置:', canvas.width, 'x', canvas.height);
          }
        };
        
        stream.value = mediaStream;
        console.log('摄像头已成功打开');
        return { success: true, isCameraOn: true, stream: mediaStream };
      } catch (error) {
        console.error('动态创建视频元素失败:', error);
        throw error;
      }
    }
  } catch (e) {
    console.error('摄像头访问错误:', e);
    isCameraOn.value = false;
    showCanvas.value = false;
    return { success: false, error: e.message };
  }
};

const startRecording = async (stream, recorder, recordedChunks, showCountdown, countdown, showCanvas) => {
  if (!stream.value) return { success: false, error: '请先开摄像头' };
  
  showCountdown.value = true;
  countdown.value = 3;
  showCanvas.value = true;
  
  const options = {
    mimeType: 'video/webm;codecs=vp9',
    videoBitsPerSecond: 1000000
  };
  
  try {
    recorder.value = new MediaRecorder(stream.value, options);
  } catch (e) {
    console.warn('使用默认编码:', e);
    recorder.value = new MediaRecorder(stream.value);
  }
  
  recordedChunks.value = [];
  
  recorder.value.ondataavailable = e => {
    if (e.data.size > 0) {
      recordedChunks.value.push(e.data);
      console.log('获取数据块:', e.data.size, 'bytes');
    }
  };
  
  recorder.value.onerror = (e) => {
    console.error('录制错误:', e);
    isRecording.value = false;
  };
  
  recorder.value.start(1000);
  return { success: true };
};

const stopRecording = (recorder) => {
  if (recorder.value?.state === 'recording') {
    recorder.value.stop();
    return { success: true };
  }
  return { success: false, error: '未在录制' };
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

const analyzeFrame = async (videoElement, canvasElement, showCanvas, selectedExercise, analysisResult, uploadedVideoPath, API_BASE_URL) => {
  try {
    if (!videoElement.value) {
      const videoEl = document.querySelector('#videoElement');
      if (videoEl) {
        videoElement.value = videoEl;
      } else {
        return;
      }
    }
    
    showCanvas.value = true;
    
    await new Promise(resolve => setTimeout(resolve, 50));
    
    let canvasEl = canvasElement.value;
    if (!canvasEl) {
      canvasEl = document.querySelector('canvas.canvas-overlay') || document.querySelector('canvas');
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
          const token = uni.getStorageSync('token') || '';
          const r = await fetch(`${API_BASE_URL}/analyze/frame`, { 
            method: 'POST', 
            body: f,
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
          const j = await r.json();
          if (j.success) {
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
    console.error('分析帧时发生错误:', error);
  }
};

export {
  toggleCamera,
  startRecording,
  stopRecording,
  drawSkeleton,
  drawDebug,
  analyzeFrame
};
