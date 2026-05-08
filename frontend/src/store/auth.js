import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/client.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const accessToken = ref(localStorage.getItem('accessToken') || null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(email, password) {
    const { data } = await api.post('/api/v1/auth/login', { email, password })
    _persist(data)
    await fetchMe()
  }

  async function register(email, password) {
    const { data } = await api.post('/api/v1/auth/register', { email, password })
    _persist(data)
    await fetchMe()
  }

  async function fetchMe() {
    const { data } = await api.get('/api/v1/auth/me')
    user.value = data
    localStorage.setItem('user', JSON.stringify(data))
  }

  function logout() {
    localStorage.clear()
    user.value = null
    accessToken.value = null
  }

  function _persist(data) {
    accessToken.value = data.accessToken
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
  }

  return { user, accessToken, isAuthenticated, isAdmin, login, register, logout, fetchMe }
})
