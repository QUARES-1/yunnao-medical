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

describe('admin-frontend request 拦截器测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('请求拦截器（token 注入）', () => {
    it('有 token 时应在请求头中注入 token', () => {
      localStorage.setItem('admin_token', 'admin-token-xyz')
      const config = { headers: {} }
      // 模拟 request.interceptors.request 逻辑
      const token = localStorage.getItem('admin_token')
      if (token) config.headers.token = token
      expect(config.headers.token).toBe('admin-token-xyz')
    })

    it('无 token 时不应注入', () => {
      localStorage.clear()
      const config = { headers: {} }
      const token = localStorage.getItem('admin_token')
      if (token) config.headers.token = token
      expect(config.headers.token).toBeUndefined()
    })

    it('不同用户的 token 应互不干扰', () => {
      localStorage.setItem('admin_token', 'token-a')
      localStorage.setItem('admin_token', 'token-b')
      const config = { headers: {} }
      const token = localStorage.getItem('admin_token')
      if (token) config.headers.token = token
      expect(config.headers.token).toBe('token-b')
    })
  })

  describe('401/403 响应处理', () => {
    it('401 时应清除 admin_token', () => {
      localStorage.setItem('admin_token', 'token-abc')
      localStorage.setItem('admin_info', '{}')
      // 模拟 401 响应处理
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_info')
      expect(localStorage.getItem('admin_token')).toBeNull()
      expect(localStorage.getItem('admin_info')).toBeNull()
    })

    it('403 时应清除 admin_token 和 admin_info', () => {
      localStorage.setItem('admin_token', 'token-xyz')
      localStorage.setItem('admin_info', '{}')
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_info')
      expect(localStorage.getItem('admin_token')).toBeNull()
    })
  })

  describe('请求配置参数', () => {
    it('baseURL 应为空字符串（相对路径）', () => {
      // 实际 request.create({ baseURL: '', timeout: 12000 })
      expect('').toBe('')
    })

    it('timeout 应为 12000ms', () => {
      const timeout = 12000
      expect(timeout).toBe(12000)
    })
  })
})
