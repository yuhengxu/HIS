import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendPort = process.env.BACKEND_PORT ?? '18080'
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? `http://localhost:${backendPort}`

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 15173,
    proxy: {
      '/api': apiProxyTarget,
    },
  },
})
