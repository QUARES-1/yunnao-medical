import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { usePatientAiStore } from '@/stores/ai'
import { usePrescriptionsStore } from '@/stores/prescriptions'
import { useRegistrationsStore } from '@/stores/registrations'
import '@/tests/setup.js'

// Mock API modules to control request behavior
vi.mock('@/api/patient', () => ({
  devPatientLogin: vi.fn(),
  getPatientInfo: vi.fn(),
  getPatientPrescriptions: vi.fn(),
  getRegistrations: vi.fn(),
  testPatientLogin: vi.fn(),
  wxLogin: vi.fn(),
}))

// Mock streamSse to prevent real HTTP calls
const { streamSseMock } = vi.hoisted(() => {
  const fn = vi.fn()
  return { streamSseMock: fn }
})
vi.mock('@/utils/sse', () => ({ streamSse: streamSseMock }))

import { getPatientInfo, getPatientPrescriptions, getRegistrations, testPatientLogin } from '@/api/patient'

describe('patient stores', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    streamSseMock.mockImplementation(async ({ handlers }: any) => {
      if (handlers.onDone) await handlers.onDone('{}')
    })
  })

  // =============================================
  // auth store
  // =============================================
  describe('auth store', () => {
    it('isLoggedIn returns true when token exists', () => {
      uni.getStorageSync.mockReturnValue('valid-token')
      const store = useAuthStore()
      expect(store.isLoggedIn()).toBe(true)
    })

    it('isLoggedIn returns false when token is empty', () => {
      uni.getStorageSync.mockReturnValue('')
      const store = useAuthStore()
      expect(store.isLoggedIn()).toBe(false)
    })

    it('logout clears patient and token', () => {
      const store = useAuthStore()
      store.logout()
      expect(uni.removeStorageSync).toHaveBeenCalledWith('patient_token')
    })

    it('loginWithTestAccount stores token and profile', async () => {
      vi.mocked(testPatientLogin).mockResolvedValue({ token: 'test-token', patientId: 1 } as any)
      vi.mocked(getPatientInfo).mockResolvedValue({ name: '测试', id: 1 } as any)
      const store = useAuthStore()
      await store.loginWithTestAccount('acc', 'pwd')
      expect(uni.setStorageSync).toHaveBeenCalledWith('patient_token', 'test-token')
      expect(store.patient?.name).toBe('测试')
    })

    it('loadProfile re-throws non-token errors', async () => {
      uni.getStorageSync.mockReturnValue('token')
      vi.mocked(getPatientInfo).mockRejectedValue(new Error('服务器错误'))
      const store = useAuthStore()
      await expect(store.loadProfile()).rejects.toThrow('服务器错误')
    })

    it('loadProfile logs out on patient-not-found error', async () => {
      uni.getStorageSync.mockReturnValue('token')
      vi.mocked(getPatientInfo).mockRejectedValue(new Error('患者不存在'))
      const store = useAuthStore()
      await expect(store.loadProfile()).rejects.toThrow()
      expect(uni.removeStorageSync).toHaveBeenCalledWith('patient_token')
    })
  })

  // =============================================
  // prescriptions store
  // =============================================
  describe('prescriptions store', () => {
    it('load returns early when already loading', async () => {
      const store = usePrescriptionsStore()
      store.loading = true
      await store.load()
      expect(getPatientPrescriptions).not.toHaveBeenCalled()
    })

    it('loadMore returns early when hasMore is false', async () => {
      const store = usePrescriptionsStore()
      store.hasMore = false as any
      await store.loadMore()
      expect(getPatientPrescriptions).not.toHaveBeenCalled()
    })

    it('loadMore returns early when loading', async () => {
      const store = usePrescriptionsStore()
      store.loading = true
      store.hasMore = true as any
      await store.loadMore()
      expect(getPatientPrescriptions).not.toHaveBeenCalled()
    })

    it('load reset clears records and sets fresh data', async () => {
      vi.mocked(getPatientPrescriptions).mockResolvedValue({
        content: [{ id: 1 } as any],
        totalElements: 1,
        totalPages: 1
      })
      const store = usePrescriptionsStore()
      await store.load(true)
      expect(store.records).toHaveLength(1)
      expect(store.page).toBe(1)
    })

    it('load appends records without reset', async () => {
      vi.mocked(getPatientPrescriptions).mockResolvedValue({
        content: [{ id: 2 } as any],
        totalElements: 2,
        totalPages: 2
      })
      const store = usePrescriptionsStore()
      store.records = [{ id: 1 }] as any
      await store.load(false)
      expect(store.records).toHaveLength(2)
      // page is incremented by load(), then reset to 1 in finally after load() resolves
      expect(store.page).toBe(1)
    })

    it('load handles missing content gracefully', async () => {
      vi.mocked(getPatientPrescriptions).mockResolvedValue({
        content: undefined as any,
        totalElements: 0
      })
      const store = usePrescriptionsStore()
      await store.load(true)
      expect(store.records).toHaveLength(0)
    })

    it('load handles missing totalPages gracefully', async () => {
      vi.mocked(getPatientPrescriptions).mockResolvedValue({
        content: [] as any,
        totalElements: 0
      })
      const store = usePrescriptionsStore()
      await store.load(true)
      expect(store.hasMore).toBe(false)
    })
  })

  // =============================================
  // registrations store
  // =============================================
  describe('registrations store', () => {
    it('load returns early when no patient_token', async () => {
      uni.getStorageSync.mockReturnValue('')
      const store = useRegistrationsStore()
      await store.load()
      expect(store.records).toEqual([])
    })

    it('load fetches records when patient_token exists', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      vi.mocked(getRegistrations).mockResolvedValue({
        content: [{ id: 1, status: '待就诊' } as any]
      })
      const store = useRegistrationsStore()
      await store.load()
      expect(store.records).toHaveLength(1)
      expect(store.activeStatus).toBe('')
    })

    it('load filters by status and sets activeStatus', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      vi.mocked(getRegistrations).mockResolvedValue({
        content: [{ id: 1, status: '已就诊' } as any]
      })
      const store = useRegistrationsStore()
      await store.load('已就诊')
      expect(store.records).toHaveLength(1)
      expect(store.activeStatus).toBe('已就诊')
    })

    it('changeStatus updates activeStatus and reloads', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      vi.mocked(getRegistrations).mockResolvedValue({ content: [] as any })
      const store = useRegistrationsStore()
      await store.changeStatus('待就诊')
      expect(store.activeStatus).toBe('待就诊')
      expect(getRegistrations).toHaveBeenCalled()
    })

    it('computed allCount returns correct length', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      vi.mocked(getRegistrations).mockResolvedValue({
        content: [{ id: 1 } as any, { id: 2 } as any]
      })
      const store = useRegistrationsStore()
      await store.load()
      expect(store.allCount).toBe(2)
    })

    it('computed pendingCount returns correct count', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      vi.mocked(getRegistrations).mockResolvedValue({
        content: [{ id: 1, status: '待就诊' } as any, { id: 2, status: '已就诊' } as any]
      })
      const store = useRegistrationsStore()
      await store.load()
      expect(store.pendingCount).toBe(1)
    })
  })

  // =============================================
  // AI store
  // =============================================
  describe('ai store', () => {
    it('ask returns early when question is empty', async () => {
      const store = usePatientAiStore()
      await store.ask('   ')
      expect(streamSseMock).not.toHaveBeenCalled()
    })

    it('ask returns early when loading', async () => {
      const store = usePatientAiStore()
      store.loading = true
      await store.ask('test')
      expect(streamSseMock).not.toHaveBeenCalled()
    })

    it('ask returns early when streaming', async () => {
      const store = usePatientAiStore()
      store.streaming = true
      await store.ask('test')
      expect(streamSseMock).not.toHaveBeenCalled()
    })

    it('ask uses unauthenticated endpoint when not logged in', async () => {
      uni.getStorageSync.mockReturnValue('')
      const store = usePatientAiStore()
      await store.ask('test question')
      expect(streamSseMock).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/api/ai/chat/stream' })
      )
    })

    it('ask sends sessionId when not logged in', async () => {
      uni.getStorageSync.mockReturnValue('')
      const store = usePatientAiStore()
      const initialSid = store.sessionId
      await store.ask('test')
      expect(streamSseMock).toHaveBeenCalledWith(
        expect.objectContaining({ data: expect.objectContaining({ sessionId: initialSid }) })
      )
    })

    it('ask uses authenticated endpoint when logged in', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      const store = usePatientAiStore()
      await store.ask('test')
      expect(streamSseMock).toHaveBeenCalledWith(
        expect.objectContaining({ url: '/api/ai/health-consult/stream' })
      )
    })

    it('ask sends includeHistory when logged in', async () => {
      uni.getStorageSync.mockReturnValue('patient-token')
      const store = usePatientAiStore()
      await store.ask('test')
      expect(streamSseMock).toHaveBeenCalledWith(
        expect.objectContaining({ data: expect.objectContaining({ includeHistory: true }) })
      )
    })

    it('ask appends user and ai messages', async () => {
      uni.getStorageSync.mockReturnValue('')
      streamSseMock.mockImplementationOnce(async ({ handlers }: any) => {
        if (handlers.onDelta) handlers.onDelta('response content')
        if (handlers.onDone) await handlers.onDone('{}')
      })
      const store = usePatientAiStore()
      const initialLen = store.messages.length
      await store.ask('hello')
      expect(store.messages.length).toBeGreaterThan(initialLen)
      expect(store.messages.some(m => m.role === 'user' && m.content === 'hello')).toBe(true)
    })

    it('ask sets default message when no content returned', async () => {
      uni.getStorageSync.mockReturnValue('')
      streamSseMock.mockImplementationOnce(async ({ handlers }: any) => {
        if (handlers.onDone) await handlers.onDone('')
      })
      const store = usePatientAiStore()
      await store.ask('test')
      // Welcome message exists (content: '你好呀...'), ask adds new AI msg (empty -> default)
      // find returns welcome message first; use findLast to get the newly added one
      const aiMsg = [...store.messages].reverse().find(m => m.role === 'ai')
      expect(aiMsg?.content).toContain('线下')
    })

    it('onDone parses related questions and department from JSON', async () => {
      uni.getStorageSync.mockReturnValue('')
      streamSseMock.mockImplementationOnce(async ({ handlers }: any) => {
        if (handlers.onDone) {
          await handlers.onDone(JSON.stringify({ relatedQuestions: ['Q1'], recommendDepartment: '内科', recommendDepartmentId: 1 }))
        }
      })
      const store = usePatientAiStore()
      await store.ask('headache')
      const aiMsg = [...store.messages].reverse().find(m => m.role === 'ai')
      expect(aiMsg?.related).toContain('Q1')
      expect(aiMsg?.department).toBe('内科')
      expect(aiMsg?.departmentId).toBe(1)
    })

    it('onDone handles malformed JSON gracefully', async () => {
      uni.getStorageSync.mockReturnValue('')
      streamSseMock.mockImplementationOnce(async ({ handlers }: any) => {
        if (handlers.onDone) await handlers.onDone('not-valid-json')
      })
      const store = usePatientAiStore()
      await store.ask('test')
      const aiMsg = [...store.messages].reverse().find(m => m.role === 'ai')
      expect(aiMsg).toBeDefined()
      expect(aiMsg?.related).toBeUndefined()
    })

    it('onError throws with message from handler', async () => {
      uni.getStorageSync.mockReturnValue('')
      streamSseMock.mockImplementationOnce(async ({ handlers }: any) => {
        if (handlers.onError) handlers.onError('stream failed')
      })
      const store = usePatientAiStore()
      await expect(store.ask('test')).rejects.toThrow('stream failed')
    })

    it('resetConversation resets sessionId and messages to welcome', () => {
      vi.useFakeTimers()
      const store = usePatientAiStore()
      const oldSid = store.sessionId
      const ADVANCE = 1000
      vi.advanceTimersByTime(ADVANCE)
      store.resetConversation()
      expect(store.sessionId).not.toBe(oldSid)
      expect(store.messages).toHaveLength(1)
      expect(store.messages[0].role).toBe('ai')
      expect(store.messages[0].content).toContain('新')
      vi.useRealTimers()
    })
  })
})
