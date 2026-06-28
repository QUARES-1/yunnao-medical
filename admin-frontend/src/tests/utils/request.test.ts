import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

vi.stubGlobal('localStorage', {
  data: {} as Record<string, string>,
  getItem(key: string) { return this.data[key] ?? null },
  setItem(key: string, val: string) { this.data[key] = val },
  removeItem(key: string) { delete this.data[key] },
  clear() { this.data = {} }
})

vi.stubGlobal('location', { pathname: '/dashboard', href: '' })

vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))

import { default as requestInstance } from '@/utils/request'

describe('Request Utils (admin-frontend)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should create axios instance with correct timeout', () => {
    expect(requestInstance.defaults.timeout).toBe(12000)
  })

  it('should register request interceptor', () => {
    expect(requestInstance.interceptors.request.handlers.length).toBeGreaterThan(0)
  })

  it('should register response interceptor', () => {
    expect(requestInstance.interceptors.response.handlers.length).toBeGreaterThan(0)
  })

  describe('request interceptor (token injection)', () => {
    it('should attach admin_token from localStorage', () => {
      localStorage.setItem('admin_token', 'admin-token-xyz')
      const config = { headers: {} as Record<string, string>, method: 'get', url: '/api/test' }
      const handler = requestInstance.interceptors.request.handlers[0]
      handler.fulfilled(config)
      expect(config.headers.token).toBe('admin-token-xyz')
    })

    it('should not attach token when localStorage is empty', () => {
      localStorage.clear()
      const config = { headers: {} as Record<string, string>, method: 'get', url: '/api/test' }
      const handler = requestInstance.interceptors.request.handlers[0]
      handler.fulfilled(config)
      expect(config.headers.token).toBeUndefined()
    })
  })

  describe('response interceptor (success path)', () => {
    it('should return response on code 200', async () => {
      const response = {
        data: { code: 200, msg: '', data: { id: 1 } },
        status: 200, statusText: 'OK', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      const result = await handler.fulfilled(response)
      expect(result).toBe(response)
    })
  })

  describe('response interceptor (error handling)', () => {
    it('should clear token and reject on code 401', async () => {
      localStorage.setItem('admin_token', 'token')
      localStorage.setItem('admin_info', JSON.stringify({ id: 1 }))
      const response = {
        data: { code: 401, msg: '登录状态已失效', data: null },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('登录状态已失效')
      expect(localStorage.getItem('admin_token')).toBeNull()
      expect(localStorage.getItem('admin_info')).toBeNull()
    })

    it('should clear token and reject on code 403', async () => {
      localStorage.setItem('admin_token', 'token')
      localStorage.setItem('admin_info', JSON.stringify({ id: 1 }))
      const response = {
        data: { code: 403, msg: '禁止访问', data: null },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('禁止访问')
      expect(localStorage.getItem('admin_token')).toBeNull()
    })

    it('should reject with error message on code 400', async () => {
      const response = {
        data: { code: 400, msg: '操作失败', data: null },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('操作失败')
    })

    it('should use default message when msg is empty', async () => {
      const response = {
        data: { code: 500, msg: '', data: null },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('请求失败')
    })
  })

  describe('response interceptor (network error)', () => {
    it('should reject on network error', async () => {
      const error = { message: 'Network Error', request: {}, response: undefined }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow('Network Error')
    })

    it('should reject on HTTP error status', async () => {
      const error = {
        message: 'Request failed',
        request: {},
        response: { status: 500, data: undefined, statusText: '', headers: {}, config: {} }
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow()
    })

    it('should show error with status code 404', async () => {
      const error = {
        message: 'Not Found',
        request: {},
        response: { status: 404, data: undefined, statusText: '', headers: {}, config: {} }
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow()
    })
  })
})
