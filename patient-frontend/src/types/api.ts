export interface ApiResult<T> { code: number; msg: string; data: T }
export interface PageData<T> {
  content: T[]; totalElements: number; totalPages: number;
  size: number; number: number; numberOfElements: number;
}
export interface Department { id: number; name: string; description?: string; sort?: number }
export interface Doctor {
  id: number; name: string; departmentId?: number; departmentName?: string;
  title?: string; avatar?: string; introduction?: string; specialty?: string;
}
export interface Patient {
  id: number; name: string; phone?: string; gender?: string; age?: number;
  idCard?: string; avatar?: string; address?: string;
}
export type RegistrationStatus = '待就诊' | '就诊中' | '已就诊' | '已取消'
export interface Registration {
  id: number; patientId: number; patientName: string; doctorId: number;
  doctorName: string; departmentId: number; departmentName: string;
  registrationDate: string; timeSlot: '上午' | '下午';
  status: RegistrationStatus; createTime: string;
}
