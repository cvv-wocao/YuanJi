<template>
  <div class="admin-shell">
    <Transition name="overlay-fade">
      <div
        v-if="isMobile && sidebarOpen"
        class="sidebar-overlay"
        @click="sidebarOpen = false"
      ></div>
    </Transition>

    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-top">
        <div class="brand-card">
          <div class="brand-mark">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M12 2.5C7.31 2.5 3.5 6.31 3.5 11c0 4.05 2.84 7.43 6.64 8.27V14.2H8.3V11h1.84V8.56c0-1.82 1.08-2.82 2.74-2.82.8 0 1.64.14 1.64.14v1.8h-.93c-.92 0-1.21.57-1.21 1.16V11h2.06l-.33 3.2h-1.73v5.07c3.8-.84 6.64-4.22 6.64-8.27 0-4.69-3.81-8.5-8.5-8.5Z"
              />
            </svg>
          </div>
          <div>
            <p class="eyebrow">Remote Health</p>
            <h1>管理员控制台</h1>
          </div>
        </div>

        <button v-if="isMobile" class="sidebar-close" type="button" @click="sidebarOpen = false">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div class="sidebar-panel">
        <div class="panel-kicker">智能巡检中</div>
        <p class="panel-title">今天建议优先关注异常订单和评论审核。</p>
      </div>

      <nav class="menu">
        <div v-for="section in menuSections" :key="section.label" class="menu-section">
          <p class="menu-caption">{{ section.label }}</p>
          <button
            v-for="item in section.items"
            :key="item.path"
            type="button"
            class="menu-item"
            :class="{ active: currentPath === item.path }"
            @click="navigateTo(item.path)"
          >
            <span class="menu-icon" :class="`icon-${item.icon}`">
              <svg v-if="item.icon === 'dashboard'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 13h8V3H3v10Zm10 8h8V11h-8v10ZM3 21h8v-6H3v6Zm10-10h8V3h-8v8Z" />
              </svg>
              <svg v-else-if="item.icon === 'users'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 11c1.66 0 2.99-1.79 2.99-4S17.66 3 16 3s-3 1.79-3 4 1.34 4 3 4Zm-8 0c1.66 0 3-1.79 3-4S9.66 3 8 3 5 4.79 5 7s1.34 4 3 4Zm0 2c-2.33 0-7 1.17-7 3.5V20h14v-3.5C15 14.17 10.33 13 8 13Zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.94 1.97 3.45V20h6v-3.5c0-2.33-4.67-3.5-7-3.5Z" />
              </svg>
              <svg v-else-if="item.icon === 'shield'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="m12 2 7 3v6c0 5-3.07 9.74-7 11-3.93-1.26-7-6-7-11V5l7-3Zm0 6v4m0 4h.01" />
              </svg>
              <svg v-else-if="item.icon === 'posts'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H8l-5 3V6a2 2 0 0 1 2-2Zm3 4h8m-8 4h8m-8 4h5" />
              </svg>
              <svg v-else-if="item.icon === 'comments'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 5h16v10H7l-3 3V5Zm4 3h8m-8 3h5" />
              </svg>
              <svg v-else-if="item.icon === 'products'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 2 3 6.5l9 4.5 9-4.5L12 2Zm-9 7.5 9 4.5 9-4.5V17L12 22l-9-5V9.5Z" />
              </svg>
              <svg v-else-if="item.icon === 'ai'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
              </svg>
              <svg v-else-if="item.icon === 'chart'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 3v18h18V3H3Zm16 16H5V5h14v14ZM7 12h2v5H7v-5Zm4-3h2v8h-2V9Zm4-4h2v12h-2V5Z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 7h18M7 3v4m10-4v4M5 11h14v10H5V11Zm3 3h3m2 0h3m-8 4h3" />
              </svg>
            </span>
            <span class="menu-copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.description }}</small>
            </span>
          </button>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="footer-card">
          <span>系统版本</span>
          <strong>Admin Suite 2.6</strong>
        </div>
      </div>
    </aside>

    <div class="content-shell">
      <header class="topbar">
        <div class="topbar-left">
          <button v-if="isMobile" type="button" class="menu-toggle" @click="sidebarOpen = true">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>
          <div>
            <p class="topbar-label">运营中枢</p>
            <h2>{{ pageTitle }}</h2>
          </div>
        </div>

        <div class="topbar-right">
          <div class="status-chip">
            <span class="status-dot"></span>
            <span>{{ currentTime }}</span>
          </div>
          <div class="admin-card">
            <img
              src="https://api.dicebear.com/7.x/initials/svg?seed=Admin&backgroundColor=0f8c86&fontSize=40"
              alt="管理员头像"
              class="avatar"
            />
            <div>
              <strong>{{ adminUsername }}</strong>
              <small>系统管理员</small>
            </div>
          </div>
          <button type="button" class="logout-btn" @click="handleLogout">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M10 17 15 12 10 7M15 12H3m7-9h6a2 2 0 0 1 2 2v3m0 8v3a2 2 0 0 1-2 2h-6" />
            </svg>
            退出
          </button>
        </div>
      </header>

      <main class="page-stage">
        <router-view v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();
