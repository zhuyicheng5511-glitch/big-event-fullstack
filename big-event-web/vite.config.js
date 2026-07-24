import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)) // 配置 @ 符号指向 src 目录
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 代理的目标后端接口地址
        changeOrigin: true,             // 允许跨域
        rewrite: (path) => path.replace(/^\/api/, '') // 将请求路径中的 /api 替换为空，例如: /api/user/login -> /user/login
      }
    }
  }
})