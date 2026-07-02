import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAiChatLogs, getAiOverview, getAiReportList, getQualityCheckList, streamAiOperationReport } from '@/api/ai'
import type { AiChatLog, AiOperationReport, QualityCheckRecord } from '@/types'

export const useAiAdminStore = defineStore('admin-ai', () => {
  const loading = ref(false)
  const streaming = ref(false)
  const streamText = ref('')
  const overview = ref<Record<string, unknown>>({})
  const reports = ref<AiOperationReport[]>([])
  const checks = ref<QualityCheckRecord[]>([])
  const logs = ref<AiChatLog[]>([])
  const lastStreamReportId = ref<string>()

  const keyMetrics = computed(() => (overview.value.keyMetrics || {}) as Record<string, unknown>)

  async function loadDashboard(reportType = '') {
    loading.value = true
    try {
      const [overviewRes, reportRes, checkRes, logRes] = await Promise.all([
        getAiOverview().catch(() => ({ data: { data: {} } })),
        getAiReportList({ reportType: reportType || undefined, page: 1, size: 6 }),
        getQualityCheckList({ page: 1, size: 5 }),
        getAiChatLogs({ page: 1, size: 8 })
      ])
      overview.value = overviewRes.data.data || {}
      reports.value = reportRes.data.data.content || []
      checks.value = checkRes.data.data.content || []
      logs.value = logRes.data.data.content || []
    } finally {
      loading.value = false
    }
  }

  async function loadReports(reportType = '') {
    const res = await getAiReportList({ reportType: reportType || undefined, page: 1, size: 6 })
    reports.value = res.data.data.content || []
  }

  async function generateReportStream(reportType = 'daily') {
    streaming.value = true
    streamText.value = ''
    lastStreamReportId.value = undefined
    try {
      await streamAiOperationReport({ reportType }, chunk => {
        streamText.value += chunk
      }, reportId => {
        lastStreamReportId.value = reportId
      })
      await loadDashboard()
    } finally {
      streaming.value = false
    }
  }

  return { loading, streaming, streamText, overview, keyMetrics, reports, checks, logs, lastStreamReportId, loadDashboard, loadReports, generateReportStream }
})
