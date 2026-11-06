/**
 * 错误信息归一化：迁移自旧 script.js 的 normalizeError。
 * 业务错误的 ElMessage 弹窗由 axios 拦截器统一处理，这里仅供视图内联文案（如重试区）使用。
 */

/** 把 unknown 异常转成可展示的中文文案（后端未启动/网络中断时给出启动提示） */
export function toFriendlyError(error: unknown): string {
  const message = error instanceof Error ? error.message : ''
  if (!message || message.toLowerCase().includes('network error')) {
    return '无法连接后端服务，请确认 Spring Boot 已启动。'
  }
  return message || '操作失败，请稍后重试。'
}
