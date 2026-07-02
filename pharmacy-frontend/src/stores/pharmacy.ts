import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMedicines, getPrescriptions, getStockForecastList } from '@/api/pharmacy'
import { streamSse } from '@/utils/sse'
import type { ForecastMedicine, Medicine, Prescription, StockForecast } from '@/types'

type ForecastStreamPayload = {
  period?: string
  medicines: ForecastMedicine[]
  totalForecastAmount?: number
  totalPurchaseAmount?: number
}

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

  async function streamForecastReport(payload: ForecastStreamPayload) {
    streaming.value = true
    streamText.value = ''
    try {
      await streamSse('/api/medicine/ai/stock-forecast/stream', {
        token: localStorage.getItem('pharmacy_token') || '',
        body: {
          forecastType: 'monthly',
          forecastPeriod: payload.period,
          medicines: payload.medicines,
          totalForecastAmount: payload.totalForecastAmount,
          totalPurchaseAmount: payload.totalPurchaseAmount
        },
        handlers: {
          onDelta: chunk => { streamText.value += chunk },
          onDone: async () => { await loadForecastState() },
          onError: message => { throw new Error(message || '库存预测流式生成失败') }
        }
      })
    } catch (error: any) {
      const message = error?.message || '库存预测流式生成失败，请确认后端 8080 和 AI 服务 8081 已启动'
      streamText.value = `生成失败：${message}`
      ElMessage.error(message)
      throw error
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

