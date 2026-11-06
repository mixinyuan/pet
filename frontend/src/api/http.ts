/**
 * Axios 实例与拦截器：所有 API 模块共用。
 *
 * - baseURL 为 /api，开发期由 Vite proxy 转发到 localhost:8080，生产由 Nginx 反代；
 * - 请求拦截：登录用户附加 Authorization: Bearer <token>，游客数据关联 X-Guest-Id；
 * - 响应拦截：统一解包 ApiResponse（code=0 直接返回 data），业务失败 ElMessage 提示后 reject；
 * - 401 处理：清空本地登录态并跳转登录页（管理端跳 /admin/login，商城跳 /login）。
 */
import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Router } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types/api'
import { getGuestId } from '@/utils/guest'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  // 游客购物车/订单关联：后端按 X-Guest-Id 归档游客数据，登录用户也带上无副作用
  config.headers['X-Guest-Id'] = getGuestId()
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as ApiResponse
    if (body.code === 0) {
      // 统一解包：必须直接返回业务数据本身——若 return response，
      // 调用方（get/post 封装）拿到的是 AxiosResponse 包装，所有字段都是 undefined。
      // as never 仅为满足 axios 拦截器签名（AxiosResponse | Promise<AxiosResponse>）。
      return body.data as never
    }
    // 业务失败：HTTP 200 但 code 非 0（后端部分校验错误走该通道）
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message)) as Promise<AxiosResponse>
  },
  (error: AxiosError<ApiResponse>) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    if (status === 401) {
      const auth = useAuthStore()
      auth.clearLocal()
      const current = routerRef.currentRoute.value
      if (current.path.startsWith('/admin')) {
        if (current.path !== '/admin/login') {
          routerRef.push({ path: '/admin/login', query: { redirect: current.fullPath } })
        }
      } else if (current.path !== '/login') {
        routerRef.push({ path: '/login', query: { redirect: current.fullPath } })
      }
      ElMessage.error(message || '登录已过期，请重新登录')
    } else {
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

// router 由 main.ts 启动时注入，避免 http → router → store → api 的模块循环依赖在加载期触发
let routerRef!: Router

export function bindRouter(router: Router) {
  routerRef = router
}

/** 响应拦截器已解包 ApiResponse，这里补上类型断言，调用方拿到的直接是 data */
export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.get(url, config) as unknown as Promise<T>
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, body, config) as unknown as Promise<T>
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.put(url, body, config) as unknown as Promise<T>
}

export async function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.patch(url, body, config) as unknown as Promise<T>
}

export async function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.delete(url, config) as unknown as Promise<T>
}

export default http
