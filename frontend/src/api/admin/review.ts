/**
 * 管理端评价接口：对应后端 AdminReviewController（/api/admin/reviews）。
 * C 端评价由后台统一维护（C 端无发布入口）；隐藏（INACTIVE）后 C 端立即不展示；删除为逻辑删除。
 */
import { del, get, post, put } from '../http'
import type { PageResult } from '@/types/api'
import type { AdminReviewQuery, AdminReviewVO, ReviewSavePayload, StatusUpdatePayload } from '@/types/admin'

/** 分页查询评价（后台视角不过滤展示状态；keyword 匹配内容/评价人，records 含关联商品名 productName） */
export function fetchAdminReviews(query: AdminReviewQuery) {
  return get<PageResult<AdminReviewVO>>('/admin/reviews', { params: query })
}

/** 查询单条评价（含隐藏评价，与列表单条同构，供编辑弹窗回填） */
export function fetchAdminReview(id: number) {
  return get<AdminReviewVO>(`/admin/reviews/${id}`)
}

/** 新增评价（productId 可空，空=通用评价；非空时须存在且未删除，由服务层校验） */
export function createAdminReview(payload: ReviewSavePayload) {
  return post<AdminReviewVO>('/admin/reviews', payload)
}

/** 编辑评价（整体替换语义：表单覆盖的可编辑列全量更新，含冗余商品名随动同步） */
export function updateAdminReview(id: number, payload: ReviewSavePayload) {
  return put<AdminReviewVO>(`/admin/reviews/${id}`, payload)
}

/** 切换展示状态（ACTIVE 展示 / INACTIVE 隐藏，隐藏后 C 端立即不展示） */
export function updateAdminReviewStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/reviews/${id}/status`, { status })
}

/** 删除评价（逻辑删除，删除后不可在 C 端展示） */
export function deleteAdminReview(id: number) {
  return del<void>(`/admin/reviews/${id}`)
}
