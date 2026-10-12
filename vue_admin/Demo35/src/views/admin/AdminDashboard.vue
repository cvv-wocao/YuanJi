<template>
  <div class="dashboard-page">
    <!-- Hero 区域 -->
    <section class="hero-card">
      <div class="hero-copy">
        <p class="hero-kicker">运营指挥台</p>
        <h3>欢迎回来，{{ adminUsername }}</h3>
        <p class="hero-text">
          今天的后台重点已经为你聚合完成。你可以从用户增长、社区内容、商品与履约四条线快速进入处理状态。
        </p>
        <div class="hero-actions">
          <button type="button" class="primary-action" @click="navigateTo('/admin/orders')">查看待处理订单</button>
          <button type="button" class="secondary-action" @click="navigateTo('/admin/comments')">进入评论审核</button>
        </div>
      </div>

      <div class="hero-metrics">
        <div class="metric-tile">
          <span>当前看板</span>
          <strong>{{ overviewScore }}</strong>
          <small>综合运行指数</small>
        </div>
        <div class="metric-grid">
          <div v-for="item in pulseCards" :key="item.label" class="pulse-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.note }}</small>
          </div>
        </div>
      </div>
    </section>

    <!-- 统计卡片（实时更新） -->
    <section class="stats-grid">
      <article v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-head">
          <span class="stat-icon" :class="card.tone">{{ card.short }}</span>
          <span class="stat-tag">{{ card.tag }}</span>
        </div>
        <p class="stat-label">{{ card.label }}</p>
        <strong class="stat-value">{{ card.value }}</strong>
        <small class="stat-desc">{{ card.desc }}</small>
        <!-- 实时变化提示 -->
        <transition name="fade">
          <div v-if="card.flashMessage" class="stat-flash">
            {{ card.flashMessage }}
          </div>
        </transition>
      </article>
    </section>

    <!-- 内容网格 -->
    <section class="content-grid">
      <article class="panel panel-wide">
        <div class="panel-header">
          <div>
            <p class="panel-kicker">重点任务</p>
            <h4>今日运营节奏</h4>
          </div>
        </div>
        <div class="timeline">
          <div v-for="item in timeline" :key="item.title" class="timeline-item">
            <span class="timeline-time">{{ item.time }}</span>
            <div class="timeline-card">
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <p class="panel-kicker">快捷入口</p>
            <h4>高频管理动作</h4>
          </div>
        </div>
        <div class="quick-actions">
          <button
            v-for="action in quickActions"
            :key="action.label"
            type="button"
            class="quick-action"
            @click="navigateTo(action.path)"
          >
            <strong>{{ action.label }}</strong>
            <span>{{ action.desc }}</span>
          </button>
        </div>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <p class="panel-kicker">运行观察</p>
            <h4>系统提醒</h4>
          </div>
        </div>
        <div class="notice-list">
          <div v-for="notice in notices" :key="notice.title" class="notice-item">
            <span class="notice-dot"></span>
            <div>
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.desc }}</p>
            </div>
          </div>
        </div>
      </article>
    </section>

    <!-- Kafka 实时事件流面板 -->
    <section class="realtime-panel">
      <div class="panel-header">
        <div>
          <p class="panel-kicker">Kafka 实时总线</p>
          <h4>实时事件流</h4>
        </div>
        <div class="ws-status" :class="{ connected: isConnected }">
          {{ isConnected ? '● 已连接' : '○ 未连接' }}
        </div>
      </div>
      <div class="event-list">
        <div v-if="events.length === 0" class="event-empty">暂无事件，等待推送…</div>
        <div
          v-for="(event, idx) in events"
          :key="idx"
          class="event-item"
        >
          <div class="event-meta">
            <span class="event-type" :class="getEventTypeClass(event.eventType)">
              {{ event.eventType }}
            </span>
            <span class="event-time">{{ formatTime(event.timestamp) }}</span>
          </div>
          <div class="event-detail">
            <template v-if="event.user">
              用户：{{ event.user.name }} ({{ event.user.number }})
            </template>
            <template v-else-if="event.post">
              帖子：{{ truncate(event.post.content, 50) }} (作者ID: {{ event.post.userId }})
            </template>
            <template v-else-if="event.product">
              商品：{{ event.product.name || event.product.content }}
            </template>
            <template v-else-if="event.order">
              订单：{{ event.order.orderNo || event.order.id }} - 金额 ¥{{ event.order.amount || event.order.paymentAmount }}
            </template>
            <template v-else-if="event.category">
              商品分类：{{ event.category.name }}
            </template>
            <template v-else>
              {{ JSON.stringify(event) }}
            </template>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  getAllUsers,
  getAllAdmins,
  getAllPosts,
  getAllComments,
  getAllProducts,
  getAllOrders,
  getProductCategories
} from '@/api/admin';

