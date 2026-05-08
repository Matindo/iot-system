<template>
  <div ref="chartEl" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: { type: String, default: '' },
  metric: { type: String, default: '' },
  data: { type: Array, default: () => [] },
  unit: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})

const chartEl = ref(null)
let chart = null

function buildOption() {
  const times = props.data.map(p => p.time)
  const values = props.data.map(p => p.avg_value ?? p.avgValue)
  const mins = props.data.map(p => p.min_value ?? p.minValue)
  const maxs = props.data.map(p => p.max_value ?? p.maxValue)

  const hasRange = mins.some(v => v != null)

  return {
    title: {
      text: props.title || props.metric,
      textStyle: { fontSize: 13, fontWeight: 500, color: '#444' },
      top: 8, left: 16,
    },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        const p = params[0]
        const time = new Date(p.axisValue).toLocaleString()
        let html = `<div style="font-size:12px;color:#666">${time}</div>`
        params.forEach(s => {
          html += `<div><span style="color:${s.color}">●</span> ${s.seriesName}: <b>${
            typeof s.value === 'number' ? s.value.toFixed(2) : s.value
          } ${props.unit}</b></div>`
        })
        return html
      },
    },
    legend: { show: hasRange, data: ['Avg', 'Min', 'Max'], top: 8, right: 16 },
    grid: { top: 48, bottom: 32, left: 60, right: 20 },
    xAxis: {
      type: 'time',
      axisLabel: { fontSize: 11, color: '#888' },
    },
    yAxis: {
      type: 'value',
      name: props.unit,
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
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(26,115,232,0.15)' }, { offset: 1, color: 'rgba(26,115,232,0)' }] } },
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
}

onMounted(() => {
  chart = echarts.init(chartEl.value)
  chart.setOption(buildOption())
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  chart?.dispose()
  chart = null
})

watch(() => [props.data, props.loading], () => {
  if (!chart) return
  if (props.loading) {
    chart.showLoading('default', { text: 'Loading…', color: '#1a73e8' })
  } else {
    chart.hideLoading()
    chart.setOption(buildOption())
  }
}, { deep: true })
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 260px;
}
</style>
