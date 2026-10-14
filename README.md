# Team_walk — 산책시루

7팀 산책크루의 정왕동 산책 동행 서비스 **산책시루** 저장소입니다.

> **처음 왔다면:** [빠른 시작](#빠른-시작)의 세 화면이 목표입니다. [세팅 가이드](docs/setup.md)를 순서대로 따라 하세요.
> **백엔드 담당**은 이어서 [DB 세팅](db/README.md#처음-세팅-윈도우-한-번만)까지 합니다 (프론트는 2주차에).
> 막히면 [막혔을 때](docs/troubleshooting.md)를 먼저 보세요. 지금까지 겪은 문제와 해결법이 모두 있습니다.

![산책시루 개발 구조: 브라우저 → Vite(5173) → Tomcat(8080) → DAO → MySQL(3306)](docs/images/architecture.png)

- 브라우저는 항상 **`localhost:5173`** (React)으로 접속합니다. `/api/…` 요청만 Vite가 이클립스 Tomcat(`8080`)으로 넘깁니다.
- 백엔드 Servlet은 DAO를 거쳐 MySQL(`3306`)의 `sanchaek` DB를 씁니다. 관리자 화면만 Tomcat의 JSP로 직접 엽니다.

---

## 문서 안내

| 문서 | 언제 보나 | 내용 |
| --- | --- | --- |
| **[세팅 가이드](docs/setup.md)** | 처음 한 번 | 설치, 폴더 배치, 이클립스 · Tomcat, 프론트, 환경 확인용 테스트 코드, 맥 사용자 부록 |
| **[DB 안내](db/README.md)** | 백엔드 1주차 시작 전, 테이블이 바뀔 때 | ERD, 설계 약속, MySQL 세팅, 테스트 계정, 쿼리 예시, DB 오류 |
| **[개발 규칙](docs/development.md)** | 작업할 때마다 | API · 화면 만드는 법, 브랜치 · PR 규칙, Git 명령어, 커밋 메시지 |
| **[막혔을 때](docs/troubleshooting.md)** | 오류가 났을 때 | 설치 · 이클립스 · Maven · 포트 · 프론트 · Git 문제와 해결법 |

## 관련 문서 (저장소 밖)

| 문서 | 내용 | 위치 |
| --- | --- | --- |
| 노션 | 역할 분배, 우선순위(1·2순위·보류), 일정 | 팀 노션 (디스코드 공지 링크) |
| Figma | 화면 설계, 화면 ID(A1, C1, W2 …), 📝 메모의 API(안) | [Figma 파일](https://www.figma.com/design/8g73YEapksdegpYXV7aPKp) |
| API 명세 | 주소, 요청·응답 예시, 담당자 | 노션의 "API 명세" 링크 |

---

## 빠른 시작

아래 화면이 모두 보이면 개발 환경 준비 끝입니다. **캡처해서 디스코드에 올려 주세요.**

| 주소 / 명령 | 보여야 하는 것 | 확인되는 것 |
| --- | --- | --- |
| `http://localhost:8080/` | **산책시루 서버가 동작합니다** / JSP: 정상 동작 / JSTL: JSTL 정상 동작 | 이클립스 + Tomcat + JSP |
| `http://localhost:8080/api/hello` | `{"success":true,"data":{"message":"안녕하세요, 산책시루 서버입니다", …}}` | Servlet + JSON |
| `http://localhost:5173/` | **산책시루 프론트가 동작합니다** + **✅ 연결 성공** | React + Vite + 백엔드 연결 |
| (백엔드) `source …/db/check.sql` | 5줄 모두 **OK** | MySQL + 테이블 + 테스트 데이터 ([DB 안내](db/README.md#4-확인)) |

---

## 기술 스택

| 항목 | 버전 / 도구 | 비고 |
| --- | --- | --- |
| Java (JDK) | **21** 설치 권장 (17 이상이면 됨) | 빌드 기준은 17 (`pom.xml`). 21로 빌드해도 문제없음 |
| 서버 | **Tomcat 10.1** | 팀 전원 같은 버전 |
| 백엔드 | JSP · Servlet (Jakarta EE 10), Maven | Maven은 이클립스에 내장되어 있어 따로 설치 안 해도 됨 |
| 백엔드 IDE | 이클립스 **Enterprise Java and Web Developers** | |
| 프론트 | **React 19 + Vite 8**, ESLint | |
| Node.js | **22.12 이상** (LTS) | Vite 8의 최소 요구 버전 |
| 프론트 IDE | VS Code 또는 WebStorm | |
| DB | **MySQL 8.4** (8.0도 가능) | ERD · 세팅 방법 · 테스트 계정은 [db/README.md](db/README.md) |
| 지도 | 카카오맵 JavaScript API (예정) | |
| 협업 | GitHub, 디스코드 | |

> ⚠️ **Tomcat 10.1부터는 `javax.servlet` 대신 `jakarta.servlet`** 을 씁니다. 인터넷 예제를 복사할 때 `import javax.…`를 `import jakarta.…`로 바꾸세요.
>
> ⚠️ **JSTL 주소도 바뀌었습니다:** `<%@ taglib prefix="c" uri="jakarta.tags.core" %>` (예전 `http://java.sun.com/jsp/jstl/core` 아님)

---

## 폴더 구조

```
Team_walk/
├─ README.md                         지금 보는 안내판
├─ docs/                             문서
│  ├─ setup.md                       세팅 가이드 (처음 한 번)
│  ├─ development.md                 개발 규칙 (브랜치·PR, Git, 커밋)
│  ├─ troubleshooting.md             막혔을 때
│  └─ images/                        README 그림 (.png) 과 그림 원본 (.mmd)
├─ db/                               DB (민서가 관리, 바꿀 때는 PR)
│  ├─ README.md                      ERD, 설계 약속, MySQL 세팅, 테스트 계정
│  ├─ schema.sql                     테이블 만들기 (실행하면 데이터 초기화)
│  ├─ seed.sql                       테스트 데이터
│  └─ check.sql                      세팅 확인 (5줄 모두 OK 면 성공)
├─ backend/                          백엔드 (이클립스로 여는 폴더)
│  ├─ pom.xml                        라이브러리 목록, 빌드 설정
│  └─ src/main/
│     ├─ java/kr/sanchaek/
│     │  ├─ common/                  공통 코드: DB 연결, 로그인 필터, 응답 형식 (민서)
│     │  │  └─ HelloServlet.java     ★ 환경 확인용 테스트 API (/api/hello)
│     │  ├─ match/        (예정)      동행 (민서)
│     │  ├─ user/         (예정)      계정, 후기, 신고 (정란)
│     │  └─ course/       (예정)      코스, GPX, 랜드마크 (동언)
│     ├─ resources/       (예정)      설정 파일 (DB 접속 정보 등)
│     └─ webapp/
│        ├─ index.jsp                ★ 환경 확인용 테스트 페이지
│        ├─ WEB-INF/views/admin/ (예정)  관리자 JSP
│        └─ error/        (예정)      404, 500 페이지
└─ frontend/                         프론트 (VS Code·WebStorm으로 여는 폴더)
   ├─ package.json                   라이브러리 목록 (백엔드의 pom.xml 역할)
   ├─ vite.config.js                 개발 서버 설정, /api 프록시
   ├─ index.html                     처음 열리는 빈 페이지
   └─ src/
      ├─ main.jsx                    React 시작점
      ├─ App.jsx                     ★ 환경 확인용 테스트 화면 (백엔드 연결 확인)
      ├─ pages/       (예정)          화면 단위 (Figma 화면 ID)
      ├─ components/  (예정)          공통 부품 (버튼, 헤더, 카드, 지도 …)
      ├─ api/         (예정)          백엔드 호출 함수
      └─ mocks/       (예정)          백엔드 완성 전 가짜 데이터
```

- **★ 표시 파일은 환경 확인용 테스트 코드입니다. 지우지 마세요.** ([세팅 가이드 5번](docs/setup.md#5-환경-확인용-테스트-코드) 참고)
- **자기 담당 폴더에서만 작업**하면 같은 파일을 동시에 고쳐서 생기는 충돌이 거의 없습니다.

---

## 꼭 지킬 것

- **비밀번호, API 키는 절대 커밋하지 않기** (DB 비밀번호, 카카오 키 등). 디스코드 비공개 채널로 공유합니다. 한 번이라도 커밋되면 기록에 남으니 키를 새로 발급받아야 합니다.
- **main, develop에 직접 push 금지.** 항상 feature 브랜치 → PR ([개발 규칙](docs/development.md#2-브랜치pr-규칙))
- **같이 쓰는 파일은 고치기 전에 디스코드에 알리기:** `db/schema.sql`, `backend/pom.xml`, `frontend/package.json`, 프론트 라우터
- **작업 시작 전에 항상 `git pull`**
- **환경 확인용 테스트 코드(★) 지우지 않기**
