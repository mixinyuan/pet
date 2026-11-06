<script setup lang="ts">
/**
 * 我的收藏页（/favorites，需登录）：迁移旧 script.js 个人中心抽屉的「我的收藏」板块
 * （loadAccountFavorites / favoriteToProduct / renderProductCard 的收藏复用）为独立页面。
 *
 * - 挂载时强制拉取 GET /api/favorites 并同步收藏 id 集合（爱心选中态随之刷新）；
 * - 卡片复用旧 .account-product-grid + .product-card 样式：爱心取消收藏后卡片即时移除，
 *   「加入购物车」与首页一致；商品图片暂不可点（详情弹窗未迁移）。
 */
import { onMounted, ref } from 'vue'

import FavoriteToggle from '@/components/shop/FavoriteToggle.vue'
import { useCartStore } from '@/stores/cart'
import { useFavoriteStore } from '@/stores/favorite'
import type { FavoriteVO } from '@/types/shop'
import { toFriendlyError } from '@/utils/error'
import { formatPrice } from '@/utils/format'

const cart = useCartStore()
const favorites = useFavoriteStore()

const items = ref<FavoriteVO[]>([])
const loading = ref(true)
const error = ref('')

/** 拉取收藏列表（force 刷新：同时同步 store 的 id 集合） */
async function loadFavorites() {
  loading.value = true
  error.value = ''
  try {
    const list = await favorites.loadFavorites()
    items.value = Array.isArray(list) ? list : []
  } catch (err) {
    error.value = toFriendlyError(err)
  } finally {
    loading.value = false
  }
}

/** 爱心切换回调：取消收藏后从列表移除该卡片（对齐旧页「取消收藏后卡片应移除」） */
function handleFavoriteChanged(productId: number, favorite: boolean) {
  if (!favorite) {
    items.value = items.value.filter((item) => Number(item.productId) !== Number(productId))
  }
}

/** 加入购物车（成功提示在 store 内弹出；失败由拦截器统一弹错） */
function handleAddToCart(productId: number) {
  cart.addToCart(productId).catch(() => {})
}

onMounted(loadFavorites)
</script>

<template>
  <section class="section favorites-page" aria-labelledby="favorites-title">
    <div class="section-heading">
      <p class="eyebrow">Favorites</p>
      <h2 id="favorites-title">我的收藏</h2>
    </div>

    <div aria-live="polite">
      <div v-if="loading" class="empty-state">收藏加载中...</div>

      <div v-else-if="error" class="empty-state">
        <p>{{ error }}</p>
        <button type="button" @click="loadFavorites">重新加载</button>
      </div>

      <p v-else-if="!items.length" class="account-section-empty">
        还没有收藏，去商品列表点 ♥ 收藏喜欢的零食吧。
      </p>

      <!-- 类名沿用旧 styles.css：account-product-grid 内复用商品卡片渲染 -->
      <div v-else class="account-product-grid">
        <article v-for="item in items" :key="item.id" class="product-card">
          <FavoriteToggle
            :product-id="item.productId"
            :product-name="item.productName"
            @changed="handleFavoriteChanged"
          />
          <div class="product-image">
            <img :src="item.productImage" :alt="item.productName" loading="lazy" />
          </div>
          <div class="product-info">
            <h3>{{ item.productName }}</h3>
            <div class="meta">{{ item.petType }} · {{ item.flavor }}</div>
            <div class="rating">★ {{ item.rating }} / 5.0</div>
            <div class="price-row">
              <span class="price">{{ formatPrice(item.price) }}</span>
              <button
                type="button"
                :aria-label="`将${item.productName}加入购物车`"
                @click="handleAddToCart(item.productId)"
              >
                加入购物车
              </button>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 收藏页容器：旧 .account-product-grid 为抽屉内网格，页面化后限宽居中 */
.favorites-page {
  max-width: 760px;
  margin: 0 auto;
}
</style>