const route = useRoute();

const adminUsername = ref(localStorage.getItem('adminUsername') || 'Admin');
const sidebarOpen = ref(true);
const isMobile = ref(false);
const currentTime = ref('');

const menuSections = [
  {
    label: '总览',
    items: [
      {
        path: '/admin/dashboard',
        label: '数据总览',
        description: '查看平台运行概况',
        icon: 'dashboard'
      },
      {
        path: '/admin/bigdata',
        label: '数据大屏',
        description: '实时数据可视化大屏',
        icon: 'chart'
      }
    ]
  },
  {
    label: '账号管理',
    items: [
      {
        path: '/admin/users',
        label: '用户管理',
        description: '维护用户资料与状态',
        icon: 'users'
      },
      {
        path: '/admin/admins',
        label: '管理员管理',
        description: '分配权限与职责',
        icon: 'shield'
      }
    ]
  },
  {
    label: '业务管理',
    items: [
      {
        path: '/admin/posts',
        label: '社区动态',
        description: '审核帖子与内容安全',
        icon: 'posts'
      },
      {
        path: '/admin/comments',
        label: '评论管理',
        description: '清理互动与异常评论',
        icon: 'comments'
      },
      {
        path: '/admin/products',
        label: '商品管理',
        description: '维护商品与库存展示',
        icon: 'products'
      },
      {
        path: '/admin/orders',
        label: '订单管理',
        description: '跟进交易与履约状态',
        icon: 'orders'
      },
      {
        path: '/admin/devices', 
        label: '设备管理', 
        description: '监控树莓派设备状态',
        icon: 'users' 
      },
      {
        path: '/admin/documents',
        label: 'AI文件管理',
        description: '管理运动知识文档',
        icon: 'ai'
      }
    ]
  }
];

const currentPath = computed(() => route.path);
const pageTitle = computed(() => route.meta.title || '管理员控制台');

const syncViewport = () => {
  const mobile = window.innerWidth < 1080;
  isMobile.value = mobile;
  sidebarOpen.value = mobile ? false : true;
};

const updateClock = () => {
  const now = new Date();
  currentTime.value = now.toLocaleString('zh-CN', {
    hour12: false,
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const navigateTo = (path) => {
  if (route.path !== path) {
    router.push(path);
  }
};

const handleLogout = () => {
  localStorage.removeItem('adminLoggedIn');
  localStorage.removeItem('adminToken');
  localStorage.removeItem('adminUsername');
  router.push('/admin/login');
};

watch(
  () => route.path,
  () => {
    if (isMobile.value) {
      sidebarOpen.value = false;
    }
  }
);

let clockTimer = null;

onMounted(() => {
  syncViewport();
  updateClock();
  window.addEventListener('resize', syncViewport);
  clockTimer = window.setInterval(updateClock, 60000);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport);
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
});
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  position: relative;
  background:
    radial-gradient(circle at left top, rgba(15, 140, 134, 0.08), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.35) 0%, rgba(238, 243, 242, 0.8) 100%);
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 24px 18px;
  background: linear-gradient(180deg, rgba(17, 40, 45, 0.96) 0%, rgba(23, 50, 56, 0.97) 100%);
  color: var(--text-light);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  z-index: 20;
}

