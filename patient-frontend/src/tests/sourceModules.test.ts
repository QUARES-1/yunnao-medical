import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const requestMock = vi.hoisted(() => vi.fn())
const sseMock = vi.hoisted(() => vi.fn())

vi.mock('@/utils/request', () => ({
  API_BASE_URL: 'http://localhost:8080',
  request: requestMock
}))
vi.mock('@/utils/sse', () => ({ streamSse: sseMock }))

import * as patientApi from '@/api/patient'
import { usePatientAiStore } from '@/stores/ai'
import { useAuthStore } from '@/stores/auth'
import { useDoctorsStore } from '@/stores/doctors'
import { usePrescriptionsStore } from '@/stores/prescriptions'
import { useRegistrationsStore } from '@/stores/registrations'

describe('patient source modules', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    requestMock.mockImplementation((options: any) => {
      if (options.url === '/api/patient/test-login') return Promise.resolve({ token: 'patient-token', patientId: 1 })
      if (options.url === '/api/patient/wx-login?code=wx-code') return Promise.resolve({ token: 'wx-token', patientId: 2 })
      if (options.url === '/api/patient/info') return Promise.resolve({ id: 1, name: 'Alice' })
      if (options.url === '/api/department/list') return Promise.resolve([{ id: 1, name: '内科' }])
      if (options.url === '/api/doctor/list') return Promise.resolve([{ id: 2, name: 'Dr' }])
      if (options.url === '/api/registration/patient/list') return Promise.resolve({ content: [{ id: 1, status: '待就诊' }], totalElements: 1, totalPages: 1 })
      if (options.url === '/api/prescription/patient/list') return Promise.resolve({ content: [{ id: 1, status: '已发药' }], totalElements: 1, totalPages: 2 })
      return Promise.resolve({})
    })
    vi.mocked(uni.getStorageSync).mockReset()
    vi.mocked(uni.setStorageSync).mockReset()
    vi.mocked(uni.removeStorageSync).mockReset()
    vi.mocked(uni.login).mockReset()
  })

  it('calls patient API wrappers with expected request options', async () => {
    patientApi.getDoctors(1)
    patientApi.getDoctors()
    patientApi.getDoctorDetail(2)
    patientApi.getDoctorSchedule(2)
    patientApi.wxLogin('wx-code')
    patientApi.testPatientLogin('patient01', '123456')
    patientApi.devPatientLogin()
    patientApi.getPatientInfo()
    patientApi.updatePatientInfo({ name: 'Alice' })
    patientApi.createRegistration({ doctorId: 1, registrationDate: '2026-07-04', timeSlot: '上午' })
    patientApi.getRegistrations({ page: 1, size: 10 })
    patientApi.getRegistrations({ status: '待就诊', page: 1, size: 10 })
    patientApi.getRegistrationDetail(1)
    patientApi.cancelRegistration(1)
    patientApi.getMedicalRecords({ page: 1, size: 10 })
    patientApi.getMedicalRecordDetail(1)
    patientApi.consultTriage({ chiefComplaint: 'headache' })
    patientApi.getTriageHistory({ page: 1, size: 10 })
    patientApi.askAiChat({ question: 'q', sessionId: 's' })
    patientApi.healthConsult({ question: 'q', includeHistory: true })
    patientApi.getAiConsultHistory({ page: 1, size: 10 })
    patientApi.feedbackAiAnswer(3, 'good')
    patientApi.getMedicationGuide(4)
    patientApi.getPatientPrescriptions({ page: 1, size: 10 })
    patientApi.generateMedicationGuide(4)
    patientApi.markMedicationGuidePrinted(5)
    patientApi.getPatientExaminations({ page: 1, size: 10 })
    patientApi.getExaminationDetail(1)
    patientApi.getPatientInterpretation(1)
    patientApi.getPatientCriticalWarnings({ page: 1, size: 10 })
    patientApi.getFollowUpPlans({ page: 1, size: 10 })
    patientApi.getPendingFollowUps({ page: 1, size: 10 })
    patientApi.submitFollowUp(6, { score: 1 })
    patientApi.getFollowUpDetail(6)
    expect(requestMock).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/patient/test-login', method: 'POST' }))
    expect(requestMock).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/ai/chat/feedback/3?feedback=good', method: 'POST' }))
  })

  it('uploads avatar and handles upload response branches', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    vi.mocked(uni.uploadFile).mockImplementationOnce((options: any) => {
      expect(options.header.Authorization).toBe('Bearer patient-token')
      options.success({ data: JSON.stringify({ code: 200, data: '/avatar.png' }) })
    })
    await expect(patientApi.uploadAvatar('/tmp/a.png')).resolves.toBe('/avatar.png')

    vi.mocked(uni.getStorageSync).mockReturnValue('')
    vi.mocked(uni.uploadFile).mockImplementationOnce((options: any) => {
      expect(options.header.Authorization).toBeUndefined()
      options.success({ data: JSON.stringify({ code: 500 }) })
    })
    await expect(patientApi.uploadAvatar('/tmp/a.png')).rejects.toThrow()

    vi.mocked(uni.uploadFile).mockImplementationOnce((options: any) => {
      options.success({ data: JSON.stringify({ code: 500, msg: 'bad' }) })
    })
    await expect(patientApi.uploadAvatar('/tmp/a.png')).rejects.toThrow('bad')

    vi.mocked(uni.uploadFile).mockImplementationOnce((options: any) => {
      options.success({ data: '{bad json' })
    })
    await expect(patientApi.uploadAvatar('/tmp/a.png')).rejects.toThrow()

    vi.mocked(uni.uploadFile).mockImplementationOnce((options: any) => options.fail())
    await expect(patientApi.uploadAvatar('/tmp/a.png')).rejects.toThrow()
  })

  it('updates auth, doctors, registrations and prescriptions stores', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    const auth = useAuthStore()
    await auth.loginWithTestAccount('patient01', '123456')
    expect(auth.patient?.name).toBe('Alice')
    expect(uni.setStorageSync).toHaveBeenCalledWith('patient_token', 'patient-token')

    vi.mocked(uni.login).mockImplementation((options: any) => options.success({ code: 'wx-code' }))
    await auth.login()
    expect(uni.setStorageSync).toHaveBeenCalledWith('patient_token', 'wx-token')

    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    await auth.loadProfile()
    expect(auth.patient?.name).toBe('Alice')

    requestMock.mockRejectedValueOnce(new Error('Token expired'))
    await expect(auth.loadProfile()).rejects.toThrow('Token expired')
    expect(uni.removeStorageSync).toHaveBeenCalledWith('patient_token')

    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    requestMock.mockRejectedValueOnce(new Error('offline'))
    await expect(auth.loadProfile()).rejects.toThrow('offline')

    vi.mocked(uni.getStorageSync).mockReturnValue('')
    await auth.loadProfile()

    vi.mocked(uni.login).mockImplementationOnce((options: any) => options.fail(new Error('wx fail')))
    await expect(auth.login()).rejects.toThrow('wx fail')
    expect(auth.loading).toBe(false)

    requestMock.mockRejectedValueOnce(new Error('bad login'))
    await expect(auth.loginWithTestAccount('bad', 'bad')).rejects.toThrow('bad login')
    expect(auth.loading).toBe(false)

    const doctors = useDoctorsStore()
    await doctors.initialize(1)
    await doctors.loadDepartments()
    expect(doctors.doctorCount).toBe(1)

    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    const registrations = useRegistrationsStore()
    await registrations.load()
    await registrations.changeStatus('待就诊')
    expect(registrations.pendingCount).toBe(1)

    const prescriptions = usePrescriptionsStore()
    await prescriptions.load(true)
    await prescriptions.loadMore()
    expect(prescriptions.dispensedCount).toBeGreaterThan(0)

    auth.logout()
    expect(auth.patient).toBeNull()
  })

  it('handles AI store streaming paths and reset', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('')
    sseMock.mockImplementationOnce(async (options: any) => {
      options.handlers.onDelta('hello')
      options.handlers.onDone(JSON.stringify({ relatedQuestions: ['next'], recommendDepartment: '内科', recommendDepartmentId: 1 }))
    })
    const store = usePatientAiStore()
    await store.ask(' fever ')
    expect(store.messages.at(-1)).toMatchObject({ content: 'hello', department: '内科', departmentId: 1 })

    sseMock.mockResolvedValueOnce(undefined)
    await store.ask('empty')
    expect(store.messages.at(-1)?.content).toContain('暂时没有得到明确答案')

    await store.ask('   ')
    store.resetConversation()
    expect(store.messages).toHaveLength(1)
  })

  it('covers patient store guard branches for registrations and prescriptions', async () => {
    const registrations = useRegistrationsStore()
    vi.mocked(uni.getStorageSync).mockReturnValue('')
    await registrations.load()
    expect(registrations.records).toEqual([])

    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    requestMock.mockResolvedValueOnce({})
    await registrations.load()
    expect(registrations.records).toEqual([])

    const prescriptions = usePrescriptionsStore()
    prescriptions.loading = true
    await prescriptions.load()
    await prescriptions.loadMore()
    expect(prescriptions.page).toBe(1)

    prescriptions.loading = false
    prescriptions.hasMore = false
    await prescriptions.loadMore()
    expect(prescriptions.page).toBe(1)

    requestMock.mockResolvedValueOnce({})
    await prescriptions.load(true)
    expect(prescriptions.records).toEqual([])
    expect(prescriptions.total).toBe(0)
    expect(prescriptions.hasMore).toBe(false)
  })

  it('covers logged-in AI streaming guards and error branches', async () => {
    vi.mocked(uni.getStorageSync).mockReturnValue('patient-token')
    let capturedOptions: any
    sseMock.mockImplementationOnce(async (options: any) => {
      capturedOptions = options
      options.handlers.onDelta('secure')
      options.handlers.onDone('{bad json')
    })
    const store = usePatientAiStore()
    await store.ask(' symptom ')
    expect(capturedOptions).toMatchObject({
      url: '/api/ai/health-consult/stream',
      auth: true,
      data: { question: 'symptom', includeHistory: true }
    })
    expect(store.messages.at(-1)?.content).toBe('secure')

    store.loading = true
    await store.ask('ignored by loading')
    store.loading = false
    store.streaming = true
    await store.ask('ignored by streaming')
    store.streaming = false

    sseMock.mockImplementationOnce(async (options: any) => {
      options.handlers.onError('')
    })
    await expect(store.ask('error path')).rejects.toThrow()
    expect(store.streaming).toBe(false)
  })
})
