import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发时把 /api 和 /uploads 代理到后端，前端代码里就不用写完整域名
    proxy: {
      '/api': { target: 'http://localhost:8081', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8081', changeOrigin: true }
    }
  }
})
