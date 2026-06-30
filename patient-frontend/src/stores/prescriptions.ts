import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getPatientPrescriptions } from '@/api/patient'
import type { Prescription } from '@/types/api'

export const usePrescriptionsStore = defineStore('patient-prescriptions', () => {
  const records = ref<Prescription[]>([])
  const loading = ref(false)
  const page = ref(1)
  const size = 10
  const total = ref(0)
  const hasMore = ref(false)

  const dispensedCount = computed(() => records.value.filter(item => item.status === '已发药').length)
  const pendingCount = computed(() => records.value.filter(item => item.status !== '已发药').length)

  async function load(reset = false) {
    if (loading.value) return
    loading.value = true
    try {
      if (reset) {
        page.value = 1
        records.value = []
      }
      const result = await getPatientPrescriptions({ page: page.value, size })
      const list = result.content || []
      records.value = reset ? list : records.value.concat(list)
      total.value = result.totalElements || records.value.length
      hasMore.value = page.value < (result.totalPages || 1)
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (!hasMore.value || loading.value) return
    page.value += 1
    await load(false)
  }

  return { records, loading, page, total, hasMore, dispensedCount, pendingCount, load, loadMore }
})
