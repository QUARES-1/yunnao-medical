import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getRegistrationDetail } from '../api/doctor'

/**
 * 患者信息 Store
 * - 缓存已加载的患者基本信息（避免重复请求）
 * - 记录当前正在就诊的患者
 */
export const usePatientStore = defineStore('patient', () => {
  // ==================== 患者信息缓存 ====================
  /** Map<patientId, patientData> */
  const patientCache = ref(new Map())
  const isLoading = ref(false)

  /** 当前正在查看/就诊的患者 ID */
  const currentPatientId = ref(null)

  const currentPatient = computed(() =>
    currentPatientId.value ? patientCache.value.get(currentPatientId.value) || null : null
  )

  /**
   * 获取患者信息（有缓存直接返回，否则请求后端）
   * @param {number} patientId
   * @returns {Promise<object>} 患者数据
   */
  async function fetchPatient(patientId) {
    if (patientCache.value.has(patientId)) {
      return patientCache.value.get(patientId)
    }
    isLoading.value = true
    try {
      const r = await getRegistrationDetail(patientId)
      const data = r.data
      patientCache.value.set(patientId, data)
      return data
    } finally {
      isLoading.value = false
    }
  }

  /** 设置当前就诊患者 */
  function setCurrentPatient(patientId) {
    currentPatientId.value = patientId
  }

  /** 更新缓存中某个患者的信息（就诊中有变化时使用） */
  function updateCachedPatient(patientId, patch) {
    const existing = patientCache.value.get(patientId)
    if (existing) {
      patientCache.value.set(patientId, { ...existing, ...patch })
    }
  }

  /** 清除缓存（切换医生账号时调用） */
  function clearCache() {
    patientCache.value.clear()
    currentPatientId.value = null
  }

  return {
    // state
    patientCache, isLoading, currentPatientId,
    // computed
    currentPatient,
    // actions
    fetchPatient, setCurrentPatient, updateCachedPatient, clearCache
  }
})
