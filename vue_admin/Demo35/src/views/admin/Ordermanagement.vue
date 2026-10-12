<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-copy">
        <h2>订单管理</h2>
        <p class="subtitle">跟踪商城订单状态，集中处理退款审核与发货操作</p>
      </div>
      <button class="btn-primary" @click="handleSearch">刷新数据</button>
    </div>

    <div class="dashboard-cards">
      <div class="stat-card">
        <p class="label">订单总数</p>
        <h4>{{ statistics.totalOrders || 0 }}</h4>
      </div>
      <div class="stat-card">
        <p class="label">总金额</p>
        <h4 class="text-teal">￥{{ statistics.totalAmount || 0 }}</h4>
      </div>
      <div class="stat-card">
        <p class="label">待发货</p>
        <h4>{{ statistics.pendingOrders || 0 }}</h4>
      </div>
      <div class="stat-card">
        <p class="label">已完成</p>
        <h4>{{ statistics.completedOrders || 0 }}</h4>
      </div>
    </div>

    <div class="table-card">
      <div class="filter-section">
        <div class="filter-grid">
          <input v-model="filters.orderNo" type="text" class="form-control" placeholder="订单号" />
          <input v-model="filters.receiverName" type="text" class="form-control" placeholder="收货人" />
          <input v-model="filters.receiverPhone" type="text" class="form-control" placeholder="手机号" />
          <select v-model="filters.status" class="form-control">
            <option value="">全部状态</option>
            <option :value="0">待付款</option>
            <option :value="1">待发货</option>
            <option :value="2">待收货</option>
            <option :value="3">已完成</option>
            <option :value="4">已取消</option>
            <option :value="5">退款中</option>
          </select>
          <input v-model="filters.startDate" type="date" class="form-control" />
          <input v-model="filters.endDate" type="date" class="form-control" />
        </div>
        <div class="filter-actions">
          <button class="btn-default" @click="handleReset">重置条件</button>
          <button class="btn-primary" @click="handleSearch">搜索订单</button>
        </div>
      </div>

      <div class="table-toolbar" v-if="selectedOrders.length > 0">
        <div class="batch-info">
          <span class="badge">{{ selectedOrders.length }}</span> 项已选择
        </div>
        <button class="btn-primary" @click="handleBatchShip">批量发货</button>
      </div>

      <div class="table-wrapper">
        <table class="modern-table">
          <thead>
            <tr>
              <th class="checkbox-cell">
                <input type="checkbox" v-model="selectAll" @change="handleSelectAll" />
              </th>
              <th>订单信息</th>
              <th>收货信息</th>
              <th>订单金额</th>
              <th>状态</th>
              <th>时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="order in orders"
              :key="order.orderNo"
              :class="{ 'is-selected': selectedOrders.includes(order.orderNo) }"
            >
              <td class="checkbox-cell">
                <input type="checkbox" v-model="selectedOrders" :value="order.orderNo" />
              </td>
              <td>
                <div class="order-info-cell">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <span class="text-gray">用户ID: {{ order.userId }}</span>
                </div>
              </td>
              <td>
                <div class="receiver-cell">
                  <span>{{ order.receiverName }}</span>
                  <span class="text-gray">{{ order.receiverPhone }}</span>
                  <span class="text-gray ellipsis" :title="order.receiverAddress">{{ order.receiverAddress }}</span>
                </div>
              </td>
              <td>
                <span class="amount-text">￥{{ formatAmount(order.totalAmount) }}</span>
              </td>
              <td>
                <div class="status-stack">
                  <span class="status-badge" :class="getStatusClass(order)">
                    {{ getStatusText(order) }}
                  </span>
                  <span
                    v-if="shouldShowRefundStatus(order)"
                    class="refund-text"
                    :class="{ 'refund-error': order.refundStatus === 'REFUND_QUERY_FAILED' }"
                  >
                    {{ getRefundStatusText(order) }}
                  </span>
                </div>
              </td>
              <td class="text-gray">{{ formatDate(order.createTime) }}</td>
              <td class="text-right">
                <div class="action-buttons">
                  <button
                    v-if="canApproveRefund(order)"
                    class="btn-link text-orange"
                    @click="openRefundAuditModal(order, 'approve')"
                  >
                    同意退款
                  </button>
                  <button
                    v-if="canApproveRefund(order)"
                    class="btn-link text-danger"
                    @click="openRefundAuditModal(order, 'reject')"
                  >
                    驳回退款
                  </button>
                  <button v-if="order.status === 1" class="btn-link text-teal" @click="handleShip(order.orderNo)">
                    发货
                  </button>
                  <button v-if="canCancelOrder(order)" class="btn-link text-danger" @click="handleCancel(order.orderNo)">
                    取消
                  </button>
                  <button class="btn-link text-blue" @click="handleViewDetail(order)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="orders.length === 0 && !loading">
              <td colspan="7" class="empty-state">未找到符合条件的订单</td>
            </tr>
            <tr v-if="loading">
              <td colspan="7" class="empty-state">数据加载中...</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-wrapper" v-if="pagination.totalPages > 1">
        <button class="btn-default" :disabled="pagination.page === 1" @click="handlePageChange(pagination.page - 1)">
          上一页
        </button>
        <div class="page-info">第 <span>{{ pagination.page }}</span> / {{ pagination.totalPages }} 页</div>
        <button
          class="btn-default"
          :disabled="pagination.page >= pagination.totalPages"
          @click="handlePageChange(pagination.page + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-card">
        <div class="modal-header">
          <h4>订单详情</h4>
          <button class="btn-close" @click="closeDetailModal">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><span class="label">订单号</span><span>{{ currentOrder.orderNo }}</span></div>
            <div class="detail-item"><span class="label">订单状态</span><span>{{ getStatusText(currentOrder) }}</span></div>
            <div v-if="shouldShowRefundStatus(currentOrder)" class="detail-item">
              <span class="label">退款状态</span>
              <span>{{ getRefundStatusText(currentOrder) }}</span>
            </div>
            <div class="detail-item"><span class="label">用户ID</span><span>{{ currentOrder.userId }}</span></div>
            <div class="detail-item"><span class="label">下单时间</span><span>{{ formatDate(currentOrder.createTime) }}</span></div>
            <div class="detail-item"><span class="label">订单金额</span><span>￥{{ formatAmount(currentOrder.totalAmount) }}</span></div>
            <div class="detail-item"><span class="label">收货人</span><span>{{ currentOrder.receiverName }}</span></div>
            <div class="detail-item"><span class="label">联系电话</span><span>{{ currentOrder.receiverPhone }}</span></div>
            <div class="detail-item detail-item-block"><span class="label">收货地址</span><span>{{ currentOrder.receiverAddress }}</span></div>
            <div v-if="shouldShowRefundStatus(currentOrder)" class="detail-item detail-item-block">
              <span class="label">状态说明</span>
              <span>{{ currentOrder.refundMessage || getRefundStatusText(currentOrder) }}</span>
            </div>
            <div v-if="shouldShowRefundStatus(currentOrder)" class="detail-item">
              <span class="label">退款金额</span>
              <span>{{ formatRefundAmount(currentOrder.refundAmount) }}</span>
            </div>
            <div v-if="shouldShowRefundStatus(currentOrder)" class="detail-item">
              <span class="label">退款结果</span>
              <span>{{ getRefundResultText(currentOrder) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="closeDetailModal">关闭</button>
          <button
            v-if="canApproveRefund(currentOrder)"
            class="btn-warning"
            @click="openRefundAuditModal(currentOrder, 'approve')"
          >
            同意退款
          </button>
          <button
            v-if="canApproveRefund(currentOrder)"
            class="btn-danger-solid"
            @click="openRefundAuditModal(currentOrder, 'reject')"
          >
            驳回退款
          </button>
          <button v-if="currentOrder.status === 1" class="btn-primary" @click="handleShip(currentOrder.orderNo); closeDetailModal()">
            确认发货
          </button>
        </div>
      </div>
    </div>

    <div v-if="showRefundAuditModal" class="modal-overlay" @click.self="closeRefundAuditModal">
      <div class="modal-card modal-sm">
        <div class="modal-header">
          <h4>{{ refundAuditAction === 'approve' ? '同意退款' : '驳回退款' }}</h4>
          <button class="btn-close" @click="closeRefundAuditModal">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><span class="label">订单号</span><span>{{ refundAuditForm.orderNo }}</span></div>
            <div class="detail-item"><span class="label">当前状态</span><span>{{ getStatusText(refundAuditForm) }}</span></div>
          </div>
          <label class="form-label" for="refund-reason">
            {{ refundAuditAction === 'approve' ? '审核说明' : '驳回原因' }}
          </label>
          <textarea
            id="refund-reason"
            v-model.trim="refundAuditForm.reason"
            class="form-control refund-textarea"
            maxlength="120"
            :placeholder="refundAuditAction === 'approve' ? '可选，填写审核备注' : '请输入驳回原因'"
          ></textarea>
          <div class="text-gray text-right">{{ refundAuditForm.reason.length }}/120</div>
        </div>
        <div class="modal-footer">
          <button class="btn-default" @click="closeRefundAuditModal">取消</button>
          <button
            :class="refundAuditAction === 'approve' ? 'btn-warning' : 'btn-danger-solid'"
            :disabled="refundAuditSubmitting"
            @click="submitRefundAudit"
          >
            {{ refundAuditSubmitting ? '提交中...' : refundAuditAction === 'approve' ? '确认同意' : '确认驳回' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import {
  approveRefund,
  batchShipOrders,
  cancelOrder,
  getOrderStatistics,
  getOrders,
  queryAdminRefundStatus,
  rejectRefund,
  shipOrder
} from '@/api/admin';

const orders = ref([]);
const statistics = ref({});
const loading = ref(false);
const showDetailModal = ref(false);
const showRefundAuditModal = ref(false);
const currentOrder = ref({});
const selectedOrders = ref([]);
const selectAll = ref(false);
const refundAuditAction = ref('approve');
const refundAuditSubmitting = ref(false);
const refundAuditForm = ref({
  orderNo: '',
  status: 5,
  reason: ''
});

const refundPollingTimers = new Map();
const refundPollingLocks = new Set();

const filters = ref({
  orderNo: '',
  receiverName: '',
  receiverPhone: '',
  status: '',
  startDate: '',
  endDate: ''
});

const pagination = ref({
  page: 1,
  size: 10,
  total: 0,
  totalPages: 0
});

const refundStatusMap = {
  REFUND_SUCCESS: '退款成功',
  REFUND_PROCESSING: '退款处理中',
  REFUND_QUERY_FAILED: '退款查询失败',
  REFUND_NOT_AVAILABLE: '未找到可退款流水',
  REFUND_UNKNOWN: '退款状态未知'
};

const getPayload = (result) => (!result || typeof result !== 'object' ? {} : (result.data && typeof result.data === 'object' ? result.data : result));
const resolveOrderList = (payload) => Array.isArray(payload) ? payload : (payload.list || payload.records || payload.rows || payload.items || payload.content || payload.orders || []);
const resolvePagination = (payload, fallbackSize) => {
  const total = Number(payload.total ?? payload.totalCount ?? payload.count ?? 0);
  const pageSize = Number((payload.pageSize ?? payload.size ?? fallbackSize) || 10);
  const totalPages = Number(payload.totalPages ?? payload.pages ?? payload.pageCount ?? 0) || (total > 0 ? Math.ceil(total / pageSize) : 0);
  return { total, totalPages };
};

const normalizeStatistics = (payload = {}) => ({
  totalOrders: Number(payload.totalOrders ?? payload.orderCount ?? payload.total ?? payload.totalCount ?? 0),
  totalAmount: Number(payload.totalAmount ?? payload.amount ?? payload.totalPrice ?? payload.turnover ?? 0),
  pendingOrders: Number(payload.pendingOrders ?? payload.pendingCount ?? payload.toShipCount ?? 0),
  completedOrders: Number(payload.completedOrders ?? payload.finishedCount ?? payload.doneCount ?? 0)
});

const normalizeOrder = (order = {}) => ({
  ...order,
  orderNo: order.orderNo || order.orderNumber || order.id || '-',
  userId: order.userId || order.uid || order.user?.id || '-',
  receiverName: order.receiverName || order.consignee || order.addressName || '-',
  receiverPhone: order.receiverPhone || order.phone || order.mobile || '-',
  receiverAddress: order.receiverAddress || order.address || order.fullAddress || '-',
  totalAmount: order.totalAmount ?? order.amount ?? order.totalPrice ?? order.payAmount ?? 0,
  status: Number(order.status ?? order.orderStatus ?? 0),
  createTime: order.createTime || order.createAt || order.createdAt || order.orderTime || '',
  refundStatus: order.refundStatus || '',
  refunded: order.refunded,
  finished: order.finished,
  refundAmount: order.refundAmount,
  refundMessage: order.refundMessage || order.message || ''
});

const mergeRefundState = (nextOrder = {}, previousOrder = {}) => {
  if (!previousOrder || typeof previousOrder !== 'object') return nextOrder;

  const merged = { ...nextOrder };
  const hasRefundMeta =
    Boolean(nextOrder.refundStatus) ||
    typeof nextOrder.refunded === 'boolean' ||
    typeof nextOrder.finished === 'boolean' ||
    nextOrder.refundAmount !== undefined;

  if (!hasRefundMeta) {
    merged.refundStatus = previousOrder.refundStatus || '';
    merged.refunded = previousOrder.refunded;
    merged.finished = previousOrder.finished;
    merged.refundAmount = previousOrder.refundAmount;
    merged.refundMessage = previousOrder.refundMessage || nextOrder.refundMessage || '';
  }

  if (merged.status === 4 && previousOrder.refunded === true) {
    merged.refunded = true;
    merged.refundStatus = previousOrder.refundStatus || 'REFUND_SUCCESS';
    merged.finished = true;
    merged.refundMessage = previousOrder.refundMessage || merged.refundMessage;
  }

  return merged;
};

const updateOrderInState = (orderNo, patch = {}) => {
  orders.value = orders.value.map((order) => order.orderNo === orderNo ? { ...order, ...patch } : order);
  if (currentOrder.value?.orderNo === orderNo) {
    currentOrder.value = { ...currentOrder.value, ...patch };
  }
};

const buildRefundPatch = (data = {}) => {
  const patch = {};
  if ('orderStatus' in data || 'status' in data) patch.status = Number(data.orderStatus ?? data.status);
  if ('refundStatus' in data) patch.refundStatus = data.refundStatus || '';
  if ('refunded' in data) patch.refunded = data.refunded;
  if ('finished' in data) patch.finished = data.finished;
  if ('refundAmount' in data) patch.refundAmount = data.refundAmount;
  if ('message' in data) patch.refundMessage = data.message || '';
  return patch;
};

const shouldShowRefundStatus = (order = {}) => Number(order?.status) === 5 || Boolean(order?.refundStatus) || typeof order?.refunded === 'boolean';
const canApproveRefund = (order = {}) => Number(order?.status) === 5;
const canCancelOrder = (order = {}) => Number(order?.status) < 3 && Number(order?.status) !== 5;
const isRefundFinished = (order = {}) => Boolean(order?.finished);
const shouldPollRefund = (order = {}) => Number(order?.status) === 5 && !Boolean(order?.finished);

const getRefundStatusText = (order = {}) => {
  if (order?.refundStatus && refundStatusMap[order.refundStatus]) return refundStatusMap[order.refundStatus];
  if (order?.refunded) return '退款成功';
  if (Number(order?.status) === 5) return order?.refundMessage || '退款处理中';
  return order?.refundMessage || '';
};

const getRefundResultText = (order = {}) => {
  if (order?.refunded === true) return '退款成功';
  if (order?.finished === false) return '退款处理中';
  if (order?.finished === true && order?.refunded === false) return '退款结束但未成功';
  return '-';
};

const formatAmount = (amount) => {
  const value = Number(amount ?? 0);
  return Number.isFinite(value) ? value.toFixed(2) : amount;
};

const formatRefundAmount = (amount) => {
  if (amount === null || amount === undefined || amount === '') return '-';
  return `￥${formatAmount(amount)}`;
};

const isRefundSuccess = (order = {}) => order?.refunded === true || order?.refundStatus === 'REFUND_SUCCESS';
const getStatusText = (orderOrStatus) => {
  if (typeof orderOrStatus === 'object' && orderOrStatus !== null) {
    if (isRefundSuccess(orderOrStatus)) return '已退款';
    return ({ 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中' }[Number(orderOrStatus.status)] || '未知状态');
  }
  return ({ 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中' }[Number(orderOrStatus)] || '未知状态');
};
const getStatusClass = (orderOrStatus) => {
  if (typeof orderOrStatus === 'object' && orderOrStatus !== null) {
    if (isRefundSuccess(orderOrStatus)) return 'status-refunded';
    return ({ 0: 'status-warning', 1: 'status-info', 2: 'status-primary', 3: 'status-success', 4: 'status-danger', 5: 'status-refunding' }[Number(orderOrStatus.status)] || '');
  }
  return ({ 0: 'status-warning', 1: 'status-info', 2: 'status-primary', 3: 'status-success', 4: 'status-danger', 5: 'status-refunding' }[Number(orderOrStatus)] || '');
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return Number.isNaN(date.getTime())
    ? String(dateStr)
    : date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};

const stopRefundPolling = (orderNo) => {
  if (!refundPollingTimers.has(orderNo)) return;
  clearInterval(refundPollingTimers.get(orderNo));
  refundPollingTimers.delete(orderNo);
};

const stopAllRefundPolling = () => {
  refundPollingTimers.forEach((timer) => clearInterval(timer));
  refundPollingTimers.clear();
  refundPollingLocks.clear();
};

const syncRefundStatus = async (orderNo) => {
  if (!orderNo || refundPollingLocks.has(orderNo)) return;
  refundPollingLocks.add(orderNo);

  try {
    const result = await queryAdminRefundStatus(orderNo);
    const payload = getPayload(result);
    const patch = buildRefundPatch(payload);

    if (Object.keys(patch).length) {
      updateOrderInState(orderNo, patch);
    }

    if (!Object.keys(patch).length || patch.finished || patch.refunded || [1, 2, 4].includes(Number(patch.status))) {
      stopRefundPolling(orderNo);
      fetchOrders();
      fetchStatistics();
    }
  } catch (error) {
    console.error('查询退款状态失败:', error);
    updateOrderInState(orderNo, {
      refundStatus: 'REFUND_QUERY_FAILED',
      refundMessage: error.message || refundStatusMap.REFUND_QUERY_FAILED
    });
    stopRefundPolling(orderNo);
  } finally {
    refundPollingLocks.delete(orderNo);
  }
};

function startRefundPolling(orderNo) {
  if (!orderNo || refundPollingTimers.has(orderNo)) return;
  syncRefundStatus(orderNo);
  refundPollingTimers.set(orderNo, window.setInterval(() => syncRefundStatus(orderNo), 2500));
}

const syncRefundPolling = (list = []) => {
  const activeOrderNos = new Set(list.filter(shouldPollRefund).map((order) => order.orderNo));
  list.forEach((order) => {
    if (shouldPollRefund(order)) startRefundPolling(order.orderNo);
  });
  Array.from(refundPollingTimers.keys()).forEach((orderNo) => {
    if (!activeOrderNos.has(orderNo)) stopRefundPolling(orderNo);
  });
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const previousOrderMap = new Map(orders.value.map((order) => [order.orderNo, order]));
    const params = { page: pagination.value.page, size: pagination.value.size, ...filters.value };
    Object.keys(params).forEach((key) => { if (params[key] === '') delete params[key]; });
    const payload = getPayload(await getOrders(params));
    const list = resolveOrderList(payload).map((order) => {
      const normalizedOrder = normalizeOrder(order);
      return mergeRefundState(normalizedOrder, previousOrderMap.get(normalizedOrder.orderNo));
    });
    const pageInfo = resolvePagination(payload, pagination.value.size);
    orders.value = list;
    pagination.value.total = pageInfo.total || list.length;
    pagination.value.totalPages = pageInfo.totalPages || (list.length > 0 ? 1 : 0);
    selectedOrders.value = [];
    selectAll.value = false;
    syncRefundPolling(list);
  } catch (error) {
    console.error('获取订单失败:', error);
    orders.value = [];
    pagination.value.total = 0;
    pagination.value.totalPages = 0;
    stopAllRefundPolling();
  } finally {
    loading.value = false;
  }
};

const fetchStatistics = async () => {
  try {
    statistics.value = normalizeStatistics(getPayload(await getOrderStatistics()));
  } catch (error) {
    console.error('获取订单统计失败:', error);
    statistics.value = normalizeStatistics();
  }
};

const handleSearch = () => {
  pagination.value.page = 1;
  fetchOrders();
  fetchStatistics();
};

const handleReset = () => {
  filters.value = { orderNo: '', receiverName: '', receiverPhone: '', status: '', startDate: '', endDate: '' };
  handleSearch();
};

const handlePageChange = (page) => {
  pagination.value.page = page;
  fetchOrders();
};

const handleSelectAll = () => {
  selectedOrders.value = selectAll.value ? orders.value.map((order) => order.orderNo) : [];
};

const handleShip = async (orderNo) => {
  if (!confirm('确认发货吗？')) return;
  try {
    await shipOrder(orderNo);
    fetchOrders();
    fetchStatistics();
  } catch (error) {
    console.error('发货失败:', error);
    alert(error.message || '发货失败');
  }
};

const handleBatchShip = async () => {
  if (!selectedOrders.value.length || !confirm(`确认将选中的 ${selectedOrders.value.length} 个订单设为已发货吗？`)) return;
  try {
    await batchShipOrders(selectedOrders.value);
    selectedOrders.value = [];
    selectAll.value = false;
    fetchOrders();
    fetchStatistics();
  } catch (error) {
    console.error('批量发货失败:', error);
    alert(error.message || '批量发货失败');
  }
};

const handleCancel = async (orderNo) => {
  if (!confirm('确认取消该订单吗？')) return;
  try {
    await cancelOrder(orderNo);
    fetchOrders();
    fetchStatistics();
  } catch (error) {
    console.error('取消订单失败:', error);
    alert(error.message || '取消订单失败');
  }
};

const handleViewDetail = (order) => {
  currentOrder.value = order;
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  currentOrder.value = {};
};

const openRefundAuditModal = (order, action) => {
  refundAuditAction.value = action;
  refundAuditForm.value = {
    orderNo: order.orderNo || '',
    status: Number(order.status ?? 5),
    reason: ''
  };
  showRefundAuditModal.value = true;
};

const closeRefundAuditModal = () => {
  if (refundAuditSubmitting.value) return;
  showRefundAuditModal.value = false;
  refundAuditForm.value = { orderNo: '', status: 5, reason: '' };
};

const submitRefundAudit = async () => {
  const { orderNo, reason } = refundAuditForm.value;
  if (!orderNo) return;
  if (refundAuditAction.value === 'reject' && !reason.trim()) {
    alert('请输入驳回原因');
    return;
  }

  refundAuditSubmitting.value = true;
  try {
    if (refundAuditAction.value === 'approve') {
      await approveRefund(orderNo, reason.trim());
      updateOrderInState(orderNo, {
        refundStatus: 'REFUND_PROCESSING',
        refunded: false,
        finished: false,
        refundMessage: '退款审核成功'
      });
      alert('退款审核成功');
      startRefundPolling(orderNo);
    } else {
      await rejectRefund(orderNo, reason.trim());
      stopRefundPolling(orderNo);
      alert('退款已驳回');
    }

    closeRefundAuditModal();
    fetchOrders();
    fetchStatistics();
  } catch (error) {
    console.error('处理退款失败:', error);
    alert(error.message || '处理退款失败');
  } finally {
    refundAuditSubmitting.value = false;
  }
};

onMounted(() => {
  fetchOrders();
  fetchStatistics();
});

onBeforeUnmount(() => {
  stopAllRefundPolling();
});
</script>

<style scoped>
.page-container { padding: 32px; max-width: 1600px; margin: 0 auto; width: 100%; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 24px; }
.header-copy h2 { margin: 0 0 8px; font-size: 24px; color: var(--text-dark); }
.subtitle { margin: 0; color: var(--text-gray); font-size: 14px; }

.dashboard-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 24px; }
.stat-card, .table-card { background: var(--card-bg); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid rgba(0,0,0,0.02); }
.stat-card { padding: 20px; }
.stat-card h4, .stat-card p { margin: 0; }
.stat-card .label { color: var(--text-gray); font-size: 13px; margin-bottom: 6px; }
.text-teal { color: var(--sports-teal-dark) !important; }

.filter-section { padding: 20px 24px; border-bottom: 1px solid var(--border-color); background: #fafafa; }
.filter-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 16px; }
.form-control { width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; box-sizing: border-box; outline: none; }
.form-control:focus { border-color: var(--sports-teal); box-shadow: 0 0 0 2px rgba(0,180,216,0.1); }
.form-label { font-size: 14px; font-weight: 600; color: var(--text-dark); }
.filter-actions, .modal-footer, .action-buttons { display: flex; gap: 12px; justify-content: flex-end; flex-wrap: wrap; }

.btn-primary, .btn-default, .btn-link, .btn-warning, .btn-danger-solid { border: none; border-radius: 6px; font-size: 14px; cursor: pointer; transition: var(--transition); }
.btn-primary { background: var(--sports-teal); color: #fff; padding: 8px 16px; }
.btn-default { background: #fff; color: var(--text-dark); border: 1px solid var(--border-color); padding: 8px 16px; }
.btn-link { background: none; padding: 6px 8px; }
.btn-warning { background: #f59e0b; color: #fff; padding: 8px 16px; }
.btn-danger-solid { background: #ef4444; color: #fff; padding: 8px 16px; }
.btn-primary:hover { background: var(--sports-teal-dark); }
.btn-warning:hover { background: #d97706; }
.btn-danger-solid:hover { background: #dc2626; }
.btn-warning:disabled, .btn-danger-solid:disabled { opacity: 0.7; cursor: not-allowed; }
.text-teal { color: #0f766e; }
.text-teal:hover { background: #f0fdfa; }
.text-blue { color: #3b82f6; }
.text-blue:hover { background: #eff6ff; }
.text-danger { color: #ef4444; }
.text-danger:hover { background: #fef2f2; }
.text-orange { color: #d97706; }
.text-orange:hover { background: #fff7ed; }

.table-toolbar { padding: 16px 24px; background: #fffbeb; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.batch-info .badge { background: var(--sports-orange); color: #fff; padding: 2px 8px; border-radius: 12px; font-size: 12px; }
.table-wrapper { overflow-x: auto; }
.modern-table { width: 100%; border-collapse: collapse; text-align: left; }
.modern-table th, .modern-table td { padding: 14px 20px; border-bottom: 1px solid var(--border-color); vertical-align: top; }
.modern-table th { font-size: 13px; color: var(--text-gray); }
.modern-table tbody tr:hover { background: #f8fafc; }
.modern-table tbody tr.is-selected { background: #fef3c7; }
.checkbox-cell { width: 40px; }
.order-info-cell, .receiver-cell, .status-stack { display: flex; flex-direction: column; gap: 4px; }
.order-no { font-weight: 600; font-family: monospace; }
.ellipsis { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.amount-text { font-weight: 700; color: var(--sports-orange); }
.status-badge { display: inline-flex; padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.status-warning { background: #fef3c7; color: #d97706; }
.status-info { background: #e0f2fe; color: #0284c7; }
.status-primary { background: #dbeafe; color: #2563eb; }
.status-success { background: #d1fae5; color: #059669; }
.status-danger { background: #fee2e2; color: #dc2626; }
.status-refunding { background: #fff7ed; color: #c2410c; }
.status-refunded { background: #dcfce7; color: #15803d; }
.refund-text { font-size: 12px; color: #6b7280; }
.refund-error { color: #dc2626; }
.empty-state { text-align: center; color: var(--text-gray); padding: 48px 0 !important; }
.pagination-wrapper { padding: 16px 24px; display: flex; align-items: center; justify-content: flex-end; gap: 16px; }
.page-info span { font-weight: 600; }

.modal-overlay { position: fixed; inset: 0; background: rgba(11, 19, 43, 0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { width: min(560px, calc(100% - 24px)); max-height: 90vh; background: #fff; border-radius: 20px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); overflow: hidden; display: flex; flex-direction: column; }
.modal-sm { width: min(480px, calc(100% - 24px)); }
.modal-header { padding: 20px 24px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.modal-header h4 { margin: 0; }
.btn-close { background: none; border: none; font-size: 24px; color: var(--text-gray); cursor: pointer; }
.modal-body { padding: 24px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }
.detail-grid { background: #f8fafc; border: 1px solid #f1f5f9; border-radius: 10px; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.detail-item { display: flex; gap: 8px; font-size: 14px; }
.detail-item-block { align-items: flex-start; }
.label { width: 84px; color: var(--text-gray); flex-shrink: 0; }
.refund-textarea { min-height: 120px; resize: vertical; }
.text-right { text-align: right; }
.text-gray { color: var(--text-gray); }

@media (max-width: 768px) {
  .page-container { padding: 20px 14px; }
  .page-header { flex-direction: column; align-items: stretch; }
  .filter-actions > *, .modal-footer > * { flex: 1; }
}
</style>
