import { defineStore } from 'pinia'
import api from '../api/client.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    accessToken: localStorage.getItem('accessToken') || null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    isAdmin: (state) => state.user?.role === 'ADMIN',
  },

  actions: {
    async login(email, password) {
      const { data } = await api.post('/api/v1/auth/login', { email, password })
      this._persist(data)
      await this.fetchMe()
    },

    async register(email, password) {
      const { data } = await api.post('/api/v1/auth/register', { email, password })
      this._persist(data)
      await this.fetchMe()
    },

    async fetchMe() {
      const { data } = await api.get('/api/v1/auth/me')
      this.user = data
      localStorage.setItem('user', JSON.stringify(data))
    },

    logout() {
      localStorage.clear()
      this.user = null
      this.accessToken = null
    },

    _persist(data) {
      this.accessToken = data.accessToken
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
    },
  },
})
