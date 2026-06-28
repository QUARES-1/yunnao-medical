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

import { loginAdmin, getAdminInfo, getOverview, changeAdminPassword } from '@/api/admin'

describe('Admin API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('loginAdmin', () => {
    it('should call POST /api/admin/login with credentials', async () => {
      mockPost.mockResolvedValue({ data: { code: 200, msg: '', data: 'token' } })

      const result = await loginAdmin('admin', '123456')

      expect(mockPost).toHaveBeenCalledWith('/api/admin/login', { username: 'admin', password: '123456' })
      expect(result.data.code).toBe(200)
    })

    it('should handle login failure', async () => {
      mockPost.mockResolvedValue({ data: { code: 401, msg: '用户名或密码错误', data: null } })

      const result = await loginAdmin('wrong', 'wrong')

      expect(result.data.code).toBe(401)
      expect(result.data.msg).toBe('用户名或密码错误')
    })
  })

  describe('getAdminInfo', () => {
    it('should call GET /api/admin/info', async () => {
      const adminData = { id: 1, username: 'admin', name: '管理员', createTime: '2024-01-01' }
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: adminData } })

      const result = await getAdminInfo()

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/admin/info')
      expect(result.data.data).toEqual(adminData)
    })
  })

  describe('getOverview', () => {
    it('should call GET /api/admin/statistics/overview', async () => {
      const stats = { patientCount: 100, doctorCount: 20, departmentCount: 10, registrationCount: 500 }
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: stats } })

      const result = await getOverview()

      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/admin/statistics/overview')
      expect(result.data.data).toEqual(stats)
    })

    it('should return zero counts when no data', async () => {
      mockGet.mockResolvedValue({ data: { code: 200, msg: '', data: { patientCount: 0, doctorCount: 0, departmentCount: 0, registrationCount: 0 } } })

      const result = await getOverview()

      expect(result.data.data.patientCount).toBe(0)
    })
  })

  describe('changeAdminPassword', () => {
    it('should call PUT /api/admin/change-pwd', async () => {
      mockPut.mockResolvedValue({ data: { code: 200, msg: '密码修改成功', data: null } })

      const result = await changeAdminPassword('old123', 'new456')

      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/admin/change-pwd')
      expect(mockPut.mock.calls[0][1]).toEqual({ oldPassword: 'old123', newPassword: 'new456' })
      expect(result.data.msg).toBe('密码修改成功')
    })

    it('should handle password change failure', async () => {
      mockPut.mockResolvedValue({ data: { code: 400, msg: '旧密码错误', data: null } })

      const result = await changeAdminPassword('wrong', 'new456')

      expect(result.data.code).toBe(400)
      expect(result.data.msg).toBe('旧密码错误')
    })
  })
})
