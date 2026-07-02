import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import { getExamination, getExaminations, getManualReviews, getReviewDetail, getReviewList, getReviewStats } from '@/api/lab'
import { streamSse } from '@/utils/sse'
import type { Examination, ExaminationAiReview, ReviewStats } from '@/types'


export const useLabStore = defineStore('lab-workbench', () => {
  const loading = ref(false)
  const streaming = ref(false)
  const streamText = ref('')
  const pendingExaminations = ref<Examination[]>([])
  const completedReports = ref<Examination[]>([])
  const reviewRecords = ref<ExaminationAiReview[]>([])
  const manualReviews = ref<ExaminationAiReview[]>([])
  const currentExamination = ref<Examination | null>(null)
  const currentReview = ref<ExaminationAiReview | null>(null)
  const reviewTotal = ref(0)
  const manualTotal = ref(0)
  const stats = reactive<ReviewStats>({ total: 0, passCount: 0, manualCount: 0, rejectCount: 0, passRate: 0 })

  const pendingCount = computed(() => pendingExaminations.value.length)
  const completedCount = computed(() => completedReports.value.length)
  const abnormalReviewCount = computed(() => stats.manualCount + stats.rejectCount)

  async function loadExaminations(status: string, page = 1, size = 10) {
    loading.value = true
    try {
      const res = await getExaminations(status, page, size)
      if (status === '待检查') pendingExaminations.value = res.data.data.content || []
      if (status === '已完成') completedReports.value = res.data.data.content || []
      return res.data.data
    } finally {
      loading.value = false
    }
  }

  async function loadReviewDashboard(reviewResult = '', page = 1, size = 10) {
    loading.value = true
    try {
      const [listRes, manualRes, statsRes] = await Promise.all([
        getReviewList(reviewResult || undefined, page, size),
        getManualReviews(1, 5),
        getReviewStats()
      ])
      reviewRecords.value = listRes.data.data.content || []
      reviewTotal.value = listRes.data.data.totalElements || 0
      manualReviews.value = manualRes.data.data.content || []
      manualTotal.value = manualRes.data.data.totalElements || 0
      Object.assign(stats, statsRes.data.data)
      return listRes.data.data
    } finally {
      loading.value = false
    }
  }

  async function selectExamination(id: number) {
    currentExamination.value = (await getExamination(id)).data.data
    return currentExamination.value
  }

  async function selectReview(id: number) {
    currentReview.value = (await getReviewDetail(id)).data.data
    return currentReview.value
  }

  function parseJson(text?: string): any[] {
    try {
      const data = JSON.parse(text || '[]')
      return Array.isArray(data) ? data : []
    } catch {
      return []
    }
  }

  async function streamReview(review: ExaminationAiReview) {
    streaming.value = true
    streamText.value = ''
    try {
      await streamSse(`/api/examination/ai/review/stream/${review.examinationId}`, {
        token: localStorage.getItem('lab_token') || '',
        handlers: {
          onDelta: chunk => { streamText.value += chunk },
          onDone: async data => {
            try {
              const meta = JSON.parse(data || '{}')
              if (meta.id) await selectReview(Number(meta.id))
            } catch {}
            await loadReviewDashboard()
          },
          onError: message => { throw new Error(message || '检验AI审核流式生成失败') }
        }
      })
    } finally {
      streaming.value = false
    }
  }
  function clearStream() {
    streamText.value = ''
  }

  return {
    loading,
    streaming,
    streamText,
    pendingExaminations,
    completedReports,
    reviewRecords,
    manualReviews,
    currentExamination,
    currentReview,
    reviewTotal,
    manualTotal,
    stats,
    pendingCount,
    completedCount,
    abnormalReviewCount,
    loadExaminations,
    loadReviewDashboard,
    selectExamination,
    selectReview,
    streamReview,
    clearStream
  }
})

