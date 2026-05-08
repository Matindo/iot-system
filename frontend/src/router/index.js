import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth.js'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    component: () => import('../views/auth/RegisterView.vue'),
    meta: { guest: true },
  },
  {
    path: '/onboarding',
    component: () => import('../views/onboarding/OnboardingView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/dashboard',
    component: () => import('../views/dashboard/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/projects/:id',
    component: () => import('../views/project/ProjectView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) return next('/login')
  if (to.meta.requiresAdmin && !auth.isAdmin) return next('/dashboard')
  if (to.meta.guest && auth.isAuthenticated) return next('/dashboard')
  next()
})

export default router
