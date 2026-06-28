export interface ApiResponse<T>{code:number;msg?:string;message?:string;data:T}
export interface PageData<T>{content:T[];totalElements:number;totalPages:number;number:number;size:number}
export interface StaffInfo{id:number;username:string;name:string;role:string;phone?:string;createTime?:string}
export interface Drug{medicineId:number;medicineName:string;specification:string;quantity:number;unit:string;dosage:string}
export interface Prescription{id:number;registrationId:number;patientId:number;patientName:string;doctorId:number;doctorName:string;departmentId:number;drugs:string;drugList?:Drug[];totalAmount:number;status:string;createTime:string;dispenseTime?:string}
export interface Medicine{id:number;name:string;categoryId?:number;categoryName?:string;specification:string;unit:string;price:number;stock:number;manufacturer?:string;description?:string}
