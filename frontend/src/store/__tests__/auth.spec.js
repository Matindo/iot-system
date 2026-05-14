import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createStore } from 'vuex'
import authModule from '../auth.js'

// Mock the API client
vi.mock('../../api/client.js', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

import api from '../../api/client.js'

const makeStore = () =>
  createStore({ modules: { auth: { ...authModule, namespaced: true } } })

describe('auth store — getters', () => {
  it('isAuthenticated is false when no token', () => {
    const store = makeStore()
    expect(store.getters['auth/isAuthenticated']).toBe(false)
  })

  it('isAuthenticated is true when token is set', () => {
    const store = makeStore()
    store.commit('auth/SET_TOKEN', 'tok123')
    expect(store.getters['auth/isAuthenticated']).toBe(true)
  })

  it('isAdmin is false for regular user', () => {
    const store = makeStore()
    store.commit('auth/SET_USER', { role: 'USER' })
    expect(store.getters['auth/isAdmin']).toBe(false)
  })

  it('isAdmin is true for ADMIN role', () => {
    const store = makeStore()
    store.commit('auth/SET_USER', { role: 'ADMIN' })
    expect(store.getters['auth/isAdmin']).toBe(true)
  })

  it('isAdmin is false when no user', () => {
    const store = makeStore()
    expect(store.getters['auth/isAdmin']).toBe(false)
  })
})

describe('auth store — mutations', () => {
  it('SET_TOKEN updates accessToken in state', () => {
    const store = makeStore()
    store.commit('auth/SET_TOKEN', 'abc')
    expect(store.state.auth.accessToken).toBe('abc')
  })

  it('SET_USER updates user in state', () => {
    const store = makeStore()
    store.commit('auth/SET_USER', { email: 'a@b.com' })
    expect(store.state.auth.user.email).toBe('a@b.com')
  })

  it('CLEAR resets both user and accessToken', () => {
    const store = makeStore()
    store.commit('auth/SET_TOKEN', 'tok')
    store.commit('auth/SET_USER', { email: 'x@y.com' })
    store.commit('auth/CLEAR')
    expect(store.state.auth.accessToken).toBeNull()
    expect(store.state.auth.user).toBeNull()
  })
})

describe('auth store — actions', () => {
  beforeEach(() => vi.clearAllMocks())

  it('login commits tokens and persists to localStorage', async () => {
    api.post.mockResolvedValueOnce({ data: { accessToken: 'at', refreshToken: 'rt' } })
    api.get.mockResolvedValueOnce({ data: { email: 'u@ioteca.io', role: 'USER' } })

    const store = makeStore()
    await store.dispatch('auth/login', { email: 'u@ioteca.io', password: 'pw' })

    expect(store.state.auth.accessToken).toBe('at')
    expect(localStorage.getItem('accessToken')).toBe('at')
    expect(localStorage.getItem('refreshToken')).toBe('rt')
  })

  it('login calls POST /api/v1/auth/login', async () => {
    api.post.mockResolvedValueOnce({ data: { accessToken: 'at', refreshToken: 'rt' } })
    api.get.mockResolvedValueOnce({ data: { email: 'u@ioteca.io' } })

    const store = makeStore()
    await store.dispatch('auth/login', { email: 'u@ioteca.io', password: 'pw' })

    expect(api.post).toHaveBeenCalledWith('/api/v1/auth/login', {
      email: 'u@ioteca.io',
      password: 'pw',
    })
  })

  it('login dispatches fetchMe after setting token', async () => {
    api.post.mockResolvedValueOnce({ data: { accessToken: 'at', refreshToken: 'rt' } })
    api.get.mockResolvedValueOnce({ data: { email: 'u@ioteca.io', role: 'USER' } })

    const store = makeStore()
    await store.dispatch('auth/login', { email: 'u@ioteca.io', password: 'pw' })

    expect(api.get).toHaveBeenCalledWith('/api/v1/auth/me')
    expect(store.state.auth.user).toMatchObject({ email: 'u@ioteca.io' })
  })

  it('register calls POST /api/v1/auth/register', async () => {
    api.post.mockResolvedValueOnce({ data: { accessToken: 'at', refreshToken: 'rt' } })
    api.get.mockResolvedValueOnce({ data: { email: 'new@ioteca.io' } })

    const store = makeStore()
    await store.dispatch('auth/register', { email: 'new@ioteca.io', password: 'pw' })

    expect(api.post).toHaveBeenCalledWith('/api/v1/auth/register', {
      email: 'new@ioteca.io',
      password: 'pw',
    })
  })

  it('logout clears state and localStorage', () => {
    localStorage.setItem('accessToken', 'tok')
    localStorage.setItem('user', '{"email":"x@y.com"}')

    const store = makeStore()
    store.commit('auth/SET_TOKEN', 'tok')
    store.commit('auth/SET_USER', { email: 'x@y.com' })

    store.dispatch('auth/logout')

    expect(store.state.auth.accessToken).toBeNull()
    expect(store.state.auth.user).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
  })

  it('fetchMe stores user in state and localStorage', async () => {
    const user = { email: 'me@ioteca.io', role: 'USER' }
    api.get.mockResolvedValueOnce({ data: user })

    const store = makeStore()
    await store.dispatch('auth/fetchMe')

    expect(store.state.auth.user).toEqual(user)
    expect(JSON.parse(localStorage.getItem('user'))).toEqual(user)
  })
})
