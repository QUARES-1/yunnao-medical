<template><div class="login-page"><section class="visual-panel"><div class="visual-overlay"></div><div class="visual-content"><div class="brand"><span class="brand-icon"><el-icon><FirstAidKit /></el-icon></span><div><strong>云脑诊疗</strong><small>NEUSOFT SMART CARE</small></div></div><div class="welcome"><span>智慧医疗 · 高效协同</span><h1>让每一次诊疗，<br>都更专业从容</h1><p>连接患者、医生与诊疗资源，打造安全、高效、连续的医疗服务体验。</p></div><div class="feature-list"><div><el-icon><CircleCheckFilled /></el-icon><span><strong>智能工作台</strong><small>今日患者与诊疗进度清晰呈现</small></span></div><div><el-icon><CircleCheckFilled /></el-icon><span><strong>一体化看诊</strong><small>病历、检查、处方全流程协作</small></span></div><div><el-icon><CircleCheckFilled /></el-icon><span><strong>医疗数据安全</strong><small>JWT 身份认证与权限保护</small></span></div></div></div><div class="visual-footer">© 2026 东软智慧云脑诊疗平台</div></section><section class="form-panel"><div class="form-wrap"><div class="mobile-brand"><el-icon><FirstAidKit /></el-icon> 云脑诊疗</div><div class="form-title"><span>DOCTOR PORTAL</span><h2>{{activeTab==='login'?'欢迎回到医生工作站':'创建医生工作站账号'}}</h2><p>{{activeTab==='login'?'请输入账号信息以继续今日诊疗工作':'完成注册后即可进入工作台'}}</p></div><el-tabs v-model="activeTab" class="login-tabs" stretch><el-tab-pane label="账号登录" name="login"><el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form" label-position="top"><el-form-item label="登录账号" prop="username"><el-input v-model="loginForm.username" placeholder="请输入医生账号" size="large" :prefix-icon="User" clearable /></el-form-item><el-form-item label="登录密码" prop="password"><el-input v-model="loginForm.password" type="password" placeholder="请输入登录密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" /></el-form-item><div class="form-meta"><el-checkbox v-model="remember">记住账号</el-checkbox><span>账号问题请联系管理员</span></div><el-button class="submit-button" type="primary" size="large" :loading="loading" @click="handleLogin">进入医生工作站 <el-icon><ArrowRight /></el-icon></el-button><div class="demo-account" @click="fillDemo"><el-icon><InfoFilled /></el-icon><span>演示账号：doctor01 / 123456</span><b>一键填入</b></div></el-form></el-tab-pane><el-tab-pane label="自助注册" name="register"><el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="login-form" label-position="top"><el-form-item label="登录账号" prop="username"><el-input v-model="registerForm.username" placeholder="请设置至少3位账号" size="large" :prefix-icon="User" /></el-form-item><el-form-item label="登录密码" prop="password"><el-input v-model="registerForm.password" type="password" placeholder="请设置至少6位密码" size="large" :prefix-icon="Lock" show-password /></el-form-item><el-form-item label="医生姓名"><el-input v-model="registerForm.name" placeholder="请输入真实姓名（可选）" size="large" :prefix-icon="Avatar" /></el-form-item><el-button class="submit-button" type="primary" size="large" :loading="loading" @click="handleRegister">注册并进入工作站 <el-icon><ArrowRight /></el-icon></el-button></el-form></el-tab-pane></el-tabs><div class="security-tip"><el-icon><Lock /></el-icon> 登录即代表您同意遵守医疗数据安全规范</div></div></section></div></template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { doctorLogin, doctorRegister } from '../../api/doctor'
import { useDoctorStore, useRegistrationStore } from '../../stores'

const router = useRouter()
const doctorStore = useDoctorStore()
const regStore = useRegistrationStore()

const activeTab = ref('login'), loading = ref(false), remember = ref(true)
const loginFormRef = ref(), registerFormRef = ref()
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', name: '' })

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 3, message: '账号至少3位', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }]
}

const fillDemo = () => Object.assign(loginForm, { username: 'doctor01', password: '123456' })

onMounted(() => {
  const saved = localStorage.getItem('doctor_username')
  if (saved) loginForm.username = saved
})

const afterLoginSuccess = async () => {
  // 登录成功后预加载 store 数据，减少工作台白屏时间
  await doctorStore.fetchDoctorInfo()
  regStore.loadTodayRegistrations()
}

