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

describe('admin request source interceptors', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    history.replaceState(null, '', '/dashboard')
  })

  it('injects token and passes successful business responses', async () => {
    localStorage.setItem('admin_token', 'token-1')
    const config = hooks.requestFulfilled({ headers: {} })
    expect(config.headers.token).toBe('token-1')
    expect(hooks.responseFulfilled({ data: { code: 200 } })).toEqual({ data: { code: 200 } })
  })

  it('clears auth for 401/403 and rejects business errors', async () => {
    localStorage.setItem('admin_token', 'token-1')
    localStorage.setItem('admin_info', '{}')
    await expect(hooks.responseFulfilled({ data: { code: 401, msg: 'expired' } })).rejects.toThrow('expired')
    expect(localStorage.getItem('admin_token')).toBeNull()
    await expect(hooks.responseFulfilled({ data: { code: 500, msg: 'bad' } })).rejects.toThrow('bad')
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('rejects network errors with status-aware messages', async () => {
    await expect(hooks.responseRejected({ response: { status: 500 } })).rejects.toMatchObject({ response: { status: 500 } })
    await expect(hooks.responseRejected({})).rejects.toEqual({})
    expect(ElMessage.error).toHaveBeenCalledTimes(2)
  })

  it('covers missing token, login-page auth failure and fallback error messages', async () => {
    const config = hooks.requestFulfilled({ headers: {} })
    expect(config.headers.token).toBeUndefined()

    history.replaceState(null, '', '/login')
    await expect(hooks.responseFulfilled({ data: { code: 403 } })).rejects.toThrow()
    expect(location.pathname).toBe('/login')

    await expect(hooks.responseFulfilled({ data: { code: 500 } })).rejects.toThrow()
    expect(ElMessage.error).toHaveBeenCalled()
  })
})
