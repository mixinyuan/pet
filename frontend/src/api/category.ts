/**
 * 商品分类接口：对应后端 CategoryController（/api/categories）。
 */
import { get } from './http'
import type { CategoryVO } from '@/types/shop'

/** 查询全部启用的商品分类（公开只读，按排序字段有序返回） */
export function fetchCategories() {
  return get<CategoryVO[]>('/categories')
}
