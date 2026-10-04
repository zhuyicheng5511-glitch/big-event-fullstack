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
      },
      // 上传接口现在返回相对路径 /uploads/xxx，图片请求原样转发给后端的静态资源映射
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})