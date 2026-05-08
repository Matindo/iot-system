<template>
  <NavBar v-if="isAuthenticated" />
  <main :class="{ 'with-nav': isAuthenticated }">
    <router-view />
  </main>
</template>

<script>
import { mapGetters } from 'vuex'
import NavBar from './components/ui/NavBar.vue'

export default {
  name: 'App',
  components: { NavBar },

  computed: {
    ...mapGetters('auth', ['isAuthenticated']),
  },

  async mounted() {
    if (this.isAuthenticated && this.$store.state.project.projects.length === 0) {
      await this.$store.dispatch('project/fetchProjects').catch(() => {})
    }
  },
}
</script>

<style>
main { min-height: 100vh; }
main.with-nav { padding-top: 60px; }
</style>
