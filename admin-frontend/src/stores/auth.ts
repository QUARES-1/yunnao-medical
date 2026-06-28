import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAdminInfo, loginAdmin } from '@/api/admin'
import type { AdminInfo } from '@/types'

export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const cached = localStorage.getItem('admin_info')
  const admin = ref<AdminInfo | null>(cached ? JSON.parse(cached) : null)
  const isLoggedIn = computed(() => Boolean(token.value))

  async function login(username: string, password: string) {
    const response = await loginAdmin(username, password)
    token.value = response.data.data
    localStorage.setItem('admin_token', token.value)
    await loadInfo()
  }

  async function loadInfo() {
    const response = await getAdminInfo()
    admin.value = response.data.data
    localStorage.setItem('admin_info', JSON.stringify(admin.value))
  }

  function logout() {
    token.value = ''
    admin.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
  }

  return { token, admin, isLoggedIn, login, loadInfo, logout }
})
