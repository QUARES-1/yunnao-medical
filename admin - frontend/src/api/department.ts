import request from '@/utils/request'
import type { ApiResponse, Department } from '@/types'

export const getDepartments = () => request.get<ApiResponse<Department[]>>('/api/department/list')
export const addDepartment = (data: Department) => request.post<ApiResponse<string>>('/api/department/add', data)
export const updateDepartment = (data: Department) => request.put<ApiResponse<string>>('/api/department/update', data)
export const deleteDepartment = (id: number) => request.delete<ApiResponse<string>>(`/api/department/delete/${id}`)
