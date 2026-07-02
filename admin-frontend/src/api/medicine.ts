import request from '@/utils/request'
import type { ApiResponse, Medicine, MedicineCategory, PageData } from '@/types'

export const getMedicinePage = (params: Record<string, unknown>) => request.get<ApiResponse<PageData<Medicine>>>('/api/medicine/list', { params })
export const getMedicineCategories = () => request.get<ApiResponse<MedicineCategory[]>>('/api/medicine/category/list')
export const addMedicine = (data: Medicine) => request.post<ApiResponse<string>>('/api/medicine/add', data)
export const updateMedicine = (data: Medicine) => request.put<ApiResponse<string>>('/api/medicine/update', data)
export const deleteMedicine = (id: number) => request.delete<ApiResponse<string>>(`/api/medicine/delete/${id}`)
export const adjustMedicineStock = (id: number, quantity: number) => request.put<ApiResponse<string>>(`/api/medicine/stock/${id}`, null, { params: { quantity } })
