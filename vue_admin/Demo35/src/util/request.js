import axios from 'axios';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

request.interceptors.request.use(
  (config) => {
    const url = config.url || '';
    let token;

    if (
      url.startsWith('/admin/') ||
      url.startsWith('/api/admin/') ||
      url.startsWith('/product/') ||
      url.startsWith('/api/product/')
    ) {
      token = localStorage.getItem('adminToken');
    } else {
      token = localStorage.getItem('token') || localStorage.getItem('adminToken');
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    const { data } = response;

    if (data && typeof data === 'object' && 'success' in data) {
      if (data.success) {
        return data;
      }
      return Promise.reject(new Error(data.message || '操作失败'));
    }

    if (data.code !== 200 && data.code !== 0 && data.code !== undefined) {
      return Promise.reject(new Error(data.message || '请求失败'));
    }

    return data;
  },
  (error) => {
    const { response } = error;

    if (response) {
      switch (response.status) {
        case 401: {
          const isAdminPath = window.location.pathname.startsWith('/admin');
          if (isAdminPath) {
            localStorage.removeItem('adminToken');
            localStorage.removeItem('adminLoggedIn');
            localStorage.removeItem('adminUsername');
            window.location.href = '/admin/login';
          } else {
            localStorage.removeItem('token');
            localStorage.removeItem('isLoggedIn');
            localStorage.removeItem('userNumber');
            window.location.href = '/login';
          }
          break;
        }
        case 403:
          console.error('拒绝访问');
          break;
        case 404:
          console.error('请求地址不存在');
          break;
        case 500:
          console.error('服务器内部错误');
          console.error('错误详情:', response.data);
          console.error('请求URL:', error.config?.url);
          console.error('请求方法:', error.config?.method);
          console.error('请求参数:', error.config?.params || error.config?.data);
          break;
        default:
          console.error(`请求错误: ${response.status}`);
          console.error('错误详情:', response.data);
      }
    } else {
      console.error('网络异常，请检查网络连接');
      console.error('错误详情:', error.message);
    }

    return Promise.reject(error);
  }
);

export function get(url, params = {}, config = {}) {
  return request.get(url, { params, ...config });
}

export function post(url, data = {}, config = {}) {
  return request.post(url, data, config);
}

export function put(url, data = {}, config = {}) {
  return request.put(url, data, config);
}

export function del(url, config = {}) {
  return request.delete(url, config);
}

export function upload(url, file, config = {}) {
  const formData = new FormData();
  formData.append('file', file);

  return request.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    ...config
  });
}

export { request };
export default request;
