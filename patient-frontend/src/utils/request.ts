import type { ApiResult } from '@/types/api'

let defaultApiBaseUrl = 'http://localhost:8080'
// #ifdef H5
defaultApiBaseUrl = import.meta.env.DEV ? 'http://localhost:8080' : ''
// #endif
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export function request<T>(options: {
  url: string
  method?: HttpMethod
  data?: Record<string, unknown>
  auth?: boolean
}): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('patient_token') as string
    const header: Record<string, string> = {}
    if (token && options.auth !== false) {
      header.Authorization = `Bearer ${token}`
    }
    if ((options.method || 'GET') !== 'GET') {
      header['Content-Type'] = 'application/json;charset=utf-8'
    }
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (response) => {
        const data = response.data as ApiResult<T>
        if (data.code === 200) return resolve(data.data)
        if (data.code === 401) {
          uni.removeStorageSync('patient_token')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
        }
        reject(new Error(data.msg || '请求失败'))
      },
      fail: () => reject(new Error('网络连接失败，请检查后端服务'))
    })
  })
}

export function showError(error: unknown) {
  const message = error instanceof Error ? error.message : '操作失败，请稍后重试'
  uni.showToast({ title: message, icon: 'none', duration: 2400 })
}

