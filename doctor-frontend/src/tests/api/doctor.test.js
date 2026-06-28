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
  doctorLogin, doctorRegister, getDoctorInfo, updateDoctorInfo,
  changeDoctorPassword, uploadDoctorAvatar,
  getTodayRegistrations, getHistoryRegistrations, getRegistrationDetail,
  startConsultation, completeConsultation, saveMedicalRecord,
  getMedicalRecordByReg, getMedicalRecordDetail, getDoctorMedicalRecords,
  getExaminationItems, createExamination, cancelExamination,
  getDoctorExaminations, getExaminationsByRegistration, getExaminationDetail,
  getMedicineList, createPrescription, cancelPrescription,
  getDoctorPrescriptions, getPrescriptionsByRegistration, getPrescriptionDetail
} from '@/api/doctor'

describe('Doctor API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('auth', () => {
    it('should login doctor', async () => {
      mockPost.mockResolvedValue({ code: 200, data: 'doctor-token' })
      const result = await doctorLogin('doctor1', 'pass123')
      expect(mockPost).toHaveBeenCalledWith('/api/doctor/login', { username: 'doctor1', password: 'pass123' })
      expect(result.data).toBe('doctor-token')
    })

    it('should register doctor', async () => {
      mockPost.mockResolvedValue({ code: 200, msg: '注册成功' })
      const result = await doctorRegister('newdoc', 'pass', '新医生')
      expect(mockPost).toHaveBeenCalledWith('/api/doctor/register', { username: 'newdoc', password: 'pass', name: '新医生' })
      expect(result.msg).toBe('注册成功')
    })

    it('should get doctor info', async () => {
      const info = { id: 1, username: 'doctor1', name: '张医生', departmentId: 1 }
      mockGet.mockResolvedValue({ code: 200, data: info })
      const result = await getDoctorInfo()
      expect(result.data).toEqual(info)
    })

    it('should update doctor info', async () => {
      mockPut.mockResolvedValue({ code: 200, msg: '个人信息更新成功' })
      const data = { name: '张医生（改）', phone: '13800001111' }
      const result = await updateDoctorInfo(data)
      expect(mockPut).toHaveBeenCalledWith('/api/doctor/update', data)
      expect(result.msg).toBe('个人信息更新成功')
    })

    it('should change doctor password', async () => {
      mockPut.mockResolvedValue({ code: 200, msg: '密码修改成功' })
      const data = { oldPassword: 'old123', newPassword: 'new456' }
      const result = await changeDoctorPassword(data)
      expect(mockPut).toHaveBeenCalledWith('/api/doctor/change-password', data)
      expect(result.msg).toBe('密码修改成功')
    })

    it('should upload doctor avatar', async () => {
      const mockFile = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' })
      mockPost.mockResolvedValue({ code: 200, data: 'http://img.com/avatar.jpg' })
      const result = await uploadDoctorAvatar(mockFile)
      expect(mockPost).toHaveBeenCalled()
      expect(result.data).toBe('http://img.com/avatar.jpg')
    })
  })

  describe('registrations', () => {
    it('should get today registrations', async () => {
      const list = [{ id: 1, patientName: '患者A', status: '待看诊' }]
      mockGet.mockResolvedValue({ code: 200, data: list })
      const result = await getTodayRegistrations()
      expect(result.data).toHaveLength(1)
      expect(result.data[0].patientName).toBe('患者A')
    })

    it('should get history registrations with params', async () => {
      mockGet.mockResolvedValue({ code: 200, data: [] })
      await getHistoryRegistrations({ page: 1, size: 20 })
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/registration/doctor/list')
    })

    it('should get registration detail', async () => {
      const detail = { id: 5, patientName: '患者B', diagnosis: '感冒' }
      mockGet.mockResolvedValue({ code: 200, data: detail })
      const result = await getRegistrationDetail(5)
      expect(result.data).toEqual(detail)
    })
  })

  describe('consultation', () => {
    it('should start consultation', async () => {
      mockPut.mockResolvedValue({ code: 200, msg: '开始看诊成功' })
      const result = await startConsultation(10)
      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/registration/start/10')
      expect(result.msg).toBe('开始看诊成功')
    })

    it('should complete consultation', async () => {
      mockPut.mockResolvedValue({ code: 200, msg: '完成看诊成功' })
      const result = await completeConsultation(10)
      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/registration/complete/10')
      expect(result.msg).toBe('完成看诊成功')
    })
  })

  describe('medical records', () => {
    it('should save medical record', async () => {
      mockPost.mockResolvedValue({ code: 200, msg: '病历保存成功' })
      const data = { registrationId: 1, diagnosis: '发烧', prescription: [] }
      const result = await saveMedicalRecord(data)
      expect(mockPost).toHaveBeenCalledWith('/api/medical-record/save', data)
      expect(result.msg).toBe('病历保存成功')
    })

    it('should get medical record by registration', async () => {
      const record = { id: 1, registrationId: 5, diagnosis: '咳嗽' }
      mockGet.mockResolvedValue({ code: 200, data: record })
      const result = await getMedicalRecordByReg(5)
      expect(result.data).toEqual(record)
    })

    it('should get medical record detail', async () => {
      const detail = { id: 3, diagnosis: '感冒', symptoms: '发烧咳嗽' }
      mockGet.mockResolvedValue({ code: 200, data: detail })
      const result = await getMedicalRecordDetail(3)
      expect(result.data).toEqual(detail)
      expect(mockGet.mock.calls[0][0]).toBe('/api/medical-record/detail/3')
    })

    it('should get doctor medical records with pagination', async () => {
      const pageData = {
        content: [{ id: 1, diagnosis: '发烧' }],
        totalElements: 1, totalPages: 1, number: 1, size: 10
      }
      mockGet.mockResolvedValue({ code: 200, data: pageData })
      const result = await getDoctorMedicalRecords(1, 10)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/medical-record/doctor/list')
      expect(result.data.content).toHaveLength(1)
    })
  })

  describe('examinations', () => {
    it('should get examination items', async () => {
      const items = [{ id: 1, name: '血常规', type: '血液', price: 30 }]
      mockGet.mockResolvedValue({ code: 200, data: items })
      const result = await getExaminationItems('血液')
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/item/list')
      expect(result.data).toHaveLength(1)
    })

    it('should create examination', async () => {
      mockPost.mockResolvedValue({ code: 200, msg: '检查开立成功' })
      const data = { registrationId: 1, items: [{ itemId: 1, name: '血常规' }] }
      const result = await createExamination(data)
      expect(mockPost).toHaveBeenCalledWith('/api/examination/create', data)
      expect(result.msg).toBe('检查开立成功')
    })

    it('should cancel examination', async () => {
      mockPut.mockResolvedValue({ code: 200, msg: '检查已取消' })
      const result = await cancelExamination(5)
      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/examination/cancel/5')
      expect(result.msg).toBe('检查已取消')
    })

    it('should get doctor examinations', async () => {
      const pageData = {
        content: [{ id: 1, itemName: '血常规', status: '已完成' }],
        totalElements: 1, totalPages: 1, number: 1, size: 10
      }
      mockGet.mockResolvedValue({ code: 200, data: pageData })
      const result = await getDoctorExaminations(1, 10)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/doctor/list')
      expect(result.data.content).toHaveLength(1)
    })

    it('should get examinations by registration', async () => {
      const exams = [{ id: 1, itemName: '血常规' }]
      mockGet.mockResolvedValue({ code: 200, data: exams })
      const result = await getExaminationsByRegistration(3)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/registration/3')
      expect(result.data).toHaveLength(1)
    })

    it('should get examination detail', async () => {
      const detail = { id: 2, itemName: '尿常规', result: '正常', status: '已完成' }
      mockGet.mockResolvedValue({ code: 200, data: detail })
      const result = await getExaminationDetail(2)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/examination/detail/2')
      expect(result.data).toEqual(detail)
    })
  })

  describe('medicines', () => {
    it('should get medicine list', async () => {
      const medicines = [{ id: 1, name: '阿莫西林', price: 15, stock: 100 }]
      mockGet.mockResolvedValue({ code: 200, data: medicines })
      const result = await getMedicineList({ name: '阿莫西林' })
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/medicine/list')
      expect(result.data).toHaveLength(1)
    })
  })

  describe('prescriptions', () => {
    it('should create prescription', async () => {
      mockPost.mockResolvedValue({ code: 200, msg: '处方开具成功' })
      const data = { registrationId: 2, medicines: [{ medicineId: 1, quantity: 2 }] }
      const result = await createPrescription(data)
      expect(mockPost).toHaveBeenCalledWith('/api/prescription/create', data)
      expect(result.msg).toBe('处方开具成功')
    })

    it('should cancel prescription', async () => {
      mockPut.mockResolvedValue({ code: 200, msg: '处方已取消' })
      const result = await cancelPrescription(7)
      expect(mockPut).toHaveBeenCalled()
      expect(mockPut.mock.calls[0][0]).toBe('/api/prescription/cancel/7')
      expect(result.msg).toBe('处方已取消')
    })

    it('should get doctor prescriptions', async () => {
      const pageData = {
        content: [{ id: 1, drugs: '[{"name":"药A"}]', totalAmount: 50 }],
        totalElements: 1, totalPages: 1, number: 1, size: 10
      }
      mockGet.mockResolvedValue({ code: 200, data: pageData })
      const result = await getDoctorPrescriptions(1, 10)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/prescription/doctor/list')
      expect(result.data.content).toHaveLength(1)
    })

    it('should get prescriptions by registration', async () => {
      const prescriptions = [{ id: 1, drugs: '[{"name":"药A"}]', status: '已缴费' }]
      mockGet.mockResolvedValue({ code: 200, data: prescriptions })
      const result = await getPrescriptionsByRegistration(4)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/prescription/registration/4')
      expect(result.data).toHaveLength(1)
    })

    it('should get prescription detail', async () => {
      const detail = { id: 6, drugs: '[{"name":"药B"}]', totalAmount: 80, status: '已缴费' }
      mockGet.mockResolvedValue({ code: 200, data: detail })
      const result = await getPrescriptionDetail(6)
      expect(mockGet).toHaveBeenCalled()
      expect(mockGet.mock.calls[0][0]).toBe('/api/prescription/detail/6')
      expect(result.data).toEqual(detail)
    })
  })
})
