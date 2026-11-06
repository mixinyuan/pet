/**
 * 管理端数据看板：对应后端 AdminDashboardController（/api/admin/dashboard）。
 */
import { get } from './http'
import type { AdminDashboardVO } from '@/types/api'

export function fetchAdminDashboard() {
  return get<AdminDashboardVO>('/admin/dashboard')
}
