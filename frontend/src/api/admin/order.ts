/**
 * 管理端订单接口：对应后端 AdminOrderController（/api/admin/orders）。
 * 只读查阅（多条件分页列表 + 详情含商品快照）与管理员兜底取消。
 */
import { get, post } from '../http'
import type { PageResult } from '@/types/api'
import type { AdminOrderDetailVO, AdminOrderQuery, AdminOrderVO } from '@/types/admin'

/** 分页查询订单（orderNo 精确 / keyword 模糊 / status / from-to 日期闭区间，均可选；用户单与游客单均可见） */
export function fetchAdminOrders(query: AdminOrderQuery) {
  return get<PageResult<AdminOrderVO>>('/admin/orders', { params: query })
}

/** 查询订单详情（列表行字段之上补充商品快照、商品合计与下单用户信息） */
export function fetchAdminOrder(id: number) {
  return get<AdminOrderDetailVO>(`/admin/orders/${id}`)
}

/** 管理员取消订单（仅 CREATED 可取消；不可逆：回补商品库存 + 返还优惠券，cancel_by=ADMIN） */
export function cancelAdminOrder(id: number) {
  return post<AdminOrderDetailVO>(`/admin/orders/${id}/cancel`)
}
