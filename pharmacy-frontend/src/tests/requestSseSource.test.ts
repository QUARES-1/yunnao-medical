import { beforeEach, describe, expect, it, vi } from 'vitest'

const hooks = vi.hoisted(() => ({
  requestFulfilled: undefined as any,
  responseFulfilled: undefined as any,
  responseRejected: undefined as any
}))

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: vi.fn(fn => { hooks.requestFulfilled = fn }) },
        response: { use: vi.fn((fn, reject) => { hooks.responseFulfilled = fn; hooks.responseRejected = reject }) }
      }
    }))
  }
}))

import { ElMessage } from 'element-plus'
import '@/utils/request'
import { streamSse } from '@/utils/sse'

describe('pharmacy request and SSE sources', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('injects auth headers and handles response branches', async () => {
    localStorage.setItem('pharmacy_token', 'pharmacy-token')
    const config = hooks.requestFulfilled({ headers: {} })
    expect(config.headers.Authorization).toBe('Bearer pharmacy-token')
    expect(hooks.responseFulfilled({ data: { code: 200 } })).toEqual({ data: { code: 200 } })
    await expect(hooks.responseFulfilled({ data: { code: 500, message: 'bad' } })).rejects.toThrow('bad')
    await expect(hooks.responseRejected({ response: { status: 401, data: { code: 401 } } })).rejects.toMatchObject({ response: { status: 401 } })
    await expect(hooks.responseRejected({ message: 'offline' })).rejects.toMatchObject({ message: 'offline' })
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('covers request defaults and fallback error messages', async () => {
    const config = hooks.requestFulfilled({ headers: {} })
    expect(config.headers.token).toBeUndefined()
    expect(config.headers.Authorization).toBeUndefined()
    expect(hooks.responseFulfilled({ data: null })).toEqual({ data: null })

    await expect(hooks.responseFulfilled({ data: { code: 500, msg: 'msg-only' } })).rejects.toThrow('msg-only')
    await expect(hooks.responseFulfilled({ data: { code: 500 } })).rejects.toThrow()

    history.replaceState(null, '', '/login')
    await expect(hooks.responseRejected({ response: { status: 401 } })).rejects.toMatchObject({ response: { status: 401 } })
    await expect(hooks.responseRejected({ response: { data: { msg: 'server bad' } } })).rejects.toMatchObject({ response: { data: { msg: 'server bad' } } })
  })

  it('parses stream chunks and rejects bad SSE responses', async () => {
    const encoder = new TextEncoder()
    let index = 0
    const chunks = [encoder.encode('event: delta\ndata: stock\n\nevent: done\ndata: ok\n\n')]
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
    })))
    const seen: string[] = []
    await streamSse('/sse', { handlers: { onDelta: text => seen.push(text), onDone: text => seen.push(text) } })
    expect(seen).toEqual(['stock', 'ok'])

    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 500, body: null })))
    await expect(streamSse('/sse')).rejects.toThrow('500')
  })

  it('covers SSE defaults, error events and missing response body', async () => {
    const encoder = new TextEncoder()
    let index = 0
    const chunks = [encoder.encode('event: error\ndata: warn\n\n')]
    vi.stubGlobal('fetch', vi.fn(async (_url, options: any) => {
      expect(options.headers.token).toBeUndefined()
      expect(options.body).toBeUndefined()
      return {
        ok: true,
        body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
      }
    }))
    const seen: string[] = []
    await streamSse('/sse', { handlers: { onError: text => seen.push(text) } })
    expect(seen).toEqual(['warn'])

    index = 0
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
    })))
    await expect(streamSse('/sse')).resolves.toBeUndefined()

    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200, body: null })))
    await expect(streamSse('/sse')).rejects.toThrow('200')
  })
})
