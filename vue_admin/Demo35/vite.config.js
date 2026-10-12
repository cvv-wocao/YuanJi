import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  
  // 路径别名
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  
  // 开发服务器配置
  server: {
    port: 3000,
    open: true, // 自动打开浏览器
    proxy: {
      // 代理配置：将 /api 前缀的请求转发到后端，并去掉 /api 前缀
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  
  // 构建配置
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        // 代码分割
        manualChunks: {
          vendor: ['vue', 'axios'],
        },
      },
    },
  },
  
  // CSS 配置
  css: {
    devSourcemap: true,
  },
})
