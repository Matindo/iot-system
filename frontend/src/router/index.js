import { createRouter, createWebHistory } from 'vue-router'
import store from '../store/index.js'

const routes = [
  {
    path: '/',
    component: () => import('../views/landing/LandingView.vue'),
  },
  {
    path: '/about',
    component: () => import('../views/about/AboutView.vue'),
  },
  {
    path: '/terms',
    component: () => import('../views/legal/TermsView.vue'),
  },
  {
    path: '/privacy',
    component: () => import('../views/legal/PrivacyView.vue'),
  },
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
  const isAuthenticated = store.getters['auth/isAuthenticated']
  const isAdmin = store.getters['auth/isAdmin']
  if (to.meta.requiresAuth && !isAuthenticated) return next('/login')
  if (to.meta.requiresAdmin && !isAdmin) return next('/dashboard')
  if (to.meta.guest && isAuthenticated) return next('/dashboard')
  next()
})

export default router
