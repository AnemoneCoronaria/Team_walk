// import react from '@vitejs/plugin-react'
// import { defineConfig } from 'vite'

// // https://vite.dev/config/
// export default defineConfig({
//   plugins: [react()],
// })

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중 /api 요청은 이클립스 Tomcat(8080)으로 넘김 → CORS 문제 없음
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})