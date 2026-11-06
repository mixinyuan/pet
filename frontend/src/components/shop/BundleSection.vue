<script setup lang="ts">
/**
 * 首页场景套餐区（id=bundles）：拉取 GET /api/bundles 渲染套餐卡片。
 * 迁移自旧 index.html 的 bundle-grid 与 script.js 的 renderBundles；
 * 新 VO 额外提供 originalPrice，以划线价补充展示（旧页面仅展示套餐价）。
 * 套餐支持按套餐价加购（后端按组成商品校验库存），入口与商品卡一致。
 */
import { onMounted, ref } from 'vue'

import { fetchBundles } from '@/api/bundle'
import { useCartStore } from '@/stores/cart'
import type { BundleVO } from '@/types/shop'
import { formatPrice } from '@/utils/format'
import { toFriendlyError } from '@/utils/error'

const cart = useCartStore()
const bundles = ref<BundleVO[]>([])
const loading = ref(true)
const errorMessage = ref('')
/** 正在加购的套餐 id（按钮 loading 态，防连点） */
const addingId = ref<number | null>(null)

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

/** 套餐加入购物车（失败提示由拦截器统一弹出，这里只复位按钮态） */
async function handleAdd(bundle: BundleVO) {
  if (addingId.value !== null) return
  addingId.value = bundle.id
  try {
    await cart.addBundleToCart(bundle.id)
  } catch {
    // 库存不足/套餐下架等报错已由拦截器弹出
  } finally {
    addingId.value = null
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
          <button
            type="button"
            class="bundle-add-btn"
            :disabled="addingId !== null"
            :aria-label="`将${bundle.name}加入购物车`"
            @click="handleAdd(bundle)"
          >
            {{ addingId === bundle.id ? '加入中…' : '加入购物车' }}
          </button>
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

/* 套餐加购按钮：视觉对齐商品卡按钮（shop.css 的 .product-card button，--green 底白字圆角 8px） */
.bundle-add-btn {
  margin-top: 12px;
  min-height: 44px;
  width: 100%;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  background: var(--green);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.bundle-add-btn:hover:not(:disabled) {
  filter: brightness(1.05);
}

.bundle-add-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
