/**
 * 邮件订阅接口：对应后端 SubscriptionController（/api/subscriptions）。
 */
import { post } from './http'
import type { SubscriptionVO } from '@/types/shop'

/** 提交邮箱订阅（同一邮箱重复提交视为重新激活；登录态下额外发放新人券） */
export function subscribe(email: string) {
  return post<SubscriptionVO>('/subscriptions', { email })
}
