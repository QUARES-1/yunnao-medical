import { beforeEach, describe, expect, it, vi } from 'vitest'

import { request, showError } from '@/utils/request'
import { streamSse } from '@/utils/sse'

describe('patient request and SSE source modules', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(uni.getStorageSync).mockReset()
    vi.mocked(uni.request).mockReset()
    vi.mocked(uni.showToast).mockReset()
    vi.mocked(uni.removeStorageSync).mockReset()
  })

  it('resolves successful requests and injects auth headers', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    vi.mocked(uni.request).mockImplementation((options: any) => {
      expect(options.header.Authorization).toBe('Bearer patient-token')
      expect(options.header['Content-Type']).toBe('application/json;charset=utf-8')
      options.success({ data: { code: 200, data: { ok: true } } })
    })
    await expect(request({ url: '/api/test', method: 'POST', data: { a: 1 } })).resolves.toEqual({ ok: true })
  })

  it('handles no-auth, 401, business errors and request failures', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    vi.mocked(uni.request).mockImplementationOnce((options: any) => {
      expect(options.header.Authorization).toBeUndefined()
      options.success({ data: { code: 401, msg: 'expired' } })
    })
    await expect(request({ url: '/api/public', auth: false })).rejects.toThrow('expired')
    expect(uni.removeStorageSync).toHaveBeenCalledWith('patient_token')

    vi.mocked(uni.request).mockImplementationOnce((options: any) => options.success({ data: { code: 500, msg: 'bad' } }))
    await expect(request({ url: '/api/fail' })).rejects.toThrow('bad')

    vi.mocked(uni.request).mockImplementationOnce((options: any) => options.fail())
    await expect(request({ url: '/api/offline' })).rejects.toThrow('网络连接失败')

    showError(new Error('shown'))
    showError('plain')
    expect(uni.showToast).toHaveBeenCalled()
  })

  it('covers request utility default branches', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('')
    vi.mocked(uni.request).mockImplementationOnce((options: any) => {
      expect(options.method).toBe('GET')
      expect(options.data).toBeUndefined()
      expect(options.header.Authorization).toBeUndefined()
      expect(options.header['Content-Type']).toBeUndefined()
      options.success({ data: { code: 200, data: undefined } })
    })
    await expect(request({ url: '/api/public' })).resolves.toBeUndefined()

    vi.mocked(uni.request).mockImplementationOnce((options: any) => options.success({ data: { code: 500 } }))
    await expect(request({ url: '/api/fail-default' })).rejects.toThrow()

    showError(undefined)
    expect(uni.showToast).toHaveBeenCalled()
  })

  it('streams H5 SSE chunks and rejects failed streams', async () => {
    const encoder = new TextEncoder()
    let index = 0
    const chunks = [encoder.encode('event: delta\ndata: hi\n\nevent: done\ndata: ok\n\n')]
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
    })))
    const seen: string[] = []
    await streamSse({ url: '/api/sse', data: { q: 1 }, handlers: { onDelta: text => seen.push(text), onDone: text => seen.push(text) } })
    expect(seen).toEqual(['hi', 'ok'])

    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 500, body: null })))
    await expect(streamSse({ url: '/api/sse' })).rejects.toThrow('500')
  })
})
