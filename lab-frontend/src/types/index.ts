export interface ApiResponse<T>{code:number;msg?:string;message?:string;data:T}
export interface PageData<T>{content:T[];totalElements:number;totalPages:number;number:number;size:number}
export interface StaffInfo{id:number;username:string;name:string;role:string;phone?:string;createTime?:string}
export interface Examination{id:number;registrationId:number;patientId:number;patientName:string;doctorId:number;doctorName:string;departmentId:number;itemId:number;itemName:string;type:string;result?:string;resultImages?:string;status:string;createTime:string;completeTime?:string}
export interface ExaminationItem{id:number;name:string;type:string;price:number;description?:string}
