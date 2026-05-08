<template>
  <div ref="mapEl" class="map-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// Fix default icon paths broken by Vite bundling
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl: new URL('leaflet/dist/images/marker-icon.png', import.meta.url).href,
  shadowUrl: new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).href,
})

const props = defineProps({
  devices: { type: Array, default: () => [] },
  center: { type: Array, default: () => [-1.2921, 36.8219] },
})

const mapEl = ref(null)
let map = null
let markerLayer = null

function addMarkers() {
  markerLayer?.clearLayers()
  if (!markerLayer) return

  const withCoords = props.devices.filter(d => d.latitude && d.longitude)

  withCoords.forEach(device => {
    const lastSeen = device.lastSeenAt
      ? new Date(device.lastSeenAt).toLocaleString()
      : 'Never'

    const popup = `
      <div style="font-size:13px;min-width:160px">
        <b>${device.name || device.deviceId}</b><br/>
        <span style="color:#666">ID: ${device.deviceId}</span><br/>
        <span style="color:#666">Last seen: ${lastSeen}</span>
        ${device.locationLabel ? `<br/><span style="color:#888">${device.locationLabel}</span>` : ''}
      </div>`

    L.marker([device.latitude, device.longitude])
      .bindPopup(popup)
      .addTo(markerLayer)
  })

  if (withCoords.length > 0 && map) {
    const bounds = L.latLngBounds(withCoords.map(d => [d.latitude, d.longitude]))
    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 13 })
  }
}

onMounted(() => {
  map = L.map(mapEl.value, { center: props.center, zoom: 7 })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19,
  }).addTo(map)

  markerLayer = L.layerGroup().addTo(map)
  addMarkers()
})

onUnmounted(() => {
  map?.remove()
  map = null
})

watch(() => props.devices, addMarkers, { deep: true })
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 320px;
  border-radius: 8px;
  overflow: hidden;
}
</style>