const handleLogin = async () => {
  if (!await loginFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    const r = await doctorLogin(loginForm.username, loginForm.password)
    localStorage.setItem('token', r.data)
    if (remember.value) localStorage.setItem('doctor_username', loginForm.username)
    else localStorage.removeItem('doctor_username')
    ElMessage.success('登录成功，欢迎回来')
    await afterLoginSuccess()
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!await registerFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    const r = await doctorRegister(registerForm.username, registerForm.password, registerForm.name)
    localStorage.setItem('token', r.data)
    ElMessage.success('注册成功，即将进入工作台')
    await afterLoginSuccess()
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>.login-page{min-height:100vh;display:grid;grid-template-columns:minmax(520px,1.08fr) minmax(480px,.92fr);background:#f7fafb}.visual-panel{position:relative;overflow:hidden;min-height:100vh;padding:48px 58px;background:linear-gradient(145deg,#073b5c,#087f79 68%,#22a58e);color:#fff}.visual-panel:before,.visual-panel:after{content:'';position:absolute;border:1px solid rgba(255,255,255,.11);border-radius:50%}.visual-panel:before{width:520px;height:520px;right:-210px;top:-130px;box-shadow:0 0 0 90px rgba(255,255,255,.025)}.visual-panel:after{width:230px;height:230px;left:-120px;bottom:30px}.visual-overlay{position:absolute;inset:0;background:radial-gradient(circle at 70% 48%,rgba(59,219,187,.17),transparent 32%)}.visual-content{position:relative;z-index:2;display:flex;flex-direction:column;height:calc(100vh - 110px)}.brand{display:flex;align-items:center;gap:13px}.brand-icon{width:47px;height:47px;display:flex;align-items:center;justify-content:center;border-radius:15px;background:linear-gradient(145deg,#4bd8c1,#effffb);color:#08656b;font-size:23px}.brand strong,.brand small{display:block}.brand strong{font-size:21px;letter-spacing:1px}.brand small{margin-top:3px;color:rgba(255,255,255,.55);font-size:8px;letter-spacing:1.7px}.welcome{margin:auto 0 35px;max-width:620px}.welcome>span{display:inline-block;padding:7px 12px;border:1px solid rgba(255,255,255,.16);border-radius:20px;background:rgba(255,255,255,.08);font-size:11px;letter-spacing:1.5px}.welcome h1{margin:22px 0 18px;font-size:46px;line-height:1.25;letter-spacing:1px}.welcome p{max-width:540px;color:rgba(255,255,255,.68);font-size:14px;line-height:1.8}.feature-list{display:grid;grid-template-columns:repeat(3,1fr);gap:13px}.feature-list>div{display:flex;align-items:flex-start;gap:9px;padding:15px 13px;border:1px solid rgba(255,255,255,.11);border-radius:15px;background:rgba(255,255,255,.07);backdrop-filter:blur(6px)}.feature-list .el-icon{margin-top:2px;color:#67e3c9}.feature-list strong,.feature-list small{display:block}.feature-list strong{font-size:11px}.feature-list small{margin-top:4px;color:rgba(255,255,255,.48);font-size:9px;line-height:1.45}.visual-footer{position:absolute;z-index:2;left:58px;bottom:27px;color:rgba(255,255,255,.33);font-size:9px}.form-panel{display:flex;align-items:center;justify-content:center;padding:45px}.form-wrap{width:100%;max-width:430px}.mobile-brand{display:none}.form-title>span{color:#138f82;font-size:10px;font-weight:750;letter-spacing:2.3px}.form-title h2{margin:9px 0 8px;color:#17384a;font-size:26px}.form-title p{color:#91a1a7;font-size:12px}.login-tabs{margin-top:28px}.login-tabs :deep(.el-tabs__header){margin-bottom:24px}.login-tabs :deep(.el-tabs__nav-wrap::after){height:1px;background:#e7edef}.login-tabs :deep(.el-tabs__item){height:45px;color:#8b9ca2;font-size:13px}.login-tabs :deep(.el-tabs__item.is-active){color:#087f84;font-weight:700}.login-tabs :deep(.el-tabs__active-bar){height:3px;border-radius:3px;background:#16a394}.login-form :deep(.el-form-item){margin-bottom:19px}.login-form :deep(.el-form-item__label){padding-bottom:8px;color:#4c6872;font-size:12px;font-weight:650}.login-form :deep(.el-input__wrapper){min-height:46px;border-radius:12px;box-shadow:0 0 0 1px #dfe9eb inset}.login-form :deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 1px #15a092 inset,0 7px 20px rgba(8,127,132,.09)}.form-meta{display:flex;justify-content:space-between;margin:-2px 0 19px;color:#98a7ac;font-size:10px}.form-meta :deep(.el-checkbox__label){font-size:11px}.submit-button{width:100%;height:47px;border:0;border-radius:12px;background:linear-gradient(135deg,#087f84,#16a393);box-shadow:0 9px 22px rgba(8,127,132,.2);font-weight:700;letter-spacing:.5px}.submit-button .el-icon{margin-left:5px}.demo-account{display:flex;align-items:center;gap:7px;margin-top:15px;padding:12px 13px;border:1px solid #dcebea;border-radius:11px;background:#f0f8f6;color:#5d7c81;font-size:10px;cursor:pointer}.demo-account .el-icon{color:#169383}.demo-account b{margin-left:auto;color:#087f84}.security-tip{display:flex;align-items:center;justify-content:center;gap:6px;margin-top:25px;color:#a5b1b5;font-size:9px}@media(max-width:1000px){.login-page{grid-template-columns:1fr}.visual-panel{display:none}.form-panel{min-height:100vh}.mobile-brand{display:flex;align-items:center;gap:8px;margin-bottom:30px;color:#087f84;font-size:18px;font-weight:750}}@media(max-width:520px){.form-panel{padding:28px}.form-title h2{font-size:23px}}</style>
