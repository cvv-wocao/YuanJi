//const API_BASE_URL = 'http://localhost:5002/api';
const API_BASE_URL = 'http://localhost:5002/api';

const getToken = () => {
  return uni.getStorageSync('token') || '';
};

const fetchDevices = async () => {
  try {
    const token = getToken();
    const backendUrl = 'http://localhost:8080/api/devices';
    console.log('发送请求到:', backendUrl);
    console.log('Token:', token ? '已获取' : '为空');
    
    const response = await fetch(backendUrl, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    console.log('响应状态:', response.status);
    
    if (!response.ok) {
      const text = await response.text();
      console.error('请求失败，响应内容:', text);
      throw new Error(`HTTP错误: ${response.status}`);
    }
    
    const data = await response.json();
    console.log('获取到设备数据:', data);
    return data;
  } catch (error) {
    console.error('获取设备列表失败:', error);
    throw error;
  }
};

const showHabitAnalysis = async () => {
  try {
    console.log('\n=== AI运动顾问请求 ===');
    
    const token = getToken();
    console.log('当前token:', token ? '已获取 (' + token.length + '字符)' : '空');
    
    if (!token) {
      console.error('错误：token为空，请先登录');
      return { success: false, msg: '请先登录' };
    }
    
    const requestUrl = `${API_BASE_URL}/analysis/habit`;
    console.log('请求URL:', requestUrl);
    
    const response = await fetch(requestUrl, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('响应状态:', response.status);
    console.log('响应状态文本:', response.statusText);
    
    const data = await response.json();
    console.log('响应数据:', data);
    
    return data;
  } catch (err) {
    console.error('习惯分析请求失败:', err);
    console.error('错误详情:', err.message);
    return { success: false, msg: '网络错误，请检查后端服务' };
  }
};

const loadHistoryRecords = async () => {
  try {
    const token = getToken();
    const res = await fetch(`${API_BASE_URL}/records`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    const data = await res.json();
    return data;
  } catch (e) {
    console.error('加载历史记录失败:', e);
    throw e;
  }
};

const showReportDetail = async (record, getExerciseName) => {
  try {
    const token = getToken();
    const res = await fetch(`${API_BASE_URL}/records/${record.id}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    const data = await res.json();
    if (data.success) {
      return data.report;
    }
    return null;
  } catch (e) {
    console.error('加载报告详情失败:', e);
    return null;
  }
};

const uploadVideoFile = async (path, selectedExercise) => {
  const token = getToken();
  
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/analyze/video`,
      filePath: path,
      name: 'video',
      timeout: 240000,
      formData: {
        'exercise_type': selectedExercise
      },
      header: {
        'Authorization': `Bearer ${token}`
      },
      success: (uploadRes) => {
        try {
          const data = JSON.parse(uploadRes.data);
          console.log('后端返回数据：', data);
          if (data.success) {
            resolve({
              score: data.score || 0,
              count: data.count || 0,
              feedback: data.feedback || [],
              kneeAngle: data.kneeAngle ?? 0,
              hipAngle: data.hipAngle ?? 0,
              trunkAngle: data.trunkAngle ?? 0,
              angle: data.angle ?? 0,
              ai_analysis: data.ai_analysis || '',
              analysis_time: data.analysis_time || ''
            });
          } else {
            console.log('后端返回失败：', data.feedback);
            if (data.detected_exercise && data.detected_exercise !== data.selected_exercise) {
              uni.showModal({
                title: '运动类型不匹配',
                content: `检测到该视频是${data.detected_name}动作，与您当前选择的${data.selected_name}模块不匹配，请切换到${data.detected_name}模块进行分析。`,
                confirmText: '确定',
                showCancel: false
              });
            }
            reject(new Error(data.feedback?.[0] || '分析失败'));
          }
        } catch (e) {
          console.error('解析返回数据失败：', e);
          reject(e);
        }
      },
      fail: (err) => {
        console.error('上传失败：', err);
        reject(err);
      }
    });
  });
};

const uploadVideoForAnalysis = async (blob, selectedExercise) => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 120000);
  try {
    const form = new FormData();
    form.append('video', blob, 'video.webm');
    form.append('exercise_type', selectedExercise);
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
      return {
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
    } else {
      throw new Error(data.msg || '分析失败');
    }
  } catch (e) {
    clearTimeout(timeoutId);
    if (e.name === 'AbortError') {
      throw new Error('分析超时，请稍后重试或缩短视频时长');
    } else {
      console.error('上传分析失败:', e);
      throw new Error('服务未启动或网络错误');
    }
  }
};

export {
  API_BASE_URL,
  getToken,
  fetchDevices,
  showHabitAnalysis,
  loadHistoryRecords,
  showReportDetail,
  uploadVideoFile,
  uploadVideoForAnalysis
};
