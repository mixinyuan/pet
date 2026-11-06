<script setup lang="ts">
/**
 * 结账页（/checkout）：迁移旧 script.js 的 checkoutCart 流程并升级为独立页面。
 *
 * 旧页结账动作发生在购物车抽屉（确认条目 + 选券后一键 POST /api/orders），
 * 后端 CreateOrderRequest 仅接受可选 couponId（无收货地址/联系方式字段，
 * 旧页与后端均无此概念，故本页不虚构地址表单，保持与接口契约一致）。
 *
 * 流程：进入页面回源刷新购物车 → 确认结算商品清单 → 登录用户可选券（游客隐藏）→
 * 提交订单（携带持券实例 id）→ 成功态展示订单号/金额/状态并回源刷新购物车。
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { createOrder } from '@/api/order'
import CouponSelectField from '@/components/shop/CouponSelectField.vue'
import { useCartStore } from '@/stores/cart'
import type { OrderVO } from '@/types/shop'
import { formatPrice, orderStatusText } from '@/utils/format'

const router = useRouter()
const cart = useCartStore()

/** 提交中标记（防止重复下单） */
const submitting = ref(false)
/** 下单成功后的订单（非空时进入成功态） */
const successOrder = ref<OrderVO | null>(null)

onMounted(() => {
  // 回源刷新购物车，保证结算清单为最新；同时懒加载登录用户的优惠券
  cart.loadCart().catch(() => {
    // 错误已由拦截器弹出；loaded 已置位，页面展示空车兜底
  })
  cart.ensureCoupons()
})

/** 提交订单：携带选中的持券实例 id（未选/游客不带 body），成功后进入成功态 */
async function submitOrder() {
  if (submitting.value || !cart.hasItems) return
  submitting.value = true
  try {
    const couponId = cart.activeCoupon?.id ?? null
    const order = await createOrder(couponId)
    successOrder.value = order
    cart.selectCoupon(null)
    // 下单后回源刷新购物车（后端只清掉已结算条目）；刷新失败退化为本地清空展示
    await cart.loadCart().catch(() => cart.resetLocal())
    ElMessage.success(`订单${order.orderNo}已提交`)
  } catch {
    // 失败已由拦截器弹错，停留在结算页可重试
  } finally {
    submitting.value = false
  }
}

/** 返回首页继续选购 */
function backToHome() {
  router.push({ name: 'shop-home' })
}

/** 成功态跳转我的订单 */
function goOrders() {
  router.push({ name: 'shop-orders' })
}
</script>

