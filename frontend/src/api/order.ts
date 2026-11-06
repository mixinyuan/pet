/**
 * 订单接口：对应后端 OrderController（/api/orders/**）。
 *
 * 游客与登录用户均可用：下单从当前购物车勾选条目结算（可选携带优惠券实例 id），
 * 列表/详情/取消按 token 或 X-Guest-Id 定位归属。
 */
import { get, post } from './http'
import type { PageResult } from '@/types/api'
import type { CreateOrderPayload, OrderVO } from '@/types/shop'

/** 订单列表查询参数（page 从 1 开始，size 默认 20、上限 100） */
export interface OrderQuery {
  page: number
  size: number
}

/**
 * 从当前购物车结算并创建订单（POST /api/orders，body 可省略表示不使用券）
 *
 * 仅结算购物车中勾选（checked）的条目；couponId 为用户持券实例 id（user_coupon.id），
 * 仅登录用户可用券，游客携带该字段会被后端拒绝。
 */
export function createOrder(couponId?: number | null) {
  const body: CreateOrderPayload | undefined =
    couponId === null || couponId === undefined ? undefined : { couponId }
  return post<OrderVO>('/orders', body)
}

/** 分页查询当前身份的订单列表（按订单 id 倒序，最新在前） */
export function fetchOrders(query: OrderQuery) {
  return get<PageResult<OrderVO>>('/orders', { params: query })
}

/** 查询指定订单详情（订单不存在或不属于当前身份时后端返回 404） */
export function fetchOrderDetail(id: number) {
  return get<OrderVO>(`/orders/${id}`)
}

/** 取消订单（POST /api/orders/{id}/cancel，仅 CREATED 状态可取消；后端回补库存并返还已核销券） */
export function cancelOrder(id: number) {
  return post<OrderVO>(`/orders/${id}/cancel`)
}
