import { describe, it, expect, vi, beforeEach } from 'vitest'
import { streamSse } from '@/utils/sse'

describe('pharmacy sse utility', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function makeSseStream(chunks) {
    let index = 0
    const encoder = new TextEncoder()
    return vi.fn(async () => ({
      ok: true,
      body: {
        getReader: () => ({
          read: async () => {
            if (index < chunks.length) return { done: false, value: encoder.encode(chunks[index++]) }
            return { done: true }
          }
        })
      }
    }))
  }

  function makeFetch(mockBody) {
    return vi.fn(async () => mockBody)
  }

  it('calls fetch with correct headers and body', async () => {
    const dummyBody = { getReader: () => ({ read: async () => ({ done: true }) }) }
    vi.stubGlobal('fetch', makeFetch({ ok: true, body: dummyBody }))

    const done = []
    await streamSse('http://localhost:8080/api/test', {
      token: 'my-token',
      body: { key: 'value' },
      handlers: { onDelta: vi.fn(), onDone: (d) => done.push(d) }
    })

    expect(fetch).toHaveBeenCalled()
    const [url, opts] = fetch.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/test')
    expect(opts.method).toBe('POST')
    expect(opts.headers.token).toBe('my-token')
    expect(opts.headers.Authorization).toBe('Bearer my-token')
    expect(opts.headers.Accept).toBe('text/event-stream')
    expect(opts.body).toBe('{"key":"value"}')
  })

  it('streams delta events via onDelta', async () => {
    vi.stubGlobal('fetch', makeSseStream([
      'event: delta\ndata: chunk1\n\n',
      'event: delta\ndata: chunk2\n\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual(['chunk1', 'chunk2'])
  })

  it('streams done event via onDone', async () => {
    vi.stubGlobal('fetch', makeSseStream([
      'event: done\ndata: report-123\n\n'
    ]))

    const done: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDone: (d) => done.push(d) }
    })
    expect(done).toEqual(['report-123'])
  })

  it('error event dispatches to onError handler', async () => {
    vi.stubGlobal('fetch', makeSseStream([
      'event: error\ndata: SSE error occurred\n\n'
    ]))

    const errors: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onError: (msg) => errors.push(msg) }
    })
    expect(errors).toEqual(['SSE error occurred'])
  })

  it('handles multiple events in single chunk', async () => {
    vi.stubGlobal('fetch', makeSseStream([
      'event: delta\ndata: part1\n\n',
      'event: delta\ndata: part2\n\nevent: done\ndata: final\n\n'
    ]))

    const deltas: string[] = []
    const done: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t), onDone: (d) => done.push(d) }
    })
    expect(deltas).toEqual(['part1', 'part2'])
    expect(done).toEqual(['final'])
  })

  it('handles CR+LF line endings', async () => {
    vi.stubGlobal('fetch', makeSseStream([
      'event: delta\r\ndata: crlf\r\n\r\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual(['crlf'])
  })

  it('handles empty data value', async () => {
    vi.stubGlobal('fetch', makeSseStream([
      'event: delta\ndata:\n\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual([''])
  })

  it('throws when response.ok is false', async () => {
    vi.stubGlobal('fetch', makeFetch({ ok: false, status: 503, body: null }))

    await expect(
      streamSse('http://localhost:8080/api/test', { handlers: {} })
    ).rejects.toThrow()
  })

  it('throws when response.body is null', async () => {
    vi.stubGlobal('fetch', makeFetch({ ok: true, status: 200, body: null }))

    await expect(
      streamSse('http://localhost:8080/api/test', { handlers: {} })
    ).rejects.toThrow('SSE连接失败')
  })

  it('omits token header when token is not provided', async () => {
    const dummyBody = { getReader: () => ({ read: async () => ({ done: true }) }) }
    vi.stubGlobal('fetch', makeFetch({ ok: true, body: dummyBody }))

    await streamSse('http://localhost:8080/api/test', { handlers: {} })

    const headers = fetch.mock.calls[0][1].headers
    expect(headers.token).toBeUndefined()
    expect(headers.Authorization).toBeUndefined()
  })

  it('does not stringify body when body is undefined', async () => {
    const dummyBody = { getReader: () => ({ read: async () => ({ done: true }) }) }
    vi.stubGlobal('fetch', makeFetch({ ok: true, body: dummyBody }))

    await streamSse('http://localhost:8080/api/test', { handlers: {} })

    expect(fetch.mock.calls[0][1].body).toBeUndefined()
  })
})
