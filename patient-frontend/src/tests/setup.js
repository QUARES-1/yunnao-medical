import { vi } from 'vitest'

// ---------- uni-app 全局 mock ----------
vi.stubGlobal('uni', {
  getStorageSync: vi.fn(),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  showToast: vi.fn(),
  reLaunch: vi.fn(),
  redirectTo: vi.fn(),
  switchTab: vi.fn(),
  login: vi.fn(),
  request: vi.fn(),
  uploadFile: vi.fn(),
  chooseImage: vi.fn(),
  chooseAvatar: vi.fn(),
  getImageInfo: vi.fn(),
  getSystemInfo: vi.fn(),
  getDeviceInfo: vi.fn(),
  canIUse: vi.fn(() => false),
  createSelectorQuery: vi.fn(() => ({
    select: vi.fn(() => ({ boundingClientRect: vi.fn(cb => cb()) })),
    selectAll: vi.fn(() => ({ boundingClientRect: vi.fn(cb => cb()) })),
    in: vi.fn()
  })),
  onShow: vi.fn(),
  offShow: vi.fn(),
  onHide: vi.fn(),
  offHide: vi.fn(),
  getLocation: vi.fn(),
  chooseLocation: vi.fn()
})
