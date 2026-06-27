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
    put: vi.fn((url, data, config) => mockPut(url, data, config)),
    delete: vi.fn((url) => mockDelete(url))
  }
}))

import {
  login, getInfo, changePassword, getPrescriptions,
  getPrescription, dispense, getMedicines, adjustStock
} from '@/api/pharmacy'

describe('Pharmacy API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('auth', () => {
    it('should login with role=pharmacy', async () => {
      mockPost.mockResolvedValue({ data: { code: 200, data: 'pharmacy-token' } })

      const result = await login('pharma', 'pass')

      expect(mockPost).toHaveBeenCalledWith('/api/staff/login', { username: 'pharma', password: 'pass', role: 'pharmacy' })
      expect(result.data.data).toBe('pharmacy-token')
    })

    it('should handle login failure', async () => {
      mockPost.mockResolvedValue({ data: { code: 401, msg: '用户名或密码错误' } })

      const result = await login('wrong', 'wrong')

      expect(result.data.code).toBe(401)
    })

    it('should get staff info', async () => {
      const staff = { id: 1, username: 'pharma', name: '药师', role: 'pharmacy' }
      mockGet.mockResolvedValue({ data: { code: 200, data: staff } })

      const result = await getInfo()

      expect(result.data.data).toEqual(staff)
    })

    it('should change password', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '密码修改成功' } })

      const result = await changePassword('old', 'new')

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/staff/change-password')
      expect(mockPut.mock.calls[0][1]).toEqual({ oldPassword: 'old', newPassword: 'new' })
      expect(result.data.msg).toBe('密码修改成功')
    })
  })

  describe('prescriptions', () => {
    it('should get prescriptions by status', async () => {
      const pageData = {
        content: [{ id: 1, patientName: '患者A', drugs: '[{"name":"药A"}]', totalAmount: 50, status: '待发药' }],
        totalElements: 1, totalPages: 1, number: 1, size: 10
      }
      mockGet.mockResolvedValue({ data: { code: 200, data: pageData } })

      const result = await getPrescriptions('待发药', 1, 10)

      expect(mockGet).toHaveBeenCalled()
      expect(result.data.data.content).toHaveLength(1)
    })

    it('should return empty when no prescriptions', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, data: { content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 } } })

      const result = await getPrescriptions('已发药')

      expect(result.data.data.content).toHaveLength(0)
    })

    it('should get prescription detail', async () => {
      const prescription = { id: 5, patientName: '患者B', drugs: '[{"name":"药B"}]', totalAmount: 30, status: '已发药' }
      mockGet.mockResolvedValue({ data: { code: 200, data: prescription } })

      const result = await getPrescription(5)

      expect(result.data.data).toEqual(prescription)
    })

    it('should dispense prescription', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '发药成功' } })

      const result = await dispense(3)

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/prescription/dispense/3')
      expect(result.data.msg).toBe('发药成功')
    })

    it('should handle dispense failure', async () => {
      mockPut.mockResolvedValue({ data: { code: 400, msg: '处方已取消，无法发药' } })

      const result = await dispense(999)

      expect(result.data.code).toBe(400)
    })
  })

  describe('medicines', () => {
    it('should get medicines list', async () => {
      const pageData = {
        content: [{ id: 1, name: '阿莫西林', price: 15, stock: 100 }],
        totalElements: 1, totalPages: 1, number: 1, size: 10
      }
      mockGet.mockResolvedValue({ data: { code: 200, data: pageData } })

      const result = await getMedicines({ name: '阿莫西林' })

      expect(mockGet).toHaveBeenCalled()
      expect(result.data.data.content).toHaveLength(1)
    })

    it('should adjust medicine stock', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '库存调整成功' } })

      const result = await adjustStock(1, 50)

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/medicine/stock/1')
      expect(result.data.msg).toBe('库存调整成功')
    })

    it('should handle negative stock adjustment', async () => {
      mockPut.mockResolvedValue({ data: { code: 400, msg: '库存不足' } })

      const result = await adjustStock(1, -1000)

      expect(result.data.code).toBe(400)
    })
  })
})
