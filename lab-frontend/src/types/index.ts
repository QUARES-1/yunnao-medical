export interface ApiResponse<T>{code:number;msg?:string;message?:string;data:T}
export interface PageData<T>{content:T[];totalElements:number;totalPages:number;number:number;size:number}
export interface StaffInfo{id:number;username:string;name:string;role:string;phone?:string;createTime?:string}
export interface Examination{id:number;registrationId:number;patientId:number;patientName:string;doctorId:number;doctorName:string;departmentId:number;itemId:number;itemName:string;type:string;result?:string;resultImages?:string;status:string;createTime:string;completeTime?:string}
export interface ExaminationItem{id:number;name:string;type:string;price:number;description?:string}
export interface ExaminationAiReview{id:number;examinationId:number;patientId:number;labStaffId?:number;reviewResult:string;reviewScore:number;abnormalItems?:string;logicIssues?:string;historyCompare?:string;warnings?:string;suggestions?:string;rawResponse?:string;reviewTime:string;itemName?:string;patientName?:string;doctorName?:string;result?:string;examinationStatus?:string}
export interface ReviewStats{total:number;passCount:number;manualCount:number;rejectCount:number;passRate:number}

