import request from '@/utils/request'
import type { AiChatLog, AiKnowledge, AiOperationReport, AiOverview, ApiResponse, PageData, QualityCheckRecord } from '@/types'

export const getAiOverview = () => request.get<ApiResponse<AiOverview>>('/api/admin/ai/operation-overview')

export const generateAiReport = (data: { reportType: string; startDate?: string; endDate?: string }) =>
  request.post<ApiResponse<Record<string, unknown>>>('/api/admin/ai/operation-report/generate', data)

export const getAiReportList = (params: { reportType?: string; page?: number; size?: number }) =>
  request.get<ApiResponse<PageData<AiOperationReport>>>('/api/admin/ai/operation-report/list', { params })

export const startQualityCheck = (data: { checkType: string; sampleSize: number }) =>
  request.post<ApiResponse<Record<string, unknown>>>('/api/admin/ai/quality-check/start', data)

export const getQualityCheckList = (params: { page?: number; size?: number }) =>
  request.get<ApiResponse<PageData<QualityCheckRecord>>>('/api/admin/ai/quality-check/list', { params })

export const getDoctorQualityStats = () => request.get<ApiResponse<Record<string, unknown>>>('/api/admin/ai/quality-check/doctor-stats')

export const getAiChatLogs = (params: { page?: number; size?: number }) =>
  request.get<ApiResponse<PageData<AiChatLog>>>('/api/admin/ai/chat-log', { params })

export const getKnowledgeList = (params: { category?: string; keyword?: string; page?: number; size?: number }) =>
  request.get<ApiResponse<PageData<AiKnowledge>>>('/api/admin/ai/knowledge/list', { params })

export const addKnowledge = (data: AiKnowledge) => request.post<ApiResponse<string>>('/api/admin/ai/knowledge/add', data)
export const updateKnowledge = (data: AiKnowledge) => request.put<ApiResponse<string>>('/api/admin/ai/knowledge/update', data)
export const deleteKnowledge = (id: number) => request.delete<ApiResponse<string>>(`/api/admin/ai/knowledge/delete/${id}`)