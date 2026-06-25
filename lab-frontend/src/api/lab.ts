import request from'@/utils/request';import type{ApiResponse,Examination,ExaminationItem,PageData,StaffInfo}from'@/types';
export const login=(username:string,password:string)=>request.post<ApiResponse<string>>('/api/staff/login',{username,password,role:'lab'});
export const getInfo=()=>request.get<ApiResponse<StaffInfo>>('/api/staff/info');
export const changePassword=(oldPassword:string,newPassword:string)=>request.put<ApiResponse<string>>('/api/staff/change-password',{oldPassword,newPassword});
export const getExaminations=(status:string,page=1,size=10)=>request.get<ApiResponse<PageData<Examination>>>('/api/examination/lab/list',{params:{status,page,size}});
export const getExamination=(id:number)=>request.get<ApiResponse<Examination>>(`/api/examination/detail/${id}`);
export const updateResult=(data:{id:number;result:string;resultImages?:string})=>request.put<ApiResponse<string>>('/api/examination/update-result',data);
export const getItems=(type?:string)=>request.get<ApiResponse<ExaminationItem[]>>('/api/examination/item/list',{params:{type}});
export const uploadFile=(file:File)=>{const form=new FormData();form.append('file',file);return request.post<ApiResponse<string>>('/api/file/upload',form,{headers:{'Content-Type':'multipart/form-data'}})};
