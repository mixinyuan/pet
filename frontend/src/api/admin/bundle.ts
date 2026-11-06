/**
 * 管理端场景套餐接口：对应后端 AdminBundleController（/api/admin/bundles）。
 * 列表为后台视角（不过滤上架状态，含下架套餐）；新增/编辑共用全量表单，明细整体替换。
 */
import { del, get, post, put } from '../http'
import type { PageResult } from '@/types/api'
import type { AdminBundleQuery, AdminBundleVO, BundleSavePayload, StatusUpdatePayload } from '@/types/admin'

/** 分页查询套餐（keyword 套餐名模糊；status 为 ACTIVE/INACTIVE，均可选；records 含明细与汇总字段） */
export function fetchAdminBundles(query: AdminBundleQuery) {
  return get<PageResult<AdminBundleVO>>('/admin/bundles', { params: query })
}

/** 查询套餐详情（含明细子表，供编辑弹窗回填；不存在时后端 404） */
export function fetchAdminBundle(id: number) {
  return get<AdminBundleVO>(`/admin/bundles/${id}`)
}

/** 新增套餐（原价 ≥ 现价、明细 ≥ 1 行、同一商品不可重复，由服务层校验） */
export function createAdminBundle(payload: BundleSavePayload) {
  return post<AdminBundleVO>('/admin/bundles', payload)
}

/** 编辑套餐（整体替换：主表全量更新，明细先删后插；套餐与订单无关联，不影响历史数据） */
export function updateAdminBundle(id: number, payload: BundleSavePayload) {
  return put<AdminBundleVO>(`/admin/bundles/${id}`, payload)
}

/** 切换上架状态（ACTIVE 上架 / INACTIVE 下架，下架套餐 C 端立即不可见） */
export function updateAdminBundleStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/bundles/${id}/status`, { status })
}

/** 删除套餐（bundle 主表逻辑删除，C 端套餐区立即不再展示） */
export function deleteAdminBundle(id: number) {
  return del<void>(`/admin/bundles/${id}`)
}
