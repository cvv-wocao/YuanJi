import { get, post, put, del } from '@/util/request.js';
export { getMyFollowStats } from '@/api/follow.js';

// 用户登录 - 使用 POST + URL 参数（适配后端 @PostMapping + @RequestParam）
export function login(number, password) {
  // 使用 POST 请求，参数放在 URL query string 中
  return post(`/api/user/login?number=${encodeURIComponent(number)}&password=${encodeURIComponent(password)}`, null, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
}

// 用户注册（JSON 格式，无头像）
export function register(data) {
  return post('/api/user/register', data);
}

// 用户注册（表单格式，支持头像上传）
export function registerWithAvatar(formData) {
  return post('/api/user/register', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// 获取用户信息
export function getUserInfo() {
  return get('/api/user/info');
}

// 更新用户信息（JSON格式）
export function updateUserInfo(data) {
  return put('/api/user/info', data);
}

// 更新用户信息（表单格式，支持头像上传）
export function updateUser(formData) {
  return put('/api/user/update', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// 获取用户列表
export function getUserList(params) {
  return get('/api/user/list', params);
}

// 根据ID获取用户信息
export function getUserById(id) {
  return get(`/api/user/${id}`);
}

// 删除用户
export function deleteUser(id) {
  return del(`/api/user/${id}`);
}
