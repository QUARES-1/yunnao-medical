import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/doctor/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../views/doctor/LayoutView.vue'),
    meta: { requiresAuth: true },
    redirect: '/workbench',
    children: [
      {
        path: 'workbench',
        name: 'Workbench',
        component: () => import('../views/doctor/WorkbenchView.vue'),
        meta: { title: '工作台', icon: 'Monitor' }
      },
      {
        path: 'consultation/:regId',
        name: 'Consultation',
        component: () => import('../views/doctor/ConsultationView.vue'),
        meta: { title: '看诊' }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('../views/doctor/HistoryView.vue'),
        meta: { title: '历史记录', icon: 'Document' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/doctor/ProfileView.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转到登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
