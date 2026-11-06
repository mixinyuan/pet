<script setup lang="ts">
/**
 * 商品收藏爱心按钮（.favorite-toggle）：迁移自旧 script.js 商品卡片上的
 * data-favorite 按钮与 toggleFavorite 的游客分支。
 *
 * - 登录用户：乐观切换 POST/DELETE /api/favorites/{productId}（失败由 store 回滚）；
 * - 游客：提示「登录后可收藏商品」并跳转 /login（携带 redirect 便于登录后回跳）；
 * - 切换成功后 emit changed，收藏列表页据此移除被取消的卡片。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'
import { useFavoriteStore } from '@/stores/favorite'

const props = defineProps<{
  /** 商品 id */
  productId: number
  /** 商品名称（无障碍标签与提示文案使用） */
  productName: string
}>()

const emit = defineEmits<{
  /** 收藏态切换成功（回滚不算）：productId + 切换后的收藏态 */
  (e: 'changed', productId: number, favorite: boolean): void
}>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const favorites = useFavoriteStore()

/** 派生收藏态：store 集合变化时按钮自动同步（替代旧页 syncFavoriteButtons） */
const isFavorite = computed(() => favorites.isFavorite(props.productId))

async function handleToggle() {
  if (!auth.isLoggedIn) {
    // 对齐旧页 toast 文案，并按 SPA 交互引导去登录页
    ElMessage('登录后可收藏商品')
    router.push({ name: 'shop-login', query: { redirect: route.fullPath } })
    return
  }
  const favorite = await favorites.toggleFavorite(props.productId)
  emit('changed', props.productId, favorite)
}
</script>

<template>
  <button
    class="favorite-toggle"
    :class="{ active: isFavorite }"
    type="button"
    :aria-pressed="isFavorite"
    :aria-label="isFavorite ? `取消收藏${props.productName}` : `收藏${props.productName}`"
    @click.stop="handleToggle"
  >
    {{ isFavorite ? '♥' : '♡' }}
  </button>
</template>
