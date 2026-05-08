import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/client.js'

export const useProjectStore = defineStore('project', () => {
  const projects = ref([])
  const current = ref(null)

  async function fetchProjects() {
    const { data } = await api.get('/api/v1/projects')
    projects.value = data
    if (!current.value && data.length > 0) {
      current.value = data[0]
    }
    return data
  }

  function select(project) {
    current.value = project
  }

  async function createProject(payload) {
    const { data } = await api.post('/api/v1/projects', payload)
    projects.value.push(data)
    current.value = data
    return data
  }

  async function updateProject(id, payload) {
    const { data } = await api.put(`/api/v1/projects/${id}`, payload)
    const idx = projects.value.findIndex(p => p.id === id)
    if (idx !== -1) projects.value[idx] = data
    if (current.value?.id === id) current.value = data
    return data
  }

  async function deleteProject(id) {
    await api.delete(`/api/v1/projects/${id}`)
    projects.value = projects.value.filter(p => p.id !== id)
    if (current.value?.id === id) {
      current.value = projects.value[0] || null
    }
  }

  return { projects, current, fetchProjects, select, createProject, updateProject, deleteProject }
})
