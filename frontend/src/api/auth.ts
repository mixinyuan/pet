/**
 * 认证接口：对应后端 AuthController（/api/auth/**）。
 */
import { get, post } from './http'
import type { AuthResponse, LoginPayload, RegisterPayload, UserProfileVO } from '@/types/api'

/** 登录（账号支持用户名或邮箱），成功返回 JWT 与用户信息 */
export function login(payload: LoginPayload) {
  return post<AuthResponse>('/auth/login', payload)
}

/** 注册（用户名 + 邮箱 + 密码，昵称可选），成功即视为登录 */
export function register(payload: RegisterPayload) {
  return post<AuthResponse>('/auth/register', payload)
}

/** 登出（无状态接口，失败不阻塞本地清理） */
export function logout() {
  return post<void>('/auth/logout')
}

/** 获取当前登录用户信息（token 失效时后端返回 401，由拦截器统一处理） */
export function fetchMe() {
  return get<UserProfileVO>('/auth/me')
}