const router = useRouter();
const adminUsername = ref(localStorage.getItem('adminUsername') || 'Admin');

// ========== 统计数据 ==========
const stats = ref({
  users: 0,
  admins: 0,
  posts: 0,
  comments: 0,
  products: 0,
  orders: 0,
  categories: 0
});

// 每个卡片对应的闪动消息
const flashMessages = ref({
  users: '',
  admins: '',
  posts: '',
  comments: '',
  products: '',
  orders: '',
  categories: ''
});

const showFlash = (key, message) => {
  flashMessages.value[key] = message;
  setTimeout(() => {
    if (flashMessages.value[key] === message) {
      flashMessages.value[key] = '';
    }
  }, 4000);
};

// ========== WebSocket 实时事件 ==========
const ws = ref(null);
const isConnected = ref(false);
const events = ref([]);

// ========== 计算属性 ==========
const quickActions = [
  { label: '处理订单', desc: '优先处理待发货与异常状态', path: '/admin/orders' },
  { label: '审核评论', desc: '清理风险内容与垃圾互动', path: '/admin/comments' },
  { label: '管理商品', desc: '检查上新、推荐和库存展示', path: '/admin/products' },
  { label: '查看用户', desc: '排查用户信息与账号状态', path: '/admin/users' }
];

const timeline = computed(() => [
  {
    time: '09:00',
    title: '检查平台概览',
    desc: `当前共接入 ${stats.value.users} 位用户和 ${stats.value.admins} 位管理员，先确认整体数据是否有异常波动。`
  },
  {
    time: '11:00',
    title: '处理社区内容',
    desc: `社区当前累计 ${stats.value.posts} 条动态、${stats.value.comments} 条评论，建议优先清理敏感内容。`
  },
  {
    time: '15:00',
    title: '检查商品与履约',
    desc: `商品池当前展示 ${stats.value.products} 个条目，确认推荐位和订单链路是否正常。`
  }
]);

const notices = computed(() => [
  {
    title: '用户活跃数据已同步',
    desc: `用户总量 ${stats.value.users}，建议结合新增趋势继续观察注册转化。`
  },
  {
    title: '社区内容审核待跟进',
    desc: `当前可追踪内容总数 ${stats.value.posts + stats.value.comments}，评论区需要持续巡检。`
  },
  {
    title: '商品信息可继续优化',
    desc: `商品池共有 ${stats.value.products} 条记录，可重点检查推荐位和热销位。`
  }
]);

const overviewScore = computed(() => {
  const score = 82 + Math.min(12, Math.floor((stats.value.comments + stats.value.posts) / 50));
  return `${Math.min(score, 98)}%`;
});

const pulseCards = computed(() => [
  { label: '内容池', value: stats.value.posts + stats.value.comments, note: '帖子与评论' },
  { label: '商品池', value: stats.value.products, note: '商品展示中' }
]);

