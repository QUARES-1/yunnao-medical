import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.stubGlobal('localStorage', {
  data: {},
  getItem(key) { return this.data[key] ?? null },
  setItem(key, val) { this.data[key] = val },
  removeItem(key) { delete this.data[key] },
  clear() { this.data = {} }
})

vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn() } }))
vi.mock('@/router', () => ({ default: { push: vi.fn() } }))

import requestInstance from '@/utils/request'

describe('Request Utils (doctor-frontend)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('should create axios instance with correct baseURL and timeout', () => {
    expect(requestInstance.defaults.baseURL).toBe('http://localhost:8080')
    expect(requestInstance.defaults.timeout).toBe(10000)
  })

  it('should register request interceptor', () => {
    expect(requestInstance.interceptors.request.handlers.length).toBeGreaterThan(0)
  })

  it('should register response interceptor', () => {
    expect(requestInstance.interceptors.response.handlers.length).toBeGreaterThan(0)
  })

  describe('request interceptor (token injection)', () => {
    it('should attach token from localStorage', () => {
      localStorage.setItem('token', 'doctor-token-xyz')
      const config = { headers: {}, method: 'get', url: '/api/test' }
      requestInstance.interceptors.request.handlers[0].fulfilled(config)
      expect(config.headers.token).toBe('doctor-token-xyz')
    })

    it('should not attach token when localStorage is empty', () => {
      localStorage.clear()
      const config = { headers: {}, method: 'get', url: '/api/test' }
      requestInstance.interceptors.request.handlers[0].fulfilled(config)
      expect(config.headers.token).toBeUndefined()
    })
  })

  describe('response interceptor (success)', () => {
    it('should return res data when code is 200', async () => {
      const response = {
        data: { code: 200, msg: '', data: { id: 1 } },
        status: 200, statusText: 'OK', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      const result = await handler.fulfilled(response)
      expect(result).toEqual({ code: 200, msg: '', data: { id: 1 } })
    })
  })

  describe('response interceptor (401)', () => {
    it('should clear token and doctorInfo and redirect on 401', async () => {
      localStorage.setItem('token', 'doctor-token')
      localStorage.setItem('doctorInfo', JSON.stringify({ id: 1 }))
      const response = {
        data: { code: 401, msg: '登录已过期，请重新登录' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('登录已过期，请重新登录')
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('doctorInfo')).toBeNull()
    })
  })

  describe('response interceptor (non-200 non-401)', () => {
    it('should reject with msg on code 400', async () => {
      const response = {
        data: { code: 400, msg: '操作失败' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('操作失败')
    })

    it('should reject with empty message when msg is empty', async () => {
      const response = {
        data: { code: 500, msg: '' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('')
    })
  })

  describe('response interceptor (network error)', () => {
    it('should reject on network error', async () => {
      const error = { message: 'Network Error', request: {}, response: undefined }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow('Network Error')
    })
  })
})

describe('Request Utils (doctor-frontend)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should create axios instance with correct baseURL and timeout', () => {
    expect(requestInstance.defaults.baseURL).toBe('http://localhost:8080')
    expect(requestInstance.defaults.timeout).toBe(10000)
  })

  it('should register request interceptor', () => {
    expect(requestInstance.interceptors.request.handlers.length).toBeGreaterThan(0)
  })

  it('should register response interceptor', () => {
    expect(requestInstance.interceptors.response.handlers.length).toBeGreaterThan(0)
  })

  describe('request interceptor (token injection)', () => {
    it('should attach token from localStorage', () => {
      localStorage.setItem('token', 'doctor-token-xyz')
      const config = { headers: {}, method: 'get', url: '/api/test' }
      requestInstance.interceptors.request.handlers[0].fulfilled(config)
      expect(config.headers.token).toBe('doctor-token-xyz')
    })

    it('should not attach token when localStorage is empty', () => {
      localStorage.clear()
      const config = { headers: {}, method: 'get', url: '/api/test' }
      requestInstance.interceptors.request.handlers[0].fulfilled(config)
      expect(config.headers.token).toBeUndefined()
    })
  })

  describe('response interceptor (success)', () => {
    it('should return res data when code is 200', async () => {
      const response = {
        data: { code: 200, msg: '', data: { id: 1 } },
        status: 200, statusText: 'OK', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      const result = await handler.fulfilled(response)
      expect(result).toEqual({ code: 200, msg: '', data: { id: 1 } })
    })
  })

  describe('response interceptor (401)', () => {
    it('should clear token and doctorInfo and redirect on 401', async () => {
      localStorage.setItem('token', 'doctor-token')
      localStorage.setItem('doctorInfo', JSON.stringify({ id: 1 }))
      const response = {
        data: { code: 401, msg: '登录已过期，请重新登录' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('登录已过期，请重新登录')
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('doctorInfo')).toBeNull()
    })
  })

  describe('response interceptor (non-200 non-401)', () => {
    it('should reject with msg on code 400', async () => {
      const response = {
        data: { code: 400, msg: '操作失败' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('操作失败')
    })

    it('should reject with empty message when msg is empty', async () => {
      const response = {
        data: { code: 500, msg: '' },
        status: 200, statusText: '', headers: {}, config: {}
      }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.fulfilled(response)).rejects.toThrow('')
    })
  })

  describe('request interceptor (error path)', () => {
    it('should reject when getItem throws', async () => {
      const origGetItem = localStorage.getItem
      localStorage.getItem = vi.fn(() => { throw new Error('Storage unavailable') })
      try {
        const config = { headers: {}, method: 'get', url: '/api/test' }
        const handler = requestInstance.interceptors.request.handlers[0]
        await expect(handler.rejected(new Error('Storage unavailable'))).rejects.toThrow('Storage unavailable')
      } finally {
        localStorage.getItem = origGetItem
      }
    })
  })

  describe('response interceptor (network error)', () => {
    it('should reject on network error', async () => {
      const error = { message: 'Network Error', request: {}, response: undefined }
      const handler = requestInstance.interceptors.response.handlers[0]
      await expect(handler.rejected(error)).rejects.toThrow('Network Error')
    })
  })
})
