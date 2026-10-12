// 存储工具类，适配uni-app和H5

// 设置存储
export function setStorage(key, value) {
  try {
    const data = typeof value === 'object' ? JSON.stringify(value) : value;
    uni.setStorageSync(key, data);
    return true;
  } catch (error) {
    console.error('存储数据失败:', error);
    return false;
  }
}

// 获取存储
export function getStorage(key, parseJson = true) {
  try {
    const value = uni.getStorageSync(key);
    if (!value) return null;
    
    if (parseJson) {
      try {
        return JSON.parse(value);
      } catch {
        return value;
      }
    }
    return value;
  } catch (error) {
    console.error('获取存储数据失败:', error);
    return null;
  }
}

// 删除存储
export function removeStorage(key) {
  try {
    uni.removeStorageSync(key);
    return true;
  } catch (error) {
    console.error('删除存储数据失败:', error);
    return false;
  }
}

// 清空所有存储
export function clearStorage() {
  try {
    uni.clearStorageSync();
    return true;
  } catch (error) {
    console.error('清空存储失败:', error);
    return false;
  }
}

// 获取用户信息
export function getUserInfo() {
  const userInfo = getStorage('userInfo');
  if (!userInfo) {
    return {
      id: getStorage('userId'),
      name: getStorage('userName') || getStorage('userNumber') || '用户',
      number: getStorage('userNumber'),
      avatar: getStorage('userAvatar')
    };
  }
  return userInfo;
}

// 设置用户信息
export function setUserInfo(userInfo) {
  setStorage('userInfo', userInfo);
  if (userInfo.id) setStorage('userId', userInfo.id);
  if (userInfo.name) setStorage('userName', userInfo.name);
  if (userInfo.number) setStorage('userNumber', userInfo.number);
  if (userInfo.avatar) setStorage('userAvatar', userInfo.avatar);
  if (userInfo.token) setStorage('token', userInfo.token);
}

// 清除用户信息
export function clearUserInfo() {
  removeStorage('userInfo');
  removeStorage('userId');
  removeStorage('userName');
  removeStorage('userNumber');
  removeStorage('userAvatar');
  removeStorage('token');
  removeStorage('isLoggedIn');
}

// 检查是否登录
export function isLoggedIn() {
  return !!getStorage('isLoggedIn');
}
