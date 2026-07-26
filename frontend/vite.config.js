import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发时把 /api 代理到后端 8080，避免跨域
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
