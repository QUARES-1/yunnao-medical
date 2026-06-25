import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    {
      path: '/', component: () => import('@/layouts/AdminLayout.vue'), redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '数据概览' } },
        { path: 'departments', component: () => import('@/views/DepartmentView.vue'), meta: { title: '科室管理' } },
        { path: 'doctors', component: () => import('@/views/DoctorView.vue'), meta: { title: '医生管理' } },
        { path: 'medicines', component: () => import('@/views/MedicineView.vue'), meta: { title: '药品管理' } },
        { path: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人中心' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach(to => {
  const token = localStorage.getItem('admin_token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
})

export default router
