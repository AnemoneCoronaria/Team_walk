# frontend — 산책시루 화면 (React + Vite)

설치, 실행, 개발 방법은 **저장소 맨 위의 [README](../README.md)** 를 보세요.

```bash
npm install     # 처음 한 번, 또는 package.json이 바뀌었을 때
npm run dev     # 개발 서버 (실제 API, 백엔드 필요) → http://localhost:5173
npm run dev:mock  # 개발 서버 (가짜 데이터, 백엔드 없이)
npm run lint    # 코드 검사 (ESLint)
npm run build   # 빌드가 되는지 확인 → dist/ (배포는 하지 않음)
```

- `/api/…`, `/files/…` 요청은 `vite.config.js`의 프록시 설정으로 백엔드(`localhost:8080`)에 전달됩니다.
- 환경 확인용 테스트 화면은 **`/health`** 주소입니다 (`src/pages/HealthCheck.jsx`). 지우지 마세요.

## `.env.local` (각자 만들기, 깃허브에 안 올라감)

`frontend/.env.local` 파일을 만들고 아래처럼 적습니다. 카카오 키는 디스코드 비공개 채널에서 받으세요.

```
VITE_KAKAO_JS_KEY=카카오_JavaScript_키
```

`.env.local`을 바꾼 뒤에는 `npm run dev`를 껐다가 다시 켜야 적용됩니다.

- 키가 없거나 지도를 못 불러오면 지도 자리에 안내 문구가 나옵니다. 카카오 개발자 콘솔의 **JavaScript SDK 도메인**에 `http://localhost:5173`이 있는지, **제품 설정 → 카카오맵 사용 설정**이 켜져 있는지 확인하세요.

## 가짜 데이터 / 실제 API

| 명령 | 데이터 | 언제 |
| --- | --- | --- |
| `npm run dev` | **실제 API** (백엔드 8080) | 기본. 백엔드를 켜 놓고 씁니다 |
| `npm run dev:mock` | **가짜 데이터** (`src/mocks/`) | 백엔드 없이 화면만 볼 때. 화면 오른쪽 아래에 `MOCK` 배지가 보여요 |

- 배포 빌드(`npm run build`)에서는 설정과 상관없이 가짜 데이터가 켜지지 않습니다.
- 가짜 데이터의 내용은 `db/seed.sql`과 같게 맞춰 두어서, 실제 API로 바꿔도 같은 코스 · 동행이 보입니다. (동행 101 · 102번은 화면 확인용으로 가짜 데이터에만 있음)
- 화면은 `src/api/`의 함수만 부릅니다. 실제 API로 바꿀 때 화면 코드는 그대로 두고 `src/api/` 함수만 확인하면 됩니다.
- 가짜 데이터에서 로그인한 사람은 **민서**예요. 동행 1번은 민서가 모집자라 W4(모집자 화면), 2번은 참여하기(W2-1), 3번은 이미 참여 중(W5 참여 취소), 4번은 마감 상태로 보여요. 가짜로 등록 · 참여한 내용은 새로고침하면 처음으로 돌아가요.
- 가짜 데이터에서 **로그인 전 화면(M0)** 보기: 브라우저 콘솔에서 `localStorage.setItem('mockGuest', '1')` 후 새로고침 (되돌리기: `localStorage.removeItem('mockGuest')`)

## 폴더

| 폴더 | 내용 |
| --- | --- |
| `src/pages/` | 화면 (Figma 화면 ID로 시작: `M1Main.jsx`, `C1CourseList.jsx` …) |
| `src/pages/admin/` | 관리자 화면 AD1 · AD2 · AD4 (정란), AD3 (민서), 공통 틀 `AdminLayout` · `AdminParts` |
| `src/components/` | 공통 부품: `Header`, `Footer`, `KakaoMap`, `MatchCard`, `CourseCard`, `Layout` (동훈) · `Button`, `Input`, `Chip`, `Avatar`, `Modal`, `Toast`, `ReportModal`, `LandmarkVerify`, `MyPageLayout` (승민, 사용법은 각 파일 맨 위 주석) |
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
| `/courses/new` | C3-1 ~ C3-3 코스 등록 (C3-1E 업로드 오류) — 한 주소에서 단계만 바뀜 | 동훈 |
| `/courses/:id/reviews` | C4 후기 전체 (최신 · 별점 · 사진 후기, 페이지 넘기기) | 승민 |
| `/matches` | W1 동행 찾기 (E1 결과 없음) | 동훈 |
| `/matches/:id` | W2 동행 상세 (W2-1 참여 확인, W5 참여 취소). 내가 모집자면 W4 (마감 · 취소 · 내보내기 · 공지 · 댓글) | 동훈 |
| `/matches/new`, `/matches/:id/edit` | W3 모집 글 쓰기 · 수정 | 동훈 |
| `/login` | A1 로그인 (실패하면 A2 오류 상태). 가짜 데이터에서는 `db/seed.sql` 테스트 계정 + `test1234!` | 승민 |
| `/signup` | A3 계정 정보 → A4 프로필 · 태그 → A5 완료 (한 주소에서 단계만 바뀜) | 승민 |
| `/mypage` | P1 마이페이지 · 내 동행 (참여 예정 · 내가 모집한 글 · 지난 동행). 왼쪽 프로필 · 메뉴 · 로그아웃은 `MyPageLayout` | 승민 |
| `/mypage/courses` | P3 내가 올린 코스 (정보 수정 · GPX · 삭제) | 승민 |
| `/mypage/reviews` | P4 내 후기 (삭제) · 스탬프 · 배지는 R2 로 | 승민 |
| `/mypage/settings` | P5 계정 설정 (비밀번호 변경 창, P5-1 회원 탈퇴 창) | 승민 |
| `/mypage/profile` | P2 프로필 · 태그 수정 (로그인 필요, 저장하면 P1 으로) | 승민 |
| `/courses/:id/review` | R1 후기 쓰기 (동행 후기면 `?matchId=`) + 랜드마크 GPX 인증 (R1-1 동의 · R2-1 성공 · R1-2 실패 창) | 승민 |
| `/stamps` | R2 랜드마크 스탬프 투어 (지도 · 8곳 · 배지 · GPX 인증). 가짜 데이터에서는 `db/sample-uploads/gpx/course-3.gpx` 로 인증 성공 | 승민 |
| `/admin/reports` | AD1 신고 처리 (관리자 계정만, 아니면 로그인 화면으로) | 정란 |
| `/admin/users` | AD2 회원 관리 | 정란 |
| `/admin/matches` | AD3 모집 글 관리 (모집 중 · 마감 · 숨김 탭, 숨김 · 복구) | 민서 |
| `/admin/courses` | AD4 코스 관리 | 정란 |
| `/500` | X2 서버 오류 (확인 · 시연용. 화면에서 API 5xx 일 때 `<X2ServerError />` 를 보여 주면 됨) | 승민 |
| 그 밖의 주소 | X1 404 (상세 화면에서 API 404 일 때도 `<X1NotFound />`) | 승민 |
| `/health` | 환경 확인 | - |