const statCards = computed(() => [
  {
    label: '平台用户', value: stats.value.users, desc: '可用于跟踪用户规模与基础增长',
    short: 'U', tag: '增长', tone: 'tone-teal',
    flashMessage: flashMessages.value.users
  },
  {
    label: '管理员席位', value: stats.value.admins, desc: '用于分配后台权限与巡检任务',
    short: 'A', tag: '协作', tone: 'tone-slate',
    flashMessage: flashMessages.value.admins
  },
  {
    label: '社区动态', value: stats.value.posts, desc: '需要重点巡检图片、文案与互动安全',
    short: 'P', tag: '内容', tone: 'tone-orange',
    flashMessage: flashMessages.value.posts
  },
  {
    label: '互动评论', value: stats.value.comments, desc: '评论增量越高，越要关注风控与审核',
    short: 'C', tag: '审核', tone: 'tone-rose',
    flashMessage: flashMessages.value.comments
  },
  {
    label: '商品总数', value: stats.value.products, desc: '建议结合订单页检查推荐和上新节奏',
    short: 'G', tag: '商品', tone: 'tone-cyan',
    flashMessage: flashMessages.value.products
  },
  {
    label: '订单总数', value: stats.value.orders, desc: '实时追踪订单创建与取消情况',
    short: 'O', tag: '交易', tone: 'tone-purple',
    flashMessage: flashMessages.value.orders
  },
  {
    label: '商品分类', value: stats.value.categories, desc: '商品分类数量动态变化',
    short: 'C', tag: '分类', tone: 'tone-indigo',
    flashMessage: flashMessages.value.categories
  }
]);

// ========== 实时事件处理：更新统计数据并显示闪动消息 ==========
const handleRealtimeEvent = (event) => {
  const { eventType, user, post, product, order, category } = event;

  // 用户事件
  if (user) {
    if (eventType === 'REGISTER') {
      stats.value.users++;
      showFlash('users', `🎉 新用户注册：${user.name}`);
    } else if (eventType === 'DELETE') {
      stats.value.users = Math.max(0, stats.value.users - 1);
      showFlash('users', `⚠️ 用户注销：${user.name}`);
    } else if (eventType === 'UPDATE') {
      showFlash('users', `✏️ 用户信息更新：${user.name}`);
    }
  }

  // 帖子事件
  if (post) {
    if (eventType === 'post_created') {
      stats.value.posts++;
      showFlash('posts', `📝 新帖发布：${truncate(post.content, 20)}`);
    } else if (eventType === 'post_delete') {
      stats.value.posts = Math.max(0, stats.value.posts - 1);
      showFlash('posts', `🗑️ 帖子被删除`);
    } else if (eventType === 'post_update') {
      showFlash('posts', `✏️ 帖子内容更新`);
    }
  }

  // 商品事件
  if (product) {
    if (eventType === 'CREATE_PRODUCT') {
      stats.value.products++;
      showFlash('products', `🆕 新商品上架：${product.name}`);
    } else if (eventType === 'DELETE_PRODUCT') {
      stats.value.products = Math.max(0, stats.value.products - 1);
      showFlash('products', `🗑️ 商品已下架：${product.name}`);
    } else if (eventType === 'UPDATE_PRODUCT') {
      showFlash('products', `✏️ 商品信息更新：${product.name}`);
    } else if (eventType === 'SET_HOT') {
      showFlash('products', `🔥 商品设为热门：${product.name}`);
    } else if (eventType === 'SET_NEW') {
      showFlash('products', `✨ 商品设为新品：${product.name}`);
    } else if (eventType === 'SET_RECOMMEND') {
      showFlash('products', `👍 商品设为推荐：${product.name}`);
    }
  }

  // 订单事件
  if (order) {
    if (eventType === 'CreateOrder') {
      stats.value.orders++;
      showFlash('orders', `🛒 新订单创建：${order.orderNo || order.id}`);
    } else if (eventType === 'CancelOrder') {
      stats.value.orders = Math.max(0, stats.value.orders - 1);
      showFlash('orders', `❌ 订单取消：${order.orderNo || order.id}`);
    } else if (eventType === 'PayOrder') {
      showFlash('orders', `💰 订单支付成功：${order.orderNo || order.id}`);
    } else if (eventType === 'ConfirmOrder') {
      showFlash('orders', `✅ 订单确认收货：${order.orderNo || order.id}`);
    } else if (eventType === 'ApproveRefund') {
      showFlash('orders', `↩️ 退款已批准：${order.orderNo || order.id}`);
    } else if (eventType === 'RejectRefund') {
      showFlash('orders', `🚫 退款被拒绝：${order.orderNo || order.id}`);
    }
  }

  // 分类事件
  if (category) {
    if (eventType === 'ADD_CATEGORY') {
      stats.value.categories++;
      showFlash('categories', `📂 新增分类：${category.name}`);
    } else if (eventType === 'DELETE_CATEGORY') {
      stats.value.categories = Math.max(0, stats.value.categories - 1);
      showFlash('categories', `🗑️ 删除分类：${category.name}`);
    }
  }

  // 评论事件（若后端有发送）
  if (eventType === 'NEW_COMMENT') {
    stats.value.comments++;
    showFlash('comments', `💬 新增评论`);
  } else if (eventType === 'DELETE_COMMENT') {
    stats.value.comments = Math.max(0, stats.value.comments - 1);
    showFlash('comments', `🗑️ 评论被删除`);
  }
};

