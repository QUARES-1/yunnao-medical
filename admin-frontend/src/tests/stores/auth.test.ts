import { setActivePinia, createPinia } from 'pinia'
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'

const mockLoginAdmin = vi.hoisted(() => vi.fn())
const mockGetAdminInfo = vi.hoisted(() => vi.fn())

vi.mock('@/api/admin', () => ({
  loginAdmin: mockLoginAdmin,
  getAdminInfo: mockGetAdminInfo
}))

vi.stubGlobal('localStorage', {
  data: {} as Record<string, string>,
  getItem(key: string) { return this.data[key] ?? null },
  setItem(key: string, val: string) { this.data[key] = val },
  removeItem(key: string) { delete this.data[key] },
  clear() { this.data = {} }
})

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    localStorage.clear()
  })

  describe('initial state', () => {
    it('should have empty token and admin when localStorage is empty', () => {
      localStorage.clear()
      const store = useAuthStore()
      expect(store.token).toBe('')
      expect(store.admin).toBeNull()
      expect(store.isLoggedIn).toBe(false)
    })

    it('should restore token from localStorage', () => {
      localStorage.setItem('admin_token', 'test-token-123')
      const store = useAuthStore()
      expect(store.token).toBe('test-token-123')
      expect(store.isLoggedIn).toBe(true)
    })

    it('should restore admin info from localStorage', () => {
      localStorage.setItem('admin_token', 'token')
      localStorage.setItem('admin_info', JSON.stringify({ id: 1, username: 'admin', name: '管理员' }))
      const store = useAuthStore()
      expect(store.admin).toEqual({ id: 1, username: 'admin', name: '管理员' })
    })
  })

  describe('login', () => {
    it('should login successfully and set token', async () => {
      mockLoginAdmin.mockResolvedValue({ data: { code: 200, msg: '', data: 'new-token' } })
      mockGetAdminInfo.mockResolvedValue({ data: { code: 200, msg: '', data: { id: 1, username: 'admin', name: 'Admin' } } })

      const store = useAuthStore()
      await store.login('admin', 'password')

      expect(mockLoginAdmin).toHaveBeenCalledWith('admin', 'password')
      expect(store.token).toBe('new-token')
      expect(localStorage.getItem('admin_token')).toBe('new-token')
      expect(store.isLoggedIn).toBe(true)
    })

    it('should set admin info after login', async () => {
      const adminData = { id: 1, username: 'admin', name: '管理员' }
      mockLoginAdmin.mockResolvedValue({ data: { code: 200, msg: '', data: 'token' } })
      mockGetAdminInfo.mockResolvedValue({ data: { code: 200, msg: '', data: adminData } })

      const store = useAuthStore()
      await store.login('admin', 'password')

      expect(store.admin).toEqual(adminData)
      expect(localStorage.getItem('admin_info')).toBe(JSON.stringify(adminData))
    })

    it('should propagate login error', async () => {
      mockLoginAdmin.mockRejectedValue(new Error('用户名或密码错误'))

      const store = useAuthStore()
      await expect(store.login('wrong', 'wrong')).rejects.toThrow('用户名或密码错误')
    })
  })

  describe('logout', () => {
    it('should clear all auth state', () => {
      localStorage.setItem('admin_token', 'token')
      localStorage.setItem('admin_info', JSON.stringify({ id: 1, username: 'admin', name: 'Admin' }))

      const store = useAuthStore()
      store.logout()

      expect(store.token).toBe('')
      expect(store.admin).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(localStorage.getItem('admin_token')).toBeNull()
      expect(localStorage.getItem('admin_info')).toBeNull()
    })
  })

  describe('loadInfo', () => {
    it('should load admin info from API', async () => {
      const adminData = { id: 2, username: 'admin2', name: '管理员2' }
      mockGetAdminInfo.mockResolvedValue({ data: { code: 200, msg: '', data: adminData } })

      const store = useAuthStore()
      await store.loadInfo()

      expect(mockGetAdminInfo).toHaveBeenCalled()
      expect(store.admin).toEqual(adminData)
    })

    it('should update localStorage after loading info', async () => {
      const adminData = { id: 3, username: 'admin3', name: '管理员3' }
      mockGetAdminInfo.mockResolvedValue({ data: { code: 200, msg: '', data: adminData } })

      const store = useAuthStore()
      await store.loadInfo()

      expect(localStorage.getItem('admin_info')).toBe(JSON.stringify(adminData))
    })
  })
})
