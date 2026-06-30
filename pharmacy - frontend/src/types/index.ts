export interface ApiResponse<T>{code:number;msg?:string;message?:string;data:T}
export interface PageData<T>{content:T[];totalElements:number;totalPages:number;number:number;size:number}
export interface StaffInfo{id:number;username:string;name:string;role:string;phone?:string;createTime?:string}
export interface Drug{medicineId:number;medicineName:string;specification:string;quantity:number;unit:string;dosage:string}
export interface Prescription{id:number;registrationId:number;patientId:number;patientName:string;doctorId:number;doctorName:string;departmentId:number;drugs:string;drugList?:Drug[];totalAmount:number;status:string;createTime:string;dispenseTime?:string}
export interface Medicine{id:number;name:string;categoryId?:number;categoryName?:string;specification:string;unit:string;price:number;stock:number;manufacturer?:string;description?:string}
export interface ForecastMedicine{medicineId:number;name:string;specification?:string;categoryName?:string;currentStock:number;history30:number;history90:number;dailyAverage:number;trendRate:number;seasonFactor:number;visitFactor:number;forecastConsume:number;safetyStock:number;suggestPurchase:number;stockCoverageDays:number;riskLevel:'高风险'|'需补货'|'库存合理'|'可能积压';unit:string;reason:string}
export interface StockForecast{id:number;forecastType:string;forecastPeriod:string;forecastData:string;purchaseSuggestions:string;totalForecastAmount:number;totalPurchaseAmount:number;factors:string;createTime:string}
