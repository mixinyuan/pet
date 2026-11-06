<script setup lang="ts">
/**
 * 个人中心页（/me，需登录）：迁移旧 script.js 个人中心抽屉（renderAccountPanel 的
 * 登录态分支 + accountLinksHtml）为独立页面。
 *
 * - 资料卡：首字母头像 + 昵称 + 用户名/邮箱（useAuthStore，邮箱未绑定显示「未设置」）；
 * - 功能入口：我的订单 / 我的收藏 / 我的优惠券（旧页为抽屉内联展开，SPA 迁移为跳转独立页面）；
 * - 登出入口：退出后清空本地登录态与收藏集合，回首页（对齐旧页 logout）。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useFavoriteStore } from '@/stores/favorite'

const router = useRouter()
const auth = useAuthStore()
const favorites = useFavoriteStore()

/** 首字母圆形头像字符（昵称优先，缺省回退用户名/「宠」，对齐旧页 avatarChar） */
const avatarChar = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || ''
  return String(name).trim().charAt(0).toUpperCase() || '宠'
})

async function handleLogout() {
  await auth.logout()
  favorites.resetFavorites()
  router.push({ name: 'shop-home' })
}
</script>

<template>
  <section class="section account-page" aria-labelledby="account-title">
    <div class="section-heading">
      <p class="eyebrow">Account</p>
      <h2 id="account-title">个人中心</h2>
    </div>

    <div class="account-page-panel">
      <!-- 用户资料卡（类名沿用旧 styles.css 的 account-profile） -->
      <div class="account-profile">
        <div class="account-avatar" aria-hidden="true">{{ avatarChar }}</div>
        <div class="account-profile-info">
          <h3>{{ auth.displayName || '宠物家长' }}</h3>
          <div class="account-profile-meta">
            <span>用户名：{{ auth.user?.username || '—' }}</span>
            <span>邮箱：{{ auth.user?.email || '未设置' }}</span>
          </div>
        </div>
      </div>

      <!-- 功能入口（旧页在抽屉内联展开，这里跳转对应页面） -->
      <nav class="account-links" aria-label="个人中心入口">
        <RouterLink class="account-link" :to="{ name: 'shop-orders' }">
          <span class="account-link-icon" aria-hidden="true">📦</span>
          <span>我的订单</span>
          <span class="account-link-arrow" aria-hidden="true">›</span>
        </RouterLink>
        <RouterLink class="account-link" :to="{ name: 'shop-favorites' }">
          <span class="account-link-icon" aria-hidden="true">🧡</span>
          <span>我的收藏</span>
          <span class="account-link-arrow" aria-hidden="true">›</span>
        </RouterLink>
        <RouterLink class="account-link" :to="{ name: 'shop-coupons' }">
          <span class="account-link-icon" aria-hidden="true">🎟️</span>
          <span>我的优惠券</span>
          <span class="account-link-arrow" aria-hidden="true">›</span>
        </RouterLink>
      </nav>

      <div class="account-footer">
        <button class="account-logout-button" type="button" @click="handleLogout">退出登录</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 个人中心页容器：旧 .account-panel 为抽屉内滚动列，页面化后限宽居中 */
.account-page {
  max-width: 560px;
  margin: 0 auto;
}

.account-page-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 旧 .account-footer 依赖抽屉 flex 的 auto 边距，页面内改为常规上边距 */
.account-page-panel .account-footer {
  margin-top: 14px;
}
</style>
