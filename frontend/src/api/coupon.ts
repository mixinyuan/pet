/**
 * 优惠券接口：对应后端 CouponController（/api/coupons，需登录）。
 *
 * 考证结论：后端 CouponController 仅有「查询持券列表」一个接口，旧静态页
 * script.js 也只调用 GET /coupons（无任何领取动作）——新券由后端在注册/订阅时
 * 自动发放，因此商城侧不存在「可领优惠券列表 + 领取」接口，此处不虚构领取方法。
 */
import { get } from './http'
import type { CouponVO } from '@/types/shop'

/** 查询当前用户的持券列表（需登录，按领取时间倒序；游客请求会被后端 401 拒绝） */
export function fetchMyCoupons() {
  return get<CouponVO[]>('/coupons')
}
