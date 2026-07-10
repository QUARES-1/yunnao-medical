import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn()
}))
const sseMock = vi.hoisted(() => vi.fn())

vi.mock('@/utils/request', () => ({ default: requestMock }))
vi.mock('@/utils/sse', () => ({ streamSse: sseMock }))
// parseJson is NOT mocked — tests in parseJson.test.ts cover it directly

import * as labApi from '@/api/lab'
import { useAuthStore } from '@/stores/auth'
import { useLabStore } from '@/stores/lab'

describe('lab source modules', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    requestMock.post.mockResolvedValue({ data: { data: 'lab-token' } })
    requestMock.put.mockResolvedValue({ data: { data: 'ok' } })
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/staff/info') return Promise.resolve({ data: { data: { id: 1, name: 'Lab' } } })
      if (url === '/api/examination/lab/list') return Promise.resolve({ data: { data: { content: [{ id: 1, status: '待检查' }], totalElements: 1 } } })
      if (url.startsWith('/api/examination/detail/')) return Promise.resolve({ data: { data: { id: 1, itemName: 'CT' } } })
      if (url === '/api/examination/ai/review-list') return Promise.resolve({ data: { data: { content: [{ id: 9, examinationId: 1 }], totalElements: 1 } } })
      if (url === '/api/examination/ai/manual-list') return Promise.resolve({ data: { data: { content: [{ id: 10 }], totalElements: 1 } } })
      if (url === '/api/examination/ai/review-stats') return Promise.resolve({ data: { data: { total: 2, passCount: 1, manualCount: 1, rejectCount: 0, passRate: 50 } } })
      if (url.startsWith('/api/examination/ai/review-detail/')) return Promise.resolve({ data: { data: { id: 9, reviewResult: 'manual' } } })
      return Promise.resolve({ data: { data: [] } })
    })
  })

  it('calls API wrappers and updates auth store', async () => {
    labApi.login('u', 'p')
    labApi.changePassword('old', 'new')
    labApi.getItems('CT')
    labApi.getItems()
    labApi.uploadFile(new File(['x'], 'a.txt'))
    labApi.updateResult({ id: 1, result: 'normal' })
    labApi.getCriticalWarnings()
    labApi.reviewExamination(1)
    labApi.getReviewList()
    labApi.manualConfirm(1)
    labApi.rejectReview(1, 'bad')
    expect(requestMock.post).toHaveBeenCalledWith('/api/staff/login', { username: 'u', password: 'p', role: 'lab' })
    expect(requestMock.put).toHaveBeenCalledWith('/api/staff/change-password', { oldPassword: 'old', newPassword: 'new' })

    const auth = useAuthStore()
    await auth.signIn('u', 'p')
    expect(auth.token).toBe('lab-token')
    expect(auth.user?.name).toBe('Lab')
    auth.signOut()
    expect(auth.token).toBe('')
    await auth.loadUser()
    expect(auth.user).toBeNull()
  })

  it('loads lab dashboard state and handles streaming review callbacks', async () => {
    const store = useLabStore()
    await store.loadExaminations('待检查')
    await store.loadExaminations('已完成')
    await store.loadReviewDashboard('manual')
    await store.selectExamination(1)
    await store.selectReview(9)
    expect(store.pendingCount).toBe(1)
    expect(store.completedCount).toBe(1)
    expect(store.abnormalReviewCount).toBe(1)

    sseMock.mockImplementation(async (_url, options) => {
      options.handlers.onDelta('chunk')
      await options.handlers.onDone(JSON.stringify({ id: 9 }))
    })
    await store.streamReview({ examinationId: 1 } as any)
    expect(store.streamText).toBe('chunk')
    store.clearStream()
    expect(store.streamText).toBe('')
  })

  it('covers lab store fallback and stream error branches', async () => {
    const store = useLabStore()
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/examination/lab/list') return Promise.resolve({ data: { data: { content: undefined, totalElements: undefined } } })
      if (url === '/api/examination/ai/review-list') return Promise.resolve({ data: { data: {} } })
      if (url === '/api/examination/ai/manual-list') return Promise.resolve({ data: { data: {} } })
      if (url === '/api/examination/ai/review-stats') return Promise.resolve({ data: { data: { manualCount: 0, rejectCount: 0 } } })
      return Promise.resolve({ data: { data: {} } })
    })

    await store.loadExaminations('other')
    expect(store.pendingExaminations).toEqual([])
    await store.loadReviewDashboard()
    expect(store.reviewRecords).toEqual([])
    expect(store.manualReviews).toEqual([])
    expect(store.reviewTotal).toBe(0)
    expect(store.manualTotal).toBe(0)

    // onDone with bad JSON triggers parseJson catch branch; loadReviewDashboard still runs
    let dashboardCalled = false
    requestMock.get.mockImplementation((url) => {
      if (url === '/api/examination/ai/review-list') { dashboardCalled = true; return Promise.resolve({ data: { data: { content: [] } } }) }
      if (url === '/api/examination/ai/manual-list') return Promise.resolve({ data: { data: { content: [] } } })
      if (url === '/api/examination/ai/review-stats') return Promise.resolve({ data: { data: { manualCount: 0, rejectCount: 0 } } })
      return Promise.resolve({ data: { data: {} } })
    })
    sseMock.mockImplementationOnce(async (_url, options) => {
      await options.handlers.onDone('{bad json')  // triggers parseJson catch
    })
    await store.streamReview({ examinationId: 1 } as any)
    expect(dashboardCalled).toBe(true)  // verify onDone error was caught and flow continued

    sseMock.mockImplementationOnce(async (_url, options) => {
      await options.handlers.onDone(JSON.stringify({}))
    })
    await store.streamReview({ examinationId: 1 } as any)

    sseMock.mockImplementationOnce(async (_url, options) => {
      options.handlers.onError('')
    })
    await expect(store.streamReview({ examinationId: 1 } as any)).rejects.toThrow()
  })
})
