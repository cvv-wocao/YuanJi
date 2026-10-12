import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/components/Login.vue';
import Register from '@/components/Register.vue';
import Home from '@/views/Home.vue';
import Profile from '@/views/Profile.vue';
import PostDetail from '@/views/PostDetail.vue';
import MyPosts from '@/views/MyPosts.vue';
import FollowList from '@/views/FollowList.vue';
import UserProfile from '@/views/UserProfile.vue';

// 管理员端组件
import AdminLogin from '@/components/admin/AdminLogin.vue';
import AdminLayout from '@/views/admin/AdminLayout.vue';
import AdminDashboard from '@/views/admin/AdminDashboard.vue';
import UserManagement from '@/views/admin/UserManagement.vue';
import AdminManagement from '@/views/admin/AdminManagement.vue';
import PostManagement from '@/views/admin/PostManagement.vue';
import CommentManagement from '@/views/admin/CommentManagement.vue';
import ProductManagement from '@/views/admin/ProductManagement.vue';
import OrderManagement from '@/views/admin/Ordermanagement.vue';
import DocumentManagement from '@/views/admin/DocumentManagement.vue';

//数据大屏
import Dashboard from '../views/Dashboard.vue'  // 导入大屏组件
import { compile } from 'vue';

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  // 用户端路由
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: PostDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/my-posts',
    name: 'MyPosts',
    component: MyPosts,
    meta: { requiresAuth: true }
  },
  {
    path: '/following',
    name: 'Following',
    component: FollowList,
    meta: { requiresAuth: true }
  },
  {
    path: '/followers',
    name: 'Followers',
    component: FollowList,
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: UserProfile,
    meta: { requiresAuth: true }
  },
  // 管理员端路由
  {
    path: '/device-monitor',
    name: 'DeviceMonitor',
    component: () => import('@/views/DeviceMonitor.vue'),
    meta: { requiresAdminAuth: true }  // 需要管理员登录
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: AdminLogin
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAdminAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard,
        //component: Dashboard,  // 使用大屏组件
        meta: { title: '数据总览' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: UserManagement,
        meta: { title: '用户管理' }
      },
      {
        path: 'admins',
        name: 'AdminManagement',
        component: AdminManagement,
        meta: { title: '管理员管理' }
      },
      {
        path: 'posts',
        name: 'PostManagement',
        component: PostManagement,
        meta: { title: '社区动态' }
      },
      {
        path: 'comments',
        name: 'CommentManagement',
        component: CommentManagement,
        meta: { title: '评论管理' }
      },
      {
        path: 'products',
        name: 'ProductManagement',
        component: ProductManagement,
        meta: { title: '商品管理' }
      },
      {
        path: 'orders',
        name: 'OrderManagement',
        component: OrderManagement,
        meta: { title: '订单管理' }
      },
      {
        path: 'devices',
        name: 'DeviceList',
        component: () => import('@/views/admin/DeviceList.vue'),
        meta: { title: '设备管理', requiresAdminAuth: true  }
      },
      {
        path: 'devices/:deviceId',
        name: 'DeviceDetail',
        component: () => import('@/views/admin/DeviceDetail.vue'),
        meta: { title: '设备详情', requiresAdminAuth: true }
      },
      {
        path: 'documents',
        name: 'DocumentManagement',
        component: DocumentManagement,
        meta: { title: 'AI文件管理' }
      },
      {
        path: 'bigdata',
        name: 'BigDataScreen',
        component: () => import('@/views/admin/BigDataScreen.vue'),
        meta: { title: '数据大屏' }
      }
    ]
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫：检查权限
router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
  const isAdminLoggedIn = localStorage.getItem('adminLoggedIn') === 'true';
  
  // 用户端权限检查
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login');
    return;
  }
  
  // 管理员端权限检查
  if (to.meta.requiresAdminAuth && !isAdminLoggedIn) {
    next('/admin/login');
    return;
  }
  
  // 已登录用户访问登录页，跳转到首页
  if (to.path === '/login' && isLoggedIn) {
    next('/home');
    return;
  }
  
  // 已登录管理员访问管理员登录页，跳转到控制台
  if (to.path === '/admin/login' && isAdminLoggedIn) {
    next('/admin/dashboard');
    return;
  }
  
  next();
});

export default router;
