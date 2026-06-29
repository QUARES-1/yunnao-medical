import { API_BASE_URL, request } from '@/utils/request'
import type { ApiResult, Department, Doctor, PageData, Patient, Registration, MedicalRecord, TriageResult, TriageRecord, AiChatResult, AiChatRecord, MedicationGuide, Prescription, Examination, ExaminationInterpretation, CriticalValueWarning, FollowUpPlan, FollowUpRecord } from '@/types/api'

export const getDepartments = () => request<Department[]>({ url: '/api/department/list', auth: false })
export const getDoctors = (departmentId?: number) => request<Doctor[]>({ url: '/api/doctor/list', data: departmentId ? { departmentId } : undefined, auth: false })
export const getDoctorDetail = (id: number) => request<Doctor>({ url: `/api/doctor/detail/${id}`, auth: false })
export interface ScheduleAvailability { date: string; timeSlot: '上午' | '下午'; capacity: number; remaining: number }
export const getDoctorSchedule = (id: number) => request<{ dates: string[]; timeSlots: ('上午' | '下午')[]; availability: ScheduleAvailability[] }>({ url: `/api/doctor/${id}/schedule`, auth: false })
export const wxLogin = (code: string) => request<{ token: string; patientId: number; name: string; phone?: string; needCompleteInfo: boolean }>({ url: `/api/patient/wx-login?code=${encodeURIComponent(code)}`, method: 'POST', auth: false })
export const testPatientLogin = (account: string, password: string) => request<{ token: string; patientId: number; name: string; phone?: string; needCompleteInfo: boolean }>({ url: '/api/patient/test-login', method: 'POST', data: { account, password }, auth: false })
export const devPatientLogin = () => request<{ token: string; patientId: number; name: string; phone?: string; needCompleteInfo: boolean }>({ url: '/api/dev/patient-login', method: 'POST', auth: false })
export const getPatientInfo = () => request<Patient>({ url: '/api/patient/info' })
export const updatePatientInfo = (data: Partial<Patient>) => request<string>({ url: '/api/patient/update', method: 'PUT', data })
export const uploadAvatar = (filePath: string) => new Promise<string>((resolve, reject) => {
  const token = uni.getStorageSync('patient_token') as string
  uni.uploadFile({
    url: `${API_BASE_URL}/api/file/upload`, filePath, name: 'file',
    header: token ? { Authorization: `Bearer ${token}` } : {},
    success: (response) => {
      try { const result = JSON.parse(response.data) as ApiResult<string>; if (result.code === 200) return resolve(result.data); reject(new Error(result.msg || '头像上传失败')) }
      catch { reject(new Error('头像上传响应异常')) }
    },
    fail: () => reject(new Error('头像上传失败，请检查后端服务')),
  })
})
export const createRegistration = (data: { doctorId: number; registrationDate: string; timeSlot: string }) => request<Registration>({ url: '/api/registration/create', method: 'POST', data })
export const getRegistrations = ({ status, page, size }: { status?: string; page: number; size: number }) => request<PageData<Registration>>({ url: '/api/registration/patient/list', data: status ? { status, page, size } : { page, size } })
export const getRegistrationDetail = (id: number) => request<Registration>({ url: `/api/registration/detail/${id}` })
export const cancelRegistration = (id: number) => request<string>({ url: `/api/registration/cancel/${id}`, method: 'PUT' })
export const getMedicalRecords = ({ page, size }: { page: number; size: number }) => request<PageData<MedicalRecord>>({ url: '/api/medical-record/patient/list', data: { page, size } })
export const getMedicalRecordDetail = (id: number) => request<MedicalRecord>({ url: `/api/medical-record/detail/${id}` })

export const consultTriage = (data: { chiefComplaint: string; patientId?: number }) => request<TriageResult>({ url: '/api/triage/consult', method: 'POST', data, auth: false })
export const getTriageHistory = ({ page, size }: { page: number; size: number }) => request<PageData<TriageRecord>>({ url: '/api/triage/patient/list', data: { page, size } })
export const askAiChat = (data: { question: string; sessionId: string }) => request<AiChatResult>({ url: '/api/ai/chat', method: 'POST', data, auth: false })
export const healthConsult = (data: { question: string; includeHistory?: boolean }) => request<AiChatResult>({ url: '/api/ai/health-consult', method: 'POST', data })
export const getAiConsultHistory = ({ page, size }: { page: number; size: number }) => request<PageData<AiChatRecord>>({ url: '/api/ai/consult/history', data: { page, size } })
export const feedbackAiAnswer = (id: number, feedback: 'good' | 'bad') => request<string>({ url: `/api/ai/chat/feedback/${id}?feedback=${feedback}`, method: 'POST' })

export const getMedicationGuide = (prescriptionId: number) => request<MedicationGuide>({ url: `/api/medication/guide/${prescriptionId}`, auth: false })
export const getPatientPrescriptions = ({ page, size }: { page: number; size: number }) => request<PageData<Prescription>>({ url: '/api/prescription/patient/list', data: { page, size } })
export const generateMedicationGuide = (prescriptionId: number) => request<MedicationGuide>({ url: '/api/medication/guide/generate', method: 'POST', data: { prescriptionId } })
export const markMedicationGuidePrinted = (id: number) => request<string>({ url: `/api/medication/guide/print/${id}`, method: 'POST' })
export const getPatientExaminations = ({ page, size }: { page: number; size: number }) => request<PageData<Examination>>({ url: '/api/examination/patient/list', data: { page, size } })
export const getExaminationDetail = (id: number) => request<Examination>({ url: `/api/examination/detail/${id}` })
export const getPatientInterpretation = (examinationId: number) => request<ExaminationInterpretation>({ url: `/api/examination/ai/interpret-patient/${examinationId}`, auth: false })
export const getPatientCriticalWarnings = ({ page, size }: { page: number; size: number }) =>
  request<PageData<CriticalValueWarning>>({ url: '/api/examination/ai/critical-list', data: { page, size } })
export const getFollowUpPlans = ({ page, size }: { page: number; size: number }) => request<PageData<FollowUpPlan>>({ url: '/api/follow-up/patient/plans', data: { page, size } })
export const getPendingFollowUps = ({ page, size }: { page: number; size: number }) => request<PageData<FollowUpRecord>>({ url: '/api/follow-up/pending', data: { page, size } })
export const submitFollowUp = (id: number, answers: Record<string, unknown>) => request<string>({ url: `/api/follow-up/submit/${id}`, method: 'POST', data: { answers: JSON.stringify(answers) } })
export const getFollowUpDetail = (id: number) => request<Record<string, any>>({ url: `/api/follow-up/detail/${id}` })
