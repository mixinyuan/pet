<script setup lang="ts">
/**
 * 管理台布局：左侧固定导航（九大模块）+ 顶栏（面包屑/账号）+ 内容出口。
 * 进入本布局的路由均带 meta.requiresAdmin，守卫已保证 ADMIN 身份。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// el-menu 开启 router 模式后按 index 作为路由路径 push，必须用 path 而非路由 name
const navItems = [
  { path: '/admin/dashboard', title: '数据看板' },
  { path: '/admin/products', title: '商品管理' },
  { path: '/admin/bundles', title: '场景套餐' },
  { path: '/admin/orders', title: '订单管理' },
  { path: '/admin/users', title: '用户管理' },
  { path: '/admin/reviews', title: '评价管理' },
  { path: '/admin/coupons', title: '优惠券管理' },
  { path: '/admin/logs', title: '日志管理' },
]

const currentTitle = computed(() => (route.meta.title as string) || '')

async function handleCommand(command: string) {
  if (command === 'logout') {
    await auth.logout()
    router.push({ name: 'admin-login' })
  } else if (command === 'shop') {
    router.push({ name: 'shop-home' })
  }
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-brand">
        <span class="admin-brand-mark">🐾</span>
        <div>
          <div class="admin-brand-title">PawBites</div>
          <div class="admin-brand-sub">管理控制台</div>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          {{ item.title }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
        </el-breadcrumb>

        <el-dropdown @command="handleCommand">
          <span class="admin-account">
            <el-avatar :size="30">{{ auth.displayName.slice(0, 1) }}</el-avatar>
            {{ auth.displayName }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="shop">返回商城</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell {
  height: 100vh;
}

.admin-aside {
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.admin-brand-mark {
  font-size: 26px;
}

.admin-brand-title {
  font-weight: 700;
  color: #1f2937;
}

.admin-brand-sub {
  font-size: 12px;
  color: #9ca3af;
}

.admin-menu {
  border-right: none;
  flex: 1;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  height: 56px;
}

.admin-account {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #374151;
  font-size: 14px;
  outline: none;
}

.admin-main {
  background: #f5f6f8;
  overflow-y: auto;
}
</style>
