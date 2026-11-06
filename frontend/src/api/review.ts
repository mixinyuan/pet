/**
 * 用户评价接口：对应后端 ReviewController（/api/reviews）。
 */
import { get } from './http'
import type { ReviewVO } from '@/types/shop'

/** 查询展示中的用户评价（productId 缺省时返回全部） */
export function fetchReviews(productId?: number) {
  return get<ReviewVO[]>('/reviews', { params: productId === undefined ? {} : { productId } })
}
