/**
 * 管理端分类接口：对应后端 AdminCategoryController（/api/admin/categories）。
 * 列表含停用分类并带引用商品数；编码创建后不可修改；系统内置 all 不可删除。
 */
import { del, get, post, put } from '../http'
import type { AdminCategoryVO, CategoryCreatePayload, CategoryUpdatePayload, StatusUpdatePayload } from '@/types/admin'

/** 查询全部分类（含停用，按 sortOrder、id 升序，每项带引用商品数 productCount） */
export function fetchAdminCategories() {
  return get<AdminCategoryVO[]>('/admin/categories')
}

/** 查询单个分类（不存在时后端 404） */
export function fetchAdminCategory(id: number) {
  return get<AdminCategoryVO>(`/admin/categories/${id}`)
}

/** 新增分类（编码必填且全局唯一，不允许 all；状态默认 ACTIVE） */
export function createAdminCategory(payload: CategoryCreatePayload) {
  return post<AdminCategoryVO>('/admin/categories', payload)
}

/** 编辑分类（仅 name/description/sortOrder；编码不可改，不传 code 视为不变） */
export function updateAdminCategory(id: number, payload: CategoryUpdatePayload) {
  return put<AdminCategoryVO>(`/admin/categories/${id}`, payload)
}

/** 切换分类状态（ACTIVE 启用 / INACTIVE 停用，停用后不出现在 C 端导航） */
export function updateAdminCategoryStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/categories/${id}/status`, { status })
}

/** 删除分类（逻辑删除；系统内置 all 或被商品引用时后端拒绝） */
export function deleteAdminCategory(id: number) {
  return del<void>(`/admin/categories/${id}`)
}
