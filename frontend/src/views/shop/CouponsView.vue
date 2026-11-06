<script setup lang="ts">
/**
 * 我的优惠券页（/coupons，需登录）：迁移旧 script.js 个人中心抽屉的「我的优惠券」板块
 * （loadAccountCoupons / renderCouponCard / couponStatusText）为独立页面。
 *
 * 考证结论：旧页与后端 CouponController 均无「领取优惠券」入口——新券由后端在
 * 注册/订阅时自动发放，故本页只做持券列表展示（可用/已使用/已过期三态卡片）。
 */
import { onMounted, ref } from 'vue'

import { fetchMyCoupons } from '@/api/coupon'
import type { CouponVO } from '@/types/shop'
import { toFriendlyError } from '@/utils/error'
import { couponStatusText, formatDateTime } from '@/utils/format'

const coupons = ref<CouponVO[]>([])
const loading = ref(true)
const error = ref('')

async function loadCoupons() {
  loading.value = true
  error.value = ''
  try {
    const list = await fetchMyCoupons()
    coupons.value = Array.isArray(list) ? list : []
  } catch (err) {
    error.value = toFriendlyError(err)
  } finally {
    loading.value = false
  }
}

/** 券卡片状态类名（对齐旧页 renderCouponCard：0-usable 1-used 2-expired） */
function statusClass(coupon: CouponVO): string {
  const status = Number(coupon.status)
  return status === 0 ? 'usable' : status === 1 ? 'used' : 'expired'
}

onMounted(loadCoupons)
</script>

<template>
  <section class="section coupons-page" aria-labelledby="coupons-title">
    <div class="section-heading">
      <p class="eyebrow">Coupons</p>
      <h2 id="coupons-title">我的优惠券</h2>
    </div>

    <div aria-live="polite">
      <div v-if="loading" class="empty-state">优惠券加载中...</div>

      <div v-else-if="error" class="empty-state">
        <p>{{ error }}</p>
        <button type="button" @click="loadCoupons">重新加载</button>
      </div>

      <p v-else-if="!coupons.length" class="account-section-empty">暂无优惠券，注册/订阅可领新人券</p>

      <!-- 类名沿用旧 styles.css 的 coupon-card（金额 + 门槛 + 有效期 + 状态） -->
      <template v-else>
        <article v-for="coupon in coupons" :key="coupon.id" class="coupon-card" :class="statusClass(coupon)">
          <div class="coupon-amount">
            <strong>¥{{ coupon.amount }}</strong>
            <small>满¥{{ coupon.minSpend }}可用</small>
          </div>
          <div class="coupon-info">
            <span class="coupon-name">{{ coupon.name }}</span>
            <span class="coupon-expire">有效期至 {{ formatDateTime(coupon.expiresAt) }}</span>
          </div>
          <span class="coupon-status">{{ couponStatusText(coupon.status) }}</span>
        </article>
      </template>
    </div>
  </section>
</template>

<style scoped>
/* 券包页容器：旧 .coupon-card 为抽屉内堆叠列表，页面化后限宽居中 */
.coupons-page {
  max-width: 640px;
  margin: 0 auto;
}
</style>
