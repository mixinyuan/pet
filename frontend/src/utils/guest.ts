/**
 * 游客身份工具：购物车/订单接口对游客开放，通过 X-Guest-Id 请求头关联游客数据。
 * 沿用旧版静态页的存储键 pawbites-guest-id，迁移期间游客购物车无缝衔接。
 */

const GUEST_STORAGE_KEY = 'pawbites-guest-id'

/** 获取（不存在则生成并持久化）游客 ID，localStorage 不可用时回退内存副本 */
export function getGuestId(): string {
  try {
    let guestId = localStorage.getItem(GUEST_STORAGE_KEY)
    if (!guestId) {
      guestId = crypto.randomUUID()
      localStorage.setItem(GUEST_STORAGE_KEY, guestId)
    }
    return guestId
  } catch {
    // 隐私模式等场景 localStorage 可能不可用，退化为会话内固定值
    guestIdFallback ??= crypto.randomUUID()
    return guestIdFallback
  }
}

let guestIdFallback: string | undefined
