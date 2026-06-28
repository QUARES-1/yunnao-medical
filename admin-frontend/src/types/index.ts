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
