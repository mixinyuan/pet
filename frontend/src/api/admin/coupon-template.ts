/**
 * 管理端优惠券模板接口：对应后端 AdminCouponTemplateController（/api/admin/coupon-templates）。
 * 全量列表不分页（模板量小），每项附已发放/已使用统计；
 * 发放规则（注册/订阅自动发）不变，停用后不再自动发放。
 */
import { del, get, post, put } from '../http'
import type { AdminCouponTemplateVO, CouponTemplateSavePayload, StatusUpdatePayload } from '@/types/admin'

/** 查询全部券模板（按 id 倒序，含停用模板，附 issuedCount/usedCount 统计） */
export function fetchAdminCouponTemplates() {
  return get<AdminCouponTemplateVO[]>('/admin/coupon-templates')
}

/** 查询单个券模板（含停用模板与统计，供编辑弹窗回填） */
export function fetchAdminCouponTemplate(id: number) {
  return get<AdminCouponTemplateVO>(`/admin/coupon-templates/${id}`)
}

/** 新增券模板（名称全局唯一，重复 409「券名称已存在：xxx」；状态默认 ACTIVE 可被自动发放） */
export function createAdminCouponTemplate(payload: CouponTemplateSavePayload) {
  return post<AdminCouponTemplateVO>('/admin/coupon-templates', payload)
}

/** 编辑券模板（仅名称/面额/门槛/有效天数/备注；user_coupon 无金额快照，编辑实时作用于存量持券） */
export function updateAdminCouponTemplate(id: number, payload: CouponTemplateSavePayload) {
  return put<AdminCouponTemplateVO>(`/admin/coupon-templates/${id}`, payload)
}

/** 切换模板状态（ACTIVE 启用恢复自动发放 / INACTIVE 停用不再自动发放，存量持券不受影响） */
export function updateAdminCouponTemplateStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/coupon-templates/${id}/status`, { status })
}

/** 删除券模板（物理删除；已发放数 > 0 时后端 400 拒绝，仅支持停用） */
export function deleteAdminCouponTemplate(id: number) {
  return del<void>(`/admin/coupon-templates/${id}`)
}
