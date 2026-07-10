import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn()
}))

vi.mock('@/utils/request', () => ({ default: requestMock }))

import * as adminApi from '@/api/admin'
import * as aiApi from '@/api/ai'
import * as departmentApi from '@/api/department'
import * as doctorApi from '@/api/doctor'
import * as medicineApi from '@/api/medicine'
import { useAiAdminStore } from '@/stores/ai'
import { useAuthStore } from '@/stores/auth'
import { useMedicalStore } from '@/stores/medical'

describe('admin source modules', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/admin/info') return Promise.resolve({ data: { data: { id: 1, username: 'admin' } } })
      if (url === '/api/admin/statistics/overview') return Promise.resolve({ data: { data: { patientCount: 1, doctorCount: 2, departmentCount: 3, registrationCount: 4 } } })
      if (url === '/api/department/list') return Promise.resolve({ data: { data: [{ id: 1, name: '内科' }] } })
      if (url === '/api/doctor/page') return Promise.resolve({ data: { data: { content: [{ id: 2, name: 'Dr' }] } } })
      if (url === '/api/medicine/list') return Promise.resolve({ data: { data: { content: [{ id: 3, name: 'Med', stock: 10 }] } } })
      return Promise.resolve({ data: { data: { content: [] } } })
    })
    requestMock.post.mockResolvedValue({ data: { data: 'token' } })
    requestMock.put.mockResolvedValue({ data: { data: 'ok' } })
    requestMock.delete.mockResolvedValue({ data: { data: 'ok' } })
  })

  it('calls API wrappers with expected methods and payloads', async () => {
    adminApi.loginAdmin('admin', 'pwd')
    adminApi.registerAdmin('new', 'pwd', 'name')
    adminApi.changeAdminPassword('old', 'new')
    departmentApi.addDepartment({ name: 'dept', sort: 1 })
    departmentApi.updateDepartment({ id: 1, name: 'dept', sort: 2 })
    departmentApi.deleteDepartment(1)
    doctorApi.getDoctorPage()
    doctorApi.addDoctor({ name: 'doctor' } as any)
    doctorApi.resetDoctorPassword(2)
    doctorApi.deleteDoctor(2)
    medicineApi.getMedicinePage({ page: 1 })
    medicineApi.getMedicineCategories()
    medicineApi.addMedicine({ name: 'med' } as any)
    medicineApi.updateMedicine({ id: 1, name: 'med' } as any)
    medicineApi.deleteMedicine(1)
    medicineApi.adjustMedicineStock(1, 5)
    aiApi.generateAiReport({ reportType: 'daily' })
    aiApi.startQualityCheck({ checkType: 'sample', sampleSize: 5 })
    aiApi.getKnowledgeList({ page: 1 })
    aiApi.addKnowledge({ title: 'k' } as any)
    aiApi.updateKnowledge({ id: 1, title: 'k' } as any)
    aiApi.deleteKnowledge(1)

    expect(requestMock.post).toHaveBeenCalledWith('/api/admin/login', { username: 'admin', password: 'pwd' })
    expect(requestMock.delete).toHaveBeenCalledWith('/api/department/delete/1')
    expect(requestMock.put).toHaveBeenCalledWith('/api/medicine/stock/1', null, { params: { quantity: 5 } })
  })

  it('updates auth, medical and AI stores from real store actions', async () => {
    const auth = useAuthStore()
    await auth.login('admin', 'pwd')
    expect(auth.token).toBe('token')
    expect(auth.isLoggedIn).toBe(true)

    const medical = useMedicalStore()
    await medical.loadSnapshot()
    expect(medical.lowStockMedicines).toHaveLength(1)
    expect(medical.departmentMap.get(1)).toBe('内科')

    const ai = useAiAdminStore()
    await ai.loadDashboard('daily')
    await ai.loadReports()
    expect(ai.loading).toBe(false)

    auth.logout()
    expect(auth.isLoggedIn).toBe(false)
  })

  it('initializes auth from cached storage and covers medical fallback branches', async () => {
    localStorage.setItem('admin_token', 'cached-token')
    localStorage.setItem('admin_info', JSON.stringify({ id: 9, username: 'cached' }))
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.admin?.username).toBe('cached')

    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/admin/statistics/overview') return Promise.resolve({ data: { data: { patientCount: 0, doctorCount: 0, departmentCount: 0, registrationCount: 0 } } })
      if (url === '/api/department/list') return Promise.resolve({ data: { data: null } })
      if (url === '/api/doctor/page') return Promise.resolve({ data: { data: {} } })
      if (url === '/api/medicine/list') {
        return Promise.resolve({
          data: {
            data: {
              content: [
                { id: 1, name: 'Enough', stock: 31 },
                { id: 2, name: 'Empty', stock: 0 },
                { id: 3, name: 'Missing' }
              ]
            }
          }
        })
      }
      return Promise.resolve({ data: { data: {} } })
    })

    const medical = useMedicalStore()
    await medical.loadSnapshot()
    expect(medical.departmentMap.size).toBe(0)
    expect(medical.doctors).toEqual([])
    expect(medical.lowStockMedicines.map(item => item.name)).toEqual(['Empty', 'Missing'])
  })

  it('streams AI operation reports and stores streamed metadata', async () => {
    localStorage.setItem('admin_token', 'admin-token')
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event: delta\ndata: hello\n\n'),
      encoder.encode('event: done\ndata: report-1\n\n')
    ]
    let index = 0
    vi.stubGlobal('fetch', vi.fn(async (_url, options: any) => {
      expect(options.headers.token).toBe('admin-token')
      return {
        ok: true,
        body: {
          getReader: () => ({
            read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true }
          })
        }
      }
    }))

    const deltas: string[] = []
    let doneId = ''
    await aiApi.streamAiOperationReport({ reportType: 'daily' }, chunk => deltas.push(chunk), id => { doneId = id || '' })
    expect(deltas).toEqual(['hello'])
    expect(doneId).toBe('report-1')

    index = 0
    const store = useAiAdminStore()
    await store.generateReportStream('monthly')
    expect(store.streamText).toBe('hello')
    expect(store.lastStreamReportId).toBe('report-1')

    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 500, body: null })))
    await expect(aiApi.streamAiOperationReport({ reportType: 'daily' }, vi.fn())).rejects.toThrow('500')
  })

  it('covers AI dashboard fallback data and additional stream branches', async () => {
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/api/admin/ai/operation-overview') return Promise.reject(new Error('offline'))
      if (url === '/api/admin/ai/operation-report/list') return Promise.resolve({ data: { data: {} } })
      if (url === '/api/admin/ai/quality-check/list') return Promise.resolve({ data: { data: {} } })
      if (url === '/api/admin/ai/chat-log') return Promise.resolve({ data: { data: {} } })
      return Promise.resolve({ data: { data: {} } })
    })

    const store = useAiAdminStore()
    await store.loadDashboard()
    expect(store.keyMetrics).toEqual({})
    expect(store.reports).toEqual([])
    expect(store.checks).toEqual([])
    expect(store.logs).toEqual([])

    const encoder = new TextEncoder()
    let chunks = [encoder.encode('event: message\ndata: first\n\ndata: second\n\n')]
    let index = 0
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
    })))
    const deltas: string[] = []
    await aiApi.streamAiOperationReport({ reportType: 'weekly' }, chunk => deltas.push(chunk))
    expect(deltas).toEqual(['first', 'second'])

    chunks = [encoder.encode('event: done\ndata: report-no-callback\n\n')]
    index = 0
    await expect(aiApi.streamAiOperationReport({ reportType: 'weekly' }, vi.fn())).resolves.toBeUndefined()

    chunks = [encoder.encode('event: error\ndata:\n\n')]
    index = 0
    await expect(aiApi.streamAiOperationReport({ reportType: 'weekly' }, vi.fn())).rejects.toThrow()
  })

  it('covers AI store null payloads and default stream report type', async () => {
    requestMock.get.mockImplementation((url: string, config?: any) => {
      if (url === '/api/admin/ai/operation-overview') return Promise.resolve({ data: { data: null } })
      if (url === '/api/admin/ai/operation-report/list') {
        return Promise.resolve({ data: { data: { requestedReportType: config?.params?.reportType, content: null } } })
      }
      if (url === '/api/admin/ai/quality-check/list') return Promise.resolve({ data: { data: { content: null } } })
      if (url === '/api/admin/ai/chat-log') return Promise.resolve({ data: { data: { content: null } } })
      return Promise.resolve({ data: { data: {} } })
    })
    const store = useAiAdminStore()
    await store.loadDashboard()
    expect(store.overview).toEqual({})
    expect(store.reports).toEqual([])

    await store.loadReports('monthly')
    expect(requestMock.get).toHaveBeenCalledWith('/api/admin/ai/operation-report/list', { params: { reportType: 'monthly', page: 1, size: 6 } })

    const encoder = new TextEncoder()
    let index = 0
    const chunks = [encoder.encode('event: delta\ndata: daily\n\n')]
    vi.stubGlobal('fetch', vi.fn(async (_url, options: any) => {
      expect(JSON.parse(options.body).reportType).toBe('daily')
      return {
        ok: true,
        body: { getReader: () => ({ read: async () => index < chunks.length ? { done: false, value: chunks[index++] } : { done: true } }) }
      }
    }))
    await store.generateReportStream()
    expect(store.streamText).toBe('daily')
  })
})
