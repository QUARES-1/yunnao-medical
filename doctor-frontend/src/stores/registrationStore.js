import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTodayRegistrations, startConsultation } from '../api/doctor'

/**
 * 挂号记录 Store
 * - 管理今日挂号队列
 * - 提供状态筛选、搜索等计算属性
 * - 封装开始就诊的业务逻辑
 */
export const useRegistrationStore = defineStore('registration', () => {
  const registrations = ref([])
  const isLoading = ref(false)
  const lastLoadTime = ref(null)

  // ==================== 过滤条件 ====================
  const keyword = ref('')
  const statusFilter = ref('')

  const STATUSES = ['待就诊', '就诊中', '已就诊', '已取消']

  // ==================== 计算属性 ====================
  const filteredList = computed(() =>
    registrations.value.filter(r =>
      (!statusFilter.value || r.status === statusFilter.value) &&
      (!keyword.value || r.patientName?.includes(keyword.value.trim()))
    )
  )

  const totalCount = computed(() => registrations.value.length)

  const statusCount = computed(() => {
    const map = {}
    STATUSES.forEach(s => {
      map[s] = registrations.value.filter(r => r.status === s).length
    })
    return map
  })

  const waitingCount = computed(() => statusCount.value['待就诊'] || 0)
  const activeCount = computed(() => statusCount.value['就诊中'] || 0)
  const doneCount = computed(() => statusCount.value['已就诊'] || 0)

  // ==================== 当前就诊挂号单 ====================
  const currentRegistrationId = ref(null)
  const currentRegistration = computed(() =>
    currentRegistrationId.value
      ? registrations.value.find(r => r.id === currentRegistrationId.value) || null
      : null
  )

  // ==================== Actions ====================

  /** 加载今日挂号列表 */
  async function loadTodayRegistrations() {
    isLoading.value = true
    try {
      const r = await getTodayRegistrations()
      registrations.value = r.data || []
      lastLoadTime.value = new Date()
    } finally {
      isLoading.value = false
    }
  }

  /** 开始就诊（更新本地状态，减少不必要的重新请求） */
  async function startRegistration(registrationId) {
    await startConsultation(registrationId)
    // 乐观更新本地状态
    const item = registrations.value.find(r => r.id === registrationId)
    if (item) item.status = '就诊中'
    currentRegistrationId.value = registrationId
  }

  /** 就诊完成后更新状态 */
  function markDone(registrationId) {
    const item = registrations.value.find(r => r.id === registrationId)
    if (item) item.status = '已就诊'
  }

  /** 设置当前操作的挂号单 */
  function setCurrentRegistration(id) {
    currentRegistrationId.value = id
  }

  /** 设置过滤关键词 */
  function setKeyword(val) {
    keyword.value = val
  }

  /** 设置状态过滤 */
  function setStatusFilter(val) {
    statusFilter.value = val
  }

  /** 重置过滤条件 */
  function resetFilter() {
    keyword.value = ''
    statusFilter.value = ''
  }

  /** 清除所有数据（退出登录时） */
  function clearAll() {
    registrations.value = []
    currentRegistrationId.value = null
    resetFilter()
  }

  return {
    // state
    registrations, isLoading, lastLoadTime, keyword, statusFilter,
    STATUSES,
    // computed
    filteredList, totalCount, statusCount, waitingCount, activeCount, doneCount,
    currentRegistrationId, currentRegistration,
    // actions
    loadTodayRegistrations, startRegistration, markDone, setCurrentRegistration,
    setKeyword, setStatusFilter, resetFilter, clearAll
  }
})
