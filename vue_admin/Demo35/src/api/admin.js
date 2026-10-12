import { get, post, put, del, upload, request } from '@/util/request.js';

// 管理员登录
export function adminLogin(username, password) {
  return post(`/api/admin/login?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`);
}

// 获取当前管理员信息
export function getAdminInfo() {
  return get('/api/admin/info');
}

// 获取所有用户列表
export function getAllUsers() {
  return get('/api/admin/users');
}

// 根据ID获取用户
export function getUserById(id) {
  return get(`/api/admin/user/${id}`);
}

// 更新用户信息
export function updateUser(id, data) {
  return put(`/api/admin/user/${id}`, data);
}

// 删除用户
export function deleteUser(id) {
  return del(`/api/admin/user/${id}`);
}

// 批量删除用户
export function batchDeleteUsers(ids) {
  return del('/api/admin/users/batch', { 
    data: ids,
    headers: { 'Content-Type': 'application/json' }
  });
}

// 获取所有管理员列表
export function getAllAdmins() {
  return get('/api/admin/list');
}

// 创建管理员
export function createAdmin(formData) {
  return post('/api/admin/create', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// 更新管理员
export function updateAdmin(id, data) {
  return put(`/api/admin/${id}`, data);
}

// 删除管理员
export function deleteAdmin(id) {
  return del(`/api/admin/${id}`);
}

// ==================== 帖子管理 ====================

// 获取所有帖子列表（管理员）
export function getAllPosts(page = 1, size = 10) {
  return get('/api/admin/posts', { page, size });
}

// 删除帖子（管理员）
export function deletePost(postId) {
  return del(`/api/admin/post/${postId}`);
}

// 批量删除帖子
export function batchDeletePosts(postIds) {
  return del('/api/admin/posts/batch', {
    data: postIds,
    headers: { 'Content-Type': 'application/json' }
  });
}

// ==================== 评论管理 ====================

// 获取所有评论列表（管理员）
export function getAllComments(page = 1, size = 10) {
  return get('/api/admin/comments', { page, size });
}

// 删除评论（管理员）
export function deleteComment(commentId) {
  return del(`/api/admin/comment/${commentId}`);
}

// 批量删除评论
export function batchDeleteComments(commentIds) {
  return del('/api/admin/comments/batch', {
    data: commentIds,
    headers: { 'Content-Type': 'application/json' }
  });
}

// ==================== 商品管理 ====================

// 设置商品热销状态
export function setProductHot(id, isHot) {
  return post(`/api/product/setHot/${id}?isHot=${isHot}`);
}

// 设置商品新品状态
export function setProductNew(id, isNew) {
  return post(`/api/product/setNew/${id}?isNew=${isNew}`);
}

// 设置商品推荐状态
export function setProductRecommend(id, isRecommend) {
  return post(`/api/product/setRecommend/${id}?isRecommend=${isRecommend}`);
}

// ==================== 商品分类管理 ====================

// 添加商品分类
export function addCategory(name) {
  return post(`/api/product/category/add?name=${encodeURIComponent(name)}`);
}

// 删除商品分类
export function deleteCategory(id) {
  return del(`/api/product/category/delete/${id}`);
}

// 获取商品分类列表
export function getProductCategories() {
  return get('/api/product/categories');
}

// 获取商品列表（管理员）
export function getAllProducts(params = {}) {
  // 构建查询参数字符串
  let queryString = '';
  if (params) {
    const queryParts = [];
    if (params.page !== undefined) {
      queryParts.push(`page=${String(params.page)}`);
    }
    if (params.pageSize !== undefined) {
      queryParts.push(`pageSize=${String(params.pageSize)}`);
    }
    if (params.keyword !== undefined) {
      queryParts.push(`keyword=${encodeURIComponent(String(params.keyword))}`);
    }
    if (params.categoryId !== undefined) {
      queryParts.push(`categoryId=${String(params.categoryId)}`);
    }
    if (queryParts.length > 0) {
      queryString = `?${queryParts.join('&')}`;
    }
  }
  return get(`/api/product/list${queryString}`);
}

// 根据ID获取商品详情
export function getProductById(id) {
  return get(`/api/product/detail/${id}`);
}

// 创建商品
export function createProduct(formData) {
  return post('/api/product/create', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// 上传商品图片
export function uploadProductImage(file) {
  return upload('/api/product/upload/image', file);
}


// 更新商品
export function updateProduct(id, formData) {
  return put(`/api/product/update/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

// 删除商品
export function deleteProduct(id) {
  return del(`/api/product/delete/${id}`);
}

// 批量删除商品
export function batchDeleteProducts(ids) {
  return del('/api/product/batch/delete', {
    data: ids,
    headers: { 'Content-Type': 'application/json' }
  });
}

// 获取热门商品
export function getHotProducts(limit = 10) {
  return get('/api/product/hot', { limit });
}

// 获取新品推荐
export function getNewProducts(limit = 10) {
  return get('/api/product/new', { limit });
}

// 获取推荐商品
export function getRecommendProducts(limit = 10) {
  return get('/api/product/recommend', { limit });
}

// ==================== 订单管理 ====================

// 获取所有订单
export function getAllOrders(params) {
  const queryParams = new URLSearchParams();
  queryParams.append('page', params.page || 1);
  queryParams.append('pageSize', params.pageSize || params.size || 10);
  if (params.status !== undefined) queryParams.append('status', params.status);
  if (params.orderNo) queryParams.append('orderNo', params.orderNo);
  if (params.receiverName) queryParams.append('receiverName', params.receiverName);
  if (params.receiverPhone) queryParams.append('receiverPhone', params.receiverPhone);
  if (params.startDate) queryParams.append('startDate', params.startDate);
  if (params.endDate) queryParams.append('endDate', params.endDate);
  
  return get(`/api/admin/orders?${queryParams}`);
}

// 根据用户ID获取订单
export function getOrdersByUserId(userId, params) {
  const queryParams = new URLSearchParams();
  queryParams.append('page', params.page || 1);
  queryParams.append('pageSize', params.pageSize || 10);
  if (params.status !== undefined) queryParams.append('status', params.status);
  
  return get(`/api/admin/orders/user/${userId}?${queryParams}`);
}

// 更新订单状态
export function updateOrderStatus(orderNo, status, reason = '') {
  return put(`/api/admin/order/status/${orderNo}?status=${status}&reason=${encodeURIComponent(reason)}`);
}

// 批量发货
export function batchShipOrders(orderNos, reason = '') {
  return put(`/api/admin/orders/batchShip?reason=${encodeURIComponent(reason)}`, orderNos);
}

// 获取订单统计
export function getOrderStatistics(startDate, endDate) {
  let url = '/api/admin/orders/statistics';
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  if (params.toString()) url += `?${params}`;
  
  return get(url);
}

// 兼容旧页面中的导出命名
export function getOrders(params = {}) {
  return getAllOrders(params);
}

export function shipOrder(orderNo, reason = '') {
  return updateOrderStatus(orderNo, 2, reason);
}

export function cancelOrder(orderNo, reason = '') {
  return updateOrderStatus(orderNo, 4, reason);
}

export function approveRefund(orderNo, reason = '') {
  return request({
    url: `/api/admin/order/refund/approve/${encodeURIComponent(orderNo)}`,
    method: 'POST',
    params: { reason }
  });
}

export function rejectRefund(orderNo, reason = '') {
  return request({
    url: `/api/admin/order/refund/reject/${encodeURIComponent(orderNo)}`,
    method: 'POST',
    params: { reason }
  });
}

export function queryAdminRefundStatus(orderNo) {
  return request({
    url: `/api/admin/order/refund/query/${encodeURIComponent(orderNo)}`,
    method: 'GET'
  });
}

// ==================== AI文件管理 ====================

// 上传运动知识文档
export function uploadSportDocument(file, folderName = 'running') {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('folderName', folderName);
  //return post('/api/sport-ai/upload', formData);
    return request({
    url: '/api/sport-ai/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'  // 设置正确的 Content-Type
    }
  });
}

// 通过链接上传文档
export function uploadDocumentByLink(link, title) {
  return post('/api/sport-ai/upload-link', { link, title });
}

// 上传原始文本作为文档
export function uploadDocumentByText(text, title) {
  return post('/api/sport-ai/upload-text', { text, title });
}

// 删除文档
export function deleteDocument(docName) {
  return del(`/api/sport-ai/document/${docName}`);
}

// 获取文档列表
export function getDocumentList() {
  return get('/api/sport-ai/documents');
}
