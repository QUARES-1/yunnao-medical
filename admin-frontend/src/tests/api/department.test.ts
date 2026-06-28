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
    put: vi.fn((url, data) => mockPut(url, data)),
    delete: vi.fn((url) => mockDelete(url))
  }
}))

import { getDepartments, addDepartment, updateDepartment, deleteDepartment } from '@/api/department'

describe('Department API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getDepartments', () => {
    it('should call GET /api/department/list', async () => {
      const departments = [
        { id: 1, name: '内科', description: '内科门诊', sort: 1 },
        { id: 2, name: '外科', description: '外科门诊', sort: 2 }
      ]
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: departments } })

      const result = await getDepartments()

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/department/list')
      expect(result.data.data).toHaveLength(2)
      expect(result.data.data[0].name).toBe('内科')
    })

    it('should return empty array when no departments', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: [] } })

      const result = await getDepartments()

      expect(result.data.data).toEqual([])
    })
  })

  describe('addDepartment', () => {
    it('should call POST /api/department/add', async () => {
      mockPost.mockResolvedValue({ data: { code: 200, msg: '科室添加成功', data: null } })
      const dept = { name: '儿科', description: '儿科门诊', sort: 3 }

      const result = await addDepartment(dept)

      expect(mockPost).toHaveBeenCalledWith('/api/department/add', dept)
      expect(result.data.msg).toBe('科室添加成功')
    })

    it('should handle duplicate department name', async () => {
      mockPost.mockResolvedValue({ data: { code: 400, msg: '科室名称已存在', data: null } })

      const result = await addDepartment({ name: '内科', sort: 1 })

      expect(result.data.code).toBe(400)
    })
  })

  describe('updateDepartment', () => {
    it('should call PUT /api/department/update', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '科室更新成功', data: null } })
      const dept = { id: 1, name: '内科（改）', description: '修改后描述', sort: 1 }

      const result = await updateDepartment(dept)

      expect(mockPut).toHaveBeenCalledWith('/api/department/update', dept)
      expect(result.data.msg).toBe('科室更新成功')
    })
  })

  describe('deleteDepartment', () => {
    it('should call DELETE /api/department/delete/:id', async () => {
      mockDelete.mockResolvedValue({ data: { code: 200, msg: '科室删除成功', data: null } })

      const result = await deleteDepartment(3)

      expect(mockDelete).toHaveBeenCalledWith('/api/department/delete/3')
      expect(result.data.code).toBe(200)
    })

    it('should handle department not found', async () => {
      mockDelete.mockResolvedValue({ data: { code: 404, msg: '科室不存在', data: null } })

      const result = await deleteDepartment(9999)

      expect(result.data.code).toBe(404)
    })

    it('should handle department with doctors', async () => {
      mockDelete.mockResolvedValue({ data: { code: 400, msg: '该科室下存在医生，无法删除', data: null } })

      const result = await deleteDepartment(1)

      expect(result.data.code).toBe(400)
      expect(result.data.msg).toBe('该科室下存在医生，无法删除')
    })
  })
})
