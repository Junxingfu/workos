import axios from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types'

/**
 * 统一响应解包：后端返回 { code, message, data }
 * code === 0 视为成功并直接返回 data，其余抛出可读错误
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const text = body.messageArray?.length ? body.messageArray.join('；') : body.message
      return Promise.reject(new Error(text || '操作失败'))
    }
    return body?.data
  },
  (error) => {
    const text = error?.response
      ? `请求失败（${error.response.status}）`
      : '无法连接后端服务，请确认后端已启动'
    return Promise.reject(new Error(text))
  }
)

export function fail(error: unknown) {
  message.error(error instanceof Error ? error.message : '操作失败')
}

export default http