// ========== 数据加载 ==========
const loadDashboard = async () => {
  try {
    // 并行请求所有数据
    const [
      usersRes, adminsRes, postsRes, commentsRes,
      productsRes, ordersRes, categoriesRes
    ] = await Promise.all([
      getAllUsers(),
      getAllAdmins(),
      getAllPosts(1, 1000),
      getAllComments(1, 1000),
      getAllProducts({ page: 1, pageSize: 1000 }),
      getAllOrders({ page: 1, pageSize: 1 }),   // 仅获取总数，取 total 字段
      getProductCategories()
    ]);

    if (usersRes.success) stats.value.users = usersRes.data?.length || 0;
    if (adminsRes.success) stats.value.admins = adminsRes.data?.length || 0;
    if (postsRes.success) {
      const posts = postsRes.data?.posts || postsRes.data || [];
      stats.value.posts = Array.isArray(posts) ? posts.length : 0;
    }
    if (commentsRes.success) {
      const comments = commentsRes.data?.comments || commentsRes.data || [];
      stats.value.comments = Array.isArray(comments) ? comments.length : 0;
    }
    if (productsRes.success) {
      const products = productsRes.data?.list || productsRes.data?.records || productsRes.data || [];
      stats.value.products = Array.isArray(products) ? products.length : 0;
    }
    // 订单总数：优先取 total 字段，否则取数组长度
    if (ordersRes.success) {
      const total = ordersRes.data?.total ?? (Array.isArray(ordersRes.data) ? ordersRes.data.length : 0);
      stats.value.orders = total;
    }
    // 分类总数
    if (categoriesRes.success) {
      const categories = categoriesRes.data?.list || categoriesRes.data || [];
      stats.value.categories = Array.isArray(categories) ? categories.length : 0;
    }
  } catch (error) {
    console.error('加载管理员首页数据失败:', error);
  }
};

// ========== WebSocket 连接 ==========
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  return new Date(timestamp).toLocaleTimeString();
};

const truncate = (text, len) => {
  if (!text) return '';
  return text.length > len ? text.slice(0, len) + '…' : text;
};

