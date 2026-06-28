import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('doctorInfo')
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    } else {
      ElMessage.error(res.msg || '操作失败')
      return Promise.reject(new Error(res.msg))
    }
  },
  error => {
    ElMessage.error('网络错误，请检查后端是否启动')
    return Promise.reject(error)
  }
)

export default request
