import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    globals: true,
  },
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
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
