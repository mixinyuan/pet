/**
 * 购物车接口：对应后端 CartController（/api/cart/**）。
 *
 * 游客与登录用户均可用（http.ts 自动携带 X-Guest-Id / Authorization），
 * 所有写操作的返回都是更新后的完整购物车（CartVO）。
 * 条目分单品（PRODUCT）与套餐（BUNDLE）两类：加购时 productId/bundleId 二选一；
 * 改量/删除按「关联 id + type」定位，套餐条目必须带 type=BUNDLE。
 */
import { del, get, post, put } from './http'
import type { CartVO } from '@/types/shop'

/** 查询当前购物车（购物车不存在时后端返回空购物车，不报错） */
export function fetchCart() {
  return get<CartVO>('/cart')
}

/**
 * 添加商品到购物车（POST /api/cart/items）
 * 同一商品重复添加会合并数量；数量缺省按 1 件计。
 */
export function addCartItem(productId: number, quantity = 1) {
  return post<CartVO>('/cart/items', { productId, quantity })
}

/**
 * 添加套餐到购物车（POST /api/cart/items）
 * 按套餐价结算，库存按组成商品校验；同一套餐重复添加会合并数量。
 */
export function addCartBundle(bundleId: number, quantity = 1) {
  return post<CartVO>('/cart/items', { bundleId, quantity })
}

/** 条目类型（改量/删除接口的 type 查询参数） */
export type CartItemType = 'PRODUCT' | 'BUNDLE'

/**
 * 修改购物车中指定条目的购买数量（PUT /api/cart/items/{id}?type=，quantity 1~999）
 * 套餐条目传 type='BUNDLE'（id 为套餐 id）。
 */
export function updateCartItem(refId: number, quantity: number, type: CartItemType = 'PRODUCT') {
  return put<CartVO>(`/cart/items/${refId}`, { quantity }, { params: { type } })
}

/** 删除购物车中指定的条目（DELETE /api/cart/items/{id}?type=，套餐条目传 type='BUNDLE'） */
export function removeCartItem(refId: number, type: CartItemType = 'PRODUCT') {
  return del<CartVO>(`/cart/items/${refId}`, { params: { type } })
}

/** 清空当前购物车（DELETE /api/cart） */
export function clearCartRequest() {
  return del<void>('/cart')
}
