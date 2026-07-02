export interface ApiResponse<T> { code: number; msg: string; data: T }
export interface PageData<T> { content: T[]; totalElements: number; totalPages: number; size: number; number: number }
export interface AdminInfo { id: number; username: string; name: string; createTime?: string }
export interface OverviewStatistics { patientCount: number; doctorCount: number; departmentCount: number; registrationCount: number }
export interface Department { id?: number; name: string; description?: string; sort: number; createTime?: string; updateTime?: string }
export interface Doctor {
  id?: number; username: string; password?: string; name: string; phone?: string;
  departmentId?: number; departmentName?: string; title?: string; avatar?: string;
  introduction?: string; specialty?: string; createTime?: string
}
export interface MedicineCategory { id: number; name: string; sort?: number }
export interface Medicine {
  id?: number; name: string; categoryId?: number; categoryName?: string; specification?: string;
  unit?: string; price: number; stock: number; manufacturer?: string; description?: string; createTime?: string
}
export interface AiKnowledge {
  id?: number; category?: string; question: string; answer: string; keywords?: string;
  sort?: number; status?: number; createTime?: string; updateTime?: string
}
export interface AiOperationReport {
  id: number; reportType?: string; startDate?: string; endDate?: string; summary?: string;
  keyMetrics?: string; trendsAnalysis?: string; forecasts?: string; warnings?: string;
  suggestions?: string; rawResponse?: string; createTime?: string
}
export interface QualityCheckRecord {
  id: number; checkType?: string; checkDate?: string; totalCount?: number; passCount?: number;
  avgScore?: number; problemSummary?: string; improvementSuggestions?: string; checkerType?: string; createTime?: string
}
export interface AiChatLog {
  id: number; userType?: string; userId?: number; question?: string; answer?: string;
  source?: string; feedback?: string; sessionId?: string; category?: string; createTime?: string
}
export interface AiOverview { [key: string]: unknown }