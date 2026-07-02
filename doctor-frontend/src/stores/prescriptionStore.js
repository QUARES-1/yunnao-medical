import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 处方记录 Store
 * - 管理当前就诊的处方草稿
 * - 缓存 AI 审核结果（避免重复请求）
 * - 维护处方提交状态流转
 */
export const usePrescriptionStore = defineStore('prescription', () => {
  // ==================== 处方草稿 ====================
  /** 当前就诊挂号ID对应的处方草稿 Map<registrationId, draftData> */
  const drafts = ref(new Map())

  /** 正在编辑的挂号ID */
  const activeRegistrationId = ref(null)

  const activeDraft = computed(() =>
    activeRegistrationId.value
      ? drafts.value.get(activeRegistrationId.value) || null
      : null
  )

  /** 初始化或切换处方草稿 */
  function initDraft(registrationId, initialData = {}) {
    activeRegistrationId.value = registrationId
    if (!drafts.value.has(registrationId)) {
      drafts.value.set(registrationId, {
        diagnosis: '',
        diagnosisCode: '',
        prescription: '',
        drugs: [],        // [{ name, spec, dosage, frequency, days, amount }]
        notes: '',
        ...initialData
      })
    }
  }

  /** 更新当前草稿字段 */
  function updateDraft(registrationId, patch) {
    const draft = drafts.value.get(registrationId)
    if (draft) {
      drafts.value.set(registrationId, { ...draft, ...patch })
    }
  }

  /** 清除草稿（提交成功后） */
  function clearDraft(registrationId) {
    drafts.value.delete(registrationId)
    if (activeRegistrationId.value === registrationId) {
      activeRegistrationId.value = null
    }
  }

  // ==================== AI 审核结果缓存 ====================
  /**
   * Map<registrationId, reviewResult>
   * reviewResult: { reviewResult, reviewScore, warnings, suggestions, drugInteractions, ... }
   */
  const reviewResults = ref(new Map())
  const isReviewing = ref(false)

  function getReviewResult(registrationId) {
    return reviewResults.value.get(registrationId) || null
  }

  function setReviewResult(registrationId, result) {
    reviewResults.value.set(registrationId, {
      ...result,
      reviewedAt: new Date().toISOString()
    })
  }

  function clearReviewResult(registrationId) {
    reviewResults.value.delete(registrationId)
  }

  function setReviewing(val) {
    isReviewing.value = val
  }

  // ==================== 提交状态 ====================
  const isSubmitting = ref(false)
  const submitHistory = ref([]) // [{ registrationId, submittedAt, prescriptionId }]

  function setSubmitting(val) {
    isSubmitting.value = val
  }

  function recordSubmit(registrationId, prescriptionId) {
    submitHistory.value.push({
      registrationId,
      prescriptionId,
      submittedAt: new Date().toISOString()
    })
    clearDraft(registrationId)
    clearReviewResult(registrationId)
  }

  // ==================== 统计计算 ====================
  const todaySubmitCount = computed(() => {
    const today = new Date().toDateString()
    return submitHistory.value.filter(
      h => new Date(h.submittedAt).toDateString() === today
    ).length
  })

  /** 清除所有数据（退出登录时） */
  function clearAll() {
    drafts.value.clear()
    reviewResults.value.clear()
    submitHistory.value = []
    activeRegistrationId.value = null
    isReviewing.value = false
    isSubmitting.value = false
  }

  return {
    // state
    drafts, activeRegistrationId, reviewResults, isReviewing, isSubmitting, submitHistory,
    // computed
    activeDraft, todaySubmitCount,
    // actions
    initDraft, updateDraft, clearDraft,
    getReviewResult, setReviewResult, clearReviewResult, setReviewing,
    setSubmitting, recordSubmit, clearAll
  }
})
