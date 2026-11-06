/**
 * 管理端用户接口：对应后端 AdminUserController（/api/admin/users）。
 * 只读查阅（关键字/状态分页列表 + 聚合详情）与账号禁用/启用。
 */
import { get, put } from '../http'
import type { PageResult } from '@/types/api'
import type { AdminUserDetailVO, AdminUserQuery, AdminUserVO, StatusUpdatePayload } from '@/types/admin'

/** 分页查询用户（keyword 匹配用户名/昵称/邮箱；status 为 ACTIVE/INACTIVE，均可选；records 含订单/消费/持券汇总） */
export function fetchAdminUsers(query: AdminUserQuery) {
  return get<PageResult<AdminUserVO>>('/admin/users', { params: query })
}

/** 查询用户详情（聚合视图：基本资料含角色 + 全部订单 + 收藏 + 持券 + 邮箱订阅状态） */
export function fetchAdminUser(id: number) {
  return get<AdminUserDetailVO>(`/admin/users/${id}`)
}

/** 切换账号状态（ACTIVE 启用 / INACTIVE 禁用；禁用不允许作用于 role=ADMIN 的账号，由服务层校验） */
export function updateAdminUserStatus(id: number, status: StatusUpdatePayload['status']) {
  return put<void>(`/admin/users/${id}/status`, { status })
}
