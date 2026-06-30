import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getMedicines, getPrescriptions, getStockForecastList } from '@/api/pharmacy'
import type { ForecastMedicine, Medicine, Prescription, StockForecast } from '@/types'

type ForecastStreamPayload = {
  period?: string
  medicines: ForecastMedicine[]
  totalForecastAmount?: number
  totalPurchaseAmount?: number
}

const sleep = (ms: number) => new Promise<void>(resolve => window.setTimeout(resolve, ms))
const parseArray = <T>(value?: string): T[] => {
  try {
    return JSON.parse(value || '[]')
  } catch {
    return []
  }
}

export const usePharmacyStore = defineStore('pharmacy-workbench', () => {
  const pendingPrescriptions = ref<Prescription[]>([])
  const dispensedPrescriptions = ref<Prescription[]>([])
  const medicines = ref<Medicine[]>([])
  const forecasts = ref<StockForecast[]>([])
  const loading = ref(false)
  const streaming = ref(false)
  const streamText = ref('')

  const pendingCount = computed(() => pendingPrescriptions.value.length)
  const dispensedCount = computed(() => dispensedPrescriptions.value.length)
  const lowStockMedicines = computed(() => medicines.value.filter(item => Number(item.stock || 0) <= 20))
  const latestForecastRows = computed(() => parseArray<ForecastMedicine>(forecasts.value[0]?.forecastData))

  async function loadPrescriptionState() {
    loading.value = true
    try {
      const [pending, dispensed] = await Promise.all([
        getPrescriptions('待发药', 1, 20),
        getPrescriptions('已发药', 1, 20)
      ])
      pendingPrescriptions.value = pending.data.data.content || []
      dispensedPrescriptions.value = dispensed.data.data.content || []
    } finally {
      loading.value = false
    }
  }

  async function loadMedicineState() {
    loading.value = true
    try {
      const result = await getMedicines({ page: 1, size: 100 })
      medicines.value = result.data.data.content || []
    } finally {
      loading.value = false
    }
  }

  async function loadForecastState() {
    loading.value = true
    try {
      const result = await getStockForecastList(1, 10)
      forecasts.value = result.data.data.content || []
    } finally {
      loading.value = false
    }
  }

  function buildForecastReport(payload: ForecastStreamPayload) {
    const rows = payload.medicines || []
    const highRisk = rows.filter(item => item.riskLevel === '高风险')
    const needPurchase = rows.filter(item => Number(item.suggestPurchase || 0) > 0)
    const overStock = rows.filter(item => item.riskLevel === '可能积压')
    const topPurchase = [...needPurchase].sort((a, b) => Number(b.suggestPurchase || 0) - Number(a.suggestPurchase || 0)).slice(0, 5)

    const lines = [
      '【AI药品库存预测报告】',
      `预测周期：${payload.period || '未来 1 个月'}`,
      '',
      '一、总体结论',
      `本次共分析 ${rows.length} 种药品。预计未来 30 天消耗金额约 ¥${Number(payload.totalForecastAmount || 0).toLocaleString('zh-CN')}，建议采购金额约 ¥${Number(payload.totalPurchaseAmount || 0).toLocaleString('zh-CN')}。`,
      `其中需重点处理 ${needPurchase.length} 种药品，高风险缺货 ${highRisk.length} 种，可能积压 ${overStock.length} 种。`,
      '',
      '二、优先采购建议',
      topPurchase.length
        ? topPurchase.map((item, index) => `${index + 1}. ${item.name}：当前库存 ${item.currentStock}${item.unit}，未来30天预计消耗 ${item.forecastConsume}${item.unit}，建议采购 ${item.suggestPurchase}${item.unit}。`).join('\n')
        : '当前安全库存充足，暂无必须采购药品。',
      '',
      '三、风险提醒',
      highRisk.length
        ? highRisk.map(item => `- ${item.name} 存在缺货风险：${item.reason || '库存覆盖天数不足，建议优先补货。'}`).join('\n')
        : '- 暂未发现高风险缺货药品。',
      overStock.length
        ? overStock.map(item => `- ${item.name} 可能积压：建议减少采购频次，优先消耗现有库存。`).join('\n')
        : '- 暂未发现明显积压风险。',
      '',
      '四、药房执行建议',
      '1. 对高风险药品优先发起采购申请；',
      '2. 对库存不足但近期消耗稳定的药品维持安全库存；',
      '3. 对可能积压药品暂停大批量采购，结合门诊处方量动态观察；',
      '4. 每周复核一次预测结果，避免季节性疾病波动造成断货。'
    ]
    return lines.join('\n')
  }

  async function streamForecastReport(payload: ForecastStreamPayload) {
    streaming.value = true
    streamText.value = ''
    try {
      const report = buildForecastReport(payload)
      for (const char of report) {
        streamText.value += char
        await sleep(char === '\n' ? 28 : 9)
      }
    } finally {
      streaming.value = false
    }
  }

  function clearStream() {
    streamText.value = ''
  }

  return {
    pendingPrescriptions,
    dispensedPrescriptions,
    medicines,
    forecasts,
    loading,
    streaming,
    streamText,
    pendingCount,
    dispensedCount,
    lowStockMedicines,
    latestForecastRows,
    loadPrescriptionState,
    loadMedicineState,
    loadForecastState,
    streamForecastReport,
    clearStream
  }
})
