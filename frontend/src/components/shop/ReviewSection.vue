<script setup lang="ts">
/**
 * 首页用户评价区（id=reviews）：拉取 GET /api/reviews 渲染铲屎官评价卡片，
 * 迁移自旧 script.js 的 renderReviews（展示名取 petName，缺省回退 buyerName）。
 */
import { onMounted, ref } from 'vue'

import { fetchReviews } from '@/api/review'
import type { ReviewVO } from '@/types/shop'
import { toFriendlyError } from '@/utils/error'

const reviews = ref<ReviewVO[]>([])
const loading = ref(true)
const errorMessage = ref('')

async function loadReviews() {
  loading.value = true
  errorMessage.value = ''
  try {
    const list = await fetchReviews()
    reviews.value = Array.isArray(list) ? list : []
  } catch (error) {
    errorMessage.value = toFriendlyError(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadReviews)

/** 卡片展示名：宠物名优先，未填时回退买家名（对齐旧页面） */
function displayName(review: ReviewVO): string {
  return review.petName || review.buyerName
}
</script>

<template>
  <section class="section" id="reviews" aria-labelledby="reviews-title">
    <div class="section-heading">
      <p class="eyebrow">Reviews</p>
      <h2 id="reviews-title">铲屎官反馈</h2>
    </div>

    <div class="review-grid">
      <div v-if="loading" class="empty-state">评价加载中...</div>
      <div v-else-if="errorMessage" class="empty-state">
        <p>{{ errorMessage }}</p>
        <button type="button" @click="loadReviews">重新加载</button>
      </div>
      <div v-else-if="!reviews.length" class="empty-state">暂无评价。</div>
      <template v-else>
        <article v-for="review in reviews" :key="review.id" class="review-card">
          <img :src="review.avatarUrl" :alt="`${displayName(review)}头像`" loading="lazy" />
          <p>“{{ review.content }}”</p>
          <strong>{{ displayName(review) }} · {{ review.petBreed }}</strong>
          <span>购买：{{ review.purchasedProduct }}</span>
        </article>
      </template>
    </div>
  </section>
</template>
