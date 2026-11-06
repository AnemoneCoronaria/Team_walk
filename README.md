# 🚶 산책시루 (Team_walk)

7팀 **산책크루**의 시흥시 정왕동 산책 동행 서비스 저장소입니다.

> [!IMPORTANT]
> **백엔드는 VS Code + 스프링부트 4.1, 실행은 Gradle `bootRun`입니다.** (JSP · 이클립스 · Tomcat은 쓰지 않음)
>
> **처음 온 사람이 할 것**
> 1. Git · Node.js · VS Code · JDK 21 설치, 저장소 받기 → [세팅 가이드 1 · 2번](docs/setup.md#1-설치-처음-한-번)
> 2. MySQL 설치 → DB 만들고 `schema.sql` · `seed.sql` · `check.sql` 실행 → [DB 안내](db/README.md#처음-세팅-윈도우-한-번만) (윈도우 Workbench, 맥 DataGrip)
> 3. 백엔드 실행 → [backend/README.md](backend/README.md#실행하기) · 프론트 실행 → [세팅 가이드 4번](docs/setup.md#4-프론트-세팅-처음-한-번)

<br>

## 목차

1. [한눈에 보기](#-한눈에-보기)
2. [기술 스택](#-기술-스택)
3. [누가 뭘 하나](#-누가-뭘-하나)
4. [문서 안내](#-문서-안내)
5. [빠른 시작](#-빠른-시작)
6. [깃허브로 작업하는 법](#-깃허브로-작업하는-법)
7. [폴더 구조](#-폴더-구조)
8. [꼭 지킬 것](#-꼭-지킬-것)
9. [시연 전 체크](#-시연-전-체크)

<br>

## 📌 한눈에 보기

![산책시루 개발 구조: 브라우저 → Vite(5173) → 스프링부트(8080) → MySQL(3306) · 업로드 폴더](docs/images/architecture.png)

- 브라우저는 항상 **`localhost:5173`** 으로 접속합니다.<br>
  사용자 화면과 관리자 화면 모두 React입니다.

- `/api/…`, `/files/…` 요청만 Vite가 **스프링부트(`8080`)** 로 넘깁니다.

- 스프링부트는 각자 PC의 **MySQL(`3306`)** 에 있는 `sanchaek` DB를 씁니다.

- **배포는 하지 않습니다.**<br>
  시연은 노트북 한 대에 DB · 백엔드 · 프론트를 모두 띄워서 합니다.

<br>

## 🧰 기술 스택

| 항목 | 사용 |
| --- | --- |
| 프론트 | React 19 + Vite 8, ESLint |
| 백엔드 | 스프링부트 4.1 (Spring Initializr), Gradle |
| Java | JDK 21 |
| Node.js | 22.12 이상 (LTS) |
| DB | MySQL 8.4 (8.0도 가능), 각자 PC에 설치 |
| DB 접근 | JdbcTemplate (SQL을 직접 작성) |
| 개발 도구 | VS Code (프론트는 WebStorm도 가능) |
| DB 화면 도구 | MySQL Workbench (윈도우), DataGrip (맥) |
| 지도 | 카카오맵 JavaScript API |
| 협업 | GitHub |
| 배포 | 하지 않음 (로컬 시연) |

> [!TIP]
> 인터넷 예제는 대부분 스프링부트 3 기준이라 거의 그대로 됩니다.<br>
> 다른 점 몇 가지는 [세팅 가이드 3번](docs/setup.md#3-백엔드-스프링부트)의 TIP에 정리했습니다. (`javax.` → `jakarta.` 등)

<br>

## 👥 누가 뭘 하나

| 이름 | 역할 | 맡은 기능 | 1순위 API |
| --- | --- | --- | --- |
| 민서 | 백엔드 · 팀장 | 동행, 공통 코드, 관리자 AD3 | 15개 |
| 정란 | 백엔드 | 계정, 신고, 관리자 AD1 · AD2 · AD4 | 16개 |
| 동언 | 백엔드 | 코스 · GPX, 후기, 랜드마크 · 스탬프 | 15개 |
| 동훈 | 프론트 | 지도 · 코스 · 동행 화면 | |
| 승민 | 프론트 | 계정 · 마이페이지 · 후기 · 스탬프 · 신고 화면, 에러 화면(X1 · X2), 공통 부품 | |

- 관리자 화면(AD1~AD4)은 **그 API를 만든 백엔드 담당이 React로 직접** 만듭니다.
- API별 담당은 **API 명세의 "한눈에 보기"**, 화면별 분배와 일정은 **팀 노션**에 있습니다.

<br>

## 📚 문서 안내

| 문서 | 언제 보나 | 내용 |
| --- | --- | --- |
| [세팅 가이드](docs/setup.md) | 처음 한 번 | 설치, VS Code 확장, 저장소 받기, 백엔드 · 프론트 실행 |
| [프론트 안내](frontend/README.md) | 프론트를 실행할 때 | `npm run dev` · `dev:mock`, 폴더, 화면 주소 |
| [백엔드 안내](backend/README.md) | 백엔드를 실행할 때 | Gradle `bootRun` 실행, ▶ Run 오류, 폴더 구조, 라이브러리 |
| [DB 안내](db/README.md) | DB 세팅할 때<br>테이블이 바뀔 때 | ERD, 설계 약속, 테스트 계정, 쿼리 예시 |
| [개발 규칙](docs/development.md) | 작업할 때마다 | 화면 · API 만드는 법, **깃허브 사용법**, 커밋 메시지 |
| [막혔을 때](docs/troubleshooting.md) | 오류가 났을 때 | 지금까지 겪은 문제와 해결법 |

**저장소 밖 문서**

- 🗒 **노션** — 역할 분배, 우선순위(1순위 · 2순위 · 보류), 일정
- 🎨 **[Figma](https://www.figma.com/design/8g73YEapksdegpYXV7aPKp)** — 화면 설계, 화면 ID(A1, C1, W2 …), 📝 메모
- 📄 **API 명세** — 주소, 요청 · 응답 예시, 담당자 (노션의 "API 명세" 링크)

<br>

## ✅ 빠른 시작

세팅이 끝나면 아래 세 가지가 모두 보여야 합니다.<br>
다 되면 화면을 캡처해서 팀에 공유해 주세요.

**1. 백엔드** — Gradle `bootRun` 실행 후 `http://localhost:8080/api/hello`

```json
{"success":true,"data":{"message":"안녕하세요, 산책시루 서버입니다", …, "landmarks":8}}
```

**2. 프론트** — `npm run dev` 후 `http://localhost:5173/health`

> 산책시루 프론트가 동작합니다<br>
> ✅ 연결 성공

백엔드 없이 화면만 볼 때는 `npm run dev:mock` (가짜 데이터, 오른쪽 아래에 `MOCK` 배지) → [프론트 안내](frontend/README.md#가짜-데이터--실제-api)

**3. DB** — Workbench · DataGrip에서 `db/check.sql` 실행

> 7줄 모두 **OK** ([DB 안내 4)](db/README.md#4-확인))

<br>

## 🌿 깃허브로 작업하는 법

**기능 하나 = 브랜치 하나 = PR 하나**

main · develop에는 직접 올리지 않습니다.<br>
항상 **내 브랜치 → PR → 리뷰 → 합치기** 순서입니다.

![깃허브 작업 흐름: ① develop 받기 → ② 브랜치 만들기 → ③ 커밋 → ④ develop 반영 → ⑤ push → ⑥ PR 만들기 → ⑦ 리뷰 → ⑧ 합치기](docs/images/git-workflow.png)

```bash
# ① 최신 develop 받기
git checkout develop
git pull

# ② 내 브랜치 만들기 (feature/화면ID-기능)
git checkout -b feature/W2-match-detail

# ③ 작업하고 커밋 (작게, 자주)
git add backend
git commit -m "feat: 동행 상세 API 추가"

# ④ 그사이 바뀐 develop 반영
git pull origin develop

# ⑤ 깃허브에 올리기 → 깃허브 화면에서 PR 만들기 (base: develop)
git push -u origin feature/W2-match-detail
```

**처음이라면 여기를 보세요**

| 상황 | 자세히 |
| --- | --- |
| 이름 · 이메일 설정 (처음 한 번) | [3-1. 처음 한 번](docs/development.md#3-1-처음-한-번) |
| PR 만드는 화면, 리뷰어 지정 | [3-3. PR 올리는 법](docs/development.md#3-3-pr-올리는-법-깃허브-화면) |
| 리뷰어가 할 일<br>(Approve → Merge → Delete branch) | [3-4. 리뷰하고 합치는 법](docs/development.md#3-4-리뷰하고-합치는-법-리뷰어) |
| `CONFLICT`가 났다 | [3-5. 충돌이 났을 때](docs/development.md#3-5-충돌이-났을-때) |
| develop에서 바로 작업했다<br>커밋 메시지를 틀렸다 등 | [3-6. 자주 하는 실수와 되돌리기](docs/development.md#3-6-자주-하는-실수와-되돌리기) |

<br>

## 🗂 폴더 구조

```
Team_walk/
├─ README.md                지금 보는 안내판
├─ docs/                    문서
│  ├─ setup.md              세팅 가이드
│  ├─ development.md        개발 규칙 · 깃허브 사용법
│  ├─ troubleshooting.md    막혔을 때
│  └─ images/               README 그림 (.png) 과 원본 (.mmd)
│
├─ db/                      DB (민서 관리, 바꿀 때는 PR)
│  ├─ README.md             ERD, 설계 약속, MySQL 세팅, 테스트 계정
│  ├─ schema.sql            테이블 만들기 (실행하면 데이터 초기화)
│  ├─ seed.sql              테스트 데이터
│  ├─ check.sql             세팅 확인 (7줄 모두 OK면 성공)
│  └─ sample-uploads/       테스트용 후기 사진 · GPX 파일
│
├─ backend/                 백엔드 (VS Code로 열기) — 실행 방법은 backend/README.md
│  ├─ build.gradle          라이브러리 목록 (스프링부트 4.1)
│  ├─ gradlew               실행: ./gradlew bootRun (윈도우 cmd는 gradlew.bat bootRun)
│  └─ src/main/
│     ├─ java/kr/sanchaek/
│     │  ├─ SanchaekApplication.java   시작점
│     │  ├─ common/         공통 코드 (민서) · ★ HelloController (/api/hello)
│     │  ├─ match/          동행 · 댓글 · 관리자 AD3 (민서)
│     │  ├─ user/           계정 · 신고 · 관리자 AD1 · AD2 · AD4 (정란)
│     │  ├─ course/         코스 · GPX (동언)
│     │  ├─ review/         후기 (동언)
│     │  └─ landmark/       랜드마크 · 스탬프 · 배지 (동언)
│     └─ resources/         application.properties (공통) · application-local.properties (각자, 비밀번호)
│
└─ frontend/                프론트 (VS Code · WebStorm으로 열기)
   ├─ package.json          라이브러리 목록, npm run dev · dev:mock
   ├─ vite.config.js        개발 서버 설정, /api · /files 프록시
   ├─ .env.mock             npm run dev:mock 설정 (가짜 데이터)
   └─ src/
      ├─ main.jsx           React 시작점
      ├─ App.jsx            화면 주소 (라우터)
      ├─ pages/             화면 하나 = 파일 하나, Figma 화면 ID로 이름
      │  ├─ HealthCheck.jsx ★ 환경 확인용 테스트 화면 (/health)
      │  └─ admin/          관리자 화면 AD1 ~ AD4
      ├─ components/        공통 부품 (버튼, 헤더, 카드, 지도, 신고 창 …)
      ├─ api/               백엔드 호출 함수
      └─ mocks/             가짜 데이터 (npm run dev:mock 일 때만)
```

- **★ 표시는 환경 확인용 테스트 코드입니다. 지우지 마세요.**
- **자기 담당 폴더에서만 작업**하면 충돌이 거의 생기지 않습니다.

<br>

## 🚨 꼭 지킬 것

**🔒 비밀번호 · API 키는 절대 커밋하지 않기**

- DB 비밀번호는 각자 PC의 설정 파일에만 넣습니다 (깃허브에 안 올라가는 파일).
- 카카오 키처럼 같이 써야 하는 키는 커밋하지 말고 팀원에게 따로 전달합니다.
- 한 번이라도 커밋되면 기록에 남아서, 키를 새로 발급받아야 합니다.

**🚫 main · develop에 직접 push 하지 않기**

- 항상 내 브랜치 → PR ([브랜치 · PR 규칙](docs/development.md#2-브랜치pr-규칙))

**📣 같이 쓰는 파일은 고치기 전에 팀에 먼저 알리기**

- `db/schema.sql`, `backend/build.gradle`, `frontend/package.json`, 프론트 라우터

**🔄 작업 시작 전에 항상 `git pull`**

**⭐ 환경 확인용 테스트 코드(★) 지우지 않기**

<br>

## 🎬 시연 전 체크

시연은 노트북 한 대에 DB · 백엔드 · 프론트를 모두 띄웁니다.

1. **최신 코드**: `git checkout main` → `git pull`, 프론트 폴더에서 `npm install`
2. **DB 초기화**: `schema.sql` → `seed.sql` → `check.sql` (7줄 모두 OK). **시연 직전에** 합니다. 테스트 동행 날짜가 실행한 날 기준이라, 5일쯤 지나면 모집 중인 글이 사라집니다 ([DB 안내 4)](db/README.md#4-확인))
3. **업로드 폴더**: `db/sample-uploads`의 `gpx` · `reviews`를 복사 ([DB 안내 5)](db/README.md#5-업로드-폴더-만들기-후기-사진--gpx))
4. **설정 파일**: `application-local.properties`, `frontend/.env.local`의 카카오 키 (`localhost:5173`이 카카오 콘솔에 등록되어 있어야 지도가 나옴)
5. **실행**: 백엔드 `./gradlew bootRun`, 프론트 `npm run dev` (`dev:mock` 아님, 오른쪽 아래에 `MOCK` 배지가 없어야 함)
6. **8080을 다른 프로그램이 쓰고 있지 않은지** 확인: `http://localhost:8080/api/hello` 에서 `"landmarks":8`
