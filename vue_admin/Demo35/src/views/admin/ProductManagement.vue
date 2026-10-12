<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>运动装备管理</h2>
        <p class="subtitle">管理商城内的商品分类、上下架与热销推荐设置</p>
      </div>
      <div class="header-right">
        <button class="btn-primary" @click="openAddModal">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          添加商品
        </button>
      </div>
    </div>

    <!-- 分类管理区域 -->
    <div class="category-section">
      <div class="section-title">
        <h3>分类管理</h3>
      </div>
      <div class="category-toolbar">
        <div class="add-category">
          <input
            v-model="newCategoryName"
            type="text"
            placeholder="输入新分类名称..."
            class="form-control category-input"
            @keyup.enter="handleAddCategory"
          />
          <button class="btn-secondary" @click="handleAddCategory">添加分类</button>
        </div>
      </div>
      <div class="category-tags">
        <div class="category-tag" v-for="category in categories" :key="category.id">
          <span class="tag-name">{{ category.name }}</span>
          <button class="btn-remove-tag" @click="handleDeleteCategory(category.id)">×</button>
        </div>
        <div v-if="categories.length === 0" class="text-gray text-sm">暂无分类数据</div>
      </div>
    </div>

    <!-- 商品管理区域 -->
    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <div class="search-box">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input 
              type="text" 
              v-model="searchQuery" 
              placeholder="搜索装备名称..."
              class="search-input"
              @keyup.enter="applyFilter"
            />
          </div>
          <div class="custom-select-wrapper">
            <select v-model="filterCategory" class="form-control custom-select" @change="applyFilter">
              <option value="">全部分类</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </div>
          <div class="custom-select-wrapper">
            <select v-model="filterType" class="form-control custom-select" @change="applyFilter">
              <option value="">全部装备</option>
              <option value="hot">热销精选</option>
              <option value="new">新品上市</option>
              <option value="recommend">店长推荐</option>
            </select>
          </div>
        </div>
        <div class="toolbar-right">
          <button class="btn-default" @click="loadProducts">刷新</button>
        </div>
      </div>

      <div class="table-wrapper">
        <table class="modern-table">
          <thead>
            <tr>
              <th>装备信息</th>
              <th>分类</th>
              <th>价格与库存</th>
              <th>状态标签</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in filteredProducts" :key="product.id">
              <td>
                <div class="product-cell">
                  <img :src="getProductImage(product)" :alt="product.name" class="product-img" />
                  <div class="product-info">
                    <span class="product-name">{{ product.name }}</span>
                    <span class="product-id text-gray">ID: {{ product.id }}</span>
                  </div>
                </div>
              </td>
              <td>
                <span class="category-badge">{{ getCategoryName(product.categoryId) }}</span>
              </td>
              <td>
                <div class="price-stock-info">
                  <div class="price-row">
                    <span class="price-current">¥{{ product.price }}</span>
                    <span class="price-original" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
                  </div>
                  <div class="stock-row text-gray text-sm">
                    库存: {{ product.stock || 0 }} | 销量: {{ product.sales || 0 }}
                  </div>
                </div>
              </td>
              <td>
                <div class="status-tags">
                  <button class="status-tag tag-hot" :class="{ active: product.isHot }" @click="toggleHot(product)">
                    热销
                  </button>
                  <button class="status-tag tag-new" :class="{ active: product.isNew }" @click="toggleNew(product)">
                    新品
                  </button>
                  <button class="status-tag tag-recommend" :class="{ active: product.isRecommend }" @click="toggleRecommend(product)">
                    推荐
                  </button>
                </div>
              </td>
              <td class="text-right">
                <div class="action-buttons">
                  <button class="btn-icon-text text-teal" @click="openEditModal(product)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                    编辑
                  </button>
                  <button class="btn-icon-text text-danger" @click="handleDeleteProduct(product.id)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredProducts.length === 0">
              <td colspan="5" class="empty-state">
                <div class="empty-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#CBD5E1" stroke-width="1.5"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>
                  <p>暂无相关装备</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 添加/编辑商品弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card modal-lg">
        <div class="modal-header">
          <h4>{{ isEdit ? '编辑装备信息' : '添加新装备' }}</h4>
          <button class="btn-close" @click="closeModal">×</button>
        </div>
        <div class="modal-body scrollable-body">
          <form @submit.prevent="submitForm" class="modern-form">
            <div class="form-row">
              <div class="form-group flex-2">
                <label>装备名称 <span class="required">*</span></label>
                <input type="text" v-model="formData.name" class="form-control" required placeholder="例如：专业跑鞋" />
              </div>
              <div class="form-group flex-1">
                <label>所属分类 <span class="required">*</span></label>
                <div class="custom-select-wrapper">
                  <select v-model.number="formData.categoryId" class="form-control custom-select" required>
                    <option value="" disabled>请选择</option>
                    <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>售价 (¥) <span class="required">*</span></label>
                <input type="number" v-model.number="formData.price" class="form-control" required min="0" step="0.01" />
              </div>
              <div class="form-group">
                <label>原价 (¥)</label>
                <input type="number" v-model.number="formData.originalPrice" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group">
                <label>库存</label>
                <input type="number" v-model.number="formData.stock" class="form-control" min="0" />
              </div>
              <div class="form-group">
                <label>初始销量</label>
                <input type="number" v-model.number="formData.sales" class="form-control" min="0" />
              </div>
            </div>

            <div class="form-group">
              <label>商品图片 <span class="text-gray text-sm">(支持多张)</span></label>
              <div class="image-upload-area">
                <div class="preview-images">
                  <div v-for="(image, index) in formData.images" :key="index" class="preview-item">
                    <img :src="image" alt="预览图" />
                    <button type="button" class="btn-remove-img" @click="removeImage(index)">×</button>
                  </div>
                  <label class="upload-trigger">
                    <input type="file" multiple accept="image/*" @change="handleImageUpload" class="hidden-input" />
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                  </label>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>装备描述</label>
              <textarea v-model="formData.description" class="form-control" rows="3" placeholder="简要描述装备特点..."></textarea>
            </div>
            
            <div class="form-group">
              <label>规格参数</label>
              <textarea v-model="formData.specifications" class="form-control" rows="2" placeholder="例如：颜色、尺寸等..."></textarea>
            </div>

            <div class="form-group">
              <label>营销标签</label>
              <div class="checkbox-group">
                <label class="custom-checkbox-label">
                  <input type="checkbox" v-model="formData.isHot" />
                  <span class="fake-box"></span>
                  设为热销
                </label>
                <label class="custom-checkbox-label">
                  <input type="checkbox" v-model="formData.isNew" />
                  <span class="fake-box"></span>
                  设为新品
                </label>
                <label class="custom-checkbox-label">
                  <input type="checkbox" v-model="formData.isRecommend" />
                  <span class="fake-box"></span>
                  店长推荐
                </label>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-default" @click="closeModal">取消</button>
          <button type="button" class="btn-primary" @click="submitForm">{{ isEdit ? '保存修改' : '确认添加' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import {
  getAllProducts,
  getProductCategories,
  getProductById,
  createProduct,
  updateProduct,
  deleteProduct,
  addCategory,
  deleteCategory,
  setProductHot,
  setProductNew,
  setProductRecommend,
  getHotProducts,
  getNewProducts,
  getRecommendProducts
} from '@/api/admin';

// 分类管理
const newCategoryName = ref('');
const categories = ref([]);

const handleAddCategory = async () => {
  if (!newCategoryName.value.trim()) return alert('请输入分类名称');
  try {
    const result = await addCategory(newCategoryName.value);
    if (result.success) {
      newCategoryName.value = '';
      loadCategories();
    }
  } catch (error) {
    console.error(error);
  }
};

const handleDeleteCategory = async (id) => {
  if (!confirm('确定要删除这个分类吗？')) return;
  try {
    const result = await deleteCategory(id);
    if (result.success) loadCategories();
  } catch (error) {
    console.error(error);
  }
};

const loadCategories = async () => {
  try {
    const result = await getProductCategories();
    if (result.success) categories.value = result.data || [];
  } catch (error) {
    console.error(error);
  }
};

// 商品管理
const products = ref([]);
const loading = ref(false);
const page = ref(1);
const pageSize = ref(100);

const searchQuery = ref('');
const filterCategory = ref('');
const filterType = ref('');

const loadProducts = async () => {
  loading.value = true;
  try {
    if (filterType.value) {
      let result;
      if (filterType.value === 'hot') result = await getHotProducts(100);
      else if (filterType.value === 'new') result = await getNewProducts(100);
      else if (filterType.value === 'recommend') result = await getRecommendProducts(100);
      
      if (result?.success) products.value = result.data || [];
    } else {
      const params = { page: page.value, pageSize: pageSize.value };
      if (searchQuery.value) params.keyword = searchQuery.value;
      if (filterCategory.value) params.categoryId = filterCategory.value;

      const result = await getAllProducts(params);
      if (result.success) products.value = result.data || [];
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const filteredProducts = computed(() => {
  let result = [...products.value];
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    result = result.filter(p => p.name.toLowerCase().includes(q));
  }
  if (filterCategory.value) {
    result = result.filter(p => p.categoryId === parseInt(filterCategory.value));
  }
  if (filterType.value) {
    if (filterType.value === 'hot') result = result.filter(p => p.isHot);
    if (filterType.value === 'new') result = result.filter(p => p.isNew);
    if (filterType.value === 'recommend') result = result.filter(p => p.isRecommend);
  }
  return result;
});

const applyFilter = () => { loadProducts(); };

const getCategoryName = (id) => {
  const cat = categories.value.find(c => c.id === id);
  return cat ? cat.name : '未分类';
};

const getProductImage = (product) => {
  try {
    let images = [];
    if (typeof product.images === 'string') images = JSON.parse(product.images);
    else if (Array.isArray(product.images)) images = product.images;
    if (images?.length > 0) return images[0];
    if (product.thumbnail) return product.thumbnail;
    return 'https://placehold.co/100x100?text=No+Image';
  } catch (e) {
    return product.thumbnail || 'https://placehold.co/100x100?text=No+Image';
  }
};

// 状态标签切换
const toggleHot = async (p) => {
  if (await setProductHot(p.id, !p.isHot).then(r => r.success)) p.isHot = !p.isHot;
};
const toggleNew = async (p) => {
  if (await setProductNew(p.id, !p.isNew).then(r => r.success)) p.isNew = !p.isNew;
};
const toggleRecommend = async (p) => {
  if (await setProductRecommend(p.id, !p.isRecommend).then(r => r.success)) p.isRecommend = !p.isRecommend;
};

// 删除
const handleDeleteProduct = async (id) => {
  if (!confirm('确定删除商品吗？')) return;
  try {
    if (await deleteProduct(id).then(r => r.success)) loadProducts();
  } catch (error) {
    console.error(error);
  }
};

// 弹窗表单
const showModal = ref(false);
const isEdit = ref(false);
const hasNewImages = ref(false);

const formData = ref({
  id: null, name: '', price: 0, originalPrice: 0, stock: 0, sales: 0,
  categoryId: '', images: [], description: '', specifications: '',
  isHot: false, isNew: false, isRecommend: false
});

const openAddModal = () => {
  isEdit.value = false;
  hasNewImages.value = false;
  formData.value = {
    id: null, name: '', price: 0, originalPrice: 0, stock: 0, sales: 0,
    categoryId: '', images: [], description: '', specifications: '',
    isHot: false, isNew: false, isRecommend: false
  };
  showModal.value = true;
};

const openEditModal = async (product) => {
  isEdit.value = true;
  hasNewImages.value = false;
  try {
    const result = await getProductById(product.id);
    if (result.success && result.data) {
      const p = result.data;
      let imgs = [];
      try {
        if (typeof p.images === 'string') imgs = JSON.parse(p.images);
        else if (Array.isArray(p.images)) imgs = [...p.images];
      } catch (e) { imgs = []; }

      formData.value = {
        id: p.id, name: p.name || '', price: p.price || 0,
        originalPrice: p.originalPrice || 0, stock: p.stock || 0,
        sales: p.sales || 0, categoryId: p.categoryId || '',
        images: imgs, description: p.description || '',
        specifications: p.specifications || '',
        isHot: !!p.isHot, isNew: !!p.isNew, isRecommend: !!p.isRecommend
      };
      showModal.value = true;
    }
  } catch (error) {
    console.error(error);
  }
};

const closeModal = () => { showModal.value = false; };

const handleImageUpload = (e) => {
  const files = e.target.files;
  if (files.length > 0) {
    hasNewImages.value = true;
    for (let i = 0; i < files.length; i++) {
      const reader = new FileReader();
      reader.onload = (ev) => formData.value.images.push(ev.target.result);
      reader.readAsDataURL(files[i]);
    }
  }
};
const removeImage = (idx) => formData.value.images.splice(idx, 1);

const submitForm = async () => {
  try {
    const fd = new FormData();
    fd.append('name', formData.value.name || '');
    fd.append('price', formData.value.price || 0);
    fd.append('originalPrice', formData.value.originalPrice || 0);
    fd.append('stock', formData.value.stock || 0);
    fd.append('sales', formData.value.sales || 0);
    fd.append('categoryId', formData.value.categoryId || '');
    fd.append('description', formData.value.description || '');
    fd.append('specifications', formData.value.specifications || '');
    fd.append('isHot', formData.value.isHot ? 1 : 0);
    fd.append('isNew', formData.value.isNew ? 1 : 0);
    fd.append('isRecommend', formData.value.isRecommend ? 1 : 0);

    if (isEdit.value) {
      if (hasNewImages.value) {
        const exist = formData.value.images.filter(img => !img.startsWith('data:'));
        if (exist.length > 0) fd.append('images', JSON.stringify(exist));
        const news = formData.value.images.filter(img => img.startsWith('data:'));
        for (let img of news) {
          const res = await fetch(img);
          fd.append('imageFiles', await res.blob());
        }
      } else {
        if (formData.value.images?.length > 0) fd.append('images', JSON.stringify(formData.value.images));
      }
    } else {
      if (formData.value.images?.length > 0) {
        for (let img of formData.value.images) {
          if (img.startsWith('data:')) {
            const res = await fetch(img);
            fd.append('imageFiles', await res.blob());
          } else {
            const exist = formData.value.images.filter(i => !i.startsWith('data:'));
            if (exist.length > 0) fd.append('images', JSON.stringify(exist));
          }
        }
      }
    }

    let result = isEdit.value ? await updateProduct(formData.value.id, fd) : await createProduct(fd);
    if (result.success) {
      closeModal();
      loadProducts();
    }
  } catch (error) {
    console.error(error);
  }
};

onMounted(() => {
  loadCategories();
  loadProducts();
});
</script>

<style scoped>
/* Page & Headers */
.page-container { padding: 32px; max-width: 1600px; margin: 0 auto; width: 100%; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.page-header h2 { margin: 0 0 8px 0; font-size: 24px; color: var(--text-dark); font-weight: 700; }
.subtitle { margin: 0; color: var(--text-gray); font-size: 14px; }

/* Buttons */
.btn-primary { display: inline-flex; align-items: center; gap: 8px; background: var(--sports-teal); color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 600; font-size: 14px; cursor: pointer; transition: var(--transition); box-shadow: 0 4px 12px rgba(0, 180, 216, 0.2); }
.btn-primary:hover { background: var(--sports-teal-dark); transform: translateY(-1px); }
.btn-secondary { background: #F1F5F9; color: var(--sports-teal-dark); border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; font-size: 13px; cursor: pointer; transition: var(--transition); }
.btn-secondary:hover { background: #E2E8F0; }
.btn-default { background: white; color: var(--text-dark); border: 1px solid var(--border-color); padding: 10px 20px; border-radius: 8px; font-weight: 500; font-size: 14px; cursor: pointer; transition: var(--transition); }
.btn-default:hover { background: #F8FAFC; }
.btn-icon-text { display: inline-flex; align-items: center; gap: 4px; background: none; border: none; font-size: 13px; font-weight: 500; cursor: pointer; padding: 6px 10px; border-radius: 6px; transition: var(--transition); }
.text-teal { color: var(--sports-teal-dark); }
.text-teal:hover { background: #F0FDF4; color: var(--sports-teal); }
.text-danger { color: #EF4444; }
.text-danger:hover { background: #FEF2F2; }

/* Category Section */
.category-section { background: white; border-radius: var(--radius-md); padding: 20px 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm); border: 1px solid rgba(0,0,0,0.02); }
.section-title h3 { margin: 0 0 16px 0; font-size: 16px; color: var(--text-dark); }
.category-toolbar { margin-bottom: 16px; }
.add-category { display: flex; gap: 12px; }
.category-input { width: 240px; }
.category-tags { display: flex; flex-wrap: wrap; gap: 12px; }
.category-tag { display: inline-flex; align-items: center; background: #F8FAFC; border: 1px solid var(--border-color); border-radius: 20px; padding: 4px 12px; font-size: 13px; color: var(--text-dark); }
.btn-remove-tag { background: none; border: none; color: #94A3B8; margin-left: 6px; cursor: pointer; font-size: 16px; line-height: 1; padding: 0 4px; }
.btn-remove-tag:hover { color: #EF4444; }

/* Form Controls */
.form-control { width: 100%; padding: 10px 14px; border: 1px solid #CBD5E1; border-radius: 8px; font-size: 14px; transition: var(--transition); font-family: inherit; box-sizing: border-box; }
.form-control:focus { outline: none; border-color: var(--sports-teal); box-shadow: 0 0 0 3px rgba(0, 180, 216, 0.1); }
.custom-select-wrapper { position: relative; }
.custom-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%238D99AE' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 14px center; padding-right: 36px; }

/* Toolbar */
.table-toolbar { padding: 16px 24px; background: #F8FAFC; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; }
.toolbar-left { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.search-box { position: relative; display: flex; align-items: center; }
.search-icon { position: absolute; left: 14px; color: var(--text-gray); }
.search-input { padding: 10px 16px 10px 40px; border: 1px solid var(--border-color); border-radius: 8px; font-size: 14px; width: 220px; transition: var(--transition); outline: none; }
.search-input:focus { border-color: var(--sports-teal); width: 260px; }

/* Table */
.table-card { background: var(--card-bg); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid rgba(0,0,0,0.02); overflow: hidden; }
.table-wrapper { overflow-x: auto; }
.modern-table { width: 100%; border-collapse: collapse; text-align: left; }
.modern-table th { padding: 16px 24px; font-size: 13px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; border-bottom: 1px solid var(--border-color); background: #FFFFFF; }
.modern-table td { padding: 16px 24px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-color); vertical-align: middle; }
.modern-table tbody tr { transition: var(--transition); }
.modern-table tbody tr:hover { background: #F8FAFC; }
.text-right { text-align: right; }
.text-gray { color: var(--text-gray) !important; }
.text-sm { font-size: 13px; }

/* Product Cells */
.product-cell { display: flex; align-items: center; gap: 12px; }
.product-img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; border: 1px solid #F1F5F9; }
.product-info { display: flex; flex-direction: column; gap: 4px; }
.product-name { font-weight: 600; font-size: 14px; }
.category-badge { background: #F1F5F9; color: #475569; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 500; }
.price-stock-info { display: flex; flex-direction: column; gap: 4px; }
.price-current { font-weight: 700; color: var(--sports-orange); font-size: 15px; margin-right: 8px; }
.price-original { text-decoration: line-through; color: #94A3B8; font-size: 13px; }

/* Tags */
.status-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.status-tag { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; border: 1px solid transparent; cursor: pointer; transition: var(--transition); opacity: 0.5; filter: grayscale(1); }
.status-tag.active { opacity: 1; filter: grayscale(0); }
.tag-hot { background: #FEF2F2; color: #EF4444; border-color: #FCA5A5; }
.tag-new { background: #F0FDF4; color: #10B981; border-color: #6EE7B7; }
.tag-recommend { background: #FFF7ED; color: #F59E0B; border-color: #FCD34D; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(11, 19, 43, 0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: white; width: 100%; max-width: 600px; border-radius: var(--radius-md); box-shadow: 0 20px 40px rgba(0,0,0,0.1); display: flex; flex-direction: column; max-height: 90vh; animation: modalFadeIn 0.3s ease; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.modal-header { padding: 20px 24px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.modal-header h4 { margin: 0; font-size: 18px; font-weight: 600; }
.btn-close { background: none; border: none; font-size: 24px; color: var(--text-gray); cursor: pointer; }
.scrollable-body { padding: 24px; overflow-y: auto; }
.modern-form .form-row { display: flex; gap: 16px; margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.flex-1 { flex: 1; } .flex-2 { flex: 2; }
.form-group label { display: block; margin-bottom: 8px; font-size: 13px; font-weight: 500; color: var(--text-dark); }
.required { color: #EF4444; }

/* Checkbox group */
.checkbox-group { display: flex; gap: 20px; }
.custom-checkbox-label { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px; }
.custom-checkbox-label input { display: none; }
.fake-box { width: 18px; height: 18px; border: 2px solid #CBD5E1; border-radius: 4px; position: relative; transition: var(--transition); }
.custom-checkbox-label input:checked + .fake-box { background: var(--sports-teal); border-color: var(--sports-teal); }
.custom-checkbox-label input:checked + .fake-box::after { content: ''; position: absolute; left: 5px; top: 2px; width: 4px; height: 8px; border: solid white; border-width: 0 2px 2px 0; transform: rotate(45deg); }

/* Images Upload */
.image-upload-area { border: 1px dashed var(--border-color); padding: 12px; border-radius: 8px; background: #F8FAFC; }
.preview-images { display: flex; flex-wrap: wrap; gap: 12px; }
.preview-item { position: relative; width: 80px; height: 80px; border-radius: 8px; overflow: hidden; border: 1px solid var(--border-color); }
.preview-item img { width: 100%; height: 100%; object-fit: cover; }
.btn-remove-img { position: absolute; top: 4px; right: 4px; background: rgba(0,0,0,0.6); color: white; border: none; width: 20px; height: 20px; border-radius: 50%; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.upload-trigger { width: 80px; height: 80px; border: 1px dashed #94A3B8; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #94A3B8; cursor: pointer; transition: var(--transition); }
.upload-trigger:hover { border-color: var(--sports-teal); color: var(--sports-teal); background: #F0FDF4; }
.hidden-input { display: none; }

.modal-footer { padding: 16px 24px; background: #F8FAFC; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; gap: 12px; }

.empty-state { text-align: center; padding: 64px 0 !important; }
.empty-content { display: flex; flex-direction: column; align-items: center; gap: 16px; color: var(--text-gray); }
</style>