
// 地址相关API
import { get, post, put, del } from '@/utils/request.js';

// 获取地址列表
export const getAddressList = () => {
  return get('/address/list');
};

// 获取默认地址
export const getDefaultAddress = () => {
  return get('/address/default');
};

// 获取地址详情
export const getAddressDetail = (id) => {
  return get(`/address/detail/${id}`);
};

// 添加地址
export const addAddress = (data) => {
  return post('/address/add', data);
};

// 更新地址
export const updateAddress = (id, data) => {
  return put(`/address/update/${id}`, data);
};

// 删除地址
export const deleteAddress = (id) => {
  return del(`/address/delete/${id}`);
};

// 设置默认地址
export const setDefaultAddress = (id) => {
  return post(`/address/setDefault/${id}`);
};

export default {
  getAddressList,
  getDefaultAddress,
  getAddressDetail,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
};
