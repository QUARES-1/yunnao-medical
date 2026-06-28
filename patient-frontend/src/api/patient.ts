import { API_BASE_URL, request } from '@/utils/request'
import type { ApiResult, Department, Doctor, PageData, Patient, Registration } from '@/types/api'

export const getDepartments = () => request<Department[]>({ url: '/api/department/list', auth: false })
export const getDoctors = (departmentId?: number) => request<Doctor[]>({
  url: '/api/doctor/list', data: departmentId ? { departmentId } : undefined, auth: false
})
export const getDoctorDetail = (id: number) => request<Doctor>({ url: `/api/doctor/detail/${id}`, auth: false })
export interface ScheduleAvailability {
  date: string
  timeSlot: '上午' | '下午'
  capacity: number
  remaining: number
}
export const getDoctorSchedule = (id: number) => request<{
  dates: string[]
  timeSlots: ('上午' | '下午')[]
  availability: ScheduleAvailability[]
}>({
  url: `/api/doctor/${id}/schedule`, auth: false
})
export const wxLogin = (code: string) => request<{ token: string; patientId: number; name: string; phone?: string; needCompleteInfo: boolean }>({
  url: `/api/patient/wx-login?code=${encodeURIComponent(code)}`, method: 'POST', auth: false
})
export const devPatientLogin = () => request<{ token: string; patientId: number; name: string; phone?: string; needCompleteInfo: boolean }>({
  url: '/api/dev/patient-login', method: 'POST', auth: false
})
export const getPatientInfo = () => request<Patient>({ url: '/api/patient/info' })
export const updatePatientInfo = (data: Partial<Patient>) => request<string>({ url: '/api/patient/update', method: 'PUT', data })
export const uploadAvatar = (filePath: string) => new Promise<string>((resolve, reject) => {
  const token = uni.getStorageSync('patient_token') as string
  uni.uploadFile({
    url: `${API_BASE_URL}/api/file/upload`, filePath, name: 'file',
    header: token ? { Authorization: `Bearer ${token}` } : {},
    success: (response) => {
      try {
        const result = JSON.parse(response.data) as ApiResult<string>
        if (result.code === 200) return resolve(result.data)
        reject(new Error(result.msg || '头像上传失败'))
      } catch { reject(new Error('头像上传响应异常')) }
    },
    fail: () => reject(new Error('头像上传失败，请检查后端服务')),
  })
})
export const createRegistration = (data: { doctorId: number; registrationDate: string; timeSlot: string }) =>
  request<Registration>({ url: '/api/registration/create', method: 'POST', data })
export const getRegistrations = ({ status, page, size }: { status?: string; page: number; size: number }) =>
  request<PageData<Registration>>({
    url: '/api/registration/patient/list',
    data: status ? { status, page, size } : { page, size }
  })
export const getRegistrationDetail = (id: number) => request<Registration>({ url: `/api/registration/detail/${id}` })
export const cancelRegistration = (id: number) => request<string>({ url: `/api/registration/cancel/${id}`, method: 'PUT' })
