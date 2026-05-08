<template>
  <NavBar v-if="auth.isAuthenticated" />
  <main :class="{ 'with-nav': auth.isAuthenticated }">
    <router-view />
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './store/auth.js'
import { useProjectStore } from './store/project.js'
import NavBar from './components/ui/NavBar.vue'

const auth = useAuthStore()
const projectStore = useProjectStore()

onMounted(async () => {
  if (auth.isAuthenticated && projectStore.projects.length === 0) {
    await projectStore.fetchProjects().catch(() => {})
  }
})
</script>

<style>
main { min-height: 100vh; }
main.with-nav { padding-top: 60px; }
</style>
