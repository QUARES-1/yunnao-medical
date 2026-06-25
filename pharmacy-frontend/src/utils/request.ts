import axios from'axios';import{ElMessage}from'element-plus';
const request=axios.create({timeout:12000});
request.interceptors.request.use(config=>{const token=localStorage.getItem('pharmacy_token');if(token){config.headers.token=token;config.headers.Authorization=`Bearer ${token}`}return config});
request.interceptors.response.use(response=>{const body=response.data;if(body&&body.code!==200){ElMessage.error(body.msg||body.message||'操作失败');return Promise.reject(new Error(body.msg||body.message))}return response},error=>{if(error.response?.status===401||error.response?.data?.code===401){localStorage.removeItem('pharmacy_token');if(location.pathname!='/login')location.href='/login'}else ElMessage.error(error.response?.data?.msg||error.message||'网络连接失败');return Promise.reject(error)});
export default request
