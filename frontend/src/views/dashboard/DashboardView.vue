<template>
  <div class="dashboard">

    <div v-if="!current" class="empty-state">
      <h2>No project selected</h2>
      <p>Create a project to get started.</p>
      <router-link to="/onboarding" class="btn-primary">Create project</router-link>
    </div>

    <template v-else>
      <div class="page-header">
        <div>
          <h1>{{ current.name }}</h1>
          <span class="badge">{{ current.region }}</span>
          <span class="badge" :class="current.environment">{{ current.environment }}</span>
        </div>
        <select v-model="timeRange" @change="loadData">
          <option value="1h">Last 1 hour</option>
          <option value="6h">Last 6 hours</option>
          <option value="24h">Last 24 hours</option>
          <option value="7d">Last 7 days</option>
          <option value="30d">Last 30 days</option>
        </select>
      </div>

      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">{{ devices.length }}</div>
          <div class="stat-label">Active Devices</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ alertRules.length }}</div>
          <div class="stat-label">Alert Rules</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ availableMetrics.length }}</div>
          <div class="stat-label">Metrics Tracked</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ devicesOnMap }}</div>
          <div class="stat-label">Devices on Map</div>
        </div>
      </div>

      <div class="section-header">
        <h2>Time-series data</h2>
        <div class="metric-controls">
          <select v-model="selectedMetric" @change="loadChartData">
            <option value="">Select metric...</option>
            <option v-for="m in availableMetrics" :key="m" :value="m">{{ m }}</option>
          </select>
          <select v-model="selectedDevice" @change="loadChartData">
            <option value="">All devices</option>
            <option v-for="d in devices" :key="d.deviceId" :value="d.deviceId">
              {{ d.name || d.deviceId }}
            </option>
          </select>
        </div>
      </div>

      <div class="card chart-card">
        <TimeSeriesChart
          :metric="selectedMetric || 'Select a metric above'"
          :data="chartData"
          :loading="chartLoading"
          :unit="metricUnit"
        />
        <p v-if="!selectedMetric" class="chart-hint">Select a metric from the dropdown above to view data.</p>
      </div>

      <div class="section-header">
        <h2>Device locations</h2>
        <span class="hint">{{ devicesOnMap }} of {{ devices.length }} devices have GPS coordinates</span>
      </div>
      <div class="card">
        <DeviceMap :devices="devices" />
      </div>

      <div class="section-header">
        <h2>Devices</h2>
        <router-link :to="'/projects/' + current.id" class="link-btn">Manage &rarr;</router-link>
      </div>
      <div class="card">
        <table class="device-table">
          <thead>
            <tr>
              <th>Device ID</th>
              <th>Name</th>
              <th>Location</th>
              <th>Last seen</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="devices.length === 0">
              <td colspan="5" class="empty-row">No devices yet. Send your first message to auto-register a device.</td>
            </tr>
            <tr v-for="d in devices" :key="d.id">
              <td class="mono">{{ d.deviceId }}</td>
              <td>{{ d.name || '-' }}</td>
              <td>{{ d.locationLabel || '-' }}</td>
              <td>{{ d.lastSeenAt ? formatTime(d.lastSeenAt) : 'Never' }}</td>
              <td>
                <span class="status-dot" :class="isOnline(d.lastSeenAt) ? 'online' : 'offline'"></span>
                {{ isOnline(d.lastSeenAt) ? 'Online' : 'Offline' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '../../api/client.js'
import TimeSeriesChart from '../../components/charts/TimeSeriesChart.vue'
import DeviceMap from '../../components/map/DeviceMap.vue'

const UNITS = {
  temperature: 'C', humidity: '%', pressure: 'hPa',
  battery_v: 'V', lux: 'lx', soil_moisture: '%',
}

export default {
  name: 'DashboardView',
  components: { TimeSeriesChart, DeviceMap },

  data() {
    return {
      devices:          [],
      alertRules:       [],
      availableMetrics: [],
      chartData:        [],
      chartLoading:     false,
      timeRange:        '24h',
      selectedMetric:   '',
      selectedDevice:   '',
    }
  },

  computed: {
    ...mapState('project', ['current']),

    devicesOnMap() {
      return this.devices.filter((d) => d.latitude && d.longitude).length
    },

    metricUnit() {
      return UNITS[this.selectedMetric] || ''
    },
  },

  watch: {
    current(val) {
      if (val) this.loadData()
    },
  },

  async mounted() {
    if (this.$store.state.project.projects.length === 0) {
      await this.$store.dispatch('project/fetchProjects').catch(() => {})
    }
    if (this.current) await this.loadData()
  },

  methods: {

    timeRangeToParams() {
      const to   = new Date()
      const from = new Date(to)
      const hours = { '1h': 1, '6h': 6, '24h': 24, '7d': 168, '30d': 720 }
      from.setHours(from.getHours() - (hours[this.timeRange] || 24))
      return { from: from.toISOString(), to: to.toISOString() }
    },

    async loadData() {
      if (!this.current) return
      const id = this.current.id
      const [devRes, ruleRes, metricRes] = await Promise.allSettled([
        api.get(`/api/v1/projects/${id}/devices`),
        api.get(`/api/v1/projects/${id}/alert-rules`),
        api.get(`/api/v1/projects/${id}/data/metrics`),
      ])
      if (devRes.status    === 'fulfilled') this.devices          = devRes.value.data
      if (ruleRes.status   === 'fulfilled') this.alertRules       = ruleRes.value.data
      if (metricRes.status === 'fulfilled') {
        this.availableMetrics = metricRes.value.data
        if (!this.selectedMetric && this.availableMetrics.length) {
          this.selectedMetric = this.availableMetrics[0]
          await this.loadChartData()
        }
      }
    },

    async loadChartData() {
      if (!this.selectedMetric || !this.current) return
      this.chartLoading = true
      try {
        const { from, to } = this.timeRangeToParams()
        const params = { metric: this.selectedMetric, from, to }
        if (this.selectedDevice) params.deviceId = this.selectedDevice
        const { data } = await api.get(`/api/v1/projects/${this.current.id}/data`, { params })
        this.chartData = data.points
      } catch {
        this.chartData = []
      } finally {
        this.chartLoading = false
      }
    },

    isOnline(lastSeen) {
      if (!lastSeen) return false
      return Date.now() - new Date(lastSeen).getTime() < 5 * 60 * 1000
    },

    formatTime(iso) {
      return new Date(iso).toLocaleString()
    },
  },
}
</script>

<style scoped>
.dashboard { padding: 24px; max-width: 1400px; margin: 0 auto; }
.empty-state { text-align: center; padding: 80px 20px; }
.empty-state h2 { color: #333; margin-bottom: 8px; }
.empty-state p  { color: #888; margin-bottom: 24px; }
.btn-primary {
  display: inline-block;
  padding: 12px 24px;
  background: #1a73e8;
  color: white;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 500;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}
.page-header h1 { font-size: 1.5rem; color: #1a1a2e; margin-bottom: 8px; }
.page-header select {
  padding: 8px 12px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.875rem;
  outline: none;
  background: white;
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  margin-right: 6px;
  background: #e8f0fe;
  color: #1a73e8;
}
.badge.production  { background: #e6f4ea; color: #137333; }
.badge.staging     { background: #fef7e0; color: #b06000; }
.badge.development { background: #fce8e6; color: #c5221f; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-value { font-size: 2rem; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 0.8rem; color: #888; margin-top: 4px; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28px 0 12px;
}
.section-header h2 { font-size: 1rem; font-weight: 600; color: #333; }
.hint { font-size: 0.8rem; color: #aaa; }
.link-btn { font-size: 0.875rem; color: #1a73e8; text-decoration: none; }

.metric-controls { display: flex; gap: 10px; }
.metric-controls select {
  padding: 7px 12px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.875rem;
  outline: none;
  background: white;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}
.chart-hint { text-align: center; color: #aaa; font-size: 0.875rem; padding: 16px 0 8px; }

.device-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.device-table th { text-align: left; padding: 10px 12px; color: #888; font-weight: 500; border-bottom: 1px solid #f0f0f0; }
.device-table td { padding: 12px; border-bottom: 1px solid #f8f8f8; color: #333; }
.device-table tr:last-child td { border-bottom: none; }
.mono { font-family: monospace; color: #555; }
.empty-row { text-align: center; color: #aaa; padding: 32px !important; }

.status-dot {
  display: inline-block;
  width: 8px; height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.status-dot.online  { background: #34a853; }
.status-dot.offline { background: #ccc; }
</style>
