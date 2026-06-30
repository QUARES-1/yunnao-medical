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
export interface MedicalRecord {
  id: number; registrationId?: number; patientId?: number; patientName?: string;
  doctorId?: number; doctorName?: string; departmentId?: number;
  chiefComplaint?: string; presentIllness?: string; pastHistory?: string;
  physicalExamination?: string; diagnosis?: string; treatment?: string;
  createTime?: string; updateTime?: string;
}

export interface TriageDoctor { id: number; name: string; title?: string; specialty?: string }
export interface TriageResult {
  id: number; recommendDepartment: string; recommendDepartmentId?: number;
  recommendDoctors?: TriageDoctor[]; analysis?: string; confidence?: number; createTime?: string;
}
export interface TriageRecord {
  id: number; chiefComplaint: string; recommendDepartment?: string; recommendDepartmentId?: number;
  aiAnalysis?: string; confidence?: number; status?: string; createTime?: string;
}
export interface AiChatResult {
  id?: number; answer: string; source?: string; relatedQuestions?: string[];
  recommendDepartment?: string; recommendDepartmentId?: number;
}
export interface AiChatRecord {
  id: number; question: string; answer: string; source?: string; category?: string; feedback?: string; createTime?: string;
}
export interface MedicationItem {
  name: string; specification: string; quantity: number; unit: string; usage: string;
  takingTime: string; dietRestrictions: string; adverseReactions: string;
  precautions: string; missedDose: string;
}
export interface PrescriptionDrug {
  id?: number; name: string; specification?: string; quantity?: number;
  unit?: string; price?: number; usage?: string;
}
export interface Prescription {
  id: number; patientId?: number; patientName?: string; doctorId?: number; doctorName?: string;
  departmentId?: number; drugs?: string; totalAmount?: number;
  status?: string; createTime?: string; dispenseTime?: string;
}
export interface Examination {
  id: number; registrationId?: number; patientId?: number; patientName?: string;
  doctorId?: number; doctorName?: string; departmentId?: number;
  itemId?: number; itemName?: string; type?: string; result?: string;
  resultImages?: string; status?: string; createTime?: string; completeTime?: string;
}
export interface MedicationGuide {
  id: number; prescriptionId: number; patientId?: number; patientName?: string;
  patientAge?: number; patientGender?: string; allergyHistory?: string; diagnosis?: string;
  doctorName?: string; prescriptionStatus?: string; medications: MedicationItem[];
  generalAdvice?: string[]; followUpAdvice?: string; guideContent?: string;
  printCount?: number; createTime?: string; aiGenerated?: boolean; source?: string;
}
export interface ExaminationInterpretation {
  id?: number; examinationId?: number; patientId?: number; abnormalItems?: string;
  interpretationPro?: string; interpretationPatient?: string; suggestions?: string; reviewReminder?: string;
  createTime?: string; rawResponse?: string;
}
export interface CriticalValueWarning {
  id: number; examinationId: number; patientId?: number; patientName?: string;
  doctorId?: number; doctorName?: string; criticalItems?: string;
  warningLevel?: string; status?: 'pending' | 'confirmed' | 'processed';
  doctorConfirmTime?: string; doctorRemark?: string; labRemark?: string;
  createTime?: string;
}
export interface FollowUpPlan { id: number; disease?: string; planType?: string; totalTimes?: number; completedTimes?: number; status?: string; createTime?: string }
export interface FollowUpRecord { id: number; planId?: number; followUpTime?: string; questionnaireJson?: string; answerJson?: string; aiAnalysis?: string; status?: string; abnormalFlag?: number; doctorRemark?: string; createTime?: string }
