/**
 * 路由与守卫：商城（/）与管理台（/admin）两套 layout。
 *
 * - 商城游客可浏览（与后端 permitAll 的商品/分类/套餐/评价只读一致）；
 * - meta.requiresAuth：需要登录的商城页面（收藏/优惠券/下单等迁移时使用）；
 * - meta.requiresAdmin：管理台全部页面，未登录跳 /admin/login，非 ADMIN 拒绝进入；
 * - 管理台路由全部懒加载，商城首屏不受管理台代码体积影响。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // 锚点滚动：顶栏/hero 的 #categories|#products|#bundles|#reviews 跳转后平滑定位到对应区块
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/ShopLayout.vue'),
      children: [
        {
          path: '',
          name: 'shop-home',
          component: () => import('@/views/shop/HomeView.vue'),
        },
        {
          path: 'login',
          name: 'shop-login',
          component: () => import('@/views/shop/LoginView.vue'),
        },
        {
          path: 'checkout',
          name: 'shop-checkout',
          component: () => import('@/views/shop/CheckoutView.vue'),
          meta: { title: '确认订单' },
        },
        {
          path: 'orders',
          name: 'shop-orders',
          component: () => import('@/views/shop/OrdersView.vue'),
          meta: { title: '我的订单' },
        },
        {
          path: 'favorites',
          name: 'shop-favorites',
          component: () => import('@/views/shop/FavoritesView.vue'),
          meta: { title: '我的收藏', requiresAuth: true },
        },
        {
          path: 'coupons',
          name: 'shop-coupons',
          component: () => import('@/views/shop/CouponsView.vue'),
          meta: { title: '我的优惠券', requiresAuth: true },
        },
        {
          path: 'me',
          name: 'shop-me',
          component: () => import('@/views/shop/AccountView.vue'),
          meta: { title: '个人中心', requiresAuth: true },
        },
        // 商城其余页面（商品详情弹窗等）迁移时在此追加
      ],
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      meta: { requiresAdmin: true, guestAllowed: true },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: { name: 'admin-dashboard' },
        },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { title: '数据看板' },
        },
        {
          path: 'products',
          name: 'admin-products',
          component: () => import('@/views/admin/ProductsView.vue'),
          meta: { title: '商品管理' },
        },
        {
          path: 'bundles',
          name: 'admin-bundles',
          component: () => import('@/views/admin/BundlesView.vue'),
          meta: { title: '场景套餐管理' },
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/views/admin/OrdersView.vue'),
          meta: { title: '订单管理' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UsersView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'reviews',
          name: 'admin-reviews',
          component: () => import('@/views/admin/ReviewsView.vue'),
          meta: { title: '评价管理' },
        },
        {
          path: 'coupons',
          name: 'admin-coupons',
          component: () => import('@/views/admin/CouponsView.vue'),
          meta: { title: '优惠券管理' },
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('@/views/admin/LogsView.vue'),
          meta: { title: '日志管理' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAdmin && !to.meta.guestAllowed) {
    if (!auth.isLoggedIn) {
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
    if (!auth.isAdmin) {
      ElMessage.error('该页面仅管理员可访问')
      return { name: 'shop-home' }
    }
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'shop-login', query: { redirect: to.fullPath } }
  }
})

export default router
