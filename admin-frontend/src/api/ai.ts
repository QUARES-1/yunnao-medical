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
export async function streamAiOperationReport(
  data: { reportType: string; startDate?: string; endDate?: string },
  onDelta: (chunk: string) => void,
  onDone?: (reportId?: string) => void
) {
  const token = localStorage.getItem('admin_token') || ''
  const response = await fetch('/api/admin/ai/operation-report/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', token },
    body: JSON.stringify(data)
  })
  if (!response.ok || !response.body) throw new Error(`流式接口请求失败（${response.status}）`)
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''
    for (const block of blocks) {
      const event = block.split('\n').find(line => line.startsWith('event:'))?.replace('event:', '').trim()
      const payload = block.split('\n').filter(line => line.startsWith('data:')).map(line => line.replace(/^data:\s?/, '')).join('\n')
      if (event === 'done') onDone?.(payload)
      else if (event === 'error') throw new Error(payload || '流式生成失败')
      else if (payload) onDelta(payload)
    }
  }
}
