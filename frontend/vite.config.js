import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중 /api 요청은 백엔드 스프링부트(8080)로 넘김 → CORS 문제 없음, 로그인 세션 쿠키도 그대로
    // /files 는 후기 사진 · GPX 파일 주소 (예: /files/reviews/1-1.jpg). 이게 없으면 <img>에 사진 대신 빈 화면이 옴
    proxy: {
      '/api': 'http://localhost:8080',
      '/files': 'http://localhost:8080',
    },
  },
})
