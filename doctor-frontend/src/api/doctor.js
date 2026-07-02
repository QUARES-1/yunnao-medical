import request from '../utils/request'

// 登录
export const doctorLogin = (username, password) =>
  request.post('/api/doctor/login', { username, password })

// 注册
export const doctorRegister = (username, password, name) =>
  request.post('/api/doctor/register', { username, password, name })

// 获取当前医生信息
export const getDoctorInfo = () =>
  request.get('/api/doctor/info')

// 修改个人信息
export const updateDoctorInfo = (data) =>
  request.put('/api/doctor/update', data)

export const changeDoctorPassword = (data) =>
  request.put('/api/doctor/change-password', data)

export const uploadDoctorAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/file/upload', formData)
}

// 今日挂号患者列表
export const getTodayRegistrations = () =>
  request.get('/api/registration/doctor/today')

// 历史挂号列表
export const getHistoryRegistrations = (params = {}) =>
  request.get('/api/registration/doctor/list', { params })

// 挂号详情
export const getRegistrationDetail = (id) =>
  request.get(`/api/registration/detail/${id}`)

// 开始看诊
export const startConsultation = (id) =>
  request.put(`/api/registration/start/${id}`)

// 完成看诊
export const completeConsultation = (id) =>
  request.put(`/api/registration/complete/${id}`)

// 保存病历
export const saveMedicalRecord = (data) =>
  request.post('/api/medical-record/save', data)

// 根据挂号ID查询病历
export const getMedicalRecordByReg = (regId) =>
  request.get(`/api/medical-record/registration/${regId}`)

// 病历详情
export const getMedicalRecordDetail = (id) =>
  request.get(`/api/medical-record/detail/${id}`)

// 我写的病历列表
export const getDoctorMedicalRecords = (page = 1, size = 10) =>
  request.get('/api/medical-record/doctor/list', { params: { page, size } })

// 检查项目列表
export const getExaminationItems = (type) =>
  request.get('/api/examination/item/list', { params: { type } })

// 开立检查
export const createExamination = (data) =>
  request.post('/api/examination/create', data)

export const cancelExamination = (id) =>
  request.put(`/api/examination/cancel/${id}`)

// 我开的检查列表
export const getDoctorExaminations = (page = 1, size = 10) =>
  request.get('/api/examination/doctor/list', { params: { page, size } })

export const getExaminationsByRegistration = (regId) =>
  request.get(`/api/examination/registration/${regId}`)

// 检查详情
export const getExaminationDetail = (id) =>
  request.get(`/api/examination/detail/${id}`)

// 药品列表
export const getMedicineList = (params) =>
  request.get('/api/medicine/list', { params })

// 开具处方
export const createPrescription = (data) =>
  request.post('/api/prescription/create', data)

export const cancelPrescription = (id) =>
  request.put(`/api/prescription/cancel/${id}`)

// 我开的处方列表
export const getDoctorPrescriptions = (page = 1, size = 10) =>
  request.get('/api/prescription/doctor/list', { params: { page, size } })

export const getPrescriptionsByRegistration = (regId) =>
  request.get(`/api/prescription/registration/${regId}`)

// 处方详情
export const getPrescriptionDetail = (id) =>
  request.get(`/api/prescription/detail/${id}`)

// AI 生成病历
export const aiGenerateRecord = (data) =>
  request.post('/api/medical-record/ai/generate', data)

// AI 处方审核
export const aiCheckPrescription = (data) =>
  request.post('/api/prescription/ai/check', data)

// AI 流式生成病历 (SSE)
export const aiGenerateRecordStream = (data) => {
  const token = localStorage.getItem('token')
  return fetch('http://localhost:8080/api/medical-record/ai/generate-stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token
    },
    body: JSON.stringify(data)
  })
}
