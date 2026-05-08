import api from '../api/client.js'

export default {
  namespaced: true,

  state: () => ({
    projects: [],
    current: null,
  }),

  mutations: {
    SET_PROJECTS(state, projects) { state.projects = projects },
    SET_CURRENT(state, project) { state.current = project },
    ADD_PROJECT(state, project) { state.projects.push(project) },
    UPDATE_PROJECT(state, updated) {
      const idx = state.projects.findIndex((p) => p.id === updated.id)
      if (idx !== -1) state.projects.splice(idx, 1, updated)
      if (state.current?.id === updated.id) state.current = updated
    },
    REMOVE_PROJECT(state, id) {
      state.projects = state.projects.filter((p) => p.id !== id)
      if (state.current?.id === id) state.current = state.projects[0] || null
    },
  },

  actions: {
    async fetchProjects({ commit, state }) {
      const { data } = await api.get('/api/v1/projects')
      commit('SET_PROJECTS', data)
      if (!state.current && data.length > 0) commit('SET_CURRENT', data[0])
      return data
    },

    select({ commit }, project) {
      commit('SET_CURRENT', project)
    },

    async createProject({ commit }, payload) {
      const { data } = await api.post('/api/v1/projects', payload)
      commit('ADD_PROJECT', data)
      commit('SET_CURRENT', data)
      return data
    },

    async updateProject({ commit }, { id, payload }) {
      const { data } = await api.put(`/api/v1/projects/${id}`, payload)
      commit('UPDATE_PROJECT', data)
      return data
    },

    async deleteProject({ commit }, id) {
      await api.delete(`/api/v1/projects/${id}`)
      commit('REMOVE_PROJECT', id)
    },
  },
}
