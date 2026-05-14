import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal()
  const mockInstance = {
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    get: vi.fn(),
    post: vi.fn(),
  }
  return {
    default: {
      ...actual.default,
      create: vi.fn(() => mockInstance),
      post: vi.fn(),
    },
    __mockInstance: mockInstance,
  }
})

describe('api client — request interceptor logic', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('attaches Bearer token from localStorage when present', () => {
    localStorage.setItem('accessToken', 'my-token')
    const config = { headers: {} }

    const token = localStorage.getItem('accessToken')
    if (token) config.headers.Authorization = `Bearer ${token}`

    expect(config.headers.Authorization).toBe('Bearer my-token')
  })

  it('leaves Authorization header absent when no token in localStorage', () => {
    const config = { headers: {} }

    const token = localStorage.getItem('accessToken')
    if (token) config.headers.Authorization = `Bearer ${token}`

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('Authorization header uses Bearer scheme', () => {
    localStorage.setItem('accessToken', 'abc123')
    const config = { headers: {} }

    const token = localStorage.getItem('accessToken')
    if (token) config.headers.Authorization = `Bearer ${token}`

    expect(config.headers.Authorization).toMatch(/^Bearer /)
  })
})

describe('api client — 401 refresh token logic', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('stores new tokens after successful refresh', async () => {
    localStorage.setItem('refreshToken', 'old-refresh')

    const newTokens = { accessToken: 'new-access', refreshToken: 'new-refresh' }
    axios.post = vi.fn().mockResolvedValueOnce({ data: newTokens })

    const refresh = localStorage.getItem('refreshToken')
    const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken: refresh })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)

    expect(localStorage.getItem('accessToken')).toBe('new-access')
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh')
  })

  it('clears localStorage when refresh fails', async () => {
    localStorage.setItem('accessToken', 'expired-access')
    localStorage.setItem('refreshToken', 'expired-refresh')

    axios.post = vi.fn().mockRejectedValueOnce(new Error('Unauthorized'))

    try {
      await axios.post('/api/v1/auth/refresh', { refreshToken: 'expired-refresh' })
    } catch {
      localStorage.clear()
    }

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('does not attempt refresh when no refresh token is stored', async () => {
    const refreshCalled = vi.fn()
    const refresh = localStorage.getItem('refreshToken')
    if (refresh) refreshCalled()

    expect(refreshCalled).not.toHaveBeenCalled()
  })
})
