import { describe, it, expect, vi, beforeEach } from 'vitest'

const mockGetDepartments = vi.fn()
const mockGetDoctors = vi.fn()
const mockGetDoctorDetail = vi.fn()
const mockGetDoctorSchedule = vi.fn()
const mockDevPatientLogin = vi.fn()
const mockWxLogin = vi.fn()
const mockGetPatientInfo = vi.fn()
const mockUpdatePatientInfo = vi.fn()
const mockCreateRegistration = vi.fn()
const mockGetRegistrations = vi.fn()
const mockGetRegistrationDetail = vi.fn()
const mockCancelRegistration = vi.fn()

vi.mock('@/api/patient', () => ({
  getDepartments: mockGetDepartments,
  getDoctors: mockGetDoctors,
  getDoctorDetail: mockGetDoctorDetail,
  getDoctorSchedule: mockGetDoctorSchedule,
  devPatientLogin: mockDevPatientLogin,
  wxLogin: mockWxLogin,
  getPatientInfo: mockGetPatientInfo,
  updatePatientInfo: mockUpdatePatientInfo,
  createRegistration: mockCreateRegistration,
  getRegistrations: mockGetRegistrations,
  getRegistrationDetail: mockGetRegistrationDetail,
  cancelRegistration: mockCancelRegistration
}))

vi.mock('@/utils/request', () => ({
  request: vi.fn(),
  showError: vi.fn(),
  API_BASE_URL: 'http://localhost:8080'
}))

vi.stubGlobal('uni', {
  getStorageSync: vi.fn(() => ''),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  login: vi.fn(),
  uploadFile: vi.fn(),
  showToast: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({}))
})

describe('Patient API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('departments & doctors', () => {
    it('should get department list', async () => {
      const departments = [{ id: 1, name: '内科' }, { id: 2, name: '外科' }]
      mockGetDepartments.mockResolvedValue(departments)

      const result = await mockGetDepartments()

      expect(result).toHaveLength(2)
      expect(result[0].name).toBe('内科')
    })

    it('should get doctors by department', async () => {
      const doctors = [{ id: 1, name: '张医生', departmentId: 1 }]
      mockGetDoctors.mockResolvedValue(doctors)

      const result = await mockGetDoctors(1)

      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('张医生')
    })

    it('should get doctor detail', async () => {
      const doctor = { id: 3, name: '李医生', title: '主任医师', specialty: '心血管' }
      mockGetDoctorDetail.mockResolvedValue(doctor)

      const result = await mockGetDoctorDetail(3)

      expect(result.name).toBe('李医生')
      expect(result.specialty).toBe('心血管')
    })

    it('should return empty list when no departments', async () => {
      mockGetDepartments.mockResolvedValue([])

      const result = await mockGetDepartments()

      expect(result).toEqual([])
    })
  })

  describe('schedule', () => {
    it('should get doctor schedule', async () => {
      const schedule = {
        dates: ['2024-06-20', '2024-06-21'],
        timeSlots: ['上午', '下午'] as ('上午' | '下午')[],
        availability: [{ date: '2024-06-20', timeSlot: '上午', capacity: 20, remaining: 15 }]
      }
      mockGetDoctorSchedule.mockResolvedValue(schedule)

      const result = await mockGetDoctorSchedule(1)

      expect(result.dates).toHaveLength(2)
      expect(result.availability[0].remaining).toBe(15)
    })
  })

  describe('registrations', () => {
    it('should create registration', async () => {
      const registration = { id: 1, doctorId: 1, status: '待就诊', registrationDate: '2024-06-20', timeSlot: '上午' as const }
      mockCreateRegistration.mockResolvedValue(registration)

      const result = await mockCreateRegistration({ doctorId: 1, registrationDate: '2024-06-20', timeSlot: '上午' })

      expect(result.id).toBe(1)
      expect(result.status).toBe('待就诊')
    })

    it('should get patient registrations', async () => {
      const pageData = {
        content: [{ id: 1, patientName: '患者', status: '待就诊' }],
        totalElements: 1, totalPages: 1, number: 1, size: 10, numberOfElements: 1
      }
      mockGetRegistrations.mockResolvedValue(pageData)

      const result = await mockGetRegistrations({ page: 1, size: 10 })

      expect(result.content).toHaveLength(1)
      expect(result.totalElements).toBe(1)
    })

    it('should filter registrations by status', async () => {
      const pageData = { content: [], totalElements: 0, totalPages: 0, number: 1, size: 10, numberOfElements: 0 }
      mockGetRegistrations.mockResolvedValue(pageData)

      const result = await mockGetRegistrations({ status: '已就诊', page: 1, size: 10 })

      expect(result.content).toHaveLength(0)
    })

    it('should get registration detail', async () => {
      const detail = { id: 5, doctorName: '张医生', status: '待就诊', registrationDate: '2024-06-20' }
      mockGetRegistrationDetail.mockResolvedValue(detail)

      const result = await mockGetRegistrationDetail(5)

      expect(result.doctorName).toBe('张医生')
    })

    it('should cancel registration', async () => {
      mockCancelRegistration.mockResolvedValue('取消成功')

      const result = await mockCancelRegistration(3)

      expect(result).toBe('取消成功')
    })

    it('should handle cancel failure', async () => {
      mockCancelRegistration.mockRejectedValue(new Error('无法取消已就诊的挂号'))

      await expect(mockCancelRegistration(3)).rejects.toThrow('无法取消已就诊的挂号')
    })
  })
})
