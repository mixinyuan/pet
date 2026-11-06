<script setup lang="ts">
/**
 * 购物车抽屉：迁移旧 index.html 的 .cart-drawer / .drawer-backdrop 结构与
 * script.js 的 renderCart / changeCartQuantity / submitQuantityInput / clearCart 逻辑。
 *
 * - 类名全部沿用旧 styles.css（shop.css），开合靠 .open 类平移；
 * - 条目数量支持 −/+ 步进与直接输入（非法输入回退并提示，数量归零即删除）；
 * - 登录用户在摘要区展示选券下拉（CouponSelectField），游客隐藏；
 * - 「去结算」跳转 /checkout 结账页；Escape / 遮罩点击 / 路由切换关闭抽屉。
 */
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import CouponSelectField from '@/components/shop/CouponSelectField.vue'
import { useCartStore } from '@/stores/cart'
import type { CartItemVO } from '@/types/shop'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()

/** 打开抽屉时同步刷新登录用户的优惠券列表（游客为空） */
watch(
  () => cart.drawerOpen,
  (open) => {
    if (open) {
      cart.ensureCoupons()
    }
  },
)

/** 路由切换时收起抽屉（旧页为单页无此场景，SPA 跳转结账页等需要收起） */
watch(
  () => route.fullPath,
  () => {
    cart.closeDrawer()
  },
)

/** Escape 关闭抽屉（对齐旧页全局 keydown 行为） */
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    cart.closeDrawer()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})

/** −/+ 步进：数量减到 0 时按旧逻辑转为删除该条目（套餐条目带 type=BUNDLE） */
function step(item: CartItemVO, delta: number) {
  const type = item.itemType === 'BUNDLE' ? 'BUNDLE' : 'PRODUCT'
  cart.updateQuantity(item.productId, Number(item.quantity) + delta, type).catch(() => {
    // 失败已由拦截器弹错，本地态未变更无需回滚
  })
}

/** 直接输入数量：非法（空/非整数/小于 1）时回退输入框并提示（对齐旧页 submitQuantityInput） */
function onQuantityChange(item: CartItemVO, event: Event) {
  const input = event.target as HTMLInputElement
  const value = input.value.trim()

  if (!value) {
    input.value = String(item.quantity)
    ElMessage.warning('请输入商品数量')
    return
  }

  const nextQuantity = Number(value)
  if (!Number.isInteger(nextQuantity) || nextQuantity < 1) {
    input.value = String(item.quantity)
    ElMessage.warning('数量必须是大于 0 的整数')
    return
  }

  if (nextQuantity !== item.quantity) {
    const type = item.itemType === 'BUNDLE' ? 'BUNDLE' : 'PRODUCT'
    cart.updateQuantity(item.productId, nextQuantity, type).catch(() => {
      input.value = String(item.quantity)
    })
  }
}

/** 输入框内按 Enter 直接失焦提交（对齐旧页 keydown 行为） */
function onQuantityKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    ;(event.target as HTMLInputElement).blur()
  }
}

/** 清空购物车（失败已弹错） */
function handleClear() {
  cart.clearCart().catch(() => {})
}

/** 去结算：收起抽屉后进入结账页 */
function goCheckout() {
  cart.closeDrawer()
  router.push({ name: 'shop-checkout' })
}
</script>

<template>
  <div v-show="cart.drawerOpen" class="drawer-backdrop" @click="cart.closeDrawer()"></div>

  <aside class="cart-drawer" :class="{ open: cart.drawerOpen }" :aria-hidden="!cart.drawerOpen" aria-label="购物车">
    <div class="drawer-header">
      <h2>购物车</h2>
      <button class="icon-button close-cart" type="button" aria-label="关闭购物车" @click="cart.closeDrawer()">
        ×
      </button>
    </div>

    <div class="cart-items">
      <article v-for="item in cart.items" :key="`${item.itemType}-${item.productId}`" class="cart-item">
        <img :src="item.productImage" :alt="item.productName" />
        <div>
          <h3>
            {{ item.productName }}
            <span v-if="item.itemType === 'BUNDLE'" class="cart-item-type">套餐</span>
          </h3>
          <div class="cart-line">
            <span>{{ formatPrice(item.price) }}</span>
            <div class="stepper" :aria-label="`${item.productName}数量`">
              <button
                type="button"
                :aria-label="`减少${item.productName}数量`"
                @click="step(item, -1)"
              >
                −
              </button>
              <input
                type="number"
                min="1"
                step="1"
                inputmode="numeric"
                :value="item.quantity"
                :aria-label="`${item.productName}数量`"
                @change="onQuantityChange(item, $event)"
                @keydown="onQuantityKeydown"
              />
              <button
                type="button"
                :aria-label="`增加${item.productName}数量`"
                @click="step(item, 1)"
              >
                +
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div v-show="!cart.hasItems" class="cart-empty">购物车还是空的，先挑一份毛孩子喜欢的吧。</div>

    <div class="cart-summary">
      <div class="cart-summary-line">
        <span>小计</span>
        <strong class="cart-total">{{ formatPrice(cart.totalAmount) }}</strong>
      </div>
      <!-- 优惠券行：登录用户展示（旧页 id=couponSelect），游客由组件内部隐藏 -->
      <CouponSelectField id="couponSelect" />
      <div class="cart-actions">
        <button class="clear-cart" type="button" :disabled="!cart.hasItems" @click="handleClear">
          清空
        </button>
        <button class="checkout-button" type="button" :disabled="!cart.hasItems" @click="goCheckout">
          去结算
        </button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
/* 套餐条目标识：金色小药丸，与单品行区分（套餐价即结算单价） */
.cart-item-type {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--gold);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  vertical-align: middle;
}
</style>
