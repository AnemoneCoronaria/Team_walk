import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중 /api 요청은 이클립스 Tomcat(8080)으로 넘김 → CORS 문제 없음
    // /files 는 후기 사진 주소 (예: /files/reviews/1-1.jpg). 이게 없으면 <img>에 사진 대신 빈 화면이 옴
    proxy: {
      '/api': 'http://localhost:8080',
      '/files': 'http://localhost:8080',
    },
  },
})
