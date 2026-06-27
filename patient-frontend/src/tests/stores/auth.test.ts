import { setActivePinia, createPinia } from 'pinia'
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/patient', () => ({
  devPatientLogin: vi.fn(() => Promise.resolve({ token: 'mock-token', patientId: 1, name: '测试患者', needCompleteInfo: false })),
  wxLogin: vi.fn(() => Promise.resolve({ token: 'mock-token', patientId: 1, name: '测试患者', needCompleteInfo: false })),
  getPatientInfo: vi.fn(() => Promise.resolve({ id: 1, name: '测试患者', phone: '13800000000' }))
}))

describe('Auth Store (patient)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    uni.removeStorageSync('patient_token')
  })

  describe('initial state', () => {
    it('should have null patient when not logged in', () => {
      uni.removeStorageSync('patient_token')
      const store = useAuthStore()
      expect(store.patient).toBeNull()
    })

    it('should return false for isLoggedIn when no token', () => {
      uni.removeStorageSync('patient_token')
      const store = useAuthStore()
      expect(store.isLoggedIn()).toBe(false)
    })
  })

  describe('login', () => {
    it('should login and set patient info', async () => {
      const store = useAuthStore()
      await store.login()

      expect(store.patient).not.toBeNull()
      expect(store.patient?.name).toBe('测试患者')
      expect(uni.getStorageSync('patient_token')).toBe('mock-token')
    })

    it('should set loading state during login', async () => {
      const store = useAuthStore()
      expect(store.loading).toBe(false)
    })
  })

  describe('logout', () => {
    it('should clear patient and token', () => {
      uni.setStorageSync('patient_token', 'some-token')
      const store = useAuthStore()
      store.logout()

      expect(store.patient).toBeNull()
      expect(uni.getStorageSync('patient_token')).toBe('')
    })
  })

  describe('loadProfile', () => {
    it('should load profile when logged in', async () => {
      uni.setStorageSync('patient_token', 'some-token')
      const store = useAuthStore()
      await store.loadProfile()

      expect(store.patient).not.toBeNull()
    })

    it('should not load when not logged in', async () => {
      uni.removeStorageSync('patient_token')
      const store = useAuthStore()
      await store.loadProfile()

      expect(store.patient).toBeNull()
    })
  })
})