const getEventTypeClass = (type) => {
  const map = {
    REGISTER: 'type-user', DELETE: 'type-user', UPDATE: 'type-user',
    post_created: 'type-post', post_update: 'type-post', post_delete: 'type-post',
    CREATE_PRODUCT: 'type-product', DELETE_PRODUCT: 'type-product', UPDATE_PRODUCT: 'type-product',
    SET_HOT: 'type-product', SET_NEW: 'type-product', SET_RECOMMEND: 'type-product',
    CreateOrder: 'type-order', CancelOrder: 'type-order', PayOrder: 'type-order',
    ConfirmOrder: 'type-order', ApproveRefund: 'type-order', RejectRefund: 'type-order',
    ADD_CATEGORY: 'type-category', DELETE_CATEGORY: 'type-category',
    NEW_COMMENT: 'type-comment', DELETE_COMMENT: 'type-comment'
  };
  return map[type] || 'type-default';
};

const connectWebSocket = () => {
  const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080';
  const endpoint = '/ws/dashboard';
  const fullUrl = `${wsUrl}${endpoint}`;
  ws.value = new WebSocket(fullUrl);

  ws.value.onopen = () => {
    console.log('Kafka 实时总线已连接');
    isConnected.value = true;
  };

  ws.value.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log('收到实时事件:', data);
      events.value.unshift(data);
      if (events.value.length > 30) events.value.pop();
      handleRealtimeEvent(data);
    } catch (err) {
      console.error('解析事件失败', err);
    }
  };

  ws.value.onerror = (err) => {
    console.error('WebSocket 错误', err);
    isConnected.value = false;
  };

  ws.value.onclose = () => {
    console.log('WebSocket 断开，3秒后重连...');
    isConnected.value = false;
    setTimeout(() => connectWebSocket(), 3000);
  };
};

const navigateTo = (path) => router.push(path);

onMounted(() => {
  loadDashboard();
  connectWebSocket();
});

onUnmounted(() => {
  if (ws.value) ws.value.close();
});
</script>

<style scoped>
/* 原有样式请保留，此处仅补充新增样式和缺失部分 */

.dashboard-page {
  display: grid;
  gap: 22px;
}

/* Hero 卡片样式 */
.hero-card {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 20px;
  padding: 30px;
  border-radius: 30px;
  background: radial-gradient(circle at top left, rgba(255, 255, 255, 0.24), transparent 28%),
              linear-gradient(135deg, #174a56 0%, #0f8c86 55%, #74c1af 100%);
  color: white;
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}
.hero-copy, .hero-metrics { position: relative; z-index: 1; }
.hero-kicker, .panel-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.hero-kicker { color: rgba(255, 255, 255, 0.7); }
.hero-copy h3 { margin: 10px 0 14px; font-size: 34px; line-height: 1.12; }
.hero-text {
  margin: 0;
  max-width: 560px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.86);
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}
.primary-action, .secondary-action {
  min-height: 48px;
  padding: 0 18px;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}
.primary-action {
  background: white;
  color: #174a56;
}
.secondary-action {
  background: rgba(255, 255, 255, 0.12);
  color: white;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}
.primary-action:hover, .secondary-action:hover {
  transform: translateY(-2px);
}
.hero-metrics {
  display: grid;
  gap: 16px;
}
.metric-tile {
  padding: 20px;
  border-radius: 24px;
  background: rgba(10, 36, 41, 0.24);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}
.metric-tile span, .pulse-card span { color: rgba(255, 255, 255, 0.74); font-size: 13px; }
.metric-tile strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 46px;
  line-height: 1;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.pulse-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}
