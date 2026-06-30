import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '', timeout: 12000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.token = token
  return config
})

request.interceptors.response.use(
  response => {
    const result = response.data
    if (result.code === 200) return response
    if (result.code === 401 || result.code === 403) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_info')
      ElMessage.error(result.msg || '登录状态已失效')
      if (location.pathname !== '/login') location.href = '/login'
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
    return Promise.reject(new Error(result.msg || '请求失败'))
  },
  (error: AxiosError) => {
    const status = error.response?.status
    ElMessage.error(status ? `服务请求失败（${status}）` : '无法连接后端服务，请确认后端已启动')
    return Promise.reject(error)
  }
)

export default request
