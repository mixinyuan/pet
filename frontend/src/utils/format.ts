/**
 * 商城展示格式化工具：迁移自旧 script.js 的 formatPrice / formatDateTime / couponStatusText。
 */

/** 价格展示：整数位 ¥ 前缀（与旧页面一致，如 ¥89） */
export function formatPrice(value: number | null | undefined): string {
  return `¥${Number(value || 0).toFixed(0)}`
}

/** 后端 LocalDateTime 字符串（yyyy-MM-dd HH:mm:ss）→ yyyy-MM-dd HH:mm */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 订单状态 → 中文文案（迁移自旧 script.js 的 orderStatusText；CREATED 可取消） */
export function orderStatusText(status: string | null | undefined): string {
  const statusMap: Record<string, string> = {
    CREATED: '已创建',
    CANCELLED: '已取消',
  }
  return statusMap[status || ''] || status || '未知'
}

/** 优惠券展示状态 → 中文文案（迁移自旧 script.js 的 couponStatusText：0-未使用 1-已使用 2-已过期） */
export function couponStatusText(status: number | null | undefined): string {
  const statusMap: Record<number, string> = {
    0: '可用',
    1: '已使用',
    2: '已过期',
  }
  return statusMap[Number(status)] || '未知'
}
