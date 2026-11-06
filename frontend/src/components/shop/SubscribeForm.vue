<script setup lang="ts">
/**
 * 页脚邮件订阅表单：POST /api/subscriptions（同一邮箱重复提交视为重新激活）。
 * 迁移自旧 index.html 的 subscribe-form 与 script.js 的提交逻辑：
 * 邮箱本地 trim + 转小写；提交中/成功以内联文案反馈，失败由 axios 拦截器统一弹错。
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

import { subscribe } from '@/api/subscription'

const formRef = ref<HTMLFormElement>()
const email = ref('')
const message = ref('')
const submitting = ref(false)

async function handleSubmit() {
  if (submitting.value) return
  if (!formRef.value?.checkValidity()) {
    formRef.value?.reportValidity()
    return
  }

  const normalized = email.value.trim().toLowerCase()
  message.value = '正在订阅...'
  submitting.value = true
  try {
    await subscribe(normalized)
    message.value = '订阅成功，新人券稍后发送到邮箱。'
    email.value = ''
    ElMessage.success('订阅成功')
  } catch {
    // 失败提示由 axios 拦截器统一 ElMessage 弹出，这里只清掉“正在订阅”占位
    message.value = ''
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form ref="formRef" class="subscribe-form" @submit.prevent="handleSubmit">
    <label for="subscribe-email">订阅优惠</label>
    <div>
      <input
        id="subscribe-email"
        v-model="email"
        type="email"
        placeholder="输入邮箱获取新人券"
        autocomplete="email"
        required
      />
      <button type="submit" :disabled="submitting">订阅</button>
    </div>
    <p class="subscribe-message" role="status" aria-live="polite">{{ message }}</p>
  </form>
</template>
