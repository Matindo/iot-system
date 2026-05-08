<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <span class="logo">▲</span>
        <span>Afridata IoT Cloud</span>
      </div>
      <h2>Welcome back</h2>
      <form @submit.prevent="submit">
        <div class="field">
          <label>Email</label>
          <input v-model="email" type="email" placeholder="you@example.com" required autocomplete="email" />
        </div>
        <div class="field">
          <label>Password</label>
          <input v-model="password" type="password" placeholder="••••••••" required autocomplete="current-password" />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>
      <p class="switch">No account? <router-link to="/register">Create one</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../store/auth.js'
import { useProjectStore } from '../../store/project.js'

const auth = useAuthStore()
const projectStore = useProjectStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    const projects = await projectStore.fetchProjects()
    router.push(projects.length === 0 ? '/onboarding' : '/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Invalid email or password'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.auth-card {
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1a73e8;
  font-weight: 700;
  font-size: 1rem;
  margin-bottom: 24px;
}
.logo { font-size: 1.4rem; }
h2 { font-size: 1.75rem; color: #1a1a2e; margin-bottom: 28px; }
form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
label { font-size: 0.875rem; font-weight: 500; color: #555; }
input {
  padding: 12px 14px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}
input:focus { border-color: #1a73e8; }
button {
  margin-top: 4px;
  padding: 13px;
  background: #1a73e8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
button:hover:not(:disabled) { background: #1558b0; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.error {
  background: #fce8e6;
  color: #c5221f;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 0.875rem;
}
.switch { margin-top: 20px; text-align: center; color: #888; font-size: 0.9rem; }
.switch a { color: #1a73e8; text-decoration: none; font-weight: 500; }
</style>
