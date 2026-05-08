import api from '../api/client.js'

export default {
  namespaced: true,

  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    accessToken: localStorage.getItem('accessToken') || null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    isAdmin: (state) => state.user?.role === 'ADMIN',
  },

  mutations: {
    SET_USER(state, user) { state.user = user },
    SET_TOKEN(state, token) { state.accessToken = token },
    CLEAR(state) { state.user = null; state.accessToken = null },
  },

  actions: {
    async login({ commit, dispatch }, { email, password }) {
      const { data } = await api.post('/api/v1/auth/login', { email, password })
      commit('SET_TOKEN', data.accessToken)
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await dispatch('fetchMe')
    },

    async register({ commit, dispatch }, { email, password }) {
      const { data } = await api.post('/api/v1/auth/register', { email, password })
      commit('SET_TOKEN', data.accessToken)
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await dispatch('fetchMe')
    },

    async fetchMe({ commit }) {
      const { data } = await api.get('/api/v1/auth/me')
      commit('SET_USER', data)
      localStorage.setItem('user', JSON.stringify(data))
    },

    logout({ commit }) {
      localStorage.clear()
      commit('CLEAR')
    },
  },
}
