import { setActivePinia, createPinia } from 'pinia'
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'

const mockLogin = vi.hoisted(() => vi.fn())
const mockGetInfo = vi.hoisted(() => vi.fn())

vi.mock('@/api/lab', () => ({
  login: mockLogin,
  getInfo: mockGetInfo
}))

vi.stubGlobal('localStorage', {
  data: {} as Record<string, string>,
  getItem(key: string) { return this.data[key] ?? null },
  setItem(key: string, val: string) { this.data[key] = val },
  removeItem(key: string) { delete this.data[key] },
  clear() { this.data = {} }
})

describe('Auth Store (lab)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    localStorage.clear()
  })

  describe('initial state', () => {
    it('should have empty token and user when localStorage is empty', () => {
      localStorage.clear()
      const store = useAuthStore()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })

    it('should restore token from localStorage', () => {
      localStorage.setItem('lab_token', 'lab-token-xyz')
      const store = useAuthStore()
      expect(store.token).toBe('lab-token-xyz')
    })
  })

  describe('signIn', () => {
    it('should sign in and set token', async () => {
      mockLogin.mockResolvedValue({ data: { code: 200, data: 'signed-in-token' } })
      mockGetInfo.mockResolvedValue({ data: { code: 200, data: { id: 1, username: 'labuser', name: '检验员', role: 'lab' } } })

      const store = useAuthStore()
      await store.signIn('labuser', 'password')

      expect(mockLogin).toHaveBeenCalledWith('labuser', 'password')
      expect(store.token).toBe('signed-in-token')
      expect(localStorage.getItem('lab_token')).toBe('signed-in-token')
    })

    it('should load user info after sign in', async () => {
      const staffInfo = { id: 2, username: 'lab', name: '检验科', role: 'lab' }
      mockLogin.mockResolvedValue({ data: { code: 200, data: 'token' } })
      mockGetInfo.mockResolvedValue({ data: { code: 200, data: staffInfo } })

      const store = useAuthStore()
      await store.signIn('lab', 'pass')

      expect(store.user).toEqual(staffInfo)
    })

    it('should propagate sign in error', async () => {
      mockLogin.mockRejectedValue(new Error('用户名或密码错误'))

      const store = useAuthStore()
      await expect(store.signIn('wrong', 'wrong')).rejects.toThrow('用户名或密码错误')
    })
  })

  describe('signOut', () => {
    it('should clear all auth state', () => {
      localStorage.setItem('lab_token', 'token')

      const store = useAuthStore()
      store.signOut()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(localStorage.getItem('lab_token')).toBeNull()
    })
  })

  describe('loadUser', () => {
    it('should load user info when token exists', async () => {
      localStorage.setItem('lab_token', 'token')
      const staffInfo = { id: 3, username: 'lab3', name: '检验员3', role: 'lab' }
      mockGetInfo.mockResolvedValue({ data: { code: 200, data: staffInfo } })

      const store = useAuthStore()
      await store.loadUser()

      expect(store.user).toEqual(staffInfo)
    })

    it('should not load user when no token', async () => {
      localStorage.clear()
      const store = useAuthStore()
      await store.loadUser()

      expect(mockGetInfo).not.toHaveBeenCalled()
    })
  })
})
