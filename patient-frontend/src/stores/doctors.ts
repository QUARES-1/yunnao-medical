import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getDepartments, getDoctors } from '@/api/patient'
import type { Department, Doctor } from '@/types/api'

export const useDoctorsStore = defineStore('patient-doctors', () => {
  const departments = ref<Department[]>([])
  const doctors = ref<Doctor[]>([])
  const selectedDepartmentId = ref<number>()
  const loading = ref(false)

  const doctorCount = computed(() => doctors.value.length)

  async function loadDepartments() {
    if (departments.value.length) return departments.value
    departments.value = await getDepartments()
    return departments.value
  }

  async function loadDoctors(departmentId?: number) {
    loading.value = true
    selectedDepartmentId.value = departmentId
    try {
      doctors.value = await getDoctors(departmentId)
      return doctors.value
    } finally {
      loading.value = false
    }
  }

  async function initialize(departmentId?: number) {
    await Promise.all([loadDepartments(), loadDoctors(departmentId)])
  }

  return { departments, doctors, selectedDepartmentId, loading, doctorCount, loadDepartments, loadDoctors, initialize }
})
