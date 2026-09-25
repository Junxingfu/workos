import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 开发期直接代理到后端，避免 CORS
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // 代理是"同源门面"：浏览器认为同源不会做 CORS 检查，
          // 但转发时若原样带上 Origin，后端会把它当跨域请求处理（端口变了就 403）。
          // 这里去掉 Origin 与 Referer，让后端按普通请求处理。
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin')
            proxyReq.removeHeader('referer')
          })
        }
      }
    }
  }
})
