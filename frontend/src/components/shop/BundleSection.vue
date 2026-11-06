<script setup lang="ts">
/**
 * 首页场景套餐区（id=bundles）：拉取 GET /api/bundles 渲染套餐卡片。
 * 迁移自旧 index.html 的 bundle-grid 与 script.js 的 renderBundles；
 * 新 VO 额外提供 originalPrice，以划线价补充展示（旧页面仅展示套餐价）。
 */
import { onMounted, ref } from 'vue'

import { fetchBundles } from '@/api/bundle'
import type { BundleVO } from '@/types/shop'
import { formatPrice } from '@/utils/format'
import { toFriendlyError } from '@/utils/error'

const bundles = ref<BundleVO[]>([])
const loading = ref(true)
const errorMessage = ref('')

async function loadBundles() {
  loading.value = true
  errorMessage.value = ''
  try {
    const list = await fetchBundles()
    bundles.value = Array.isArray(list) ? list : []
  } catch (error) {
    errorMessage.value = toFriendlyError(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadBundles)
</script>

<template>
  <section class="section band" id="bundles" aria-labelledby="bundles-title">
    <div class="section-heading">
      <p class="eyebrow">Bundles</p>
      <h2 id="bundles-title">场景套餐</h2>
    </div>

    <div class="bundle-grid">
      <div v-if="loading" class="empty-state">套餐加载中...</div>
      <div v-else-if="errorMessage" class="empty-state">
        <p>{{ errorMessage }}</p>
        <button type="button" @click="loadBundles">重新加载</button>
      </div>
      <div v-else-if="!bundles.length" class="empty-state">暂无套餐。</div>
      <template v-else>
        <article v-for="bundle in bundles" :key="bundle.id" class="bundle-card">
          <span v-if="bundle.discountLabel" class="discount">{{ bundle.discountLabel }}</span>
          <h3>{{ bundle.name }}</h3>
          <p>{{ bundle.description }}</p>
          <div class="bundle-price-row">
            <strong>{{ formatPrice(bundle.price) }}</strong>
            <s v-if="bundle.originalPrice && bundle.originalPrice > bundle.price" class="bundle-original-price">
              {{ formatPrice(bundle.originalPrice) }}
            </s>
          </div>
        </article>
      </template>
    </div>
  </section>
</template>

<style scoped>
/* 划线原价：旧 styles.css 无对应类，按旧视觉（muted 小号）补充 */
.bundle-price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.bundle-original-price {
  color: var(--muted);
  font-size: 15px;
}
</style>
