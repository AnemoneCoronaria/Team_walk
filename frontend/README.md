# frontend — 산책시루 화면 (React + Vite)

설치, 실행, 개발 방법은 **저장소 맨 위의 [README](../README.md)** 를 보세요.

```bash
npm install     # 처음 한 번, 또는 package.json이 바뀌었을 때
npm run dev     # 개발 서버 → http://localhost:5173
npm run lint    # 코드 검사 (ESLint)
npm run build   # 배포용 빌드 → dist/
```

- `/api/…` 요청은 `vite.config.js`의 프록시 설정으로 이클립스 Tomcat(`localhost:8080`)에 전달됩니다.
- 지금 `src/App.jsx`는 **환경 확인용 테스트 화면**입니다 (React 동작 + 백엔드 `/api/hello` 연결 확인). 지우지 마세요.
