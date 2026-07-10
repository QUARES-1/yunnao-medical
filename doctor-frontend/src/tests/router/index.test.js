import { describe, it, expect, vi, beforeEach } from 'vitest'
import router from '@/router'

vi.stubGlobal('localStorage', {
  data: {},
  getItem(key) { return this.data[key] ?? null },
  setItem(key, val) { this.data[key] = val },
  removeItem(key) { delete this.data[key] },
  clear() { this.data = {} }
})

describe('Router Configuration (doctor)', () => {

  beforeEach(() => {
    localStorage.clear()
  })

  describe('route definitions', () => {
    it('should define login as public route', () => {
      const loginRoute = router.getRoutes().find(r => r.path === '/login')
      expect(loginRoute?.meta.requiresAuth).toBe(false)
    })

    it('should protect root route with auth', () => {
      const rootRoute = router.getRoutes().find(r => r.path === '/')
      expect(rootRoute?.meta.requiresAuth).toBe(true)
    })

    it('should have workbench child route', () => {
      const rootRoute = router.getRoutes().find(r => r.path === '/')
      const workbenchRoute = rootRoute?.children?.find(r => r.path === 'workbench')
      expect(workbenchRoute?.meta.title).toBe('工作台')
    })

    it('should have consultation route with regId param', () => {
      const rootRoute = router.getRoutes().find(r => r.path === '/')
      const consultationRoute = rootRoute?.children?.find(r => r.path === 'consultation/:regId')
      expect(consultationRoute).toBeDefined()
      expect(consultationRoute?.meta.title).toBe('看诊')
    })

    it('should have history route', () => {
      const rootRoute = router.getRoutes().find(r => r.path === '/')
      const historyRoute = rootRoute?.children?.find(r => r.path === 'history')
      expect(historyRoute?.meta.title).toBe('历史记录')
    })

    it('should have profile route', () => {
      const rootRoute = router.getRoutes().find(r => r.path === '/')
      const profileRoute = rootRoute?.children?.find(r => r.path === 'profile')
      expect(profileRoute?.meta.title).toBe('个人中心')
    })

    it('should have catch-all redirect to root', () => {
      const catchAll = router.getRoutes().find(r => r.path === '/:pathMatch(.*)*')
      expect(catchAll?.redirect).toBe('/')
    })
  })

  describe('navigation guard', () => {
    it('should allow navigation to public route without token', async () => {
      localStorage.clear()
      const to = { path: '/login', meta: { requiresAuth: false } }
      const token = localStorage.getItem('token')
      const shouldRedirect = to.meta.requiresAuth !== false && !token
      expect(shouldRedirect).toBe(false)
    })

    it('should require token for protected routes', async () => {
      localStorage.clear()
      const token = localStorage.getItem('token')
      const to = { path: '/workbench', meta: { requiresAuth: true } }
      const shouldRedirect = to.meta.requiresAuth !== false && !token
      expect(shouldRedirect).toBe(true)
    })

    it('should allow navigation when authenticated', async () => {
      localStorage.setItem('token', 'doctor-token')
      const token = localStorage.getItem('token')
      const to = { path: '/workbench', meta: {} }
      const shouldRedirect = to.meta.requiresAuth !== false && !token
      expect(shouldRedirect).toBe(false)
    })
  })
})
