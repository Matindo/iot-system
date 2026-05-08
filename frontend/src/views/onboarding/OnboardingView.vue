<template>
  <div class="onboarding-page">
    <div class="onboarding-card">
      <div class="step-indicator">Step 1 of 1</div>
      <h1>Create your first project</h1>
      <p class="subtitle">Projects group your IoT devices and their data. You can create more later.</p>

      <form @submit.prevent="submit">
        <div class="field">
          <label>Project name *</label>
          <input v-model="form.name" type="text" placeholder="e.g. Nairobi Greenhouse" required />
        </div>
        <div class="field">
          <label>Description</label>
          <input v-model="form.description" type="text" placeholder="Optional description" />
        </div>
        <div class="row">
          <div class="field">
            <label>Environment</label>
            <select v-model="form.environment">
              <option value="production">Production</option>
              <option value="staging">Staging</option>
              <option value="development">Development</option>
            </select>
          </div>
          <div class="field">
            <label>Region</label>
            <select v-model="form.region">
              <option value="af-ke-1">Africa — Nairobi (af-ke-1)</option>
            </select>
          </div>
        </div>
        <div class="row">
          <div class="field">
            <label>Expected devices</label>
            <input v-model.number="form.expectedDeviceCount" type="number" min="1" />
          </div>
          <div class="field">
            <label>Send interval (seconds)</label>
            <input v-model.number="form.declaredSendIntervalS" type="number" min="1" placeholder="e.g. 60" />
          </div>
        </div>
        <div class="field">
          <label>Alert email</label>
          <input v-model="form.alertEmail" type="email" placeholder="alerts@yourdomain.com" />
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Creating…' : 'Create project & continue' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '../../store/project.js'

const projectStore = useProjectStore()
const router = useRouter()

const error = ref('')
const loading = ref(false)
const form = ref({
  name: '',
  description: '',
  environment: 'production',
  region: 'af-ke-1',
  timezone: 'Africa/Nairobi',
  expectedDeviceCount: 1,
  declaredSendIntervalS: null,
  alertEmail: '',
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await projectStore.createProject(form.value)
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to create project'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: #f0f2f5;
}
.onboarding-card {
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
}
.step-indicator {
  font-size: 0.8rem;
  color: #1a73e8;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
h1 { font-size: 1.75rem; color: #1a1a2e; margin-bottom: 8px; }
.subtitle { color: #666; margin-bottom: 32px; }
form { display: flex; flex-direction: column; gap: 20px; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
label { font-size: 0.875rem; font-weight: 500; color: #555; }
input, select {
  padding: 11px 14px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
  background: white;
}
input:focus, select:focus { border-color: #1a73e8; }
button {
  padding: 14px;
  background: #1a73e8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  margin-top: 8px;
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
</style>
