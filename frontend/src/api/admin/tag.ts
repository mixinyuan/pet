/**
 * 管理端标签接口：对应后端 AdminTagController（/api/admin/tags）。
 * 标签仅名称一个可编辑字段（需全局唯一）；状态切换走独立 /status 接口。
 */
import { del, get, post, put } from '../http'
import type { AdminTagVO, StatusUpdatePayload, TagSavePayload } from '@/types/admin'

/** 查询全部标签（含停用，按 id 升序，每项带引用商品数 productCount） */
export function fetchAdminTags() {
  return get<AdminTagVO[]>('/admin/tags')
}

/** 新增标签（名称全局唯一，新建默认启用 ACTIVE） */
export function createAdminTag(payload: TagSavePayload) {
  return post<AdminTagVO>('/admin/tags', payload)
}

/** 标签改名（需保持全局唯一；状态切换走 /status 接口） */
export function updateAdminTag(id: number, payload: TagSavePayload) {
  return put<AdminTagVO>(`/admin/tags/${id}`, payload)
}

/** 切换标签状态（ACTIVE 启用 / INACTIVE 停用） */
export function updateAdminTagStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/tags/${id}/status`, { status })
}

/** 删除标签（逻辑删除；被商品引用时后端拒绝并提示引用数） */
export function deleteAdminTag(id: number) {
  return del<void>(`/admin/tags/${id}`)
}
