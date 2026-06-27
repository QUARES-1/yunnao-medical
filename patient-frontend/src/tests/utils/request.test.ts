import { describe, it, expect, vi, beforeEach } from 'vitest'

const mockGetStorageSync = vi.fn()
const mockRemoveStorageSync = vi.fn()
const mockShowToast = vi.fn()
const mockRequest = vi.fn()

vi.stubGlobal('uni', {
  getStorageSync: mockGetStorageSync,
  removeStorageSync: mockRemoveStorageSync,
  showToast: mockShowToast,
  request: mockRequest
})

import { request, showError } from '@/utils/request'

describe('Request Utils (patient-frontend)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should make request with auth header when token exists', async () => {
    mockGetStorageSync.mockReturnValue('patient-token-xyz')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 200, data: { id: 1 } } })
    })

    const promise = request<{ id: number }>({ url: '/api/test' })
    await expect(promise).resolves.toEqual({ id: 1 })
    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        url: 'http://localhost:8080/api/test',
        method: 'GET',
        header: { Authorization: 'Bearer patient-token-xyz' }
      })
    )
  })

  it('should make request without auth header when auth is false', async () => {
    mockGetStorageSync.mockReturnValue('patient-token-xyz')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 200, data: { id: 1 } } })
    })

    const promise = request<{ id: number }>({ url: '/api/public', auth: false })
    await expect(promise).resolves.toEqual({ id: 1 })
    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({ header: {} })
    )
  })

  it('should make request without auth when token is empty', async () => {
    mockGetStorageSync.mockReturnValue('')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 200, data: {} } })
    })

    const promise = request<Record<string, unknown>>({ url: '/api/public' })
    await expect(promise).resolves.toEqual({})
    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({ header: {} })
    )
  })

  it('should resolve with data on code 200', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 200, data: { name: 'test' } } })
    })

    const promise = request<{ name: string }>({ url: '/api/test' })
    await expect(promise).resolves.toEqual({ name: 'test' })
  })

  it('should reject with error message on code 401', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 401, msg: '登录已过期，请重新登录' } })
    })

    const promise = request<unknown>({ url: '/api/test' })
    await expect(promise).rejects.toThrow('登录已过期，请重新登录')
    expect(mockRemoveStorageSync).toHaveBeenCalledWith('patient_token')
    expect(mockShowToast).toHaveBeenCalledWith({ title: '登录已过期，请重新登录', icon: 'none' })
  })

  it('should reject with error message on code 400', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 400, msg: '操作失败' } })
    })

    const promise = request<unknown>({ url: '/api/test' })
    await expect(promise).rejects.toThrow('操作失败')
    expect(mockRemoveStorageSync).not.toHaveBeenCalled()
  })

  it('should use default error message when msg is empty', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 500, msg: '' } })
    })

    const promise = request<unknown>({ url: '/api/test' })
    await expect(promise).rejects.toThrow('请求失败')
  })

  it('should reject on network failure', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.fail()
    })

    const promise = request<unknown>({ url: '/api/test' })
    await expect(promise).rejects.toThrow('网络连接失败，请检查后端服务')
  })

  it('should support POST method', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 200, data: { id: 2 } } })
    })

    const promise = request<{ id: number }>({
      url: '/api/create',
      method: 'POST',
      data: { name: 'test' }
    })
    await expect(promise).resolves.toEqual({ id: 2 })
    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({ method: 'POST', data: { name: 'test' } })
    )
  })

  it('should support PUT method', async () => {
    mockGetStorageSync.mockReturnValue('token')
    mockRequest.mockImplementation((options) => {
      options.success({ data: { code: 200, data: 'updated' } })
    })

    const promise = request<string>({
      url: '/api/update/1',
      method: 'PUT',
      data: { name: 'updated' }
    })
    await expect(promise).resolves.toEqual('updated')
    expect(mockRequest).toHaveBeenCalledWith(
      expect.objectContaining({ method: 'PUT' })
    )
  })

  describe('showError helper', () => {
    it('should show toast with error message', () => {
      showError(new Error('操作失败'))
      expect(mockShowToast).toHaveBeenCalledWith({
        title: '操作失败',
        icon: 'none',
        duration: 2400
      })
    })

    it('should show default message for non-Error', () => {
      showError('字符串错误')
      expect(mockShowToast).toHaveBeenCalledWith({
        title: '操作失败，请稍后重试',
        icon: 'none',
        duration: 2400
      })
    })

    it('should show default message when error is null', () => {
      showError(null)
      expect(mockShowToast).toHaveBeenCalledWith({
        title: '操作失败，请稍后重试',
        icon: 'none',
        duration: 2400
      })
    })
  })
})
