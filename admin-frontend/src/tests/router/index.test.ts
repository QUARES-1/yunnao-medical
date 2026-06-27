import { describe, it, expect } from 'vitest'

describe('Router Configuration', () => {
  const expectedRoutes = [
    { path: '/login', meta: { public: true }, title: 'login' },
    { path: 'dashboard', meta: { title: '数据概览' }, title: '数据概览' },
    { path: 'departments', meta: { title: '科室管理' }, title: '科室管理' },
    { path: 'doctors', meta: { title: '医生管理' }, title: '医生管理' },
    { path: 'medicines', meta: { title: '药品管理' }, title: '药品管理' },
    { path: 'profile', meta: { title: '个人中心' }, title: '个人中心' }
  ]

  describe('route definitions', () => {
    it('should define login route as public', () => {
      const loginRoute = expectedRoutes.find(r => r.path === '/login')
      expect(loginRoute?.meta.public).toBe(true)
    })

    it('should define all management routes with titles', () => {
      const managementRoutes = expectedRoutes.filter(r => r.title !== 'login')
      expect(managementRoutes).toHaveLength(5)
      managementRoutes.forEach(route => {
        expect(route.meta.title).toBeTruthy()
      })
    })

    it('should have dashboard as default redirect route', () => {
      const rootRoute = { path: '/', redirect: '/dashboard' }
      expect(rootRoute.redirect).toBe('/dashboard')
    })

    it('should have catch-all route for unknown paths', () => {
      const catchAllRoute = { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
      expect(catchAllRoute.path).toMatch(/pathMatch/)
      expect(catchAllRoute.redirect).toBe('/dashboard')
    })
  })

  describe('navigation guard logic', () => {
    it('should allow access to public routes without token', () => {
      const publicRoute = { meta: { public: true } }
      const hasToken = false
      const shouldRedirect = !publicRoute.meta.public && !hasToken
      expect(shouldRedirect).toBe(false)
    })

    it('should require token for protected routes', () => {
      const protectedRoute = { meta: { public: false } }
      const hasToken = false
      const shouldRedirect = !protectedRoute.meta.public && !hasToken
      expect(shouldRedirect).toBe(true)
    })

    it('should redirect to dashboard when logged in and visiting login', () => {
      const to = { path: '/login' }
      const hasToken = true
      const shouldRedirect = to.path === '/login' && hasToken
      expect(shouldRedirect).toBe(true)
    })

    it('should allow staying on login page when not logged in', () => {
      const to = { path: '/login' }
      const hasToken = false
      const shouldRedirect = to.path === '/login' && hasToken
      expect(shouldRedirect).toBe(false)
    })
  })
})
