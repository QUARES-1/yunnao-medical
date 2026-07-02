import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getOverview } from '@/api/admin'
import { getDoctorPage } from '@/api/doctor'
import { getDepartments } from '@/api/department'
import { getMedicinePage } from '@/api/medicine'
import type { Department, Doctor, Medicine, OverviewStatistics } from '@/types'

export const useMedicalStore = defineStore('admin-medical', () => {
  const statistics = ref<OverviewStatistics>({ patientCount: 0, doctorCount: 0, departmentCount: 0, registrationCount: 0 })
  const departments = ref<Department[]>([])
  const doctors = ref<Doctor[]>([])
  const medicines = ref<Medicine[]>([])
  const loading = ref(false)

  const departmentMap = computed(() => new Map(departments.value.map(item => [item.id, item.name])))
  const lowStockMedicines = computed(() => medicines.value.filter(item => Number(item.stock || 0) < 30))

  async function loadSnapshot() {
    loading.value = true
    try {
      const [statRes, depRes, doctorRes, medicineRes] = await Promise.all([
        getOverview(),
        getDepartments(),
        getDoctorPage(1, 8),
        getMedicinePage({ page: 1, size: 8 })
      ])
      statistics.value = statRes.data.data
      departments.value = depRes.data.data || []
      doctors.value = doctorRes.data.data.content || []
      medicines.value = medicineRes.data.data.content || []
    } finally {
      loading.value = false
    }
  }

  return { statistics, departments, doctors, medicines, departmentMap, lowStockMedicines, loading, loadSnapshot }
})
