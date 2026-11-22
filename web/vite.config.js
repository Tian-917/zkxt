import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端只跟网关打交道，网关负责路由到各微服务
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
