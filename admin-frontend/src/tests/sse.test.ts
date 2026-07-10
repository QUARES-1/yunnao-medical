import { describe, it, expect, vi, beforeEach } from 'vitest'
import { streamAiOperationReport } from '@/api/ai'

describe('admin ai SSE (streamAiOperationReport)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function makeStream(chunks) {
    let index = 0
    const encoder = new TextEncoder()
    return vi.fn(async () => ({
      ok: true,
      status: 200,
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
    return vi.fn(async () => ({ ok: true, status: 200, ...mockBody }))
  }

  it('sends POST with token and correct body', async () => {
    vi.stubGlobal('localStorage', { getItem: () => 'admin-token-xyz' })
    vi.stubGlobal('fetch', makeStream([]))

    await streamAiOperationReport({ reportType: 'daily' }, vi.fn())

    expect(fetch).toHaveBeenCalled()
    const [url, opts] = fetch.mock.calls[0]
    expect(url).toBe('/api/admin/ai/operation-report/stream')
    expect(opts.method).toBe('POST')
    expect(opts.headers.token).toBe('admin-token-xyz')
    expect(opts.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(opts.body)).toEqual({ reportType: 'daily' })
  })

  it('yields delta chunks via onDelta callback', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata: chunk1\n\n',
      'event: delta\ndata: chunk2\n\n'
    ]))

    const chunks: string[] = []
    await streamAiOperationReport({ reportType: 'weekly' }, (c) => chunks.push(c))
    expect(chunks).toEqual(['chunk1', 'chunk2'])
  })

  it('yields done event via onDone callback', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeStream([
      'event: done\ndata: report-id-42\n\n'
    ]))

    const ids: string[] = []
    await streamAiOperationReport({ reportType: 'monthly' }, vi.fn(), (id) => ids.push(id))
    expect(ids).toEqual(['report-id-42'])
  })

  it('block with no event line triggers onDelta with payload', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeStream([
      'data: implicit-event-text\n\n'
    ]))

    const chunks: string[] = []
    await streamAiOperationReport({ reportType: 'daily' }, (c) => chunks.push(c))
    expect(chunks).toEqual(['implicit-event-text'])
  })

  it('block with empty data does not trigger onDelta', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata:\n\n'
    ]))

    const chunks: string[] = []
    await streamAiOperationReport({ reportType: 'daily' }, (c) => chunks.push(c))
    expect(chunks).toEqual([])
  })

  it('block with undefined event line does not crash', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    // Second block has no event: line, so event=undefined, delta goes to onDelta
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata: ok\n\n',
      'no-event-line\ndata: should-not-crash\n\n'
    ]))

    const chunks: string[] = []
    await streamAiOperationReport({ reportType: 'daily' }, (c) => chunks.push(c))
    expect(chunks).toEqual(['ok', 'should-not-crash'])
  })

  it('throws when response.ok is false', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeFetch({ ok: false, status: 503, body: null }))

    await expect(
      streamAiOperationReport({ reportType: 'daily' }, vi.fn())
    ).rejects.toThrow()
  })

  it('throws when response.body is null', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeFetch({ ok: true, body: null }))

    await expect(
      streamAiOperationReport({ reportType: 'daily' }, vi.fn())
    ).rejects.toThrow('流式接口请求失败')
  })

  it('handles multiple blocks in one chunk', async () => {
    vi.stubGlobal('localStorage', { getItem: () => '' })
    vi.stubGlobal('fetch', makeStream([
      'event: delta\ndata: a\n\nevent: delta\ndata: b\n\n',
      'event: done\ndata: end\n\n'
    ]))

    const chunks: string[] = []
    const done: string[] = []
    await streamAiOperationReport({ reportType: 'daily' }, (c) => chunks.push(c), (d) => done.push(d))
    expect(chunks).toEqual(['a', 'b'])
    expect(done).toEqual(['end'])
  })
})
