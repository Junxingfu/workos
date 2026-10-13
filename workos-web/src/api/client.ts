import axios, { type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types'

/**
 * 统一响应解包：后端返回 { code, message, data }
 * code === 0 视为成功并直接返回 data，其余抛出可读错误
 */
const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

client.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const text = body.messageArray?.length ? body.messageArray.join('；') : body.message
      return Promise.reject(new Error(text || '操作失败'))
    }
    return response
  },
  (error) => {
    const text = error?.response
      ? `请求失败（${error.response.status}）`
      : import.meta.env.VITE_WORKOS_DESKTOP === 'true'
        ? '本地工作区暂时无法连接，请稍后重试；持续失败可退出并重新打开 WorkOS'
        : '无法连接后端服务，请确认后端已启动'
    return Promise.reject(new Error(text))
  }
)

export function fail(error: unknown) {
  message.error(error instanceof Error ? error.message : '操作失败')
}

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResponse<T>>(config)
  return response.data.data
}

const http = {
  get: <T>(url: string, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'GET', url }),
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'POST', url, data }),
  put: <T>(url: string, data?: unknown) => request<T>({ method: 'PUT', url, data }),
  delete: <T>(url: string) => request<T>({ method: 'DELETE', url })
}

export default http
