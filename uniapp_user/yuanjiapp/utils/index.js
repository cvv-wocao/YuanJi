// 工具函数集合

/**
 * 格式化时间
 * @param {string|number|Date} timeStr - 时间字符串、时间戳或Date对象
 * @returns {string} 格式化后的时间
 */
export function formatTime(timeStr) {
  if (!timeStr) return '';
  
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  if (days < 30) return `${Math.floor(days / 7)}周前`;
  
  // 超过30天显示具体日期
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
}

/**
 * 选择图片
 * @param {Object} options - 配置选项
 * @returns {Promise} 返回选择的图片路径数组
 */
export function chooseImage(options = {}) {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count: options.count || 9,
      sizeType: options.sizeType || ['compressed'],
      sourceType: options.sourceType || ['album', 'camera'],
      success: (res) => {
        resolve(res.tempFilePaths);
      },
      fail: (err) => {
        console.error('选择图片失败:', err);
        reject(err);
      }
    });
  });
}

/**
 * 预览图片
 * @param {Array} urls - 图片URL数组
 * @param {number} current - 当前显示图片的索引
 */
export function previewImage(urls = [], current = 0) {
  if (!urls || urls.length === 0) return;
  
  uni.previewImage({
    urls,
    current,
    success: () => {},
    fail: (err) => {
      console.error('预览图片失败:', err);
    }
  });
}

/**
 * 上传文件
 * @param {string} url - 上传地址
 * @param {string} filePath - 文件路径
 * @param {string} name - 文件对应的key
 * @param {Object} formData - 额外的表单数据
 * @returns {Promise}
 */
export function uploadFile(url, filePath, name = 'file', formData = {}) {
  return new Promise((resolve, reject) => {
    const uploadTask = uni.uploadFile({
      url,
      filePath,
      name,
      formData,
      success: (res) => {
        try {
          const data = JSON.parse(res.data);
          resolve(data);
        } catch (error) {
          reject(error);
        }
      },
      fail: (err) => {
        console.error('上传失败:', err);
        reject(err);
      }
    });
    
    // 返回uploadTask，可以用于监听上传进度
    return uploadTask;
  });
}

/**
 * 显示加载中
 * @param {string} title - 标题
 * @param {boolean} mask - 是否显示遮罩
 */
export function showLoading(title = '加载中...', mask = true) {
  uni.showLoading({
    title,
    mask
  });
}

/**
 * 隐藏加载中
 */
export function hideLoading() {
  uni.hideLoading();
}

/**
 * 显示提示
 * @param {string} title - 标题
 * @param {string} icon - 图标类型
 * @param {number} duration - 持续时间
 */
export function showToast(title, icon = 'none', duration = 2000) {
  uni.showToast({
    title,
    icon,
    duration
  });
}

/**
 * 显示模态对话框
 * @param {Object} options - 配置选项
 * @returns {Promise}
 */
export function showModal(options = {}) {
  return new Promise((resolve) => {
    uni.showModal({
      title: options.title || '提示',
      content: options.content || '',
      showCancel: options.showCancel !== false,
      cancelText: options.cancelText || '取消',
      confirmText: options.confirmText || '确定',
      success: (res) => {
        resolve(res.confirm);
      }
    });
  });
}

/**
 * 防抖函数
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 延迟时间（毫秒）
 * @returns {Function}
 */
export function debounce(func, wait = 300) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => {
      func.apply(this, args);
    }, wait);
  };
}

/**
 * 节流函数
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 延迟时间（毫秒）
 * @returns {Function}
 */
export function throttle(func, wait = 300) {
  let timeout;
  return function(...args) {
    if (!timeout) {
      timeout = setTimeout(() => {
        timeout = null;
        func.apply(this, args);
      }, wait);
    }
  };
}

/**
 * 获取用户ID
 * @returns {number|null}
 */
export function getCurrentUserId() {
  const userId = uni.getStorageSync('userId');
  return userId ? parseInt(userId) : null;
}

/**
 * 检查是否为自己的帖子
 * @param {number} postUserId - 帖子的用户ID
 * @returns {boolean}
 */
export function isMyPost(postUserId) {
  const currentUserId = getCurrentUserId();
  return currentUserId && currentUserId === postUserId;
}

/**
 * 复制文本到剪贴板
 * @param {string} text - 要复制的文本
 * @returns {Promise}
 */
export function copyToClipboard(text) {
  return new Promise((resolve, reject) => {
    uni.setClipboardData({
      data: text,
      success: () => {
        showToast('已复制到剪贴板');
        resolve();
      },
      fail: (err) => {
        console.error('复制失败:', err);
        showToast('复制失败');
        reject(err);
      }
    });
  });
}
