/**
 * 管理端商品接口：对应后端 AdminProductController（/api/admin/products）。
 * 列表为后台视角（不过滤上架状态）；新增/编辑共用全量表单，编辑为整体替换语义。
 */
import { del, get, post, put } from '../http'
import type { PageResult } from '@/types/api'
import type { AdminProductQuery, AdminProductVO, ProductSavePayload, StatusUpdatePayload } from '@/types/admin'

/** 分页查询商品（keyword 匹配名称/SKU/口味；category 传分类 code；status 为 ACTIVE/INACTIVE，均可选） */
export function fetchAdminProducts(query: AdminProductQuery) {
  return get<PageResult<AdminProductVO>>('/admin/products', { params: query })
}

/** 查询商品详情（含下架商品，供编辑表单回填；不存在时后端 404） */
export function fetchAdminProduct(id: number) {
  return get<AdminProductVO>(`/admin/products/${id}`)
}

/** 新增商品（SKU 全局唯一；分类至少 1 个且不允许包含内置 all，由服务层校验） */
export function createAdminProduct(payload: ProductSavePayload) {
  return post<AdminProductVO>('/admin/products', payload)
}

/** 编辑商品（主表全量更新，分类/标签关联整体替换，事务内完成） */
export function updateAdminProduct(id: number, payload: ProductSavePayload) {
  return put<AdminProductVO>(`/admin/products/${id}`, payload)
}

/** 切换上架状态（ACTIVE 上架 / INACTIVE 下架，下架商品 C 端立即不可见） */
export function updateAdminProductStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/products/${id}/status`, { status })
}

/** 删除商品（逻辑删除，历史订单快照不受影响；被未删除套餐引用时后端拒绝） */
export function deleteAdminProduct(id: number) {
  return del<void>(`/admin/products/${id}`)
}
