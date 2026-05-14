import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createStore } from 'vuex'
import projectModule from '../project.js'

vi.mock('../../api/client.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import api from '../../api/client.js'

const makeStore = () =>
  createStore({ modules: { project: { ...projectModule, namespaced: true } } })

const proj = (id, name) => ({ id, name, isActive: true })

describe('project store — mutations', () => {
  it('SET_PROJECTS replaces the list', () => {
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'A'), proj('2', 'B')])
    expect(store.state.project.projects).toHaveLength(2)
  })

  it('SET_CURRENT sets the current project', () => {
    const store = makeStore()
    store.commit('project/SET_CURRENT', proj('1', 'A'))
    expect(store.state.project.current.id).toBe('1')
  })

  it('ADD_PROJECT appends to the list', () => {
    const store = makeStore()
    store.commit('project/ADD_PROJECT', proj('1', 'First'))
    store.commit('project/ADD_PROJECT', proj('2', 'Second'))
    expect(store.state.project.projects).toHaveLength(2)
  })

  it('UPDATE_PROJECT replaces by id', () => {
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'Old Name')])
    store.commit('project/UPDATE_PROJECT', proj('1', 'New Name'))
    expect(store.state.project.projects[0].name).toBe('New Name')
  })

  it('UPDATE_PROJECT also updates current if it matches', () => {
    const store = makeStore()
    store.commit('project/SET_CURRENT', proj('1', 'Old'))
    store.commit('project/UPDATE_PROJECT', proj('1', 'Updated'))
    expect(store.state.project.current.name).toBe('Updated')
  })

  it('UPDATE_PROJECT does not touch current if ids differ', () => {
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'A'), proj('2', 'B')])
    store.commit('project/SET_CURRENT', proj('1', 'A'))
    store.commit('project/UPDATE_PROJECT', proj('2', 'B Updated'))
    expect(store.state.project.current.name).toBe('A')
  })

  it('REMOVE_PROJECT removes from list', () => {
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'A'), proj('2', 'B')])
    store.commit('project/REMOVE_PROJECT', '1')
    expect(store.state.project.projects.map((p) => p.id)).toEqual(['2'])
  })

  it('REMOVE_PROJECT clears current when removed project was current', () => {
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'A')])
    store.commit('project/SET_CURRENT', proj('1', 'A'))
    store.commit('project/REMOVE_PROJECT', '1')
    expect(store.state.project.current).toBeNull()
  })

  it('REMOVE_PROJECT sets current to first remaining if current was removed', () => {
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'A'), proj('2', 'B')])
    store.commit('project/SET_CURRENT', proj('1', 'A'))
    store.commit('project/REMOVE_PROJECT', '1')
    expect(store.state.project.current?.id).toBe('2')
  })
})

describe('project store — actions', () => {
  beforeEach(() => vi.clearAllMocks())

  it('fetchProjects calls GET /api/v1/projects', async () => {
    api.get.mockResolvedValueOnce({ data: [proj('1', 'A')] })
    const store = makeStore()
    await store.dispatch('project/fetchProjects')
    expect(api.get).toHaveBeenCalledWith('/api/v1/projects')
  })

  it('fetchProjects sets current to first project when none selected', async () => {
    api.get.mockResolvedValueOnce({ data: [proj('1', 'First'), proj('2', 'Second')] })
    const store = makeStore()
    await store.dispatch('project/fetchProjects')
    expect(store.state.project.current?.id).toBe('1')
  })

  it('fetchProjects does not override existing current', async () => {
    api.get.mockResolvedValueOnce({ data: [proj('1', 'A'), proj('2', 'B')] })
    const store = makeStore()
    store.commit('project/SET_CURRENT', proj('2', 'B'))
    await store.dispatch('project/fetchProjects')
    expect(store.state.project.current?.id).toBe('2')
  })

  it('createProject calls POST and adds project to list', async () => {
    const newProj = proj('99', 'New')
    api.post.mockResolvedValueOnce({ data: newProj })
    const store = makeStore()
    const result = await store.dispatch('project/createProject', { name: 'New' })
    expect(api.post).toHaveBeenCalledWith('/api/v1/projects', { name: 'New' })
    expect(result).toEqual(newProj)
    expect(store.state.project.projects).toContainEqual(newProj)
    expect(store.state.project.current).toEqual(newProj)
  })

  it('updateProject calls PUT and updates list', async () => {
    const updated = proj('1', 'Renamed')
    api.put.mockResolvedValueOnce({ data: updated })
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'Original')])
    await store.dispatch('project/updateProject', { id: '1', payload: { name: 'Renamed' } })
    expect(api.put).toHaveBeenCalledWith('/api/v1/projects/1', { name: 'Renamed' })
    expect(store.state.project.projects[0].name).toBe('Renamed')
  })

  it('deleteProject calls DELETE and removes project', async () => {
    api.delete.mockResolvedValueOnce({})
    const store = makeStore()
    store.commit('project/SET_PROJECTS', [proj('1', 'A')])
    await store.dispatch('project/deleteProject', '1')
    expect(api.delete).toHaveBeenCalledWith('/api/v1/projects/1')
    expect(store.state.project.projects).toHaveLength(0)
  })

  it('select action sets current project', () => {
    const store = makeStore()
    store.dispatch('project/select', proj('5', 'Selected'))
    expect(store.state.project.current?.id).toBe('5')
  })
})
