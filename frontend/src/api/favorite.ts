/**
 * 收藏接口：对应后端 FavoriteController（/api/favorites，全部需登录）。
 *
 * - 列表按收藏时间倒序，联查商品卡片字段（名称/图片/价格/petType/flavor/评分）；
 * - 收藏/取消均幂等（重复收藏、取消未收藏都直接返回成功）。
 */
import { del, get, post } from './http'
import type { FavoriteVO } from '@/types/shop'

/** 查询当前用户的收藏列表（需登录，游客请求会被后端 401 拒绝） */
export function fetchFavorites() {
  return get<FavoriteVO[]>('/favorites')
}

/** 收藏指定商品（POST /api/favorites/{productId}，重复收藏幂等） */
export function addFavorite(productId: number) {
  return post<void>(`/favorites/${productId}`)
}

/** 取消收藏指定商品（DELETE /api/favorites/{productId}，未收藏时同样成功） */
export function removeFavorite(productId: number) {
  return del<void>(`/favorites/${productId}`)
}
