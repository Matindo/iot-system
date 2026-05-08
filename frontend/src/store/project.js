import { defineStore } from 'pinia'
import api from '../api/client.js'

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [],
    current: null,
  }),

  actions: {
    async fetchProjects() {
      const { data } = await api.get('/api/v1/projects')
      this.projects = data
      if (!this.current && data.length > 0) {
        this.current = data[0]
      }
      return data
    },

    select(project) {
      this.current = project
    },

    async createProject(payload) {
      const { data } = await api.post('/api/v1/projects', payload)
      this.projects.push(data)
      this.current = data
      return data
    },

    async updateProject(id, payload) {
      const { data } = await api.put(`/api/v1/projects/${id}`, payload)
      const idx = this.projects.findIndex((p) => p.id === id)
      if (idx !== -1) this.projects[idx] = data
      if (this.current?.id === id) this.current = data
      return data
    },

    async deleteProject(id) {
      await api.delete(`/api/v1/projects/${id}`)
      this.projects = this.projects.filter((p) => p.id !== id)
      if (this.current?.id === id) {
        this.current = this.projects[0] || null
      }
    },
  },
})
