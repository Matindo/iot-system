import { describe, it, expect } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

const stub = { template: '<div />' }

const routes = [
  { path: '/',           component: stub },
  { path: '/about',      component: stub },
  { path: '/login',      component: stub, meta: { guest: true } },
  { path: '/register',   component: stub, meta: { guest: true } },
  { path: '/dashboard',  component: stub, meta: { requiresAuth: true } },
  { path: '/onboarding', component: stub, meta: { requiresAuth: true } },
  { path: '/admin',      component: stub, meta: { requiresAuth: true, requiresAdmin: true } },
]

const makeRouter = ({ isAuthenticated = false, isAdmin = false } = {}) => {
  const router = createRouter({ history: createMemoryHistory(), routes })
  router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth && !isAuthenticated) return next('/login')
    if (to.meta.requiresAdmin && !isAdmin) return next('/dashboard')
    if (to.meta.guest && isAuthenticated) return next('/dashboard')
    next()
  })
  return router
}

describe('router guards — unauthenticated user', () => {
  it('redirects /dashboard to /login', async () => {
    const router = makeRouter()
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects /onboarding to /login', async () => {
    const router = makeRouter()
    await router.push('/onboarding')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects /admin to /login', async () => {
    const router = makeRouter()
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows access to /login', async () => {
    const router = makeRouter()
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows access to /register', async () => {
    const router = makeRouter()
    await router.push('/register')
    expect(router.currentRoute.value.path).toBe('/register')
  })

  it('allows access to / (public)', async () => {
    const router = makeRouter()
    await router.push('/')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('allows access to /about (public)', async () => {
    const router = makeRouter()
    await router.push('/about')
    expect(router.currentRoute.value.path).toBe('/about')
  })
})

describe('router guards — authenticated non-admin user', () => {
  it('allows access to /dashboard', async () => {
    const router = makeRouter({ isAuthenticated: true })
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('allows access to /onboarding', async () => {
    const router = makeRouter({ isAuthenticated: true })
    await router.push('/onboarding')
    expect(router.currentRoute.value.path).toBe('/onboarding')
  })

  it('redirects /login to /dashboard (guest guard)', async () => {
    const router = makeRouter({ isAuthenticated: true })
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('redirects /register to /dashboard (guest guard)', async () => {
    const router = makeRouter({ isAuthenticated: true })
    await router.push('/register')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('redirects /admin to /dashboard (not admin)', async () => {
    const router = makeRouter({ isAuthenticated: true, isAdmin: false })
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })
})

describe('router guards — admin user', () => {
  it('allows access to /admin', async () => {
    const router = makeRouter({ isAuthenticated: true, isAdmin: true })
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/admin')
  })

  it('allows access to /dashboard', async () => {
    const router = makeRouter({ isAuthenticated: true, isAdmin: true })
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('still redirected from /login (guest guard applies to admins too)', async () => {
    const router = makeRouter({ isAuthenticated: true, isAdmin: true })
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })
})
