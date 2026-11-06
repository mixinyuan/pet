<script setup lang="ts">
/**
 * 首页分类导航（id=categories）：拉取 GET /api/categories 渲染分类卡片，
 * 点击后向父组件抛出分类 code，由首页联动商品列表重载。
 * 拉取失败或为空时退化为「全部」单卡（对齐旧 script.js 的 renderCategories 兜底）。
 */
import { onMounted, ref } from 'vue'

import { fetchCategories } from '@/api/category'
import type { CategoryVO } from '@/types/shop'

/** 接口失败/空列表时的兜底分类 */
const FALLBACK_CATEGORIES: CategoryVO[] = [{ id: 0, code: 'all', name: '全部', description: '今日精选' }]

const props = defineProps<{
  /** 当前选中分类 code（all 表示全部） */
  active: string
}>()

const emit = defineEmits<{
  select: [code: string]
}>()

const categories = ref<CategoryVO[]>([])
const loading = ref(true)

function isActive(code: string): boolean {
  return code === props.active
}

onMounted(async () => {
  try {
    const list = await fetchCategories()
    categories.value = list.length ? list : FALLBACK_CATEGORIES
  } catch {
    // 错误提示由 axios 拦截器统一弹出；此处降级为「全部」单卡，保证商品列表仍可浏览
    categories.value = FALLBACK_CATEGORIES
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="section" id="categories" aria-labelledby="categories-title">
    <div class="section-heading">
      <p class="eyebrow">Shop by need</p>
      <h2 id="categories-title">按需求挑选</h2>
    </div>

    <div class="category-grid" role="list">
      <div v-if="loading" class="empty-state">分类加载中...</div>
      <template v-else>
        <button
          v-for="category in categories"
          :key="category.code"
          class="category-card"
          :class="{ active: isActive(category.code) }"
          type="button"
          role="listitem"
          :data-filter="category.code"
          :aria-pressed="isActive(category.code)"
          @click="emit('select', category.code)"
        >
          <span>{{ category.name }}</span>
          <small>{{ category.description }}</small>
        </button>
      </template>
    </div>
  </section>
</template>
