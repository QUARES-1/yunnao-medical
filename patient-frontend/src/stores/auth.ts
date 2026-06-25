import { defineStore } from 'pinia'
import { ref } from 'vue'
import { devPatientLogin, getPatientInfo, wxLogin } from '@/api/patient'
import type { Patient } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const patient = ref<Patient | null>(null)
  const loading = ref(false)
  const isLoggedIn = () => Boolean(uni.getStorageSync('patient_token'))

  async function login() {
    loading.value = true
    try {
      // 本地联调阶段保持测试登录；配置真实 AppSecret 后再关闭。
      const useDevLogin = false
      const result = useDevLogin
        ? await devPatientLogin()
        : await new Promise<string>((resolve, reject) => {
            uni.login({ provider: 'weixin', success: r => resolve(r.code), fail: reject })
          }).then(wxLogin)
      uni.setStorageSync('patient_token', result.token)
      patient.value = await getPatientInfo()
      return result
    } finally { loading.value = false }
  }

  async function loadProfile() {
    if (!isLoggedIn()) return
    try {
      patient.value = await getPatientInfo()
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error || '')
      if (message.includes('患者不存在') || message.includes('登录已过期') || message.includes('token') || message.includes('Token')) {
        logout()
      }
      throw error
    }
  }

  function logout() {
    uni.removeStorageSync('patient_token')
    patient.value = null
  }

  return { patient, loading, isLoggedIn, login, loadProfile, logout }
})

