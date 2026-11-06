<script setup lang="ts">
/**
 * 管理台布局：对齐 docs/prototype/admin-prototype.html 的主框架（pa-shell/pa-side/pa-topbar）。
 *
 * - 深色侧栏（#24303F）：品牌区 + 八模块图标导航（悬停半透明白、选中 #409EFF）+ 底部脚注；
 * - 顶栏（52px）：「首页 / 模块名」面包屑 + 返回商城 / 账号头像 / 退出登录；
 * - 进入本布局的路由均带 meta.requiresAdmin，守卫已保证 ADMIN 身份。
 * 视觉规格（尺寸/配色/字号）取自原型 pa-* 系列样式；图标用 Element Plus 等价替换 lucide。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Box,
  Document,
  Odometer,
  Present,
  ShoppingCart,
  Star,
  SwitchButton,
  Ticket,
  User,
} from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 八模块导航：path 与路由一致（点击 router.push） */
const navItems = [
  { path: '/admin/dashboard', title: '数据看板', icon: Odometer },
  { path: '/admin/products', title: '商品管理', icon: Box },
  { path: '/admin/bundles', title: '场景套餐', icon: Present },
  { path: '/admin/orders', title: '订单管理', icon: ShoppingCart },
  { path: '/admin/users', title: '用户管理', icon: User },
  { path: '/admin/reviews', title: '评价管理', icon: Star },
  { path: '/admin/coupons', title: '优惠券管理', icon: Ticket },
  { path: '/admin/logs', title: '日志管理', icon: Document },
]

const currentTitle = computed(() => (route.meta.title as string) || '')

const displayName = computed(() => auth.displayName || '管理员')

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-shell">
    <!-- 深色侧栏（原型 pa-side：212px / #24303F / 文字 #A3B1C2） -->
    <aside class="admin-side">
      <div class="admin-brand">
        <span class="brand-mark">P</span>
        PawBites<small>管理后台</small>
      </div>

      <nav class="admin-nav" aria-label="后台模块导航">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ 'is-on': route.path === item.path }"
          @click="router.push(item.path)"
        >
          <el-icon :size="15"><component :is="item.icon" /></el-icon>
          {{ item.title }}
        </button>
      </nav>

      <div class="side-foot">PawBites 鲜宠小食 · 运营控制台</div>
    </aside>

    <div class="admin-main">
      <!-- 顶栏（原型 pa-topbar：52px，面包屑 + 右侧操作） -->
      <header class="admin-topbar">
        <span class="admin-crumb">首页 / <b>{{ currentTitle }}</b></span>

        <span class="topbar-right">
          <button type="button" class="back-btn" @click="router.push({ name: 'shop-home' })">
            返回商城
          </button>
          <span class="account">
            <span class="avatar">{{ displayName.slice(0, 1) }}</span>
            {{ displayName }}
          </span>
          <button type="button" class="icon-btn" aria-label="退出登录" title="退出登录" @click="handleLogout">
            <el-icon :size="15"><SwitchButton /></el-icon>
          </button>
        </span>
      </header>

      <div class="admin-content">
        <RouterView />
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  align-items: stretch;
  height: 100vh;
}

/* ---------- 侧栏（原型 pa-side） ---------- */
.admin-side {
  width: 212px;
  flex: none;
  background: #24303f;
  color: #a3b1c2;
  display: flex;
  flex-direction: column;
  padding: 0 12px 14px;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 10px 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  flex: none;
}

.admin-brand small {
  font-weight: 400;
  font-size: 11px;
  color: #a3b1c2;
  margin-left: auto;
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #a3b1c2;
  font-size: 13.5px;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #fff;
}

.nav-item.is-on {
  background: #409eff;
  color: #fff;
}

.side-foot {
  margin-top: auto;
  padding: 12px 10px 0;
  font-size: 11px;
  color: #8fa0b5;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

/* ---------- 主区 ---------- */
.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  flex: none;
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
  height: 52px;
}

.admin-crumb {
  color: #909399;
  font-size: 13px;
}

.admin-crumb b {
  color: #303133;
  font-weight: 500;
}

.topbar-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #606266;
}

.back-btn {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  padding: 5px 12px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.account {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex: none;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.icon-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.admin-content {
  flex: 1;
  padding: 16px 20px 22px;
  background: #f5f7fa;
  overflow-y: auto;
}

/* 窄屏：侧栏退化为横向滚动条（对齐原型响应式规则） */
@media (max-width: 900px) {
  .admin-side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    overflow-x: auto;
    padding: 0 10px;
  }

  .admin-brand {
    padding: 10px;
    white-space: nowrap;
  }

  .admin-nav {
    flex-direction: row;
    margin: 0;
  }

  .nav-item {
    width: auto;
    white-space: nowrap;
  }

  .side-foot {
    display: none;
  }

  .admin-shell {
    flex-direction: column;
    height: auto;
  }

  .admin-main {
    min-height: calc(100vh - 52px);
  }
}
</style>
