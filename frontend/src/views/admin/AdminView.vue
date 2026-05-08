<template>
  <div class="admin-page">
    <div class="page-header">
      <h1>Platform Administration</h1>
    </div>

    <div v-if="loading" class="loading">Loading...</div>

    <template v-else>
      <div class="stats-grid">
        <div class="stat-card blue">
          <div class="stat-value">{{ stats.totalUsers.toLocaleString() }}</div>
          <div class="stat-label">Active Users</div>
        </div>
        <div class="stat-card green">
          <div class="stat-value">{{ stats.totalProjects.toLocaleString() }}</div>
          <div class="stat-label">Projects</div>
        </div>
        <div class="stat-card purple">
          <div class="stat-value">{{ stats.totalDevices.toLocaleString() }}</div>
          <div class="stat-label">Registered Devices</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-value">{{ stats.totalAlertRules.toLocaleString() }}</div>
          <div class="stat-label">Alert Rules</div>
        </div>
        <div class="stat-card teal wide">
          <div class="stat-value">{{ stats.messagesLast24h.toLocaleString() }}</div>
          <div class="stat-label">Messages (last 24h)</div>
        </div>
      </div>

      <div class="card">
        <h2>Platform info</h2>
        <dl class="info-grid">
          <div class="info-item">
            <dt>Region</dt>
            <dd>Africa - Nairobi (af-ke-1)</dd>
          </div>
          <div class="info-item">
            <dt>Kafka topics</dt>
            <dd>raw.ingest.af-ke-1, processed.ingest.af-ke-1, alert.events, quota.events</dd>
          </div>
          <div class="info-item">
            <dt>Storage backend</dt>
            <dd>TimescaleDB (PostgreSQL 16)</dd>
          </div>
          <div class="info-item">
            <dt>MQTT broker</dt>
            <dd>EMQX 5.6 (ports 1883, 8883, 8083)</dd>
          </div>
        </dl>
      </div>
    </template>
  </div>
</template>

<script>
import api from '../../api/client.js'

export default {
  name: 'AdminView',

  data() {
    return {
      loading: true,
      stats: {
        totalUsers:      0,
        totalProjects:   0,
        totalDevices:    0,
        totalAlertRules: 0,
        messagesLast24h: 0,
      },
    }
  },

  async mounted() {
    try {
      const { data } = await api.get('/api/v1/admin/stats')
      this.stats = data
    } finally {
      this.loading = false
    }
  },
}
</script>

<style scoped>
.admin-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 28px; }
h1 { font-size: 1.5rem; color: #1a1a2e; }
.loading { text-align: center; padding: 60px; color: #888; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  border-top: 4px solid transparent;
}
.stat-card.wide    { grid-column: span 2; }
.stat-card.blue    { border-top-color: #1a73e8; }
.stat-card.green   { border-top-color: #34a853; }
.stat-card.purple  { border-top-color: #9c27b0; }
.stat-card.orange  { border-top-color: #f57c00; }
.stat-card.teal    { border-top-color: #00796b; }
.stat-value { font-size: 2.25rem; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 0.8rem; color: #888; margin-top: 4px; }

.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.card h2 { font-size: 1rem; font-weight: 600; color: #333; margin-bottom: 20px; }

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-item dt { font-size: 0.8rem; font-weight: 500; color: #888; margin-bottom: 4px; }
.info-item dd { font-size: 0.9rem; color: #333; }
</style>
