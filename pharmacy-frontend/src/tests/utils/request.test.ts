import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.stubGlobal('localStorage', {
  data: {} as Record<string, string>,
  getItem(key: string) { return this.data[key] ?? null },
  setItem(key: string, val: string) { this.data[key] = val },
  removeItem(key: string) { delete this.data[key] },
  clear() { this.data = {} }
})

vi.stubGlobal('location', { pathname: '/dashboard', href: '' })

vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))

import requestInstance from '@/utils/request'

describe('Request Utils (pharmacy-frontend)', () => {
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
    it('should attach token and Bearer header from localStorage', () => {
      localStorage.setItem('pharmacy_token', 'pharmacy-token-xyz')
      const config = { headers: {} as Record<string, string>, method: 'get' as const, url: '/api/test' }
      requestInstance.interceptors.request.handlers[0].fulfilled(config)
      expect(config.headers.token).toBe('pharmacy-token-xyz')
      expect(config.headers.Authorization).toBe('Bearer pharmacy-token-xyz')
    })

    it('should not attach token when localStorage is empty', () => {
      localStorage.clear()
      const config = { headers: {} as Record<string, string>, method: 'get' as const, url: '/api/test' }
      requestInstance.interceptors.request.handlers[0].fulfilled(config)
      expect(config.headers.token).toBeUndefined()
    })
  })

  describe('response interceptor (success)', () => {
    it('should return response when code is 200', async () => {
      const response = {
        data: { code: 200, msg: '', data: { id: 1 } },
        status: 200, statusText: 'OK', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      const result = await handler.fulfilled(response)
      expect(result).toBe(response)
    })
  })

  describe('response interceptor (non-200)', () => {
    it('should reject with msg when code is not 200', async () => {
      const response = {
        data: { code: 400, msg: '操作失败' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('操作失败')
    })

    it('should use message when msg is empty', async () => {
      const response = {
        data: { code: 500, message: '服务端错误' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('服务端错误')
    })

    it('should use empty string when both msg and message are empty', async () => {
      const response = {
        data: { code: 500, msg: '', message: '' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('')
    })
  })

  describe('response interceptor (401)', () => {
    it('should clear token on 401 status', async () => {
      localStorage.setItem('pharmacy_token', 'test-token')
      const error = {
        response: { status: 401, data: { code: 401 } },
        message: 'Unauthorized', request: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow()
      expect(localStorage.getItem('pharmacy_token')).toBeNull()
    })

    it('should clear token on 401 in response data', async () => {
      localStorage.setItem('pharmacy_token', 'test-token')
      const error = {
        response: { status: 200, data: { code: 401 } },
        message: '', request: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow()
      expect(localStorage.getItem('pharmacy_token')).toBeNull()
    })

    it('should not clear token on other HTTP errors', async () => {
      localStorage.setItem('pharmacy_token', 'test-token')
      const error = {
        response: { status: 404, data: { code: 404 } },
        message: 'Not Found', request: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow()
      expect(localStorage.getItem('pharmacy_token')).toBe('test-token')
    })
  })

  describe('response interceptor (network error)', () => {
    it('should reject on network error without response', async () => {
      const error = { message: 'Network Error', request: {}, response: undefined }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow('Network Error')
    })

    it('should show error on HTTP error status', async () => {
      const error = {
        message: 'Server Error',
        request: {},
        response: { status: 500, data: undefined, statusText: '', headers: {}, config: {} }
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow()
    })
  })
})
