/**
 * 购物车接口：对应后端 CartController（/api/cart/**）。
 *
 * 游客与登录用户均可用（http.ts 自动携带 X-Guest-Id / Authorization），
 * 所有写操作的返回都是更新后的完整购物车（CartVO）。
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

/** 修改购物车中指定商品的购买数量（PUT /api/cart/items/{productId}，quantity 1~999） */
export function updateCartItem(productId: number, quantity: number) {
  return put<CartVO>(`/cart/items/${productId}`, { quantity })
}

/** 删除购物车中指定的商品条目（DELETE /api/cart/items/{productId}） */
export function removeCartItem(productId: number) {
  return del<CartVO>(`/cart/items/${productId}`)
}

/** 清空当前购物车（DELETE /api/cart） */
export function clearCartRequest() {
  return del<void>('/cart')
}
