import { get, post, put, del } from '@/utils/request.js';

// 用户登录 - 使用 POST + URL 参数（适配后端 @PostMapping + @RequestParam）
export function login(number, password) {
  // 使用 POST 请求，参数放在 URL query string 中
  return post(`/user/login?number=${encodeURIComponent(number)}&password=${encodeURIComponent(password)}`);
}

// 用户注册（JSON 格式，无头像）
export function register(data) {
  return post('/user/register', data);
}

// 用户注册（表单格式，支持头像上传）
export function registerWithAvatar(formData) {
  return post('/user/register', formData, {
    header: { 'Content-Type': 'multipart/form-data' }
  });
}

// 获取用户信息
export function getUserInfo() {
  return get('/user/info');
}

// 更新用户信息（JSON格式）
export function updateUserInfo(data) {
  return put('/user/info', data);
}

// 更新用户信息（表单格式，支持头像上传）
export function updateUser(formData) {
  return put('/user/update', formData, {
    header: { 'Content-Type': 'multipart/form-data' }
  });
}

// 更新用户信息（支持头像上传，使用uni.uploadFile）
export function updateUserWithAvatar(data, avatarPath) {
  const formData = {
    name: data.name,
    number: data.number,
    email: data.email,
    age: data.age,
    sex: data.sex,
    password: data.password
  };
  
  // 移除空值
  Object.keys(formData).forEach(key => {
    if (formData[key] === undefined || formData[key] === null || formData[key] === '') {
      delete formData[key];
    }
  });
  
  if (avatarPath) {
    // 使用uni.uploadFile上传文件
    return upload('/user/update', avatarPath, formData, 'avatar');
  } else {
    // 没有头像，使用普通请求
    return put('/user/update', formData, {
      header: { 'Content-Type': 'multipart/form-data' }
    });
  }
}

// 获取用户列表
export function getUserList(params) {
  return get('/user/list', params);
}

// 根据ID获取用户信息
export function getUserById(id) {
  return get(`/user/${id}`);
}

// 删除用户
export function deleteUser(id) {
  return del(`/user/${id}`);
}
