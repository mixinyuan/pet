/**
 * 场景套餐接口：对应后端 BundleController（/api/bundles）。
 */
import { get } from './http'
import type { BundleVO } from '@/types/shop'

/** 查询全部在售优惠套餐（含套餐价、原价、折扣标签与商品清单） */
export function fetchBundles() {
  return get<BundleVO[]>('/bundles')
}
