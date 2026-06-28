import { vi } from 'vitest'

const mockStorage: Record<string, string> = {}

vi.stubGlobal('uni', {
  getStorageSync: (key: string) => mockStorage[key] ?? '',
  setStorageSync: (key: string, value: unknown) => { mockStorage[key] = String(value) },
  removeStorageSync: (key: string) => { delete mockStorage[key] },
  login: vi.fn((options?: { provider?: string; success?: (r: { code: string }) => void; fail?: (err: Error) => void }) => {
    if (options?.success) options.success({ code: 'mock-code' })
  }),
  request: vi.fn(),
  uploadFile: vi.fn(),
  showToast: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({})),
  showModal: vi.fn(),
  showLoading: vi.fn(),
  hideLoading: vi.fn(),
  navigateTo: vi.fn(),
  redirectTo: vi.fn(),
  reLaunch: vi.fn(),
  switchTab: vi.fn(),
  onLaunch: vi.fn(),
  onShow: vi.fn(),
  onHide: vi.fn()
})
