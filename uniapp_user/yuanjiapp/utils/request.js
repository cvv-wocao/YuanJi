const BASE_URL = 'http://localhost:8080'; // 后端API地址

// 请求拦截器
const requestInterceptor = (options) => {
  // 获取token
  const token = uni.getStorageSync('token');

  
  // 默认header
  options.header = {
    'Content-Type': 'application/json',
    ...options.header
  };
  
  // 添加Authorization header
  if (token) {
    options.header.Authorization = `Bearer ${token}`;
  }
  
  return options;
};

// 响应拦截器
const responseInterceptor = (response) => {
  const { statusCode, data } = response;
  
  // HTTP状态码检查
  if (statusCode >= 200 && statusCode < 300) {
    // 业务逻辑检查
    if (data && data.success === false) {
      // 业务错误
      if (data.message) {
        uni.showToast({
          title: data.message,
          icon: 'none',
          duration: 2000
        });
      }
      return Promise.reject(data);
    }
    return data;
  } else {
    // HTTP错误
    let errorMsg = '请求失败';
    
    switch (statusCode) {
      case 400:
        errorMsg = '请求参数错误';
        break;
      case 401:
        errorMsg = '未授权，请重新登录';
        // 清除登录状态并跳转到登录页
        uni.removeStorageSync('isLoggedIn');
        uni.removeStorageSync('token');
        setTimeout(() => {
          uni.reLaunch({
            url: '/pages/login/login'
          });
        }, 1500);
        break;
      case 403:
        errorMsg = '无权访问';
        break;
      case 404:
        errorMsg = '请求资源不存在';
        break;
      case 500:
        errorMsg = '服务器内部错误';
        break;
      case 503:
        errorMsg = '服务暂时不可用';
        break;
      default:
        errorMsg = `请求失败 (${statusCode})`;
    }
    
    uni.showToast({
      title: errorMsg,
      icon: 'none',
      duration: 2000
    });
    
    return Promise.reject({ success: false, message: errorMsg });
  }
};

// 统一请求方法
const request = (options) => {
  options = requestInterceptor(options);
  
  return new Promise((resolve, reject) => {
    uni.request({
      url: options.url.startsWith('http') ? options.url : `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: options.header,
      timeout: options.timeout || 30000,
      success: (res) => {
        try {
          const result = responseInterceptor(res);
          resolve(result);
        } catch (error) {
          reject(error);
        }
      },
      fail: (err) => {
        console.error('请求失败:', err);
        uni.showToast({
          title: '网络请求失败，请检查网络连接',
          icon: 'none',
          duration: 2000
        });
        reject({ success: false, message: '网络请求失败' });
      }
    });
  });
};

// GET请求
export const get = (url, params = {}) => {
  // 构建查询字符串
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');
  
  const fullUrl = queryString ? `${url}?${queryString}` : url;
  
  return request({
    url: fullUrl,
    method: 'GET'
  });
};

// POST请求
export const post = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    header: options.header
  });
};

// 使用form-urlencoded的POST请求
export const postForm = (url, data = {}) => {
  // 构建form-urlencoded字符串
  const formData = Object.keys(data)
    .filter(key => data[key] !== undefined && data[key] !== null)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
    .join('&');

  return request({
    url,
    method: 'POST',
    data: formData,
    header: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
};

// PUT请求
export const put = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    header: options.header
  });
};

// DELETE请求
export const del = (url, data = {}) => {
  return request({
    url,
    method: 'DELETE',
    data
  });
};

// 上传文件
export const upload = (url, filePath, formData = {}, name = 'file') => {
  const token = uni.getStorageSync('token');
  const header = {}
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }
  
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}${url}`,
      filePath,
      name,
      formData,
      header: header,
      success: (res) => {
        try {
          const data = JSON.parse(res.data);
          const result = responseInterceptor({ ...res, data });
          resolve(result);
        } catch (error) {
          reject(error);
        }
      },
      fail: (err) => {
        console.error('上传失败:', err);
        uni.showToast({
          title: '文件上传失败',
          icon: 'none',
          duration: 2000
        });
        reject({ success: false, message: '文件上传失败' });
      }
    });
  });
};

export default {
  get,
  post,
  put,
  del,
  upload
};
