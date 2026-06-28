import request from '@/utils/request'
import type { ApiResponse, Doctor, PageData } from '@/types'

export const getDoctorPage = (page = 1, size = 10) => request.get<ApiResponse<PageData<Doctor>>>('/api/doctor/page', { params: { page, size } })
export const addDoctor = (data: Doctor) => request.post<ApiResponse<string>>('/api/doctor/add', data)
export const resetDoctorPassword = (id: number) => request.put<ApiResponse<string>>(`/api/doctor/reset-pwd/${id}`)
export const deleteDoctor = (id: number) => request.delete<ApiResponse<string>>(`/api/doctor/delete/${id}`)
