/**
 * Pinia Store 统一入口
 * 从此处集中导出所有 store，避免组件中路径混乱
 */
export { useDoctorStore } from './doctorStore'
export { usePatientStore } from './patientStore'
export { useRegistrationStore } from './registrationStore'
export { usePrescriptionStore } from './prescriptionStore'
