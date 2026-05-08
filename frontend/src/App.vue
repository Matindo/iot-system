<template>
  <NavBar v-if="isAuthenticated" />
  <main :class="{ 'with-nav': isAuthenticated }">
    <router-view />
  </main>
</template>

<script>
import { mapState, mapActions } from 'pinia'
import { useAuthStore } from './store/auth.js'
import { useProjectStore } from './store/project.js'
import NavBar from './components/ui/NavBar.vue'

export default {
  name: 'App',
  components: { NavBar },

  computed: {
    ...mapState(useAuthStore, ['isAuthenticated']),
  },

  async mounted() {
    const projectStore = useProjectStore()
    if (this.isAuthenticated && projectStore.projects.length === 0) {
      await projectStore.fetchProjects().catch(() => {})
    }
  },
}
</script>

<style>
main { min-height: 100vh; }
main.with-nav { padding-top: 60px; }
</style>