.pulse-card strong {
  display: block;
  margin: 10px 0 4px;
  font-size: 30px;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 18px;
}
.stat-card, .panel {
  padding: 22px;
  border-radius: 26px;
  background: white;
  border: 1px solid rgba(23, 50, 56, 0.08);
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-weight: 700;
}
.tone-teal { background: rgba(15, 140, 134, 0.14); color: #0f8c86; }
.tone-slate { background: rgba(23, 50, 56, 0.1); color: #174a56; }
.tone-orange { background: rgba(239, 141, 92, 0.14); color: #c76c36; }
.tone-rose { background: rgba(210, 91, 112, 0.14); color: #c04f66; }
.tone-cyan { background: rgba(76, 174, 188, 0.14); color: #1e8fa0; }
.tone-purple { background: rgba(139, 92, 246, 0.14); color: #8b5cf6; }
.tone-indigo { background: rgba(99, 102, 241, 0.14); color: #6366f1; }
.stat-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(23, 50, 56, 0.06);
  color: #5f6b7a;
  font-size: 12px;
}
.stat-label {
  margin: 18px 0 10px;
  color: #5f6b7a;
  font-size: 14px;
}
.stat-value {
  display: block;
  font-size: 34px;
  line-height: 1;
}
.stat-desc {
  display: block;
  margin-top: 12px;
  color: #5f6b7a;
  line-height: 1.7;
}
.stat-flash {
  margin-top: 10px;
  padding: 6px 10px;
  background: #fff3e0;
  border-radius: 20px;
  font-size: 12px;
  color: #d97706;
  text-align: center;
  animation: fadeInOut 2s ease;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
@keyframes fadeInOut {
  0% { opacity: 0; transform: translateY(5px); }
  15% { opacity: 1; transform: translateY(0); }
  85% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-5px); }
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) repeat(2, minmax(280px, 1fr));
  gap: 18px;
}
.panel-wide { grid-row: span 2; }
.panel-header h4 { margin: 6px 0 0; font-size: 24px; }
.panel-kicker { color: #5f6b7a; }
.timeline {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}
.timeline-item {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}
.timeline-time {
  padding-top: 12px;
  color: #5f6b7a;
  font-size: 13px;
}
.timeline-card {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255,255,255,0.92), rgba(15,140,134,0.04));
  border: 1px solid rgba(15,140,134,0.08);
}
.timeline-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 16px;
}
.timeline-card p, .notice-item p {
  margin: 0;
  color: #5f6b7a;
  line-height: 1.75;
}
.quick-actions {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}
.quick-action {
  padding: 18px;
  border-radius: 20px;
  text-align: left;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(15,140,134,0.08), white);
  border: 1px solid rgba(15,140,134,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}
.quick-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.quick-action strong, .quick-action span { display: block; }
.quick-action span {
  margin-top: 8px;
  color: #5f6b7a;
  line-height: 1.7;
}
.notice-list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}
.notice-item {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}
.notice-item strong {
  display: block;
  margin-bottom: 6px;
  font-size: 15px;
}
.notice-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0f8c86, #ef8d5c);
}

/* 实时事件面板 */
.realtime-panel {
  background: white;
  border-radius: 26px;
  border: 1px solid rgba(23, 50, 56, 0.08);
  padding: 22px;
}
.ws-status {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 40px;
  background: #f0f2f5;
  color: #999;
}
.ws-status.connected {
  background: #e6f7e6;
  color: #2ecc71;
}
.event-list {
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
}
.event-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.event-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.event-type {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 500;
}
.type-user { background: #e6f7ff; color: #1890ff; }
.type-post { background: #f6ffed; color: #52c41a; }
.type-comment { background: #fff7e6; color: #fa8c16; }
.type-order { background: #f9f0ff; color: #722ed1; }
.type-product { background: #fef0f0; color: #f5222d; }
.type-category { background: #e6fffb; color: #13c2c2; }
.type-default { background: #f5f5f5; color: #666; }
.event-time { font-size: 12px; color: #999; }
.event-detail { font-size: 14px; color: #333; word-break: break-all; }
.event-empty {
  text-align: center;
  padding: 40px 0;
  color: #bbb;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
@media (max-width: 1080px) {
  .hero-card { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .content-grid { grid-template-columns: 1fr; }
  .panel-wide { grid-row: auto; }
}
@media (max-width: 860px) {
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metric-grid { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .hero-card, .stat-card, .panel { padding: 18px; border-radius: 22px; }
  .hero-copy h3 { font-size: 28px; }
  .stats-grid { grid-template-columns: 1fr; }
  .timeline-item { grid-template-columns: 1fr; }
}
</style>