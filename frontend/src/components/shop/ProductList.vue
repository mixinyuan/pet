<script setup lang="ts">
/**
 * 首页商品列表（id=products）：分页拉取 GET /api/products，支持「加载更多 / 到底提示 / 加载失败重试」，
 * 迁移自旧 script.js 的 loadProducts 与 setProductsFooter 状态机（page 从 1 开始，每页 20 条）。
 *
 * 父组件切换 category 后通过 watch 重置加载；请求带自增 requestId 防止快速切换分类时的竞态回写。
 *
 * 已启用的交互：
 * - 「加入购物车」按钮：POST /api/cart/items（useCartStore.addToCart，游客/登录均可用）；
 * - 收藏爱心（FavoriteToggle）：登录用户切换 POST/DELETE /api/favorites/{id}（乐观更新），
 *   游客点击提示后引导去 /login；登录态在挂载时经 ensureFavorites 懒加载一次；
 * - 点击图片打开商品详情弹窗：暂不渲染 detail-hint，等详情迁移任务启用 GET /api/products/{id}。
 */
import { onMounted, ref, watch } from 'vue'

import { fetchProducts } from '@/api/product'
import FavoriteToggle from '@/components/shop/FavoriteToggle.vue'
import { useCartStore } from '@/stores/cart'
import { useFavoriteStore } from '@/stores/favorite'
import type { ProductVO } from '@/types/shop'
import { formatPrice } from '@/utils/format'
import { toFriendlyError } from '@/utils/error'

const cart = useCartStore()
const favorites = useFavoriteStore()

/** 每页条数（与旧页面 PRODUCT_PAGE_SIZE 一致） */
const PRODUCT_PAGE_SIZE = 20

const props = withDefaults(
  defineProps<{
    /** 分类筛选 code，all 表示全部 */
    category?: string
  }>(),
  { category: 'all' },
)

/** 当前已加载的商品（跨页累积，用于「加载更多」拼接） */
const items = ref<ProductVO[]>([])
/** 已加载到的页码（从 1 开始，0 表示尚未成功加载） */
const page = ref(0)
const total = ref(0)
const hasMore = ref(false)
/** 首屏/重置加载状态 */
const firstLoading = ref(true)
const firstError = ref('')
/** 「加载更多」状态 */
const moreLoading = ref(false)
const moreError = ref('')

/** 自增请求序号：仅最后一次请求可写状态，防止分类快速切换时旧响应覆盖新结果 */
let requestId = 0

async function loadProducts(reset: boolean) {
  const targetPage = reset ? 1 : page.value + 1
  const currentRequest = ++requestId

  if (reset) {
    firstLoading.value = true
    firstError.value = ''
    moreError.value = ''
  } else {
    moreLoading.value = true
    moreError.value = ''
  }

  try {
    const result = await fetchProducts({
      page: targetPage,
      size: PRODUCT_PAGE_SIZE,
      category: props.category,
    })
    if (currentRequest !== requestId) return

    const records = Array.isArray(result?.records) ? result.records : []
    items.value = reset ? records : items.value.concat(records)
    page.value = targetPage
    total.value = Number(result?.total || 0)
    hasMore.value = records.length > 0 && items.value.length < total.value
  } catch (error) {
    if (currentRequest !== requestId) return
    const message = toFriendlyError(error)
    if (reset) {
      items.value = []
      firstError.value = message
    } else {
      moreError.value = message
    }
  } finally {
    if (currentRequest === requestId) {
      firstLoading.value = false
      moreLoading.value = false
    }
  }
}

/** 首屏/分类切换/失败重试：从第 1 页重新加载 */
function reload() {
  loadProducts(true)
}

/** 加载下一页（加载中时忽略重复点击） */
function loadMore() {
  if (!moreLoading.value) {
    loadProducts(false)
  }
}

/** 加入购物车（成功提示在 store 内弹出；失败由 axios 拦截器统一弹错，这里吞掉 rejection） */
function handleAddToCart(productId: number) {
  cart.addToCart(productId).catch(() => {})
}

onMounted(() => {
  loadProducts(true)
  // 登录用户懒加载收藏 id 集合（同步爱心选中态；游客在 store 内直接清空，失败静默）
  favorites.ensureFavorites()
})

watch(
  () => props.category,
  () => {
    loadProducts(true)
  },
)
</script>

<template>
  <section class="section" id="products" aria-labelledby="products-title">
    <div class="section-heading split">
      <div>
        <p class="eyebrow">Best sellers</p>
        <h2 id="products-title">热销商品</h2>
      </div>
      <p class="section-note">低盐低脂、适口配方，按需求快速挑选。</p>
    </div>

    <div class="product-grid" aria-live="polite">
      <div v-if="firstLoading" class="empty-state">商品加载中...</div>
      <div v-else-if="firstError" class="empty-state">
        <p>{{ firstError }}</p>
        <button type="button" @click="reload">重新加载</button>
      </div>
      <div v-else-if="!items.length" class="empty-state">暂时没有符合条件的商品。</div>
      <template v-else>
        <article v-for="product in items" :key="product.id" class="product-card">
          <!-- 收藏爱心（登录态可切换，游客点击引导去 /login），类名沿用旧 styles.css -->
          <FavoriteToggle :product-id="product.id" :product-name="product.name" />
          <!-- 注意：图片暂不可点击，故未加 .product-media（其 cursor:pointer 会造成可点击的错觉）；
               商品详情弹窗迁移时改为 button.product-image.product-media 并补 .detail-hint -->
          <div class="product-image">
            <img :src="product.imageUrl" :alt="product.name" loading="lazy" />
            <div class="tag-row">
              <span v-for="tag in product.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
          <div class="product-info">
            <h3>{{ product.name }}</h3>
            <div class="meta">{{ product.petType }} · {{ product.flavor }}</div>
            <div class="rating">★ {{ product.rating }} / 5.0</div>
            <div class="price-row">
              <span class="price">{{ formatPrice(product.price) }}</span>
              <button
                type="button"
                :aria-label="`将${product.name}加入购物车`"
                @click="handleAddToCart(product.id)"
              >
                加入购物车
              </button>
            </div>
          </div>
        </article>
      </template>
    </div>

    <div v-if="!firstLoading && !firstError && items.length" class="products-footer">
      <button
        v-if="hasMore || moreLoading"
        class="load-more-button"
        type="button"
        :disabled="moreLoading"
        @click="loadMore"
      >
        {{ moreLoading ? '加载中...' : '加载更多商品' }}
      </button>
      <p v-else-if="!moreError" class="products-end-hint">已经到底啦，更多好物正在路上～</p>
      <div v-if="moreError" class="products-load-error">
        <p>{{ moreError }}</p>
        <button type="button" @click="loadMore">重新加载</button>
      </div>
    </div>
  </section>
</template>
