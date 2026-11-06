/**
 * 管理端日志接口：对应后端 AdminLogsController（/api/admin/logs，更新日志/通知）。
 * 注意与其它模块的两点契约差异（沿自旧 LogsController）：
 * - 分页参数名是 pageSize（不是 size），响应为 {total, list}（不是 PageResult{records,total}）；
 * - 发布状态是数字 0/1（不是 ACTIVE/INACTIVE 字符串）。
 */
import { del, get, post, put } from '../http'
import type {
  AdminLogQuery,
  CreateLogPayload,
  UpdateLogDetailVO,
  UpdateLogPageVO,
  UpdateLogPayload,
} from '@/types/admin'

/** 分页查询日志（keyword 按标题模糊匹配，create_time 倒序） */
export function fetchAdminLogs(query: AdminLogQuery) {
  return get<UpdateLogPageVO>('/admin/logs', { params: query })
}

/** 查询日志详情（列表行不含 content，编辑弹窗回填前先拉详情） */
export function fetchAdminLog(id: number) {
  return get<UpdateLogDetailVO>(`/admin/logs/${id}`)
}

/** 新增日志（title/content 必填；type 空默认「AI自习室通知」；status 缺省 0 未发布） */
export function createAdminLog(payload: CreateLogPayload) {
  return post<UpdateLogDetailVO>('/admin/logs', payload)
}

/** 编辑日志（仅 title/type/content；发布状态走独立 /status 接口，切换不更新 create_time） */
export function updateAdminLog(id: number, payload: UpdateLogPayload) {
  return put<UpdateLogDetailVO>(`/admin/logs/${id}`, payload)
}

/** 切换发布状态（status：0-未发布 1-已发布） */
export function updateAdminLogStatus(id: number, status: number) {
  return put<void>(`/admin/logs/${id}/status`, { status })
}

/** 删除日志（物理删除，不可恢复） */
export function deleteAdminLog(id: number) {
  return del<void>(`/admin/logs/${id}`)
}
