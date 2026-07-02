import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getDoctorInfo } from '../api/doctor'

/**
 * 医生信息 Store
 * - 管理当前登录医生的基本信息
 * - 管理 WebSocket 通知状态
 */
export const useDoctorStore = defineStore('doctor', () => {
  // ==================== 医生基础信息 ====================
  const doctorInfo = ref(null)
  const isLoading = ref(false)

  const doctorName = computed(() => doctorInfo.value?.name || '医生')
  const doctorInitial = computed(() => (doctorInfo.value?.name || '医').slice(0, 1))
  const doctorId = computed(() => doctorInfo.value?.id || null)
  const departmentName = computed(() => doctorInfo.value?.departmentName || '未分配科室')
  const doctorTitle = computed(() => doctorInfo.value?.title || '专业医生')

  /** 从后端加载医生信息，并同步写入 localStorage */
  async function fetchDoctorInfo() {
    isLoading.value = true
    try {
      const r = await getDoctorInfo()
      doctorInfo.value = r.data
      localStorage.setItem('doctorInfo', JSON.stringify(r.data))
    } catch (e) {
      // 后端不可达时降级读缓存
      const cached = localStorage.getItem('doctorInfo')
      if (cached) doctorInfo.value = JSON.parse(cached)
    } finally {
      isLoading.value = false
    }
  }

  /** 从 localStorage 快速恢复（页面刷新时使用，避免闪烁） */
  function restoreFromCache() {
    const cached = localStorage.getItem('doctorInfo')
    if (cached && !doctorInfo.value) {
      doctorInfo.value = JSON.parse(cached)
    }
  }

  /** 更新医生信息（个人中心编辑后调用） */
  function updateDoctorInfo(data) {
    doctorInfo.value = { ...doctorInfo.value, ...data }
    localStorage.setItem('doctorInfo', JSON.stringify(doctorInfo.value))
  }

  /** 登出时清除 */
  function clearDoctorInfo() {
    doctorInfo.value = null
    localStorage.removeItem('doctorInfo')
    localStorage.removeItem('token')
  }

  // ==================== WebSocket 通知 ====================
  const notifications = ref([])
  const wsConnected = ref(false)

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  function addNotification(msg) {
    notifications.value.unshift({
      ...msg,
      read: false,
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    })
    // 最多保留 50 条
    if (notifications.value.length > 50) notifications.value.pop()
  }

  function markRead(idx) {
    if (notifications.value[idx]) notifications.value[idx].read = true
  }

  function markAllRead() {
    notifications.value.forEach(n => (n.read = true))
  }

  function setWsConnected(val) {
    wsConnected.value = val
  }

  return {
    // state
    doctorInfo, isLoading, notifications, wsConnected,
    // computed
    doctorName, doctorInitial, doctorId, departmentName, doctorTitle, unreadCount,
    // actions
    fetchDoctorInfo, restoreFromCache, updateDoctorInfo, clearDoctorInfo,
    addNotification, markRead, markAllRead, setWsConnected
  }
})
