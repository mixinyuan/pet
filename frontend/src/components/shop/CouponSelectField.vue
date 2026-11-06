<script setup lang="ts">
/**
 * 结算选券字段（迁移自旧 index.html 的 .cart-coupon 块 / script.js 的 renderCouponSummary）。
 *
 * - 游客隐藏整块（旧页 hidden=!isLoggedInUser 逻辑：优惠券接口需登录，游客无券可用）；
 * - 未使用且未过期的券进入下拉，门槛未满足的选项禁用并标注差价；
 * - 选中券后展示「券抵扣 / 实付金额」两行；选中券失效时自动回到「不使用优惠券」。
 * 票据数据与选中态都在 useCartStore 中，购物车抽屉与结账页共用同一份选择。
 */
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { formatPrice } from '@/utils/format'

const props = defineProps<{
  /** select 元素 id（购物车抽屉沿用旧页 couponSelect，结账页用独立 id 避免 DOM 重复） */
  id: string
}>()

const auth = useAuthStore()
const cart = useCartStore()

/** 当前下拉选中值：选中券不可用时回退为空串（不使用） */
const selectedValue = computed(() => (cart.activeCoupon ? String(cart.activeCoupon.id) : ''))

/** 下拉选项文案：门槛已满「名称：减¥x」，未满标注还差多少可用（对齐旧页） */
function optionLabel(coupon: { name: string; amount: number; minSpend: number }) {
  const remain = Math.max(Number(coupon.minSpend || 0) - cart.checkedTotal, 0)
  const base = `${coupon.name}：减¥${coupon.amount}`
  return cart.checkedTotal >= Number(coupon.minSpend || 0)
    ? base
    : `${base}（差¥${remain.toFixed(0)}可用）`
}

/** 是否满足门槛（决定该选项是否禁用） */
function thresholdMet(coupon: { minSpend: number }) {
  return cart.checkedTotal >= Number(coupon.minSpend || 0)
}

function onChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  cart.selectCoupon(value ? Number(value) : null)
}
</script>

<template>
  <div v-if="auth.isLoggedIn" class="cart-coupon">
    <div class="cart-summary-line coupon-select-line">
      <label :for="props.id">优惠券</label>
      <select :id="props.id" class="coupon-select" :value="selectedValue" @change="onChange">
        <option value="">不使用优惠券</option>
        <option
          v-for="coupon in cart.candidateCoupons"
          :key="coupon.id"
          :value="String(coupon.id)"
          :disabled="!thresholdMet(coupon)"
        >
          {{ optionLabel(coupon) }}
        </option>
      </select>
    </div>
    <div v-if="cart.activeCoupon" class="cart-summary-line coupon-discount-line">
      <span>券抵扣</span>
      <strong class="coupon-discount">-¥{{ cart.discountAmount.toFixed(0) }}</strong>
    </div>
    <div v-if="cart.activeCoupon" class="cart-summary-line coupon-payable-line">
      <span>实付金额</span>
      <strong class="coupon-payable">{{ formatPrice(cart.payableAmount) }}</strong>
    </div>
  </div>
</template>
