import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as departmentApi from '@/api/department'
import * as doctorApi from '@/api/doctor'
import * as medicineApi from '@/api/medicine'
import * as aiApi from '@/api/ai'

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn()
}))

vi.mock('@/utils/request', () => ({
  default: { get: mockGet, post: mockPost, put: mockPut, delete: mockDelete }
}))

describe('admin API wrappers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ data: { code: 200, data: {} } })
    mockPost.mockResolvedValue({ data: { code: 200, data: 'ok' } })
    mockPut.mockResolvedValue({ data: { code: 200, data: 'ok' } })
    mockDelete.mockResolvedValue({ data: { code: 200, data: 'ok' } })
  })

  describe('department API', () => {
    it('addDepartment POST /api/department/add', async () => {
      await departmentApi.addDepartment({ id: 1, name: '内科' } as any)
      expect(mockPost).toHaveBeenCalledWith('/api/department/add', { id: 1, name: '内科' })
    })

    it('updateDepartment PUT /api/department/update', async () => {
      await departmentApi.updateDepartment({ id: 1, name: '外科' } as any)
      expect(mockPut).toHaveBeenCalledWith('/api/department/update', { id: 1, name: '外科' })
    })

    it('deleteDepartment DELETE /api/department/delete/:id', async () => {
      await departmentApi.deleteDepartment(5)
      expect(mockDelete).toHaveBeenCalledWith('/api/department/delete/5')
    })
  })

  describe('doctor API', () => {
    it('addDoctor POST /api/doctor/add', async () => {
      await doctorApi.addDoctor({ name: '新医生', departmentId: 1 } as any)
      expect(mockPost).toHaveBeenCalledWith('/api/doctor/add', { name: '新医生', departmentId: 1 })
    })

    it('resetDoctorPassword PUT /api/doctor/reset-pwd/:id', async () => {
      await doctorApi.resetDoctorPassword(3)
      expect(mockPut).toHaveBeenCalledWith('/api/doctor/reset-pwd/3')
    })

    it('deleteDoctor DELETE /api/doctor/delete/:id', async () => {
      await doctorApi.deleteDoctor(7)
      expect(mockDelete).toHaveBeenCalledWith('/api/doctor/delete/7')
    })
  })

  describe('medicine API', () => {
    it('getMedicineCategories GET /api/medicine/category/list', async () => {
      await medicineApi.getMedicineCategories()
      expect(mockGet).toHaveBeenCalledWith('/api/medicine/category/list')
    })

    it('addMedicine POST /api/medicine/add', async () => {
      await medicineApi.addMedicine({ name: '阿莫西林' } as any)
      expect(mockPost).toHaveBeenCalledWith('/api/medicine/add', { name: '阿莫西林' })
    })

    it('updateMedicine PUT /api/medicine/update', async () => {
      await medicineApi.updateMedicine({ id: 1, name: '阿莫西林胶囊' } as any)
      expect(mockPut).toHaveBeenCalledWith('/api/medicine/update', { id: 1, name: '阿莫西林胶囊' })
    })

    it('deleteMedicine DELETE /api/medicine/delete/:id', async () => {
      await medicineApi.deleteMedicine(4)
      expect(mockDelete).toHaveBeenCalledWith('/api/medicine/delete/4')
    })
  })

  describe('AI API', () => {
    it('getDoctorQualityStats GET /api/admin/ai/quality-check/doctor-stats', async () => {
      await aiApi.getDoctorQualityStats()
      expect(mockGet).toHaveBeenCalledWith('/api/admin/ai/quality-check/doctor-stats')
    })

    it('generateAiReport POST /api/admin/ai/operation-report/generate', async () => {
      await aiApi.generateAiReport({ reportType: 'weekly', startDate: '2026-07-01', endDate: '2026-07-07' })
      expect(mockPost).toHaveBeenCalledWith('/api/admin/ai/operation-report/generate', { reportType: 'weekly', startDate: '2026-07-01', endDate: '2026-07-07' })
    })

    it('startQualityCheck POST /api/admin/ai/quality-check/start', async () => {
      await aiApi.startQualityCheck({ checkType: 'full', sampleSize: 20 })
      expect(mockPost).toHaveBeenCalledWith('/api/admin/ai/quality-check/start', { checkType: 'full', sampleSize: 20 })
    })
  })
})
