import { setActivePinia, createPinia } from 'pinia'
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'

const mockLogin = vi.hoisted(() => vi.fn())
const mockGetInfo = vi.hoisted(() => vi.fn())

vi.mock('@/api/pharmacy', () => ({
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

describe('Auth Store (pharmacy)', () => {
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
      localStorage.setItem('pharmacy_token', 'pharmacy-token-abc')
      const store = useAuthStore()
      expect(store.token).toBe('pharmacy-token-abc')
    })
  })

  describe('signIn', () => {
    it('should sign in and set token', async () => {
      mockLogin.mockResolvedValue({ data: { code: 200, data: 'signed-token' } })
      mockGetInfo.mockResolvedValue({ data: { code: 200, data: { id: 1, username: 'pharma', name: '药师', role: 'pharmacy' } } })

      const store = useAuthStore()
      await store.signIn('pharma', 'password')

      expect(mockLogin).toHaveBeenCalledWith('pharma', 'password')
      expect(store.token).toBe('signed-token')
      expect(localStorage.getItem('pharmacy_token')).toBe('signed-token')
    })

    it('should load user after sign in', async () => {
      const staff = { id: 2, username: 'pharma2', name: '药师2', role: 'pharmacy' }
      mockLogin.mockResolvedValue({ data: { code: 200, data: 'token' } })
      mockGetInfo.mockResolvedValue({ data: { code: 200, data: staff } })

      const store = useAuthStore()
      await store.signIn('pharma2', 'pass')

      expect(store.user).toEqual(staff)
    })

    it('should propagate sign in error', async () => {
      mockLogin.mockRejectedValue(new Error('用户名或密码错误'))

      const store = useAuthStore()
      await expect(store.signIn('wrong', 'wrong')).rejects.toThrow('用户名或密码错误')
    })
  })

  describe('signOut', () => {
    it('should clear all auth state', () => {
      localStorage.setItem('pharmacy_token', 'token')

      const store = useAuthStore()
      store.signOut()

      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(localStorage.getItem('pharmacy_token')).toBeNull()
    })
  })

  describe('loadUser', () => {
    it('should load user when token exists', async () => {
      localStorage.setItem('pharmacy_token', 'token')
      const staff = { id: 3, username: 'pharma3', name: '药师3', role: 'pharmacy' }
      mockGetInfo.mockResolvedValue({ data: { code: 200, data: staff } })

      const store = useAuthStore()
      await store.loadUser()

      expect(store.user).toEqual(staff)
    })

    it('should not load user when no token', async () => {
      localStorage.clear()
      const store = useAuthStore()
      await store.loadUser()

      expect(mockGetInfo).not.toHaveBeenCalled()
    })
  })
})
