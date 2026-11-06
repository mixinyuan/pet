/**
 * 与后端契约对齐的公共类型定义。
 * 来源：backend/src/main/java/com/pawbites/petshop/result/ApiResponse.java 与 vo/**。
 * 迁移页面时按需补充，字段名与 Java record 保持一致（后端 Jackson 驼峰序列化）。
 */

/** 统一响应体：code=0 成功，非 0 失败（通常与 HTTP 状态码对齐） */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** 分页结果：对齐后端 result/PageResult.java（records 当前页数据，page 从 1 开始） */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 用户信息（不含密码等敏感字段） */
export interface UserProfileVO {
  id: number
  username: string
  email: string | null
  nickname: string | null
  /** USER-普通用户 ADMIN-管理员 */
  role: 'USER' | 'ADMIN'
}

/** 登录/注册成功响应 */
export interface AuthResponse {
  token: string
  user: UserProfileVO
  role: 'USER' | 'ADMIN'
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
  nickname?: string
}

/** 管理端数据看板 —— 统计卡片 */
export interface AdminDashboardSummaryVO {
  todayOrderCount: number
  todaySalesAmount: number
  todayOrderDelta: number
  userTotal: number
  adminCount: number
  lowStockCount: number
}

/** 管理端数据看板 —— 最近订单简表 */
export interface AdminDashboardRecentOrderVO {
  id: number
  orderNo: string
  /** 下单人展示名（游客单为「游客 + guestId 前 6 位」） */
  buyerName: string
  itemCount: number
  totalAmount: number
  /** CREATED-已创建 / CANCELLED-已取消 */
  status: 'CREATED' | 'CANCELLED'
  createdAt: string
}

/** 管理端数据看板 —— 热销 TOP5 */
export interface AdminDashboardTopProductVO {
  productId: number
  productName: string
  soldQuantity: number
  salesAmount: number
}

/** 管理端数据看板 —— 库存预警（stock < 20，含已下架） */
export interface AdminDashboardLowStockVO {
  productId: number
  productName: string
  sku: string
  stock: number
  /** ACTIVE-上架 / INACTIVE-下架 */
  status: 'ACTIVE' | 'INACTIVE'
}

/** 管理端数据看板聚合视图（GET /api/admin/dashboard） */
export interface AdminDashboardVO {
  summary: AdminDashboardSummaryVO
  recentOrders: AdminDashboardRecentOrderVO[]
  topProducts: AdminDashboardTopProductVO[]
  lowStockProducts: AdminDashboardLowStockVO[]
}
