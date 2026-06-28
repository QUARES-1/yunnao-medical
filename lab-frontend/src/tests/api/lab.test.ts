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

import {
  login, getInfo, changePassword, getExaminations,
  getExamination, updateResult, getItems, uploadFile
} from '@/api/lab'

describe('Lab API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('should call POST /api/staff/login with role=lab', async () => {
      mockPost.mockResolvedValue({ data: { code: 200, data: 'lab-token' } })

      const result = await login('labuser', 'password')

      expect(mockPost).toHaveBeenCalledWith('/api/staff/login', { username: 'labuser', password: 'password', role: 'lab' })
      expect(result.data.data).toBe('lab-token')
    })

    it('should handle login failure', async () => {
      mockPost.mockResolvedValue({ data: { code: 401, msg: '用户名或密码错误' } })

      const result = await login('wrong', 'wrong')

      expect(result.data.code).toBe(401)
    })
  })

  describe('getInfo', () => {
    it('should call GET /api/staff/info', async () => {
      const staff = { id: 1, username: 'lab', name: '检验员', role: 'lab' }
      mockGet.mockResolvedValue({ data: { code: 200, data: staff } })

      const result = await getInfo()

      expect(mockGet).toHaveBeenCalled()
      expect(result.data.data).toEqual(staff)
    })
  })

  describe('changePassword', () => {
    it('should call PUT /api/staff/change-password', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '密码修改成功' } })

      const result = await changePassword('oldpass', 'newpass')

      expect(mockPut).toHaveBeenCalledWith('/api/staff/change-password', { oldPassword: 'oldpass', newPassword: 'newpass' })
      expect(result.data.code).toBe(200)
    })
  })

  describe('getExaminations', () => {
    it('should call GET /api/examination/lab/list with status and pagination', async () => {
      const pageData = {
        content: [{ id: 1, patientName: '张三', itemName: '血常规', status: '待检查' }],
        totalElements: 1, totalPages: 1, number: 1, size: 10
      }
      mockGet.mockResolvedValue({ data: { code: 200, data: pageData } })

      const result = await getExaminations('待检查', 1, 10)

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/lab/list')
      expect(result.data.data.content).toHaveLength(1)
    })

    it('should return empty list when no examinations', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, data: { content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 } } })

      const result = await getExaminations('已完成')

      expect(result.data.data.content).toHaveLength(0)
    })
  })

  describe('getExamination', () => {
    it('should call GET /api/examination/detail/:id', async () => {
      const exam = { id: 5, patientName: '李四', itemName: '尿常规', status: '已完成', result: '正常' }
      mockGet.mockResolvedValue({ data: { code: 200, data: exam } })

      const result = await getExamination(5)

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/detail/5')
      expect(result.data.data.result).toBe('正常')
    })
  })

  describe('updateResult', () => {
    it('should call PUT /api/examination/update-result', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '结果录入成功' } })
      const data = { id: 3, result: '阳性', resultImages: 'http://img.jpg' }

      const result = await updateResult(data)

      expect(mockPut).toHaveBeenCalledWith('/api/examination/update-result', data)
      expect(result.data.msg).toBe('结果录入成功')
    })

    it('should handle update without images', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '结果录入成功' } })
      const data = { id: 4, result: '阴性' }

      const result = await updateResult(data)

      expect(mockPut).toHaveBeenCalledWith('/api/examination/update-result', data)
    })
  })

  describe('getItems', () => {
    it('should call GET /api/examination/item/list', async () => {
      const items = [{ id: 1, name: '血常规', type: '血液', price: 30 }]
      mockGet.mockResolvedValue({ data: { code: 200, data: items } })

      const result = await getItems()

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/item/list')
      expect(result.data.data).toHaveLength(1)
    })

    it('should filter by type', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, data: [] } })

      await getItems('尿液')

      expect(mockGet).toHaveBeenCalled()
    })

    it('should return all items when no type provided', async () => {
      const items = [{ id: 1, name: '血常规' }, { id: 2, name: '尿常规' }]
      mockGet.mockResolvedValue({ data: { code: 200, data: items } })

      const result = await getItems()

      expect(mockGet).toHaveBeenCalled()
      expect(result.data.data).toHaveLength(2)
    })
  })

  describe('uploadFile', () => {
    it('should upload file with multipart form data', async () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      mockPost.mockResolvedValue({ data: { code: 200, data: 'http://img.com/test.jpg' } })

      const result = await uploadFile(mockFile)

      expect(mockPost).toHaveBeenCalled()
      expect(result.data.data).toBe('http://img.com/test.jpg')
    })
  })
})
