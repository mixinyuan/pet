<script setup lang="ts">
/**
 * 管理台登录门：仅 ADMIN 角色可进入。
 * 复用商城登录接口，登录成功后校验 role，非 ADMIN 直接提示并清除登录态。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)

async function handleLogin() {
  if (submitting.value) return
  submitting.value = true
  try {
    const data = await auth.login({ username: form.username, password: form.password })
    if (data.role !== 'ADMIN') {
      // 普通用户不允许进入管理台：清除刚写入的登录态，避免误入
      auth.clearLocal()
      ElMessage.error('该账号不是管理员，无法进入管理后台')
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
    router.push(redirect)
  } catch {
    // 错误提示由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="gate">
    <el-card class="gate-card">
      <div class="gate-brand">🐾 PawBites 管理后台</div>
      <p class="gate-sub">仅管理员账号可登录</p>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="管理员账号">
          <el-input v-model="form.username" placeholder="用户名或邮箱" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting" class="gate-submit">登录管理后台</el-button>
      </el-form>

      <RouterLink :to="{ name: 'shop-home' }" class="gate-back">← 返回商城</RouterLink>
    </el-card>
  </div>
</template>

<style scoped>
.gate {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
}

.gate-card {
  width: 380px;
  text-align: center;
}

.gate-brand {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.gate-sub {
  color: #9ca3af;
  font-size: 13px;
  margin: 8px 0 18px;
}

.gate-submit {
  width: 100%;
}

.gate-back {
  display: inline-block;
  margin-top: 16px;
  color: #6b7280;
  font-size: 13px;
  text-decoration: none;
}
</style>
