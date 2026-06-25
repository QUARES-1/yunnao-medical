import request from'@/utils/request';import type{ApiResponse,Medicine,PageData,Prescription,StaffInfo}from'@/types';
export const login=(username:string,password:string)=>request.post<ApiResponse<string>>('/api/staff/login',{username,password,role:'pharmacy'});
export const getInfo=()=>request.get<ApiResponse<StaffInfo>>('/api/staff/info');
export const changePassword=(oldPassword:string,newPassword:string)=>request.put<ApiResponse<string>>('/api/staff/change-password',{oldPassword,newPassword});
export const getPrescriptions=(status:string,page=1,size=10)=>request.get<ApiResponse<PageData<Prescription>>>('/api/prescription/pharmacy/list',{params:{status,page,size}});
export const getPrescription=(id:number)=>request.get<ApiResponse<Prescription>>(`/api/prescription/detail/${id}`);
export const dispense=(id:number)=>request.put<ApiResponse<string>>(`/api/prescription/dispense/${id}`);
export const getMedicines=(params:Record<string,unknown>)=>request.get<ApiResponse<PageData<Medicine>>>('/api/medicine/list',{params});
export const adjustStock=(id:number,quantity:number)=>request.put<ApiResponse<string>>(`/api/medicine/stock/${id}`,null,{params:{quantity}});
