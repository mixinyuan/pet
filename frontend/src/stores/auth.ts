/**
 * 登录态中心：token / 用户信息的唯一事实来源（Pinia）。
 *
 * token 与 user 持久化到 localStorage（key 前缀 pawbites.auth），
 * 刷新页面后仍保持登录；登出或 401 时统一走 clearLocal。
 */
import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import type { LoginPayload, RegisterPayload, UserProfileVO } from '@/types/api'

const TOKEN_KEY = 'pawbites.auth.token'
const USER_KEY = 'pawbites.auth.user'

function readStoredUser(): UserProfileVO | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserProfileVO) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    /** 展示名：昵称优先，缺省回退用户名 */
    displayName: (state) => state.user?.nickname || state.user?.username || '',
  },

  actions: {
    async login(payload: LoginPayload) {
      const data = await authApi.login(payload)
      this.applyAuth(data.token, data.user)
      return data
    },

    async register(payload: RegisterPayload) {
      const data = await authApi.register(payload)
      this.applyAuth(data.token, data.user)
      return data
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 后端登出失败不阻塞本地清理（token 过期等场景）
      }
      this.clearLocal()
    },

    /** 用 /auth/me 校验本地 token 并刷新用户信息，返回是否仍处于登录态 */
    async refreshUser() {
      if (!this.token) return false
      try {
        this.user = await authApi.fetchMe()
        localStorage.setItem(USER_KEY, JSON.stringify(this.user))
        return true
      } catch {
        this.clearLocal()
        return false
      }
    },

    applyAuth(token: string, user: UserProfileVO) {
      this.token = token
      this.user = user
      try {
        localStorage.setItem(TOKEN_KEY, token)
        localStorage.setItem(USER_KEY, JSON.stringify(user))
      } catch {
        // localStorage 不可用时仅保留内存态
      }
    },

    clearLocal() {
      this.token = ''
      this.user = null
      try {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
      } catch {
        // 忽略
      }
    },
  },
})
