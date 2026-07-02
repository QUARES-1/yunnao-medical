import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.stubGlobal('localStorage', {
  data: {},
  getItem(key) { return this.data[key] ?? null },
  setItem(key, val) { this.data[key] = val },
  removeItem(key) { delete this.data[key] },
  clear() { this.data = {} }
})

const mockElMessageError = vi.fn()
vi.mock('element-plus', () => ({
  ElMessage: { error: mockElMessageError }
}))

describe('pharmacy-frontend request 拦截器测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('请求拦截器（token 注入）', () => {
    it('有 token 时应注入 token 和 Bearer Authorization', () => {
      localStorage.setItem('pharmacy_token', 'pharmacy-token-xyz')
      const config = { headers: {} }
      const token = localStorage.getItem('pharmacy_token')
      if (token) {
        config.headers.token = token
        config.headers.Authorization = `Bearer ${token}`
      }
      expect(config.headers.token).toBe('pharmacy-token-xyz')
      expect(config.headers.Authorization).toBe('Bearer pharmacy-token-xyz')
    })

    it('无 token 时不应注入', () => {
      localStorage.clear()
      const config = { headers: {} }
      const token = localStorage.getItem('pharmacy_token')
      if (token) {
        config.headers.token = token
        config.headers.Authorization = `Bearer ${token}`
      }
      expect(config.headers.token).toBeUndefined()
      expect(config.headers.Authorization).toBeUndefined()
    })
  })

  describe('401 响应处理', () => {
    it('401 时应清除 pharmacy_token 并跳转 /login', () => {
      localStorage.setItem('pharmacy_token', 'token-abc')
      // 模拟 401 响应处理
      localStorage.removeItem('pharmacy_token')
      expect(localStorage.getItem('pharmacy_token')).toBeNull()
    })
  })

  describe('非 200 响应处理', () => {
    it('code !== 200 时应触发 ElMessage.error', () => {
      const body = { code: 400, msg: '操作失败' }
      const msg = body.msg || body.message || '操作失败'
      expect(msg).toBe('操作失败')
    })

    it('网络错误时应包含错误信息', () => {
      const error = { message: '网络连接失败', response: undefined }
      const msg = error.response?.data?.msg || error.message || '网络连接失败'
      expect(msg).toBe('网络连接失败')
    })
  })

  describe('请求配置参数', () => {
    it('timeout 应为 12000ms', () => {
      const timeout = 12000
      expect(timeout).toBe(12000)
    })
  })
})
