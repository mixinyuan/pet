<script setup lang="ts">
/**
 * 商城登录/注册页：登录与注册共用一张卡片切换。
 * 成功后回到 redirect 指定的来源页（守卫跳转时携带），缺省回首页。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  email: '',
  nickname: '',
})

async function handleSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ username: form.username, password: form.password })
    } else {
      await auth.register({
        username: form.username,
        email: form.email,
        password: form.password,
        nickname: form.nickname || undefined,
      })
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch {
    // 错误提示由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="login-tabs">
          <button :class="{ active: mode === 'login' }" type="button" @click="mode = 'login'">登录</button>
          <button :class="{ active: mode === 'register' }" type="button" @click="mode = 'register'">注册</button>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名 / 邮箱">
          <el-input v-model="form.username" placeholder="登录账号" autocomplete="username" />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" label="邮箱">
          <el-input v-model="form.email" placeholder="注册邮箱" autocomplete="email" />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" label="昵称（可选）">
          <el-input v-model="form.nickname" placeholder="展示昵称" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" autocomplete="current-password" />
        </el-form-item>

        <el-button type="primary" native-type="submit" :loading="submitting" class="login-submit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>

      <p class="login-tip">游客也可直接浏览与下单（购物车通过游客身份关联）</p>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  padding: 64px 20px;
}

.login-card {
  width: 400px;
}

.login-tabs {
  display: flex;
  gap: 8px;
}

.login-tabs button {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: #f3f4f6;
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
}

.login-tabs button.active {
  background: #16a34a;
  color: #fff;
  font-weight: 600;
}

.login-submit {
  width: 100%;
}

.login-tip {
  margin: 14px 0 0;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}
</style>
