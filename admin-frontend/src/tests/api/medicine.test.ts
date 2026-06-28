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
  getMedicinePage, getMedicineCategories, addMedicine,
  updateMedicine, deleteMedicine, adjustMedicineStock
} from '@/api/medicine'

describe('Medicine API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getMedicinePage', () => {
    it('should call GET /api/medicine/list with params', async () => {
      const pageData = {
        content: [{ id: 1, name: '阿莫西林', price: 15.5, stock: 100 }],
        totalElements: 1, totalPages: 1, size: 10, number: 1
      }
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: pageData } })

      const result = await getMedicinePage({ name: '阿莫西林', page: 1, size: 10 })

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/medicine/list')
      expect(result.data.data.content[0].name).toBe('阿莫西林')
    })

    it('should handle empty search results', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: { content: [], totalElements: 0, totalPages: 0, size: 10, number: 1 } } })

      const result = await getMedicinePage({ name: '不存在的药' })

      expect(result.data.data.content).toHaveLength(0)
    })
  })

  describe('getMedicineCategories', () => {
    it('should call GET /api/medicine/category/list', async () => {
      const categories = [{ id: 1, name: '抗生素', sort: 1 }, { id: 2, name: '感冒药', sort: 2 }]
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: categories } })

      const result = await getMedicineCategories()

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/medicine/category/list')
      expect(result.data.data).toHaveLength(2)
    })
  })

  describe('addMedicine', () => {
    it('should call POST /api/medicine/add', async () => {
      mockPost.mockResolvedValue({ data: { code: 200, msg: '药品添加成功', data: null } })
      const medicine = { name: '布洛芬', price: 20, stock: 50, unit: '盒', categoryId: 2 }

      const result = await addMedicine(medicine)

      expect(mockPost).toHaveBeenCalledWith('/api/medicine/add', medicine)
      expect(result.data.msg).toBe('药品添加成功')
    })

    it('should handle invalid price', async () => {
      mockPost.mockResolvedValue({ data: { code: 400, msg: '价格必须大于0', data: null } })

      const result = await addMedicine({ name: '药', price: -1, stock: 10 })

      expect(result.data.code).toBe(400)
    })
  })

  describe('updateMedicine', () => {
    it('should call PUT /api/medicine/update', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '药品更新成功', data: null } })
      const medicine = { id: 1, name: '阿莫西林（改）', price: 18, stock: 80 }

      const result = await updateMedicine(medicine)

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/medicine/update')
      expect(mockPut.mock.calls[0][1]).toEqual(medicine)
      expect(result.data.msg).toBe('药品更新成功')
    })
  })

  describe('deleteMedicine', () => {
    it('should call DELETE /api/medicine/delete/:id', async () => {
      mockDelete.mockResolvedValue({ data: { code: 200, msg: '药品删除成功', data: null } })

      const result = await deleteMedicine(5)

      expect(mockDelete).toHaveBeenCalledWith('/api/medicine/delete/5')
      expect(result.data.code).toBe(200)
    })
  })

  describe('adjustMedicineStock', () => {
    it('should call PUT /api/medicine/stock/:id', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '库存调整成功', data: null } })

      const result = await adjustMedicineStock(1, 50)

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/medicine/stock/1')
      expect(result.data.msg).toBe('库存调整成功')
    })

    it('should handle negative stock adjustment', async () => {
      mockPut.mockResolvedValue({ data: { code: 400, msg: '库存不足', data: null } })

      const result = await adjustMedicineStock(1, -1000)

      expect(result.data.code).toBe(400)
    })
  })
})
