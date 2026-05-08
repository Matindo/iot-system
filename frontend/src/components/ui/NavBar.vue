<template>
  <nav class="navbar">
    <div class="nav-left">
      <span class="brand">Afridata</span>
      <select v-if="projectStore.projects.length" class="project-select"
              :value="projectStore.current?.id"
              @change="selectProject">
        <option v-for="p in projectStore.projects" :key="p.id" :value="p.id">
          {{ p.name }}
        </option>
      </select>
    </div>

    <div class="nav-links">
      <router-link to="/dashboard">Dashboard</router-link>
      <router-link v-if="projectStore.current" :to="`/projects/${projectStore.current.id}`">
        Settings
      </router-link>
      <router-link v-if="auth.isAdmin" to="/admin">Admin</router-link>
    </div>

    <div class="nav-right">
      <span class="user-email">{{ auth.user?.email }}</span>
      <button class="logout-btn" @click="handleLogout">Logout</button>
    </div>
  </nav>
</template>

<script setup>
import { useAuthStore } from '../../store/auth.js'
import { useProjectStore } from '../../store/project.js'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const projectStore = useProjectStore()
const router = useRouter()

function selectProject(e) {
  const project = projectStore.projects.find(p => p.id === e.target.value)
  if (project) projectStore.select(project)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 60px;
  background: #1a1a2e;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 24px;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
.nav-left { display: flex; align-items: center; gap: 16px; flex: 1; }
.brand { color: #4fc3f7; font-weight: 700; font-size: 1.1rem; letter-spacing: 0.5px; }
.project-select {
  background: #16213e;
  color: #e0e0e0;
  border: 1px solid #333;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 0.875rem;
  cursor: pointer;
  max-width: 200px;
}
.nav-links { display: flex; gap: 4px; }
.nav-links a {
  color: #aaa;
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: all 0.15s;
}
.nav-links a:hover, .nav-links a.router-link-active { color: white; background: rgba(255,255,255,0.1); }
.nav-right { display: flex; align-items: center; gap: 12px; }
.user-email { color: #888; font-size: 0.8rem; }
.logout-btn {
  background: transparent;
  color: #aaa;
  border: 1px solid #444;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s;
}
.logout-btn:hover { background: rgba(255,255,255,0.1); color: white; }
</style>
