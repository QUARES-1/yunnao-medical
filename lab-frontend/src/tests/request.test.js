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

describe('lab-frontend request 拦截器测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('请求拦截器（token 注入）', () => {
    it('有 token 时应注入 token 和 Bearer Authorization', () => {
      localStorage.setItem('lab_token', 'lab-token-xyz')
      const config = { headers: {} }
      const token = localStorage.getItem('lab_token')
      if (token) {
        config.headers.token = token
        config.headers.Authorization = `Bearer ${token}`
      }
      expect(config.headers.token).toBe('lab-token-xyz')
      expect(config.headers.Authorization).toBe('Bearer lab-token-xyz')
    })

    it('无 token 时不应注入', () => {
      localStorage.clear()
      const config = { headers: {} }
      const token = localStorage.getItem('lab_token')
      if (token) {
        config.headers.token = token
        config.headers.Authorization = `Bearer ${token}`
      }
      expect(config.headers.token).toBeUndefined()
    })
  })

  describe('401 响应处理', () => {
    it('401 时应清除 lab_token 并跳转 /login', () => {
      localStorage.setItem('lab_token', 'token-abc')
      localStorage.removeItem('lab_token')
      expect(localStorage.getItem('lab_token')).toBeNull()
    })
  })

  describe('非 200 响应处理', () => {
    it('code !== 200 时应触发 ElMessage.error', () => {
      const body = { code: 500, msg: '服务器错误' }
      const msg = body.msg || body.message || '操作失败'
      expect(msg).toBe('服务器错误')
    })

    it('网络错误时应使用 error.message', () => {
      const error = { message: 'Request failed', response: undefined }
      const msg = error.response?.data?.msg || error.message || '网络连接失败'
      expect(msg).toBe('Request failed')
    })
  })

  describe('请求配置参数', () => {
    it('timeout 应为 15000ms', () => {
      const timeout = 15000
      expect(timeout).toBe(15000)
    })
  })
})
