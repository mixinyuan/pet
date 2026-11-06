/**
 * 商品接口：对应后端 ProductController（/api/products/**）。
 */
import { get } from './http'
import type { PageResult } from '@/types/api'
import type { ProductVO } from '@/types/shop'

/** 商品分页查询参数（page 从 1 开始，size 1~100，category=all 表示全部） */
export interface ProductQuery {
  page: number
  size: number
  /** 分类 code，缺省 all */
  category?: string
  /** 名称/口味/宠物类型关键字模糊搜索，可选，最长 64 字符 */
  keyword?: string
}

/** 分页查询在售商品（结果按评分、id 倒序） */
export function fetchProducts(query: ProductQuery) {
  return get<PageResult<ProductVO>>('/products', { params: query })
}

/** 查询指定商品详情（商品不存在或已下架时后端返回 404） */
export function fetchProduct(id: number | string) {
  return get<ProductVO>(`/products/${id}`)
}
