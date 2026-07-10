import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const requestMock = vi.hoisted(() => ({
  get: vi.fn((url) => {
    if (url === '/api/examination/ai/review-list') return Promise.resolve({ data: { data: { content: [{ id: 5, examinationId: 1 }], totalElements: 1 } } })
    if (url === '/api/examination/ai/manual-list') return Promise.resolve({ data: { data: { content: [], totalElements: 0 } } })
    if (url === '/api/examination/ai/review-stats') return Promise.resolve({ data: { data: { total: 1, passCount: 0, manualCount: 1, rejectCount: 0, passRate: 0 } } })
    return Promise.resolve({ data: { data: {} } })
  }),
  post: vi.fn(),
  put: vi.fn()
}))

vi.mock('@/utils/request', () => ({ default: requestMock }))

// Stub global fetch so real streamSse can work (fetch returns a mock ReadableStream)
function makeStream(chunks) {
  let index = 0
  const encoder = new TextEncoder()
  vi.stubGlobal('fetch', vi.fn(async () => ({
    ok: true,
    body: {
      getReader: () => ({
        read: async () => {
          if (index < chunks.length) return { done: false, value: encoder.encode(chunks[index++]) }
          return { done: true }
        }
      })
    }
  })))
}

import { useLabStore } from '@/stores/lab'

describe('lab store streamReview with real SSE parseJson', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    // reset fetch stub between tests
    vi.stubGlobal('fetch', vi.fn())
  })

  it('parses SSE delta chunks via real streamSse', async () => {
    makeStream(['event: delta\ndata: Processing...\n\n'])
    const store = useLabStore()
    await store.streamReview({ examinationId: 1 } as any)
    expect(store.streamText).toContain('Processing')
    expect(store.streaming).toBe(false)
  })

  it('handles onDone with valid JSON', async () => {
    makeStream(['event: done\ndata: {"id":5}\n\n'])
    const store = useLabStore()
    await store.streamReview({ examinationId: 1 } as any)
    expect(store.streaming).toBe(false)
  })

  it('handles onDone with bad JSON (parseJson catch block)', async () => {
    makeStream(['event: done\ndata: not-json-at-all\n\n'])
    const store = useLabStore()
    await store.streamReview({ examinationId: 1 } as any)
    expect(store.streaming).toBe(false)
  })

  it('handles onError by throwing', async () => {
    makeStream(['event: error\ndata: SSE error occurred\n\n'])
    const store = useLabStore()
    await expect(
      store.streamReview({ examinationId: 1 } as any)
    ).rejects.toThrow()
  })
})
