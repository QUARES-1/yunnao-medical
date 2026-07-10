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

describe('lab request and SSE sources', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('injects auth headers and handles success or failed business responses', async () => {
    localStorage.setItem('lab_token', 'lab-token')
    const config = hooks.requestFulfilled({ headers: {} })
    expect(config.headers.Authorization).toBe('Bearer lab-token')
    expect(hooks.responseFulfilled({ data: { code: 200 } })).toEqual({ data: { code: 200 } })
    await expect(hooks.responseFulfilled({ data: { code: 500, msg: 'bad' } })).rejects.toThrow('bad')
    await expect(hooks.responseRejected({ response: { status: 401, data: { code: 401 } } })).rejects.toMatchObject({ response: { status: 401 } })
    await expect(hooks.responseRejected({ message: 'offline' })).rejects.toMatchObject({ message: 'offline' })
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('covers request fallback branches without token or explicit messages', async () => {
    const config = hooks.requestFulfilled({ headers: {} })
    expect(config.headers.token).toBeUndefined()
    expect(config.headers.Authorization).toBeUndefined()

    expect(hooks.responseFulfilled({ data: null })).toEqual({ data: null })
    await expect(hooks.responseFulfilled({ data: { code: 500, message: 'message-only' } })).rejects.toThrow('message-only')
    await expect(hooks.responseFulfilled({ data: { code: 500 } })).rejects.toThrow()

    history.replaceState(null, '', '/login')
    await expect(hooks.responseRejected({ response: { status: 401 } })).rejects.toMatchObject({ response: { status: 401 } })
    expect(location.pathname).toBe('/login')
  })

  it('parses delta, done and error SSE events', async () => {
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: delta\ndata: hello\n\n'),
      encoder.encode('event: done\ndata: {"id":1}\n\n'),
      encoder.encode('event: error\ndata: warn\n\n')
    ]
    let index = 0
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
    })))
    const seen: string[] = []
    await streamSse('/sse', { token: 't', body: { a: 1 }, handlers: {
      onDelta: text => seen.push(`delta:${text}`),
      onDone: text => seen.push(`done:${text}`),
      onError: text => seen.push(`error:${text}`)
    } })
    expect(seen).toEqual(['delta:hello', 'done:{"id":1}', 'error:warn'])
  })

  it('covers SSE defaults, missing handlers and failed response bodies', async () => {
    const encoder = new TextEncoder()
    let index = 0
    const chunks = [encoder.encode('event: message\ndata: ignored\n\n')]
    const fetchMock = vi.fn(async (_url, options: any) => {
      expect(options.headers.token).toBeUndefined()
      expect(options.body).toBeUndefined()
      return {
        ok: true,
        body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
      }
    })
    vi.stubGlobal('fetch', fetchMock)
    await expect(streamSse('/sse')).resolves.toBeUndefined()

    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200, body: null })))
    await expect(streamSse('/sse')).rejects.toThrow('200')
  })
})
