# frontend — 산책시루 화면 (React + Vite)

설치, 실행, 개발 방법은 **저장소 맨 위의 [README](../README.md)** 를 보세요.

```bash
npm install     # 처음 한 번, 또는 package.json이 바뀌었을 때
npm run dev     # 개발 서버 → http://localhost:5173
npm run lint    # 코드 검사 (ESLint)
npm run build   # 빌드가 되는지 확인 → dist/ (배포는 하지 않음)
```

- `/api/…`, `/files/…` 요청은 `vite.config.js`의 프록시 설정으로 백엔드(`localhost:8080`)에 전달됩니다.
- 환경 확인용 테스트 화면은 **`/health`** 주소로 옮겼습니다 (`src/pages/HealthCheck.jsx`). 지우지 마세요.

## `.env.local` (각자 만들기, 깃허브에 안 올라감)

`frontend/.env.local` 파일을 만들고 아래처럼 적습니다. 카카오 키는 디스코드 비공개 채널에서 받으세요.

```
VITE_KAKAO_JS_KEY=카카오_JavaScript_키
# 백엔드를 켜 놓고 실제 API로 볼 때만 (기본은 가짜 데이터)
# VITE_USE_MOCK=false
```

`.env.local`을 바꾼 뒤에는 `npm run dev`를 껐다가 다시 켜야 적용됩니다.

- 키가 없거나 지도를 못 불러오면 지도 자리에 안내 문구가 나옵니다. 카카오 개발자 콘솔의 **JavaScript SDK 도메인**에 `http://localhost:5173`이 있는지, **제품 설정 → 카카오맵 사용 설정**이 켜져 있는지 확인하세요.

## 가짜 데이터 / 실제 API

- 기본은 **가짜 데이터**(`src/mocks/`)입니다. 내용은 `db/seed.sql`과 같게 맞춰 두어서, 실제 API로 바꿔도 같은 코스 · 동행이 보입니다. (동행 101 · 102번은 화면 확인용으로 가짜 데이터에만 있음)
- 화면은 `src/api/`의 함수만 부릅니다. 실제 API로 바꿀 때 화면 코드는 그대로 두고 `src/api/` 함수만 확인하면 됩니다.
- 가짜 데이터에서 **로그인 전 화면(M0)** 보기: 브라우저 콘솔에서 `localStorage.setItem('mockGuest', '1')` 후 새로고침 (되돌리기: `localStorage.removeItem('mockGuest')`)

## 폴더

| 폴더 | 내용 |
| --- | --- |
| `src/pages/` | 화면 (Figma 화면 ID로 시작: `M1Main.jsx`, `C1CourseList.jsx` …) |
| `src/components/` | 공통 부품: `Header`, `Footer`, `KakaoMap`, `MatchCard`, `CourseCard`, `Layout` (동훈) · `Button`, `Input`, `Chip`, `Avatar`, `Modal`, `Toast` (승민, 사용법은 각 파일 맨 위 주석) |
| `src/api/` | 백엔드 호출 함수 (`client.js` 공통, 도메인별 파일) |
| `src/mocks/` | 가짜 데이터 |
| `src/auth/` | 로그인 상태 (`useAuth()`) |
| `src/constants/labels.js` | API 코드 → 화면 글자 (`SLOW` → `🐢 천천히` 등) |
| `src/utils/` | 날짜 · 거리 형식, `useAsync` 훅, `showToast()` 알림, 입력값 검사(`validate.js`) |
| `public/avatars/` | 아바타 8종 (파일 이름 = API의 avatar 코드: `seagull.svg` …) |

## 화면 주소

| 주소 | 화면 | 담당 |
| --- | --- | --- |
| `/` | M0 메인(로그인 전) / M1 메인(로그인 후) | 동훈 |
| `/courses` | C1 코스 찾기 (C5 결과 없음, L1 로딩) | 동훈 |
| `/courses/:id` | C2 코스 상세 | 동훈 |
| `/courses/new` | C3 코스 등록 (2주차, 지금은 자리만) | 동훈 |
| `/courses/:id/reviews` | C4 후기 전체 (자리만) | 승민 |
| `/matches` | W1 동행 찾기 (E1 결과 없음) | 동훈 |
| `/matches/:id`, `/matches/new` | W2 · W4, W3 (2주차, 자리만) | 동훈 |
| `/login` | A1 로그인 (실패하면 A2 오류 상태). 가짜 데이터에서는 `db/seed.sql` 테스트 계정 + `test1234!` | 승민 |
| `/signup` | A3 계정 정보 → A4 프로필 · 태그 → A5 완료 (한 주소에서 단계만 바뀜) | 승민 |
| `/mypage` | P1 마이페이지 (2주차, 자리만) | 승민 |
| `/health` | 환경 확인 | - |
