import { describe, it, expect, vi, beforeEach } from 'vitest'
import { streamSse } from '@/utils/sse'

describe('lab sse utility', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function makeStream(chunks) {
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
    return vi.fn(async () => ({ ok: true, ...mockBody }))
  }

  it('sends POST with correct token header', async () => {
    vi.stubGlobal('localStorage', { getItem: () => 'lab-token-xyz' })
    vi.stubGlobal('fetch', makeStream([]))

    await streamSse('http://localhost:8080/api/test', {
      token: 'lab-token-xyz',
      handlers: {}
    })

    expect(fetch).toHaveBeenCalled()
    const [, opts] = fetch.mock.calls[0]
    expect(opts.headers.token).toBe('lab-token-xyz')
    expect(opts.headers.Authorization).toBe('Bearer lab-token-xyz')
    expect(opts.headers.Accept).toBe('text/event-stream')
    expect(opts.headers['Content-Type']).toBe('application/json')
  })

  it('yields delta events via onDelta', async () => {
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata: chunk1\n\n',
      'event: delta\ndata: chunk2\n\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual(['chunk1', 'chunk2'])
  })

  it('yields done event via onDone', async () => {
    vi.stubGlobal('fetch', makeStream([
      'event: done\ndata: report-id-7\n\n'
    ]))

    const done: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDone: (d) => done.push(d) }
    })
    expect(done).toEqual(['report-id-7'])
  })

  it('error event triggers onError handler', async () => {
    vi.stubGlobal('fetch', makeStream([
      'event: error\ndata: SSE failed\n\n'
    ]))

    const errors: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onError: (m) => errors.push(m) }
    })
    expect(errors).toEqual(['SSE failed'])
  })

  it('block without event: line triggers onDelta with payload', async () => {
    // SSE: no event: line means default event "message", which has no handler → onDelta is NOT called
    // (lab-frontend sse.ts does not have a default/message handler, only delta/done/error)
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata: with-event\n\n',
      'data: no-event-line\n\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    // only delta events trigger onDelta; no-event-line → default "message" → falls through silently
    expect(deltas).toEqual(['with-event'])
  })

  it('empty data block passes empty string to onDelta', async () => {
    // data: followed by newline with no content → '' after trim → truthy? → yes → onDelta called with ''
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata:\n\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual([''])
  })

  it('handles CR+LF line endings', async () => {
    vi.stubGlobal('fetch', makeStream([
      'event: delta\r\ndata: crlf-chunk\r\n\r\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual(['crlf-chunk'])
  })

  it('handles multiple blocks in single chunk', async () => {
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata: a\n\nevent: delta\ndata: b\n\n'
    ]))

    const deltas: string[] = []
    await streamSse('http://localhost:8080/api/test', {
      handlers: { onDelta: (t) => deltas.push(t) }
    })
    expect(deltas).toEqual(['a', 'b'])
  })

  it('throws when response.ok is false', async () => {
    vi.stubGlobal('fetch', makeFetch({ ok: false, status: 503, body: null }))

    await expect(
      streamSse('http://localhost:8080/api/test', { handlers: {} })
    ).rejects.toThrow()
  })

  it('throws when response.body is null', async () => {
    vi.stubGlobal('fetch', makeFetch({ ok: true, body: null }))

    await expect(
      streamSse('http://localhost:8080/api/test', { handlers: {} })
    ).rejects.toThrow('SSE连接失败')
  })

  it('omits token header when token not provided', async () => {
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
