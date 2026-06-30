import request from '@/utils/request'
import type { AdminInfo, ApiResponse, OverviewStatistics } from '@/types'

export const loginAdmin = (username: string, password: string) =>
  request.post<ApiResponse<string>>('/api/admin/login', null, { params: { username, password } })

export const registerAdmin = (username: string, password: string, name?: string) =>
  request.post<ApiResponse<string>>('/api/admin/register', null, { params: { username, password, name } })

export const getAdminInfo = () => request.get<ApiResponse<AdminInfo>>('/api/admin/info')
export const getOverview = () => request.get<ApiResponse<OverviewStatistics>>('/api/admin/statistics/overview')
export const changeAdminPassword = (oldPassword: string, newPassword: string) =>
  request.put<ApiResponse<string>>('/api/admin/change-pwd', null, { params: { oldPassword, newPassword } })