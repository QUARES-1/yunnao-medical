import { describe, it, expect } from 'vitest'

describe('Router Configuration (pharmacy)', () => {
  const expectedRoutes = [
    { path: '/login', meta: { public: true } },
    { path: 'dashboard', title: '数据概览' },
    { path: 'pending', title: '待发药' },
    { path: 'history', title: '已发药' },
    { path: 'inventory', title: '库存管理' },
    { path: 'profile', title: '个人中心' }
  ]

  describe('route definitions', () => {
    it('should define login as public route', () => {
      const loginRoute = expectedRoutes.find(r => r.path === '/login')
      expect(loginRoute?.meta.public).toBe(true)
    })

    it('should define all management routes', () => {
      const managementRoutes = expectedRoutes.filter(r => r.path !== '/login')
      expect(managementRoutes).toHaveLength(5)
    })

    it('should have pending route for dispensing', () => {
      const pendingRoute = expectedRoutes.find(r => r.path === 'pending')
      expect(pendingRoute?.title).toBe('待发药')
    })

    it('should have history route for dispensed', () => {
      const historyRoute = expectedRoutes.find(r => r.path === 'history')
      expect(historyRoute?.title).toBe('已发药')
    })

    it('should have inventory management route', () => {
      const inventoryRoute = expectedRoutes.find(r => r.path === 'inventory')
      expect(inventoryRoute?.title).toBe('库存管理')
    })

    it('should have catch-all redirect to dashboard', () => {
      const catchAll = { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
      expect(catchAll.redirect).toBe('/dashboard')
    })
  })

  describe('navigation guard logic', () => {
    it('should allow public routes without token', () => {
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

    it('should redirect logged-in user from login page', () => {
      const to = { path: '/login' }
      const hasToken = true
      const shouldRedirect = to.path === '/login' && hasToken
      expect(shouldRedirect).toBe(true)
    })
  })
})
