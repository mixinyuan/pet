/**
 * 购物车状态中心（Pinia）：迁移旧 script.js 的 cartState + couponState 及其派生逻辑。
 *
 * - 购物车：GET /api/cart 拉取，加购/改数量/删除/清空后用后端返回的完整 CartVO 覆盖本地态；
 * - 优惠券：登录用户才可持券，进入结算前拉取「我的优惠券」供选券下拉使用（券管理在个人中心任务）；
 * - 派生口径与旧页一致：小计=购物车总金额；券门槛/抵扣上限按「勾选条目小计之和」预判，
 *   实付 = max(小计 − 抵扣, 0)，最终校验以后端下单接口为准；
 * - 抽屉开合状态也放在这里，顶栏入口与抽屉组件共享。
 */
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'

import * as cartApi from '@/api/cart'
import type { CartItemType } from '@/api/cart'
import { fetchMyCoupons } from '@/api/coupon'
import { useAuthStore } from '@/stores/auth'
import type { CartItemVO, CartVO, CouponVO } from '@/types/shop'

/** 空购物车（后端在购物车不存在时也返回该形状） */
export const EMPTY_CART: CartVO = { items: [], count: 0, totalAmount: 0 }

/** 后端返回的 CartVO 可空字段归一化，避免视图层到处兜底 */
function normalizeCart(cart: CartVO | null | undefined): CartVO {
  return {
    items: Array.isArray(cart?.items) ? (cart.items as CartItemVO[]) : [],
    count: Number(cart?.count || 0),
    totalAmount: Number(cart?.totalAmount || 0),
  }
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    /** 当前购物车（已归一化：items 必为数组、count/totalAmount 必为数字） */
    cart: { ...EMPTY_CART } as CartVO,
    /** 是否成功拉取过购物车（区分「加载中」与「空车」） */
    loaded: false,
    /** 购物车抽屉开合 */
    drawerOpen: false,
    /** 我的优惠券列表（需登录，游客恒为空） */
    coupons: [] as CouponVO[],
    /** 是否已尝试拉取过优惠券（登录后进入结算/抽屉时懒加载一次） */
    couponsLoaded: false,
    /** 结算时选中的持券实例 id（null 表示不使用优惠券） */
    selectedCouponId: null as number | null,
  }),

  getters: {
    items: (state): CartItemVO[] => state.cart.items ?? [],
    count: (state): number => state.cart.count ?? 0,
    totalAmount: (state): number => Number(state.cart.totalAmount ?? 0),
    hasItems(): boolean {
      return this.items.length > 0
    },
    /** 参与结算的条目（后端仅结算勾选条目；当前加购默认勾选） */
    checkedItems(): CartItemVO[] {
      return this.items.filter((item) => item.checked !== false)
    },
    /** 勾选条目小计之和（券门槛/抵扣上限的预判口径） */
    checkedTotal(): number {
      return this.checkedItems.reduce((sum, item) => sum + Number(item.subtotal || 0), 0)
    },
    /** 下拉可展示的候选券：未使用且未过期（门槛是否满足在选择项上单独禁用） */
    candidateCoupons(): CouponVO[] {
      return this.coupons.filter((coupon) => Number(coupon.status) === 0 && Boolean(coupon.available))
    },
    /** 当前选中的券对象（未选中或已失效/门槛不满足时返回 null，对齐旧页 selectedCoupon） */
    activeCoupon(): CouponVO | null {
      if (this.selectedCouponId === null) return null
      const coupon = this.coupons.find((item) => Number(item.id) === Number(this.selectedCouponId))
      return coupon && this.isCouponUsable(coupon) ? coupon : null
    },
    /** 券抵扣金额（上限为勾选小计，不为负） */
    discountAmount(): number {
      return this.activeCoupon
        ? Math.min(Number(this.activeCoupon.amount || 0), this.checkedTotal)
        : 0
    },
    /** 预估实付金额（= 小计 − 抵扣，不为负；最终金额以后端下单结果为准） */
    payableAmount(): number {
      return Math.max(this.totalAmount - this.discountAmount, 0)
    },
    /** 返回判断函数：券是否可选（候选券且金额门槛已满足，对齐旧页 isCouponUsable） */
    isCouponUsable(): (coupon: CouponVO) => boolean {
      return (coupon: CouponVO): boolean =>
        Number(coupon.status) === 0 &&
        Boolean(coupon.available) &&
        this.checkedTotal >= Number(coupon.minSpend || 0)
    },
  },

  actions: {
    /** 拉取购物车并覆盖本地态（失败时抛出，由拦截器统一弹错） */
    async loadCart() {
      try {
        this.cart = normalizeCart(await cartApi.fetchCart())
      } finally {
        this.loaded = true
      }
    },

    /** 本地重置为空车（后端不可达等场景的兜底展示） */
    resetLocal() {
      this.cart = { ...EMPTY_CART }
    },

    /** 加入购物车（成功后弹出商品名提示，对齐旧页 addToCart） */
    async addToCart(productId: number, quantity = 1) {
      this.cart = normalizeCart(await cartApi.addCartItem(productId, quantity))
      const item = this.items.find(
        (row) => row.itemType !== 'BUNDLE' && Number(row.productId) === Number(productId),
      )
      ElMessage.success(`${item?.productName || '商品'}已加入购物车`)
      return this.cart
    },

    /** 套餐加入购物车（按套餐价结算，后端按组成商品校验库存） */
    async addBundleToCart(bundleId: number, quantity = 1) {
      this.cart = normalizeCart(await cartApi.addCartBundle(bundleId, quantity))
      const item = this.items.find(
        (row) => row.itemType === 'BUNDLE' && Number(row.productId) === Number(bundleId),
      )
      ElMessage.success(`${item?.productName || '套餐'}已加入购物车`)
      return this.cart
    },

    /**
     * 修改购买数量：数量归零/负数按旧逻辑转为删除条目；
     * 非法输入由视图层拦截，这里只发起请求；套餐条目由 item.itemType 决定 type 参数。
     */
    async updateQuantity(refId: number, quantity: number, type: CartItemType = 'PRODUCT') {
      if (quantity <= 0) {
        return this.removeItem(refId, type)
      }
      this.cart = normalizeCart(await cartApi.updateCartItem(refId, quantity, type))
      return this.cart
    },

    /** 删除购物车中指定条目（套餐条目传 type='BUNDLE'） */
    async removeItem(refId: number, type: CartItemType = 'PRODUCT') {
      this.cart = normalizeCart(await cartApi.removeCartItem(refId, type))
      return this.cart
    },

    /** 清空购物车（空车时直接跳过，对齐旧页 clearCart） */
    async clearCart() {
      if (!this.hasItems) return
      await cartApi.clearCartRequest()
      this.resetLocal()
      ElMessage.success('购物车已清空')
    },

    openDrawer() {
      this.drawerOpen = true
    },

    closeDrawer() {
      this.drawerOpen = false
    },

    /** 懒加载「我的优惠券」：游客直接清空；登录用户首次调用时拉取，失败静默降级（错误已由拦截器弹出） */
    async ensureCoupons() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.resetCoupons()
        return
      }
      if (this.couponsLoaded) return
      try {
        this.coupons = (await fetchMyCoupons()) || []
      } catch {
        this.coupons = []
      } finally {
        this.couponsLoaded = true
      }
    },

    /** 重置优惠券态（登出/游客） */
    resetCoupons() {
      this.coupons = []
      this.couponsLoaded = false
      this.selectedCouponId = null
    },

    /** 选中/取消结算用券（null 表示不使用） */
    selectCoupon(couponId: number | null) {
      this.selectedCouponId = couponId
    },
  },
})
