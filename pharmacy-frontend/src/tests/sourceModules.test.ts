import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn()
}))
const sseMock = vi.hoisted(() => vi.fn())
const messageMock = vi.hoisted(() => ({ error: vi.fn() }))

vi.mock('@/utils/request', () => ({ default: requestMock }))
vi.mock('@/utils/sse', () => ({ streamSse: sseMock }))
vi.mock('element-plus', () => ({ ElMessage: messageMock }))

import * as pharmacyApi from '@/api/pharmacy'
import { useAuthStore } from '@/stores/auth'
import { usePharmacyStore } from '@/stores/pharmacy'

describe('pharmacy source modules', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    requestMock.post.mockResolvedValue({ data: { data: 'pharmacy-token' } })
    requestMock.put.mockResolvedValue({ data: { data: 'ok' } })
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/staff/info') return Promise.resolve({ data: { data: { id: 1, name: 'Pharmacy' } } })
      if (url === '/api/prescription/pharmacy/list') return Promise.resolve({ data: { data: { content: [{ id: 1, status: '待发药' }], totalElements: 1 } } })
      if (url === '/api/medicine/list') return Promise.resolve({ data: { data: { content: [{ id: 2, name: 'Med', stock: 8 }], totalElements: 1 } } })
      if (url === '/api/medicine/ai/stock-forecast/list') {
        return Promise.resolve({ data: { data: { content: [{ id: 3, forecastData: '[{\"name\":\"Med\",\"currentStock\":8}]' }], totalElements: 1 } } })
      }
      return Promise.resolve({ data: { data: { content: [] } } })
    })
  })

  it('calls API wrappers and updates auth store', async () => {
    pharmacyApi.login('u', 'p')
    pharmacyApi.changePassword('old', 'new')
    pharmacyApi.getPrescription(1)
    pharmacyApi.dispense(1)
    pharmacyApi.getPrescriptions('待发药')
    pharmacyApi.getMedicines({ keyword: 'Med' })
    pharmacyApi.adjustStock(2, 5)
    pharmacyApi.generateStockForecast('2026-07')
    pharmacyApi.getStockForecast(3)
    pharmacyApi.getStockForecastList()
    expect(requestMock.post).toHaveBeenCalledWith('/api/staff/login', { username: 'u', password: 'p', role: 'pharmacy' })
    expect(requestMock.put).toHaveBeenCalledWith('/api/medicine/stock/2', null, { params: { quantity: 5 } })

    const auth = useAuthStore()
    await auth.signIn('u', 'p')
    expect(auth.token).toBe('pharmacy-token')
    expect(auth.user?.name).toBe('Pharmacy')
    auth.signOut()
    expect(auth.token).toBe('')
    await auth.loadUser()
    expect(auth.user).toBeNull()
  })

  it('loads pharmacy workbench state and handles stream success and failure', async () => {
    const store = usePharmacyStore()
    await store.loadPrescriptionState()
    await store.loadMedicineState()
    await store.loadForecastState()
    expect(store.pendingCount).toBe(1)
    expect(store.dispensedCount).toBe(1)
    expect(store.lowStockMedicines).toHaveLength(1)
    expect(store.latestForecastRows[0].name).toBe('Med')

    sseMock.mockImplementationOnce(async (_url, options) => {
      options.handlers.onDelta('forecast')
      await options.handlers.onDone()
    })
    await store.streamForecastReport({ period: '2026-07', medicines: [] })
    expect(store.streamText).toBe('forecast')

    sseMock.mockRejectedValueOnce(new Error('offline'))
    await expect(store.streamForecastReport({ medicines: [] })).rejects.toThrow('offline')
    expect(store.streamText).toContain('生成失败')
    expect(messageMock.error).toHaveBeenCalled()
    store.clearStream()
    expect(store.streamText).toBe('')
  })

  it('covers pharmacy fallback data, parser and stream error branches', async () => {
    localStorage.setItem('pharmacy_token', 'pharmacy-token')
    const store = usePharmacyStore()
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/prescription/pharmacy/list') return Promise.resolve({ data: { data: {} } })
      if (url === '/api/medicine/list') {
        return Promise.resolve({
          data: {
            data: {
              content: [
                { id: 1, name: 'High', stock: 21 },
                { id: 2, name: 'Low', stock: 20 },
                { id: 3, name: 'Missing' }
              ]
            }
          }
        })
      }
      if (url === '/api/medicine/ai/stock-forecast/list') {
        return Promise.resolve({ data: { data: { content: [{ id: 1, forecastData: '{bad json' }] } } })
      }
      return Promise.resolve({ data: { data: {} } })
    })

    await store.loadPrescriptionState()
    expect(store.pendingPrescriptions).toEqual([])
    expect(store.dispensedPrescriptions).toEqual([])
    await store.loadMedicineState()
    expect(store.lowStockMedicines.map(item => item.name)).toEqual(['Low', 'Missing'])
    await store.loadForecastState()
    expect(store.latestForecastRows).toEqual([])
    store.forecasts = []
    expect(store.latestForecastRows).toEqual([])

    sseMock.mockImplementationOnce(async (_url, options) => {
      expect(options.token).toBe('pharmacy-token')
      expect(options.body).toMatchObject({ forecastType: 'monthly', medicines: [] })
      options.handlers.onError('')
    })
    await expect(store.streamForecastReport({ medicines: [] })).rejects.toThrow()

    sseMock.mockRejectedValueOnce({})
    await expect(store.streamForecastReport({ medicines: [] })).rejects.toEqual({})
    expect(store.streamText).toContain('生成失败')
  })
})
