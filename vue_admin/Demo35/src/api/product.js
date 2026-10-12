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
