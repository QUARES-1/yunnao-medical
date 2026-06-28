import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

const mockPost = vi.spyOn(axios, 'post')
const mockGet = vi.spyOn(axios, 'get')
const mockPut = vi.spyOn(axios, 'put')
const mockDelete = vi.spyOn(axios, 'delete')

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn((url, data) => mockPost(url, data)),
    get: vi.fn((url, config) => mockGet(url, config)),
    put: vi.fn((url, config) => mockPut(url, config)),
    delete: vi.fn((url) => mockDelete(url))
  }
}))

import { getDoctorPage, addDoctor, resetDoctorPassword, deleteDoctor } from '@/api/doctor'

describe('Doctor API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getDoctorPage', () => {
    it('should call GET /api/doctor/page with default pagination', async () => {
      const pageData = {
        content: [{ id: 1, username: 'doc1', name: '医生1' }],
        totalElements: 1, totalPages: 1, size: 10, number: 1
      }
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: pageData } })

      const result = await getDoctorPage()

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/doctor/page')
      expect(result.data.data.content).toHaveLength(1)
    })

    it('should accept custom page and size', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: { content: [], totalElements: 0, totalPages: 0, size: 20, number: 2 } } })

      await getDoctorPage(3, 20)

      expect(mockGet.mock.calls[0][0]).toBe('/api/doctor/page')
    })

    it('should return empty content when no doctors', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: { content: [], totalElements: 0, totalPages: 0, size: 10, number: 1 } } })

      const result = await getDoctorPage()

      expect(result.data.data.content).toHaveLength(0)
      expect(result.data.data.totalElements).toBe(0)
    })
  })

  describe('addDoctor', () => {
    it('should call POST /api/doctor/add with doctor data', async () => {
      mockPost.mockResolvedValue({ data: { code: 200, msg: '添加成功', data: null } })
      const doctorData = { username: 'newdoc', name: '新医生', phone: '13800000000', departmentId: 1 }

      const result = await addDoctor(doctorData)

      expect(mockPost).toHaveBeenCalledWith('/api/doctor/add', doctorData)
      expect(result.data.msg).toBe('添加成功')
    })

    it('should handle duplicate username error', async () => {
      mockPost.mockResolvedValue({ data: { code: 400, msg: '用户名已存在', data: null } })

      const result = await addDoctor({ username: 'existing', name: '医生' })

      expect(result.data.code).toBe(400)
      expect(result.data.msg).toBe('用户名已存在')
    })
  })

  describe('resetDoctorPassword', () => {
    it('should call PUT /api/doctor/reset-pwd/:id', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '密码重置成功', data: null } })

      const result = await resetDoctorPassword(5)

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/doctor/reset-pwd/5')
      expect(result.data.msg).toBe('密码重置成功')
    })
  })

  describe('deleteDoctor', () => {
    it('should call DELETE /api/doctor/delete/:id', async () => {
      mockDelete.mockResolvedValue({ data: { code: 200, msg: '删除成功', data: null } })

      const result = await deleteDoctor(10)

      expect(mockDelete).toHaveBeenCalledWith('/api/doctor/delete/10')
      expect(result.data.code).toBe(200)
    })

    it('should handle doctor not found', async () => {
      mockDelete.mockResolvedValue({ data: { code: 404, msg: '医生不存在', data: null } })

      const result = await deleteDoctor(9999)

      expect(result.data.code).toBe(404)
      expect(result.data.msg).toBe('医生不存在')
    })
  })
})
