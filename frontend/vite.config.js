import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue':     ['vue', 'vue-router', 'vuex'],
          'vendor-echarts': ['echarts'],
          'vendor-leaflet': ['leaflet'],
        },
      },
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1/auth': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/keys': { target: 'http://localhost:8081', changeOrigin: true },
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
