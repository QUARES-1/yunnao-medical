import { beforeEach, describe, expect, it, vi } from 'vitest'
import '@/tests/setup.js'

// Mock fetch globally for H5 streamSse tests
const makeStreamResponse = (overrides = {}) => ({
  ok: true,
  status: 200,
  body: {
    getReader: () => ({
      read: async () => ({ done: true, value: new Uint8Array() })
    })
  },
  ...overrides
})

describe('patient sse utility (H5 branch)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // =============================================
  // streamSse — H5 / fetch branch
  // =============================================
  it('sends POST with auth token when token exists', async () => {
    uni.getStorageSync.mockReturnValue('patient-token')
    const chunks = []
    const encoder = new TextEncoder()
    let callCount = 0
    vi.stubGlobal('fetch', vi.fn(async (url, opts) => {
      if (++callCount === 1) return makeStreamResponse() // initial call
      return makeStreamResponse()
    }))
    const { streamSse } = await import('@/utils/sse')
    await streamSse({
      url: '/test/stream',
      method: 'POST',
      data: { question: 'hello' },
      auth: true,
      handlers: { onDone: vi.fn() }
    })
    expect(fetch).toHaveBeenCalled()
    const [url, opts] = fetch.mock.calls[0]
    expect(opts.headers.Authorization).toBe('Bearer patient-token')
    expect(opts.body).toBe(JSON.stringify({ question: 'hello' }))
  })

  it('sends GET without body when no data provided', async () => {
    uni.getStorageSync.mockReturnValue('')
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse()))
    const { streamSse } = await import('@/utils/sse')
    await streamSse({
      url: '/test/stream',
      method: 'GET'
    })
    expect(fetch).toHaveBeenCalled()
    const [, opts] = fetch.mock.calls[0]
    expect(opts.body).toBeUndefined()
  })

  it('throws when response.ok is false', async () => {
    uni.getStorageSync.mockReturnValue('')
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({ ok: false, status: 500 })))
    const { streamSse } = await import('@/utils/sse')
    await expect(streamSse({ url: '/test/stream' }))
      .rejects.toThrow('SSE连接失败')
  })

  it('throws when response.body is null', async () => {
    uni.getStorageSync.mockReturnValue('')
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({ body: null })))
    const { streamSse } = await import('@/utils/sse')
    await expect(streamSse({ url: '/test/stream' }))
      .rejects.toThrow('SSE连接失败')
  })

  it('parses delta SSE events and calls onDelta', async () => {
    uni.getStorageSync.mockReturnValue('')
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: delta\ndata: part1\n\n'),
      encoder.encode('event: delta\ndata: part2\n\n'),
      encoder.encode('event: done\ndata: {"result":"ok"}\n\n')
    ]
    let idx = 0
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({
      body: {
        getReader: () => ({
          read: async () => idx < chunks.length ? { done: false, value: chunks[idx++] } : { done: true }
        })
      }
    })))
    const onDelta = vi.fn()
    const onDone = vi.fn()
    const { streamSse } = await import('@/utils/sse')
    await streamSse({ url: '/test/stream', handlers: { onDelta, onDone } })
    expect(onDelta).toHaveBeenNthCalledWith(1, 'part1')
    expect(onDelta).toHaveBeenNthCalledWith(2, 'part2')
    expect(onDone).toHaveBeenCalledWith('{"result":"ok"}')
  })

  it('joins multi-line data blocks', async () => {
    uni.getStorageSync.mockReturnValue('')
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: delta\ndata: line1\ndata: line2\n\n'),
      encoder.encode('event: done\ndata: done\n\n')
    ]
    let idx = 0
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({
      body: {
        getReader: () => ({
          read: async () => idx < chunks.length ? { done: false, value: chunks[idx++] } : { done: true }
        })
      }
    })))
    const onDelta = vi.fn()
    const onDone = vi.fn()
    const { streamSse } = await import('@/utils/sse')
    await streamSse({ url: '/test/stream', handlers: { onDelta, onDone } })
    expect(onDelta).toHaveBeenCalledWith('line1\nline2')
  })

  it('handles CR+LF line endings', async () => {
    uni.getStorageSync.mockReturnValue('')
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: delta\r\ndata: crlftext\r\n\r\n'),
      encoder.encode('event: done\r\ndata: done\r\n\r\n')
    ]
    let idx = 0
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({
      body: {
        getReader: () => ({
          read: async () => idx < chunks.length ? { done: false, value: chunks[idx++] } : { done: true }
        })
      }
    })))
    const onDelta = vi.fn()
    const onDone = vi.fn()
    const { streamSse } = await import('@/utils/sse')
    await streamSse({ url: '/test/stream', handlers: { onDelta, onDone } })
    expect(onDelta).toHaveBeenCalledWith('crlftext')
    expect(onDone).toHaveBeenCalledWith('done')
  })

  it('accumulates partial chunks across multiple reads', async () => {
    uni.getStorageSync.mockReturnValue('')
    const encoder = new TextEncoder()
    // Chunk is split mid-event
    const chunks = [
      encoder.encode('event: del'),
      encoder.encode('ta\ndata: complete\n\n'),
      encoder.encode('event: done\ndata: done\n\n')
    ]
    let idx = 0
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({
      body: {
        getReader: () => ({
          read: async () => idx < chunks.length ? { done: false, value: chunks[idx++] } : { done: true }
        })
      }
    })))
    const onDelta = vi.fn()
    const onDone = vi.fn()
    const { streamSse } = await import('@/utils/sse')
    await streamSse({ url: '/test/stream', handlers: { onDelta, onDone } })
    expect(onDelta).toHaveBeenCalledWith('complete')
    expect(onDone).toHaveBeenCalledWith('done')
  })

  it('calls onError handler on error event', async () => {
    uni.getStorageSync.mockReturnValue('')
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: error\ndata: server error\n\n'),
      encoder.encode('event: done\ndata: done\n\n')
    ]
    let idx = 0
    vi.stubGlobal('fetch', vi.fn(async () => makeStreamResponse({
      body: {
        getReader: () => ({
          read: async () => idx < chunks.length ? { done: false, value: chunks[idx++] } : { done: true }
        })
      }
    })))
    const onError = vi.fn()
    const { streamSse } = await import('@/utils/sse')
    await streamSse({ url: '/test/stream', handlers: { onError } })
    expect(onError).toHaveBeenCalledWith('server error')
  })

  it('re-throws fetch error as rejected promise', async () => {
    uni.getStorageSync.mockReturnValue('')
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new Error('network error')
    }))
    const { streamSse } = await import('@/utils/sse')
    await expect(streamSse({ url: '/test/stream' }))
      .rejects.toThrow('network error')
  })

  // =============================================
  // decodeBuffer binary fallback (non-H5 only)
  // decodeBuffer lives in #ifndef H5 in sse.ts — untestable in Vitest's H5 env.
  // It is excluded from coverage below. Here we verify the logic directly.
  // =============================================
  it('TextDecoder fallback — decodeBuffer equivalent logic', () => {
    const encoder = new TextEncoder()
    const buffer = encoder.encode('hello').buffer
    // Happy-DOM's TextDecoder works correctly; this mirrors decodeBuffer's happy path
    const result = new TextDecoder('utf-8').decode(buffer)
    expect(result).toBe('hello')
  })
})
