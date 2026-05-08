<template>
  <div ref="chartEl" class="chart-container"></div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'TimeSeriesChart',

  props: {
    title:   { type: String,  default: '' },
    metric:  { type: String,  default: '' },
    data:    { type: Array,   default: () => [] },
    unit:    { type: String,  default: '' },
    loading: { type: Boolean, default: false },
  },

  data() {
    return { chart: null }
  },

  watch: {
    data:    { handler: 'refresh', deep: true },
    loading: { handler: 'refresh' },
  },

  mounted() {
    this.chart = echarts.init(this.$refs.chartEl)
    this.chart.setOption(this.buildOption())
    window.addEventListener('resize', this.onResize)
  },

  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    this.chart && this.chart.dispose()
  },

  methods: {
    onResize() {
      this.chart && this.chart.resize()
    },

    refresh() {
      if (!this.chart) return
      if (this.loading) {
        this.chart.showLoading('default', { text: 'Loading...', color: '#1a73e8' })
      } else {
        this.chart.hideLoading()
        this.chart.setOption(this.buildOption())
      }
    },

    buildOption() {
      const times  = this.data.map((p) => p.time)
      const values = this.data.map((p) => p.avg_value ?? p.avgValue)
      const mins   = this.data.map((p) => p.min_value ?? p.minValue)
      const maxs   = this.data.map((p) => p.max_value ?? p.maxValue)
      const hasRange = mins.some((v) => v != null)

      return {
        title: {
          text: this.title || this.metric,
          textStyle: { fontSize: 13, fontWeight: 500, color: '#444' },
          top: 8, left: 16,
        },
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const time = new Date(params[0].axisValue).toLocaleString()
            let html = `<div style="font-size:12px;color:#666">${time}</div>`
            params.forEach((s) => {
              html += `<div><span style="color:${s.color}">&#9679;</span> ${s.seriesName}: <b>${
                typeof s.value === 'number' ? s.value.toFixed(2) : s.value
              } ${this.unit}</b></div>`
            })
            return html
          },
        },
        legend: { show: hasRange, data: ['Avg', 'Min', 'Max'], top: 8, right: 16 },
        grid: { top: 48, bottom: 32, left: 60, right: 20 },
        xAxis: { type: 'time', axisLabel: { fontSize: 11, color: '#888' } },
        yAxis: {
          type: 'value',
          name: this.unit,
          nameTextStyle: { color: '#888', fontSize: 11 },
          axisLabel: { fontSize: 11, color: '#888' },
          splitLine: { lineStyle: { color: '#f0f0f0' } },
        },
        series: [
          {
            name: 'Avg',
            type: 'line',
            data: times.map((t, i) => [t, values[i]]),
            smooth: true,
            symbol: 'none',
            lineStyle: { width: 2, color: '#1a73e8' },
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(26,115,232,0.15)' },
                  { offset: 1, color: 'rgba(26,115,232,0)' },
                ],
              },
            },
          },
          ...(hasRange ? [
            {
              name: 'Min',
              type: 'line',
              data: times.map((t, i) => [t, mins[i]]),
              smooth: true,
              symbol: 'none',
              lineStyle: { width: 1, color: '#34a853', type: 'dashed' },
            },
            {
              name: 'Max',
              type: 'line',
              data: times.map((t, i) => [t, maxs[i]]),
              smooth: true,
              symbol: 'none',
              lineStyle: { width: 1, color: '#ea4335', type: 'dashed' },
            },
          ] : []),
        ],
      }
    },
  },
}
</script>

<style scoped>
.chart-container { width: 100%; height: 260px; }
</style>