.sidebar-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.brand-card {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: white;
  background: linear-gradient(135deg, var(--sports-teal) 0%, #40b0a5 100%);
  box-shadow: 0 18px 30px rgba(15, 140, 134, 0.28);
}

.brand-mark svg {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(237, 246, 246, 0.62);
}

.brand-card h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.sidebar-close {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: rgba(237, 246, 246, 0.8);
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
}

.sidebar-close svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
}

.sidebar-panel {
  margin-top: 26px;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.04));
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.panel-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: #b8d8d5;
}

.panel-title {
  margin: 12px 0 0;
  color: rgba(237, 246, 246, 0.88);
  font-size: 14px;
  line-height: 1.7;
}

.menu {
  flex: 1;
  margin-top: 28px;
  overflow-y: auto;
  padding-right: 6px;
}

.menu-section + .menu-section {
  margin-top: 24px;
}

.menu-caption {
  margin: 0 0 12px 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(237, 246, 246, 0.42);
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 18px;
  color: rgba(237, 246, 246, 0.82);
  cursor: pointer;
  transition: var(--transition);
  text-align: left;
}

.menu-item + .menu-item {
  margin-top: 8px;
}

.menu-item:hover {
  transform: translateX(4px);
  background: rgba(255, 255, 255, 0.08);
}

.menu-item.active {
  background: linear-gradient(135deg, rgba(15, 140, 134, 0.25), rgba(255, 255, 255, 0.08));
  box-shadow: inset 0 0 0 1px rgba(124, 227, 214, 0.18);
}

.menu-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.08);
}

.menu-icon svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.icon-dashboard,
.icon-users,
.icon-shield,
.icon-posts,
.icon-comments,
.icon-products,
.icon-orders {
  color: #d9f4f1;
}

.menu-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.menu-copy strong {
  font-size: 15px;
  font-weight: 600;
}

.menu-copy small {
  color: rgba(237, 246, 246, 0.52);
  font-size: 12px;
}

.sidebar-footer {
  padding-top: 20px;
}

.footer-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: rgba(237, 246, 246, 0.72);
}

.footer-card strong {
  color: #ffffff;
}

.content-shell {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 22px 28px 18px;
  backdrop-filter: blur(18px);
  background: rgba(246, 248, 247, 0.72);
  border-bottom: 1px solid rgba(23, 50, 56, 0.08);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.topbar-label {
  margin: 0;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: var(--text-gray);
}

.topbar h2 {
  margin: 4px 0 0;
  font-size: 30px;
  line-height: 1.1;
}

.menu-toggle {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
}

.menu-toggle svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: var(--text-dark);
  stroke-width: 1.8;
  stroke-linecap: round;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 11px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-gray);
  box-shadow: var(--shadow-sm);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #34c759;
  box-shadow: 0 0 0 6px rgba(52, 199, 89, 0.12);
}

.admin-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-sm);
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  object-fit: cover;
}

.admin-card strong,
.admin-card small {
  display: block;
}

.admin-card small {
  margin-top: 3px;
  color: var(--text-gray);
  font-size: 12px;
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(239, 141, 92, 0.12);
  color: #b95a27;
  cursor: pointer;
  transition: var(--transition);
}

.logout-btn:hover {
  transform: translateY(-2px);
  background: rgba(239, 141, 92, 0.18);
}

.logout-btn svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.page-stage {
  min-width: 0;
  padding: 24px 28px 32px;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(9, 18, 22, 0.42);
  backdrop-filter: blur(4px);
  z-index: 15;
}

.page-fade-enter-active,
.page-fade-leave-active,
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.24s ease;
}

.page-fade-enter-from,
.page-fade-leave-to,
.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1079px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    width: min(320px, 88vw);
    transform: translateX(-100%);
    transition: transform 0.28s ease;
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .topbar {
    padding-inline: 20px;
  }

  .page-stage {
    padding-inline: 20px;
  }
}

@media (max-width: 820px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .topbar-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .topbar h2 {
    font-size: 26px;
  }
}

@media (max-width: 640px) {
  .page-stage {
    padding: 18px 14px 24px;
  }

  .topbar {
    padding: 18px 14px 14px;
  }

  .status-chip,
  .admin-card,
  .logout-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
