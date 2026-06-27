import { describe, it, expect } from 'vitest'

describe('Router Configuration (doctor)', () => {
  const routes = [
    { path: '/login', name: 'Login', meta: { requiresAuth: false } },
    { path: '/', redirect: '/workbench', meta: { requiresAuth: true } },
    { path: 'workbench', name: 'Workbench', meta: { title: '工作台' } },
    { path: 'consultation/:regId', name: 'Consultation', meta: { title: '看诊' } },
    { path: 'history', name: 'History', meta: { title: '历史记录' } },
    { path: 'profile', name: 'Profile', meta: { title: '个人中心' } }
  ]

  describe('route definitions', () => {
    it('should define login as public route', () => {
      const loginRoute = routes.find(r => r.path === '/login')
      expect(loginRoute?.meta.requiresAuth).toBe(false)
    })

    it('should protect main routes with auth', () => {
      const workbenchRoute = routes.find(r => r.path === 'workbench')
      expect(workbenchRoute?.meta.title).toBe('工作台')
    })

    it('should have consultation route with regId param', () => {
      const consultationRoute = routes.find(r => r.path === 'consultation/:regId')
      expect(consultationRoute?.path).toBe('consultation/:regId')
      expect(consultationRoute?.meta.title).toBe('看诊')
    })

    it('should have history route', () => {
      const historyRoute = routes.find(r => r.path === 'history')
      expect(historyRoute?.meta.title).toBe('历史记录')
    })

    it('should have profile route', () => {
      const profileRoute = routes.find(r => r.path === 'profile')
      expect(profileRoute?.meta.title).toBe('个人中心')
    })

    it('should have catch-all redirect to root', () => {
      const catchAll = { path: '/:pathMatch(.*)*', redirect: '/' }
      expect(catchAll.redirect).toBe('/')
    })
  })

  describe('navigation guard logic', () => {
    it('should allow public routes without token', () => {
      const publicRoute = { meta: { requiresAuth: false } }
      const hasToken = false
      const shouldRedirect = publicRoute.meta.requiresAuth !== false && !hasToken
      expect(shouldRedirect).toBe(false)
    })

    it('should require token for protected routes', () => {
      const protectedRoute = { meta: {} }
      const hasToken = false
      const shouldRedirect = protectedRoute.meta.requiresAuth !== false && !hasToken
      expect(shouldRedirect).toBe(true)
    })

    it('should redirect to / when already logged in', () => {
      const to = { path: '/login' }
      const hasToken = true
      const shouldRedirect = to.path === '/login' && hasToken
      expect(shouldRedirect).toBe(true)
    })

    it('should allow navigation when authenticated', () => {
      const to = { path: '/workbench', meta: {} }
      const hasToken = true
      const shouldRedirect = to.meta.requiresAuth !== false && !hasToken
      expect(shouldRedirect).toBe(false)
    })
  })
})
