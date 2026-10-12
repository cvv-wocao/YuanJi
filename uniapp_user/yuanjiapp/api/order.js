
// 订单相关API
import { get, post, postForm } from '@/utils/request.js';

// 创建订单
export const createOrder = (data) => {
  return post('/order/create', data);
};

// 获取订单列表
export const getOrderList = (params) => {
  return get('/order/list', params);
};

// 获取订单详情
export const getOrderDetail = (orderNo) => {
  return get(`/order/detail/${orderNo}`);
};

// 取消订单
export const cancelOrder = (orderNo, reason) => {
  return postForm('/order/cancel', { orderNo, reason });
};

// 确认收货
export const confirmOrder = (orderNo) => {
  return postForm('/order/confirm', { orderNo });
};

// 申请退款
export const requestRefund = (orderNo, reason) => {
  return postForm('/order/refund', { orderNo, reason });
};

// 查询退款状态
export const queryRefundStatus = (orderNo) => {
  return get(`/order/refund/query/${orderNo}`);
};

// 获取订单商品
export const getOrderItems = (orderId) => {
  return get(`/order/items/${orderId}`);
};

// 支付宝沙箱支付相关接口
// 创建支付宝支付订单
export const createAlipayOrder = (data) => {
  return post('/api/alipay/create', data);
};

// 查询支付宝支付状态
export const queryAlipayStatus = (orderNo) => {
  return get(`/api/alipay/query/${orderNo}`);
};

export default {
  createOrder,
  getOrderList,
  getOrderDetail,
  cancelOrder,
  confirmOrder,
  requestRefund,
  queryRefundStatus,
  getOrderItems,
  createAlipayOrder,
  queryAlipayStatus
};
