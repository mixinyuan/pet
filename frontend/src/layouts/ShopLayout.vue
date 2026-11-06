<script setup lang="ts">
/**
 * 商城布局：迁移自旧静态页 index.html 的 <header>/<footer> 骨架，
 * 类名沿用 styles.css（已复制为 @/styles/shop.css，在此全局引入）。
 *
 * - 游客可浏览（与后端商品/分类/套餐/评价只读放行一致），登录入口引导到 /login；
 * - 顶栏「我的订单 / 购物车（含数量角标）」入口已接入：订单跳 /orders 页，购物车打开 CartDrawer；
 * - 顶栏「个人中心」按钮：登录后显示昵称入口（跳 /me，结构对齐旧页 account-button），
 *   游客显示「登录 / 注册」按钮；
 * - 挂载时拉取一次购物车（对齐旧页 loadStorefrontData 中的 loadCart）；
 * - 退出登录时同时清空收藏集合（对齐旧页 resetFavoritesAndCoupons 的收藏部分）；
 * - 底部订阅表单组件 SubscribeForm 对应 POST /api/subscriptions。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import CartDrawer from '@/components/shop/CartDrawer.vue'
import SubscribeForm from '@/components/shop/SubscribeForm.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useFavoriteStore } from '@/stores/favorite'
import '@/styles/shop.css'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const favorites = useFavoriteStore()

/** 移动端导航抽屉开合（对齐旧 script.js 的 menuToggle 逻辑） */
const menuOpen = ref(false)

function closeMenu() {
  menuOpen.value = false
}

function goLogin() {
  closeMenu()
  router.push({ name: 'shop-login' })
}

/** 顶栏「我的订单」入口：旧页为抽屉，SPA 迁移为独立页面 /orders */
function goOrders() {
  closeMenu()
  router.push({ name: 'shop-orders' })
}

/** 顶栏「购物车」入口：打开购物车抽屉（角标数量来自 cart store） */
function openCart() {
  closeMenu()
  cart.openDrawer()
}

async function handleLogout() {
  closeMenu()
  await auth.logout()
  favorites.resetFavorites()
  router.push({ name: 'shop-home' })
}

onMounted(() => {
  // 初始化购物车（游客按 X-Guest-Id 关联，失败已由拦截器弹错且不影响浏览）
  cart.loadCart().catch(() => {})
})
</script>

<template>
  <div class="shop-page">
    <header class="site-header">
      <RouterLink :to="{ name: 'shop-home' }" class="brand" aria-label="PawBites 鲜宠小食首页">
        <span class="brand-mark">P</span>
        <span>PawBites 鲜宠小食</span>
      </RouterLink>

      <button
        class="icon-button menu-toggle"
        type="button"
        :aria-label="menuOpen ? '关闭导航菜单' : '打开导航菜单'"
        :aria-expanded="menuOpen"
        @click="menuOpen = !menuOpen"
      >
        <span></span>
        <span></span>
        <span></span>
      </button>

      <nav class="nav-links" :class="{ open: menuOpen }" aria-label="主导航" @click="closeMenu">
        <RouterLink :to="{ name: 'shop-home', hash: '#categories' }">商品分类</RouterLink>
        <RouterLink :to="{ name: 'shop-home', hash: '#products' }">热销</RouterLink>
        <RouterLink :to="{ name: 'shop-home', hash: '#bundles' }">套餐</RouterLink>
        <RouterLink :to="{ name: 'shop-home', hash: '#reviews' }">评价</RouterLink>
      </nav>

      <div class="header-actions">
        <template v-if="auth.isLoggedIn">
          <!-- 个人中心入口：昵称 + 标签，结构对齐旧页 account-button（account-name/account-label） -->
          <RouterLink
            class="account-button"
            :to="{ name: 'shop-me' }"
            aria-label="打开个人中心"
            @click="closeMenu"
          >
            <span class="account-name">{{ auth.displayName }}</span>
            <span class="account-label">个人中心</span>
          </RouterLink>
          <RouterLink
            v-if="auth.isAdmin"
            class="account-button"
            :to="{ name: 'admin-dashboard' }"
            @click="closeMenu"
          >
            管理后台
          </RouterLink>
          <button class="account-button" type="button" @click="handleLogout">退出登录</button>
        </template>
        <template v-else>
          <button class="account-button" type="button" @click="goLogin">登录 / 注册</button>
        </template>
        <button class="orders-button" type="button" @click="goOrders">
          <span class="orders-icon" aria-hidden="true">📦</span>
          <span>我的订单</span>
        </button>
        <button
          class="cart-button"
          type="button"
          :aria-label="cart.count > 0 ? `打开购物车，共${cart.count}件商品` : '打开购物车'"
          @click="openCart"
        >
          <span class="cart-icon" aria-hidden="true">🛒</span>
          <span>购物车</span>
          <span class="cart-count">{{ cart.count }}</span>
        </button>
      </div>
    </header>

    <main id="top">
      <RouterView />
    </main>

    <footer class="site-footer">
      <div>
        <h2>PawBites 鲜宠小食</h2>
        <p>认真挑选每一口奖励，让陪伴更有滋味。</p>
      </div>
      <SubscribeForm />
      <div class="footer-links">
        <span>满99包邮</span>
        <span>破损包赔</span>
        <span>客服：hello@pawbites.example</span>
      </div>
    </footer>

    <!-- 购物车抽屉（含遮罩，类名沿用旧 styles.css），全局挂在布局层 -->
    <CartDrawer />
  </div>
</template>

<style scoped>
/* shop-shell 仅作布局容器：旧样式中 header/footer 均为块级自适应，无需额外样式 */
.shop-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.shop-page > main {
  flex: 1;
}

/* 登录用户名展示（复用旧 .account-name 的截断样式，追加在头部场景下的对齐） */
.header-actions .account-name {
  align-self: center;
  color: var(--muted);
  font-size: 14px;
  font-weight: 700;
}

/* 管理后台入口链接按旧 .account-button 视觉呈现（inline-flex 对齐头部按钮） */
a.account-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-strong);
  color: var(--muted);
  font-weight: 700;
}

a.account-button:hover {
  border-color: rgba(63, 125, 76, 0.38);
  color: var(--green-dark);
}

@media (max-width: 520px) {
  .header-actions .account-name {
    display: none;
  }
}
</style>