<template>
  <section class="section checkout-page" aria-labelledby="checkout-title">
    <div class="section-heading">
      <p class="eyebrow">Checkout</p>
      <h2 id="checkout-title">确认订单</h2>
    </div>

    <!-- ===== 下单成功态 ===== -->
    <div v-if="successOrder" class="checkout-success">
      <div class="checkout-success-card">
        <span class="checkout-success-icon" aria-hidden="true">✓</span>
        <h3>订单提交成功</h3>
        <p class="checkout-success-meta">
          订单号：<strong>{{ successOrder.orderNo }}</strong>
        </p>
        <p class="checkout-success-meta">
          订单金额：<strong class="order-amount">{{ formatPrice(successOrder.totalAmount) }}</strong>
        </p>
        <p class="checkout-success-meta">订单状态：{{ orderStatusText(successOrder.status) }}</p>
        <div class="checkout-success-actions">
          <button class="orders-button" type="button" @click="goOrders">查看我的订单</button>
          <button class="checkout-continue" type="button" @click="backToHome">继续选购</button>
        </div>
      </div>
    </div>

    <!-- ===== 结算表单态 ===== -->
    <template v-else>
      <div v-if="!cart.loaded" class="empty-state">结算信息加载中...</div>

      <div v-else-if="!cart.hasItems" class="empty-state">
        <p>购物车为空，先挑选商品吧。</p>
        <button type="button" @click="backToHome">去选购</button>
      </div>

      <div v-else class="checkout-grid">
        <!-- 确认商品：沿用订单明细的 .order-item 行样式 -->
        <div class="order-card checkout-items-card">
          <div class="checkout-items-head">
            <h3>确认商品</h3>
            <span>共 {{ cart.checkedItems.length }} 种 · {{ cart.count }} 件</span>
          </div>
          <div class="order-items">
            <div
              v-for="item in cart.checkedItems"
              :key="`${item.itemType}-${item.productId}`"
              class="order-item"
            >
              <img :src="item.productImage" :alt="item.productName" />
              <div>
                <span class="order-item-name">
                  {{ item.productName }}
                  <em v-if="item.itemType === 'BUNDLE'" class="order-item-type">套餐</em>
                </span>
                <span class="order-item-meta">{{ formatPrice(item.price) }} × {{ item.quantity }}</span>
              </div>
              <strong>{{ formatPrice(item.subtotal) }}</strong>
            </div>
          </div>
        </div>

        <!-- 金额摘要 + 选券 + 提交：沿用购物车摘要样式 -->
        <div class="checkout-panel">
          <div class="cart-summary">
            <div class="cart-summary-line">
              <span>小计</span>
              <strong class="cart-total">{{ formatPrice(cart.totalAmount) }}</strong>
            </div>
            <!-- 优惠券选择：登录用户展示（旧页 id=couponSelect 在抽屉内，此处为独立 id），游客隐藏 -->
            <CouponSelectField id="couponSelectCheckout" />
            <div class="cart-actions">
              <button class="clear-cart" type="button" @click="backToHome">再逛逛</button>
              <button
                class="checkout-button"
                type="button"
                :disabled="submitting || !cart.hasItems"
                @click="submitOrder"
              >
                {{ submitting ? '提交中...' : '提交订单' }}
              </button>
            </div>
            <p class="checkout-note">订单仅结算购物车中勾选的商品，提交后由后端校验库存与优惠券。</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
/* 套餐条目标识（名称后金色小药丸，套餐价即结算单价） */
.order-item-type {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--gold);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  font-style: normal;
  vertical-align: middle;
}

/* 结账页布局：左商品清单右金额摘要，窄屏折叠为单列 */
.checkout-page {
  max-width: 1040px;
  margin: 0 auto;
}

.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);
  gap: 24px;
  align-items: start;
}

.checkout-items-card {
  padding: 0 14px 14px;
}

.checkout-items-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 0 6px;
}

.checkout-items-head h3 {
  font-size: 16px;
}

.checkout-items-head span {
  color: var(--muted);
  font-size: 13px;
}

.checkout-panel {
  padding: 0 14px 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
}

/* 摘要区沿用 .cart-summary 的分隔线，去掉抽屉内的自动下推 */
.checkout-panel .cart-summary {
  margin-top: 14px;
}

.checkout-note {
  margin: 12px 0 0;
  color: var(--muted);
  font-size: 12px;
}

/* ===== 成功态卡片 ===== */
.checkout-success {
  display: grid;
  place-items: center;
  padding: 32px 0 8px;
}

.checkout-success-card {
  width: min(460px, 100%);
  padding: 32px 28px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  text-align: center;
}

.checkout-success-icon {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  margin: 0 auto 12px;
  border-radius: 999px;
  background: #edf6ee;
  color: var(--green-dark);
  font-size: 28px;
  font-weight: 800;
}

.checkout-success-card h3 {
  margin-bottom: 10px;
  font-size: 20px;
}

.checkout-success-meta {
  margin: 6px 0;
  color: var(--muted);
  font-size: 14px;
}

.checkout-success-meta strong {
  color: var(--ink);
}

.checkout-success-actions {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.checkout-continue {
  min-height: 44px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-strong);
  color: var(--ink);
  cursor: pointer;
  font-weight: 700;
}

.checkout-continue:hover {
  border-color: rgba(63, 125, 76, 0.38);
  color: var(--green-dark);
}

@media (max-width: 820px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
}
</style>
