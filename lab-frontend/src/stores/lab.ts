import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import { getExamination, getExaminations, getManualReviews, getReviewDetail, getReviewList, getReviewStats } from '@/api/lab'
import type { Examination, ExaminationAiReview, ReviewStats } from '@/types'

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

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

  function buildAuditText(review: ExaminationAiReview) {
    const abnormalItems = parseJson(review.abnormalItems)
    const logicIssues = parseJson(review.logicIssues)
    const historyItems = parseJson(review.historyCompare)
    const warnings = parseJson(review.warnings)
    const conclusion = review.reviewResult === 'pass' ? '自动通过' : review.reviewResult === 'reject' ? '退回重测' : '人工复核'
    const lines: string[] = []
    lines.push('AI正在审核检验报告...')
    lines.push('')
    lines.push(`报告编号：EX${String(review.examinationId).padStart(6, '0')}`)
    lines.push(`审核结论：${conclusion}`)
    lines.push('')
    lines.push('一、参考范围判断')
    if (abnormalItems.length) {
      abnormalItems.forEach(item => lines.push(`- ${item.name || item.item || '指标'}：${item.value || '--'}，参考范围 ${item.reference || '未提供'}，${item.status || '异常'}`))
    } else {
      lines.push('- 所有已识别指标均在参考范围内。')
    }
    lines.push('')
    lines.push('二、逻辑合理性校验')
    if (logicIssues.length) {
      logicIssues.forEach(item => lines.push(`- ${item.content || '存在逻辑疑点，需要人工复核。'}`))
    } else {
      lines.push('- 白细胞总数、分类计数及项目间关系未发现明显矛盾。')
    }
    lines.push('')
    lines.push('三、历史结果对比')
    if (historyItems.length) {
      historyItems.forEach(item => lines.push(`- ${item.item || '指标'}：上次 ${item.lastValue || '--'} → 本次 ${item.currentValue || '--'}，${item.change || '变化平稳'}`))
    } else {
      lines.push('- 未发现需要重点关注的异常波动。')
    }
    lines.push('')
    lines.push('四、风险提醒')
    if (warnings.length) {
      warnings.forEach(item => lines.push(`- ${item.content || '存在风险提醒。'}`))
    } else {
      lines.push('- 暂无危急值或重测风险。')
    }
    lines.push('')
    lines.push(`五、处理建议：${review.suggestions || '按当前审核结论处理。'}`)
    return lines.join('\n')
  }

  async function streamReview(review: ExaminationAiReview) {
    streaming.value = true
    streamText.value = ''
    try {
      const detail = await selectReview(review.id)
      const text = buildAuditText(detail)
      for (const char of text) {
        streamText.value += char
        await sleep(10)
      }
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
