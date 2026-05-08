<template>
  <div class="project-page">
    <div v-if="!project" class="loading">Loading…</div>

    <template v-else>
      <div class="page-header">
        <div>
          <router-link to="/dashboard" class="back-link">← Dashboard</router-link>
          <h1>{{ project.name }}</h1>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button v-for="tab in tabs" :key="tab.id"
                :class="{ active: activeTab === tab.id }"
                @click="activeTab = tab.id">
          {{ tab.label }}
        </button>
      </div>

      <!-- Settings tab -->
      <div v-if="activeTab === 'settings'" class="card">
        <h2>Project settings</h2>
        <form @submit.prevent="saveSettings">
          <div class="row">
            <div class="field">
              <label>Name *</label>
              <input v-model="settingsForm.name" type="text" required />
            </div>
            <div class="field">
              <label>Description</label>
              <input v-model="settingsForm.description" type="text" />
            </div>
          </div>
          <div class="row">
            <div class="field">
              <label>Alert email</label>
              <input v-model="settingsForm.alertEmail" type="email" />
            </div>
            <div class="field">
              <label>Alert webhook URL</label>
              <input v-model="settingsForm.alertWebhookUrl" type="url" placeholder="https://…" />
            </div>
          </div>
          <p v-if="settingsMsg" class="success">{{ settingsMsg }}</p>
          <div class="form-actions">
            <button type="submit" :disabled="savingSettings">
              {{ savingSettings ? 'Saving…' : 'Save changes' }}
            </button>
          </div>
        </form>
      </div>

      <!-- API Keys tab -->
      <div v-if="activeTab === 'keys'">
        <div class="card">
          <div class="section-header">
            <h2>API Keys</h2>
            <button class="btn-secondary" @click="showCreateKey = !showCreateKey">
              + New key
            </button>
          </div>

          <form v-if="showCreateKey" class="inline-form" @submit.prevent="createKey">
            <input v-model="newKey.name" placeholder="Key name" required />
            <select v-model="newKey.environment">
              <option value="live">Live</option>
              <option value="test">Test</option>
            </select>
            <button type="submit" :disabled="creatingKey">Create</button>
            <button type="button" class="btn-ghost" @click="showCreateKey = false">Cancel</button>
          </form>

          <!-- Newly created key — show full key once -->
          <div v-if="createdKey" class="key-reveal">
            <p><strong>Copy your key — it won't be shown again:</strong></p>
            <div class="key-value">
              <code>{{ createdKey.fullKey }}</code>
              <button type="button" @click="copyKey(createdKey.fullKey)">Copy</button>
            </div>
            <button type="button" class="btn-ghost" @click="createdKey = null">Dismiss</button>
          </div>

          <table class="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Prefix</th>
                <th>Environment</th>
                <th>Created</th>
                <th>Expires</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="apiKeys.length === 0">
                <td colspan="6" class="empty-row">No API keys yet.</td>
              </tr>
              <tr v-for="key in apiKeys" :key="key.id">
                <td>{{ key.name }}</td>
                <td class="mono">{{ key.keyPrefix }}…</td>
                <td><span class="badge">{{ key.environment }}</span></td>
                <td>{{ formatDate(key.createdAt) }}</td>
                <td>{{ key.expiresAt ? formatDate(key.expiresAt) : '—' }}</td>
                <td>
                  <button class="btn-danger-sm" @click="revokeKey(key.id)">Revoke</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Alert Rules tab -->
      <div v-if="activeTab === 'alerts'">
        <div class="card">
          <div class="section-header">
            <h2>Alert Rules</h2>
            <button class="btn-secondary" @click="showCreateRule = !showCreateRule">
              + New rule
            </button>
          </div>

          <form v-if="showCreateRule" class="alert-form" @submit.prevent="createRule">
            <div class="row">
              <div class="field">
                <label>Rule name *</label>
                <input v-model="newRule.name" required />
              </div>
              <div class="field">
                <label>Metric *</label>
                <input v-model="newRule.metricName" placeholder="e.g. temperature" required />
              </div>
            </div>
            <div class="row">
              <div class="field">
                <label>Condition *</label>
                <select v-model="newRule.condition">
                  <option value="gt">Greater than (&gt;)</option>
                  <option value="gte">Greater than or equal (≥)</option>
                  <option value="lt">Less than (&lt;)</option>
                  <option value="lte">Less than or equal (≤)</option>
                  <option value="eq">Equal to (=)</option>
                </select>
              </div>
              <div class="field">
                <label>Threshold *</label>
                <input v-model.number="newRule.threshold" type="number" step="any" required />
              </div>
            </div>
            <div class="row">
              <div class="field">
                <label>Device ID (optional)</label>
                <input v-model="newRule.deviceId" placeholder="Leave blank for all devices" />
              </div>
              <div class="field">
                <label>Suppression window (seconds)</label>
                <input v-model.number="newRule.suppressionWindowS" type="number" min="0" />
              </div>
            </div>
            <div class="form-actions">
              <button type="submit" :disabled="creatingRule">
                {{ creatingRule ? 'Creating…' : 'Create rule' }}
              </button>
              <button type="button" class="btn-ghost" @click="showCreateRule = false">Cancel</button>
            </div>
          </form>

          <table class="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Metric</th>
                <th>Condition</th>
                <th>Device</th>
                <th>Last fired</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="alertRules.length === 0">
                <td colspan="6" class="empty-row">No alert rules yet.</td>
              </tr>
              <tr v-for="rule in alertRules" :key="rule.id">
                <td>{{ rule.name }}</td>
                <td class="mono">{{ rule.metricName }}</td>
                <td>{{ conditionLabel(rule.condition) }} {{ rule.threshold }}</td>
                <td>{{ rule.deviceId || 'All' }}</td>
                <td>{{ rule.lastFiredAt ? formatDate(rule.lastFiredAt) : 'Never' }}</td>
                <td>
                  <button class="btn-danger-sm" @click="deleteRule(rule.id)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Devices tab -->
      <div v-if="activeTab === 'devices'" class="card">
        <h2>Devices</h2>
        <table class="data-table">
          <thead>
            <tr>
              <th>Device ID</th>
              <th>Name</th>
              <th>Firmware</th>
              <th>Location</th>
              <th>Last seen</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="devices.length === 0">
              <td colspan="5" class="empty-row">No devices yet. Devices auto-register on first message.</td>
            </tr>
            <tr v-for="d in devices" :key="d.id">
              <td class="mono">{{ d.deviceId }}</td>
              <td>{{ d.name || '—' }}</td>
              <td>{{ d.firmwareVersion || '—' }}</td>
              <td>{{ d.locationLabel || (d.latitude ? `${d.latitude.toFixed(4)}, ${d.longitude.toFixed(4)}` : '—') }}</td>
              <td>{{ d.lastSeenAt ? formatDate(d.lastSeenAt) : 'Never' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '../../store/project.js'
import api from '../../api/client.js'

const route = useRoute()
const projectStore = useProjectStore()

const project = ref(null)
const activeTab = ref('settings')
const tabs = [
  { id: 'settings', label: 'Settings' },
  { id: 'keys', label: 'API Keys' },
  { id: 'alerts', label: 'Alert Rules' },
  { id: 'devices', label: 'Devices' },
]

const settingsForm = ref({})
const savingSettings = ref(false)
const settingsMsg = ref('')

const apiKeys = ref([])
const showCreateKey = ref(false)
const creatingKey = ref(false)
const createdKey = ref(null)
const newKey = ref({ name: '', environment: 'live' })

const alertRules = ref([])
const showCreateRule = ref(false)
const creatingRule = ref(false)
const newRule = ref({ name: '', metricName: '', condition: 'gt', threshold: null, deviceId: '', suppressionWindowS: 300 })

const devices = ref([])

async function loadProject() {
  const id = route.params.id
  const { data } = await api.get(`/api/v1/projects/${id}`)
  project.value = data
  settingsForm.value = {
    name: data.name,
    description: data.description || '',
    alertEmail: data.alertEmail || '',
    alertWebhookUrl: data.alertWebhookUrl || '',
  }
  await Promise.all([loadKeys(), loadRules(), loadDevices()])
}

async function loadKeys() {
  const { data } = await api.get('/api/v1/keys', { params: { projectId: route.params.id } })
  apiKeys.value = data
}

async function loadRules() {
  const { data } = await api.get(`/api/v1/projects/${route.params.id}/alert-rules`)
  alertRules.value = data
}

async function loadDevices() {
  const { data } = await api.get(`/api/v1/projects/${route.params.id}/devices`)
  devices.value = data
}

async function saveSettings() {
  savingSettings.value = true
  settingsMsg.value = ''
  try {
    const updated = await projectStore.updateProject(route.params.id, settingsForm.value)
    project.value = updated
    settingsMsg.value = 'Settings saved.'
    setTimeout(() => settingsMsg.value = '', 3000)
  } finally {
    savingSettings.value = false
  }
}

async function createKey() {
  creatingKey.value = true
  try {
    const { data } = await api.post('/api/v1/keys', {
      projectId: route.params.id,
      name: newKey.value.name,
      environment: newKey.value.environment,
    })
    createdKey.value = data
    newKey.value = { name: '', environment: 'live' }
    showCreateKey.value = false
    await loadKeys()
  } finally {
    creatingKey.value = false
  }
}

async function revokeKey(id) {
  if (!confirm('Revoke this key? Devices using it will stop sending data.')) return
  await api.delete(`/api/v1/keys/${id}`)
  await loadKeys()
}

async function createRule() {
  creatingRule.value = true
  try {
    const payload = { ...newRule.value }
    if (!payload.deviceId) delete payload.deviceId
    await api.post(`/api/v1/projects/${route.params.id}/alert-rules`, payload)
    newRule.value = { name: '', metricName: '', condition: 'gt', threshold: null, deviceId: '', suppressionWindowS: 300 }
    showCreateRule.value = false
    await loadRules()
  } finally {
    creatingRule.value = false
  }
}

async function deleteRule(id) {
  if (!confirm('Delete this alert rule?')) return
  await api.delete(`/api/v1/projects/${route.params.id}/alert-rules/${id}`)
  await loadRules()
}

function copyKey(key) {
  navigator.clipboard.writeText(key)
}

function formatDate(iso) {
  return new Date(iso).toLocaleString()
}

const conditionLabels = { gt: '>', gte: '≥', lt: '<', lte: '≤', eq: '=' }
function conditionLabel(c) { return conditionLabels[c] || c }

onMounted(loadProject)
watch(() => route.params.id, loadProject)
</script>

<style scoped>
.project-page { padding: 24px; max-width: 1000px; margin: 0 auto; }
.loading { text-align: center; padding: 60px; color: #888; }

.page-header { margin-bottom: 24px; }
.back-link { color: #1a73e8; text-decoration: none; font-size: 0.875rem; display: block; margin-bottom: 8px; }
h1 { font-size: 1.5rem; color: #1a1a2e; }

.tabs { display: flex; gap: 4px; margin-bottom: 20px; }
.tabs button {
  padding: 8px 20px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 0.9rem;
  color: #666;
  cursor: pointer;
  transition: all 0.15s;
}
.tabs button:hover { background: #f0f0f0; color: #333; }
.tabs button.active { background: #1a73e8; color: white; }

.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}
.card h2 { font-size: 1rem; font-weight: 600; color: #333; margin-bottom: 20px; }

.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }

form { display: flex; flex-direction: column; gap: 16px; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
label { font-size: 0.8rem; font-weight: 500; color: #555; }
input, select {
  padding: 10px 12px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s;
  background: white;
}
input:focus, select:focus { border-color: #1a73e8; }

.form-actions { display: flex; gap: 10px; }
button[type="submit"] {
  padding: 10px 24px;
  background: #1a73e8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}
button[type="submit"]:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-ghost {
  padding: 10px 20px;
  background: transparent;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  color: #666;
}
.btn-secondary {
  padding: 8px 16px;
  background: white;
  border: 1.5px solid #1a73e8;
  color: #1a73e8;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
}
.btn-danger-sm {
  padding: 4px 12px;
  background: transparent;
  border: 1px solid #ea4335;
  color: #ea4335;
  border-radius: 6px;
  font-size: 0.8rem;
  cursor: pointer;
}

.inline-form {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.inline-form input, .inline-form select { flex: 1; min-width: 120px; }

.alert-form {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.key-reveal {
  background: #e8f5e9;
  border: 1px solid #a5d6a7;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.key-value { display: flex; align-items: center; gap: 10px; margin: 8px 0; }
.key-value code {
  flex: 1;
  background: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-family: monospace;
  word-break: break-all;
  border: 1px solid #c8e6c9;
}
.key-value button {
  padding: 8px 14px;
  background: #34a853;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  white-space: nowrap;
}

.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th { text-align: left; padding: 10px 12px; color: #888; font-weight: 500; border-bottom: 1px solid #f0f0f0; }
.data-table td { padding: 12px; border-bottom: 1px solid #f8f8f8; color: #333; }
.data-table tr:last-child td { border-bottom: none; }
.mono { font-family: monospace; color: #555; }
.empty-row { text-align: center; color: #aaa; padding: 32px !important; }

.badge { background: #e8f0fe; color: #1a73e8; padding: 2px 8px; border-radius: 10px; font-size: 0.75rem; }
.success { color: #137333; background: #e6f4ea; padding: 8px 12px; border-radius: 6px; font-size: 0.875rem; }
</style>
