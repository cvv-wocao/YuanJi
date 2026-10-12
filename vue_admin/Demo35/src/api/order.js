import { get, post } from '@/util/request.js';

export function applyOrderRefund(orderNo, reason) {
  const formData = new URLSearchParams();
  formData.append('orderNo', orderNo);
  formData.append('reason', reason);

  return post('/api/order/refund', formData, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
}

export function queryOrderRefundStatus(orderNo) {
  return get(`/api/order/refund/query/${encodeURIComponent(orderNo)}`);
}
