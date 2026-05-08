<template>
  <div ref="mapEl" class="map-container"></div>
</template>

<script>
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).href,
  iconUrl:       new URL('leaflet/dist/images/marker-icon.png',    import.meta.url).href,
  shadowUrl:     new URL('leaflet/dist/images/marker-shadow.png',  import.meta.url).href,
})

export default {
  name: 'DeviceMap',

  props: {
    devices: { type: Array,  default: () => [] },
    center:  { type: Array,  default: () => [-1.2921, 36.8219] },
  },

  data() {
    return {
      map: null,
      markerLayer: null,
    }
  },

  watch: {
    devices: { handler: 'addMarkers', deep: true },
  },

  mounted() {
    this.map = L.map(this.$refs.mapEl, { center: this.center, zoom: 7 })
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map)
    this.markerLayer = L.layerGroup().addTo(this.map)
    this.addMarkers()
  },

  beforeUnmount() {
    this.map && this.map.remove()
  },

  methods: {
    addMarkers() {
      if (!this.markerLayer) return
      this.markerLayer.clearLayers()

      const withCoords = this.devices.filter((d) => d.latitude && d.longitude)
      withCoords.forEach((device) => {
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
          .addTo(this.markerLayer)
      })

      if (withCoords.length > 0 && this.map) {
        const bounds = L.latLngBounds(withCoords.map((d) => [d.latitude, d.longitude]))
        this.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 13 })
      }
    },
  },
}
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 320px;
  border-radius: 8px;
  overflow: hidden;
}
</style>
