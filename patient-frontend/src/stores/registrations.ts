import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getRegistrations } from '@/api/patient'
import type { Registration } from '@/types/api'

export const useRegistrationsStore = defineStore('patient-registrations', () => {
  const records = ref<Registration[]>([])
  const activeStatus = ref('')
  const loading = ref(false)
  const allCount = computed(() => records.value.length)
  const pendingCount = computed(() => records.value.filter(item => item.status === '待就诊').length)

  async function load(status = activeStatus.value) {
    if (!uni.getStorageSync('patient_token')) {
      records.value = []
      return
    }
    loading.value = true
    activeStatus.value = status
    try {
      const result = await getRegistrations({ status: status || undefined, page: 1, size: 50 })
      records.value = result.content || []
    } finally {
      loading.value = false
    }
  }

  async function changeStatus(status: string) {
    activeStatus.value = status
    await load(status)
  }

  return { records, activeStatus, loading, allCount, pendingCount, load, changeStatus }
})
