# Team_walk — 산책시루

7팀 산책크루의 정왕동 산책 동행 서비스 **산책시루** 저장소입니다.

> **처음 왔다면:** [빠른 시작](#빠른-시작) → [설치](#1-설치-처음-한-번) → [백엔드 세팅](#3-백엔드-이클립스-세팅-처음-한-번) → [프론트 세팅](#4-프론트-세팅-처음-한-번) 순서로 따라 하세요.
> 막히면 [막혔을 때](#막혔을-때) 표를 먼저 보세요. 지금까지 겪은 문제와 해결법이 모두 있습니다.

## 관련 문서

| 문서 | 내용 | 위치 |
| --- | --- | --- |
| 노션 | 역할 분배, 우선순위(1·2순위·보류), 일정 | 팀 노션 (디스코드 공지 링크) |
| Figma | 화면 설계, 화면 ID(A1, C1, W2 …), 📝 메모의 API(안) | [Figma 파일](https://www.figma.com/design/8g73YEapksdegpYXV7aPKp) |
| API 명세 | 주소, 요청·응답 예시, 담당자 | 노션의 "API 명세" 링크 |

---

## 목차

- [빠른 시작](#빠른-시작)
- [기술 스택](#기술-스택)
- [폴더 구조](#폴더-구조)
- [1. 설치 (처음 한 번)](#1-설치-처음-한-번)
- [2. 저장소 받기와 폴더 배치](#2-저장소-받기와-폴더-배치)
- [3. 백엔드: 이클립스 세팅 (처음 한 번)](#3-백엔드-이클립스-세팅-처음-한-번)
- [4. 프론트 세팅 (처음 한 번)](#4-프론트-세팅-처음-한-번)
- [5. 환경 확인용 테스트 코드](#5-환경-확인용-테스트-코드)
- [6. 개발 방법](#6-개발-방법)
- [7. 브랜치·PR 규칙](#7-브랜치pr-규칙)
- [8. 작업 순서 (Git 명령어)](#8-작업-순서-git-명령어)
- [커밋 메시지](#커밋-메시지)
- [꼭 지킬 것](#꼭-지킬-것)
- [막혔을 때](#막혔을-때)
- [부록: 맥 사용자](#부록-맥-사용자)

---

## 빠른 시작

아래 세 화면이 모두 보이면 개발 환경 준비 끝입니다. **캡처해서 디스코드에 올려 주세요.**

| 주소 | 보여야 하는 것 | 확인되는 것 |
| --- | --- | --- |
| `http://localhost:8080/` | **산책시루 서버가 동작합니다** / JSP: 정상 동작 / JSTL: JSTL 정상 동작 | 이클립스 + Tomcat + JSP |
| `http://localhost:8080/api/hello` | `{"success":true,"data":{"message":"안녕하세요, 산책시루 서버입니다", …}}` | Servlet + JSON |
| `http://localhost:5173/` | **산책시루 프론트가 동작합니다** + **✅ 연결 성공** | React + Vite + 백엔드 연결 |

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
| DB | MySQL 8.4 (예정) | ERD 확정 후 세팅 방법 추가 |
| 지도 | 카카오맵 JavaScript API (예정) | |
| 협업 | GitHub, 디스코드 | |

> ⚠️ **Tomcat 10.1부터는 `javax.servlet` 대신 `jakarta.servlet`** 을 씁니다. 인터넷 예제를 복사할 때 `import javax.…`를 `import jakarta.…`로 바꾸세요.
>
> ⚠️ **JSTL 주소도 바뀌었습니다:** `<%@ taglib prefix="c" uri="jakarta.tags.core" %>` (예전 `http://java.sun.com/jsp/jstl/core` 아님)

---

## 폴더 구조

```
Team_walk/
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
├─ frontend/                         프론트 (VS Code·WebStorm으로 여는 폴더)
│  ├─ package.json                   라이브러리 목록 (백엔드의 pom.xml 역할)
│  ├─ vite.config.js                 개발 서버 설정, /api 프록시
│  ├─ index.html                     처음 열리는 빈 페이지
│  └─ src/
│     ├─ main.jsx                    React 시작점
│     ├─ App.jsx                     ★ 환경 확인용 테스트 화면 (백엔드 연결 확인)
│     ├─ pages/       (예정)          화면 단위 (Figma 화면 ID)
│     ├─ components/  (예정)          공통 부품 (버튼, 헤더, 카드, 지도 …)
│     ├─ api/         (예정)          백엔드 호출 함수
│     └─ mocks/       (예정)          백엔드 완성 전 가짜 데이터
├─ db/                (예정)          schema.sql (테이블), seed.sql (테스트 데이터)
└─ docs/              (예정)          API 명세, ERD
```

- **★ 표시 파일은 환경 확인용 테스트 코드입니다. 지우지 마세요.** ([5번](#5-환경-확인용-테스트-코드) 참고)
- **자기 담당 폴더에서만 작업**하면 같은 파일을 동시에 고쳐서 생기는 충돌이 거의 없습니다.

---

## 1. 설치 (처음 한 번)

> 팀원 대부분이 **윈도우**라 윈도우 기준으로 씁니다. 맥은 [부록](#부록-맥-사용자)을 보세요.
>
> 💡 **한글·공백이 없는 경로에 설치하세요.** 예: `C:\dev\…`
> `C:\Users\김동언\바탕 화면\…` 같은 경로는 Maven, Tomcat, 이클립스에서 알 수 없는 오류를 일으키기 쉽습니다.

| 순서 | 설치 | 방법 | 확인 |
| --- | --- | --- | --- |
| 1 | **Git** | [git-scm.com](https://git-scm.com) → Windows 설치 파일. 옵션은 기본값 그대로 (**Git Bash**가 같이 설치됨) | `git --version` |
| 2 | **JDK 21** | [adoptium.net](https://adoptium.net) → **Temurin 21 (LTS)** → Windows x64 **.msi**. 설치 중 **"Set JAVA_HOME variable"** 항목을 클릭해서 **"Will be installed on local hard drive"** 로 바꾸기 (기본값은 꺼져 있음) | 새 터미널에서 `java -version` → `21…` |
| 3 | **Tomcat 10.1** | [tomcat.apache.org](https://tomcat.apache.org) → Download의 **Tomcat 10** → 10.1.x의 **Binary Distributions → Core → "64-bit Windows zip"** → `C:\dev\`에 압축 풀기 | `C:\dev\apache-tomcat-10.1.xx\bin\bootstrap.jar` 가 있으면 정상 |
| 4 | **이클립스** | [eclipse.org](https://www.eclipse.org/downloads/packages/) → **Eclipse IDE for Enterprise Java and Web Developers** | |
| 5 | **Node.js** | [nodejs.org](https://nodejs.org) → **LTS** 설치 파일 | `node -v` → `v22.12` 이상 |
| 6 | **VS Code** (프론트 담당) | [code.visualstudio.com](https://code.visualstudio.com) + 확장 프로그램 **ESLint** | |

> ⚠️ **Tomcat은 반드시 zip으로.** "Windows Service Installer"(.exe)나 이름에 `-src`가 붙은 파일(소스 코드)을 받으면 이클립스에서 `Bootstrap을 찾을 수 없습니다` 오류가 납니다.
>
> 💡 **터미널은 Git Bash를 쓰세요.** 이 README의 명령어(`rm -rf`, `find` 등)가 그대로 동작합니다. 시작 메뉴 → "Git Bash".

---

## 2. 저장소 받기와 폴더 배치

**이클립스 워크스페이스와 저장소를 반드시 나란히, 분리해서 둡니다.**

```
C:\dev\jspTeam\
├─ workspace\     ← 이클립스 워크스페이스 (이클립스 개인 설정만 들어감, GitHub에 안 올라감)
└─ Team_walk\     ← 이 저장소 (팀 코드)
```

Git Bash에서:

```bash
mkdir -p /c/dev/jspTeam/workspace
cd /c/dev/jspTeam
git clone https://github.com/AnemoneCoronaria/Team_walk.git
cd Team_walk
git checkout develop
```

> ⚠️ **워크스페이스를 `Team_walk` 안이나 `backend` 안에 만들면 안 됩니다.**
> 프로젝트 불러오기가 아무 반응 없이 실패하거나, 엉뚱한 `src` 프로젝트가 생기고, 이클립스 설정 파일이 GitHub에 섞여 올라갈 수 있습니다. (실제로 겪은 문제)

---

## 3. 백엔드: 이클립스 세팅 (처음 한 번)

> 메뉴 이름은 윈도우 기준입니다. 맥은 `Window → Preferences` 대신 `Eclipse → Settings`.

### 3-1. 워크스페이스 열기

- 이클립스 실행 → Workspace: `C:\dev\jspTeam\workspace` → **Launch**
- 이미 다른 워크스페이스로 열려 있다면: **File → Switch Workspace → Other…**
- ✅ **창 맨 위 제목이 `workspace - Eclipse IDE`** 인지 확인 (제목에는 워크스페이스 폴더 이름이 나옵니다)

### 3-2. JDK 등록

> JDK, Tomcat 등록은 **워크스페이스마다 따로 저장**됩니다. 워크스페이스를 새로 만들면 다시 해야 합니다.

- **Window → Preferences → Java → Installed JREs** → **Add…** → **Standard VM** → Next
- **JRE home:** **Directory…** 버튼으로 JDK **폴더**를 선택
  - 윈도우 예: `C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x-hotspot`
  - ⚠️ `…\bin\java.exe` 까지 넣으면 안 됩니다. **`bin`이 들어 있는 상위 폴더**까지만
- 아래 JRE system libraries 목록이 자동으로 채워지면 정상 → **Finish**
- 목록에서 방금 추가한 JDK에 **체크** → **Apply and Close**

### 3-3. Tomcat 등록

- **Window → Preferences → Server → Runtime Environments** → **Add…**
- **Apache → Apache Tomcat v10.1** → Next
- **Tomcat installation directory:** `C:\dev\apache-tomcat-10.1.xx` (**`bin`, `conf`, `lib`가 바로 보이는 폴더**)
- **JRE:** 3-2에서 등록한 JDK → **Finish** → **Apply and Close**

### 3-4. 프로젝트 불러오기

- 왼쪽 Project Explorer의 **Import projects…** 링크 (또는 **File → Import…**)
- **Maven → Existing Maven Projects** → Next
- **Root Directory:** `C:\dev\jspTeam\Team_walk\backend` → `/pom.xml` 이 체크되어 있는지 확인 → **Finish**
- 오른쪽 아래 진행 표시가 끝날 때까지 기다리기 (처음엔 라이브러리를 내려받느라 1~2분)
- ✅ 왼쪽에 **`sanchaek`** 프로젝트가 보이면 성공 → 우클릭 → **Maven → Update Project** (Alt+F5) → OK

> Problems 창에 **`cvc-elt.1.a`**, **`Downloading external resources is disabled`** 두 개만 남으면 정상입니다 (XML 검사기 경고, 무시).

### 3-5. 서버 만들고 실행

1. 아래쪽 **Servers** 탭 (없으면 **Window → Show View → Servers**) → "No servers are available. Click this link…" 클릭
2. **Apache → Tomcat v10.1 Server** → Next → 왼쪽 **`sanchaek`** 를 **Add >** → **Finish**
3. **Context root를 `/`로 맞추기 (중요, 두 곳 모두 확인)**
   1. `sanchaek` 우클릭 → **Properties → Web Project Settings** → Context root: **`/`** → Apply and Close
   2. Servers 탭의 Tomcat **더블클릭** → 아래쪽 **Modules** 탭 → `sanchaek`의 Path가 `/sanchaek`이면 **Edit…** → **`/`** → OK → **Ctrl+S**
4. Tomcat 우클릭 → **Start** → 아래 Console에 `Server startup in […] milliseconds` 가 나오면 켜진 것
5. 브라우저에서 확인:
   - `http://localhost:8080/` → **산책시루 서버가 동작합니다**
   - `http://localhost:8080/api/hello` → JSON 응답

> **❓ 왜 Context root가 `/`여야 하나?**
> 이클립스는 기본으로 주소를 `localhost:8080/sanchaek/…` 처럼 **프로젝트 이름 아래**에 둡니다.
> 그런데 React는 `/api/hello` 처럼 **주소 맨 앞(루트)** 으로 요청하고, Vite가 그 요청을 `localhost:8080/api/hello` 로 그대로 넘깁니다.
> 주소가 `/sanchaek` 아래에 있으면 **JSP 페이지는 뜨는데 React에서는 전부 404**가 납니다. 배포할 때도 루트(`/`)에 올릴 예정이라 개발 때부터 맞춰 둡니다.

> **❓ `localhost:8080/`에 원래 나오던 Tomcat 고양이 화면은?**
> 이클립스는 Tomcat을 실행할 때 Tomcat 설치 폴더를 그대로 쓰지 않고, 워크스페이스 안에 따로 실행 공간을 만들어 우리 프로젝트만 올립니다.
> 그래서 이클립스로 켜면 `/`에 우리 프로젝트가 나오고, Tomcat 폴더의 `bin\startup.bat`(맥은 `bin/catalina.sh run`)으로 직접 켜면 원래 고양이 화면이 나옵니다.
> **둘 다 8080 포트를 쓰니 동시에 켜지 마세요.**

---

## 4. 프론트 세팅 (처음 한 번)

```bash
cd /c/dev/jspTeam/Team_walk/frontend
npm install        # 처음 한 번, 그리고 package.json이 바뀌었을 때마다
npm run dev        # 개발 서버 시작 → http://localhost:5173
```

- 끌 때는 터미널에서 **Ctrl+C**
- **이클립스 Tomcat이 켜져 있어야** 화면에 ✅ 연결 성공이 뜹니다. Tomcat이 꺼져 있으면 ❌ 연결 실패가 정상입니다.

| 명령 | 하는 일 |
| --- | --- |
| `npm install` | `package.json`에 적힌 라이브러리를 `node_modules/`에 설치 (GitHub엔 안 올라감) |
| `npm run dev` | 개발 서버. 파일을 저장하면 화면이 바로 바뀜 |
| `npm run lint` | 코드 검사 (ESLint). PR 올리기 전에 한 번 |
| `npm run build` | 배포용 파일 만들기 → `dist/` |

### 프론트와 백엔드는 이렇게 연결됩니다

```
브라우저 → localhost:5173 (Vite, React 화면)
              │  /api/… 로 시작하는 요청만
              ▼
           localhost:8080 (이클립스 Tomcat, Servlet)
```

- 이 연결은 `frontend/vite.config.js`의 **`proxy`** 설정이 해 줍니다.
- 같은 주소(5173)에서 오는 것처럼 보이기 때문에 **CORS 오류가 없고, 로그인 세션 쿠키도 그대로 동작**합니다.
- 그래서 프론트 코드에서는 항상 `fetch('/api/…')` 처럼 **주소 앞부분 없이** 씁니다. (`http://localhost:8080/api/…` ❌)

---

## 5. 환경 확인용 테스트 코드

새로 온 사람이 세팅이 제대로 됐는지 확인하는 기준입니다. **지우거나 내용을 바꾸지 마세요.**

| 파일 | 주소 | 확인하는 것 |
| --- | --- | --- |
| `backend/src/main/webapp/index.jsp` | `localhost:8080/` | JSP, JSTL(`jakarta.tags.core`)이 동작하는지 |
| `backend/src/main/java/kr/sanchaek/common/HelloServlet.java` | `localhost:8080/api/hello` | Servlet, Gson(JSON), 공통 응답 형식 `{ success, data }` |
| `frontend/src/App.jsx` | `localhost:5173/` | React 동작 + 프록시를 거쳐 백엔드 `/api/hello`에 연결되는지 |

> 나중에 실제 메인 화면을 만들 때 `App.jsx`가 바뀌게 되면, 이 테스트 화면은 `src/pages/HealthCheck.jsx` 같은 별도 화면으로 옮겨서 유지합니다.

---

## 6. 개발 방법

### 공통 원칙

1. **API 명세가 약속입니다.** 주소, 요청, 응답 모양은 API 명세를 따릅니다. 바꿔야 하면 **명세를 먼저 고치고 짝에게 알립니다.**
2. **서로 기다리지 않습니다.** 프론트는 가짜 데이터로 먼저 만들고, 백엔드는 명세대로 먼저 만듭니다.
3. **우선순위대로 합니다.** 노션의 1순위 → (시간 남으면) 2순위. 보류 항목은 하지 않습니다.

### 백엔드: API 하나 만드는 법

| 항목 | 규칙 | 예시 |
| --- | --- | --- |
| 위치 | `kr.sanchaek.<담당 도메인>` | 동행이면 `kr.sanchaek.match` |
| 요청 처리 | `…Servlet` + `@WebServlet("/api/…")` | `MatchListServlet` → `@WebServlet("/api/matches")` |
| DB 접근 | `…Dao` | `MatchDao` |
| 데이터 묶음 | 이름 그대로 | `Match` |
| 응답 | 공통 형식 `{ "success": true, "data": … }` / 실패 `{ "success": false, "error": { "code", "message" } }` | `HelloServlet` 참고 |
| 한글 | `resp.setContentType("application/json; charset=UTF-8")` | |

- DB 연결, 로그인 확인 필터, 응답 형식 만들기는 **공통 코드(`common`, 민서 담당)를 가져다 씁니다.** 각자 따로 만들지 않습니다.
- 다 만들면 브라우저나 `curl`로 호출해서 **API 명세의 응답 예시와 모양이 같은지** 확인합니다.
- 관리자 화면은 React가 아니라 **Servlet + JSP**로 만듭니다 (`WEB-INF/views/admin/`). JSP 과목의 핵심 산출물입니다.

### 프론트: 화면 하나 만드는 법

| 폴더 | 넣는 것 | 예시 |
| --- | --- | --- |
| `src/pages/` | 화면 하나 = 파일 하나. **Figma 화면 ID**를 이름 앞에 | `W1MatchList.jsx`, `C2CourseDetail.jsx` |
| `src/components/` | 여러 화면이 같이 쓰는 부품 | `Header.jsx`, `CourseCard.jsx`, `KakaoMap.jsx` |
| `src/api/` | 백엔드 호출 함수 (도메인별 한 파일) | `matchApi.js`의 `getMatches()` |
| `src/mocks/` | 백엔드 완성 전 가짜 데이터 (API 명세의 응답 예시 복사) | `matches.json` |

1. 백엔드가 아직이면 `src/api/`의 함수가 `src/mocks/`의 가짜 데이터를 돌려주게 만들고 화면부터 완성
2. 백엔드 API가 develop에 합쳐지면 **`src/api/`의 함수만** 실제 `fetch('/api/…')`로 교체 (화면 코드는 그대로)
3. 공통 부품은 담당자(동훈: 지도·헤더·카드 / 승민: 버튼·입력창·모달)가 만들고 서로 가져다 씀

### 같이 테스트하는 법

1. 이클립스에서 Tomcat Start (8080)
2. `frontend`에서 `npm run dev` (5173)
3. 브라우저는 항상 **`localhost:5173`** 으로 접속

### DB (예정)

ERD가 확정되면 `db/schema.sql`(테이블), `db/seed.sql`(테스트 데이터)과 MySQL 세팅 방법을 추가합니다. **테이블을 바꿀 때는 반드시 `schema.sql`을 고치고 디스코드에 알립니다.**

---

## 7. 브랜치·PR 규칙

```
main ─────────────────────────────●─────────────  제출·발표용 (팀장이 주기적으로 병합)
                                  ↑ develop → main PR (팀장)
develop ──●────●────●────●────●───●─────────────  모든 작업이 모이는 곳
          ↑    ↑    ↑    ↑    ↑
          │    │    │    │    └ feature/W2-match-detail  (민서)
          │    │    │    └ feature/C1-course-map          (동훈)
          │    │    └ feature/A1-login                    (정란)
          │    └ feature/frontend-setup   (완료)
          └ feature/backend-setup         (완료)
```

| 브랜치 | 규칙 |
| --- | --- |
| `main` | 제출·발표용. **직접 push 금지.** 팀장이 develop → main PR로만 병합 (주차 마감, 제출·발표 전) |
| `develop` | 통합 브랜치. **직접 push 금지.** feature 브랜치의 PR로만 합침 |
| `feature/화면ID-기능` | **기능 하나마다 develop에서 새로 만들고, 합치면 삭제.** 예: `feature/A1-login`, `feature/C1-course-map` |

### PR(Pull Request) 규칙

- **대상(base)은 항상 `develop`** (main 아님)
- **리뷰어:** 백엔드 PR은 **다른 백엔드 1명**, 프론트 PR은 **다른 프론트 1명**이 확인 후 합침
- 하루 넘게 리뷰가 안 되면 팀장이 확인
- 합친 뒤 **Delete branch** 버튼으로 feature 브랜치 삭제 (코드는 develop에 남아 있으니 지워도 안전)

### PR 올리기 전 체크리스트

- [ ] 내 컴퓨터에서 실행해 봤다 (백엔드: 해당 API 호출 / 프론트: 화면 확인 + `npm run lint`)
- [ ] 최신 develop을 반영했다 (`git pull origin develop`)
- [ ] 올라갈 파일을 확인했다 (`git add --dry-run <폴더>`): `node_modules`, `target`, `.metadata`, `.project`, `.settings`, `Servers` 가 **없어야 함**
- [ ] 자기 담당 폴더만 고쳤다 (공용 파일을 고쳤으면 디스코드에 알렸다)
- [ ] 비밀번호·API 키가 들어가지 않았다

---

## 8. 작업 순서 (Git 명령어)

> ⚠️ **새 브랜치를 만들기 전에, 내가 올린 이전 PR이 develop에 합쳐졌는지 먼저 확인하세요.**
> 안 합쳐진 상태에서 develop 기반으로 새 브랜치를 만들면, 이전 PR의 파일이 작업 폴더에서 **사라진 것처럼** 보입니다. (지워진 게 아니라 아직 develop에 없는 것. 실제로 겪은 문제)

### 기능 하나 작업할 때마다

```bash
# 1. 최신 develop 받기
git checkout develop
git pull

# 2. 내 작업 브랜치 만들기
git checkout -b feature/W2-match-detail

# 3. 작업 … (작게, 자주 커밋)

# 4. 올라갈 파일 미리 확인 (실제로 추가하지 않고 목록만 보여줌)
git status
git add --dry-run backend        # 프론트면 frontend

# 5. 확인했으면 추가하고 커밋
git add backend
git commit -m "feat: 동행 상세 API 추가"

# 6. PR 올리기 전에 그사이 바뀐 develop 반영
git pull origin develop

# 7. GitHub에 올리기
git push -u origin feature/W2-match-detail
```

8. GitHub 저장소 페이지의 **Compare & pull request** → **base: `develop`** 확인 → Create pull request
9. 리뷰어가 확인하고 **Merge** → **Delete branch**
10. 다음 기능은 다시 1번부터

### 충돌(conflict)이 났을 때

`git pull origin develop` 에서 `CONFLICT` 가 나오면:

1. `git status` → 빨간색 `both modified` 파일 확인
2. 파일을 열면 이런 표시가 있습니다:
   ```
   <<<<<<< HEAD
   내가 고친 내용
   =======
   develop에 먼저 들어간 내용
   >>>>>>> …
   ```
3. 둘 중 맞는 내용(또는 둘 다)만 남기고 `<<<<<<<`, `=======`, `>>>>>>>` 줄을 지움
4. `git add <파일>` → `git commit` → 다시 실행해서 확인
5. **남의 코드라서 판단이 안 서면 혼자 고치지 말고** 그 파일 담당자나 팀장에게 물어보기

> `package-lock.json` 충돌은 develop 쪽 내용을 받은 뒤 `npm install` 을 다시 하면 대부분 해결됩니다.

---

## 커밋 메시지

| 머리말 | 언제 | 예시 |
| --- | --- | --- |
| `feat:` | 기능 추가 | `feat: 코스 목록 API 추가` |
| `fix:` | 버그 수정 | `fix: 로그인 한글 깨짐 수정` |
| `docs:` | 문서 | `docs: API 명세 수정` |
| `style:` | 화면·코드 모양만 변경 | `style: 버튼 간격 조정` |
| `refactor:` | 동작은 같고 구조만 개선 | `refactor: DB 연결 코드 정리` |
| `chore:` | 설정, 폴더 등 기타 | `chore: .gitignore 추가` |

---

## 꼭 지킬 것

- **비밀번호, API 키는 절대 커밋하지 않기** (DB 비밀번호, 카카오 키 등). 디스코드 비공개 채널로 공유합니다. 한 번이라도 커밋되면 기록에 남으니 키를 새로 발급받아야 합니다.
- **main, develop에 직접 push 금지.** 항상 feature 브랜치 → PR
- **같이 쓰는 파일은 고치기 전에 디스코드에 알리기:** `db/schema.sql`, `backend/pom.xml`, `frontend/package.json`, 프론트 라우터
- **작업 시작 전에 항상 `git pull`**
- **환경 확인용 테스트 코드(★) 지우지 않기**

---

## 막혔을 때

지금까지 실제로 겪고 해결한 문제들입니다. 증상을 찾아서 해결 순서대로 해 보고, 그래도 안 되면 **화면 캡처와 함께 디스코드에** 올려 주세요.

### 설치 · Java · Tomcat

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `java -version` 은 되는데 Tomcat이 **`Unable to locate a Java Runtime`** / `JAVA_HOME … is not defined` | 시스템이 JDK 위치를 모름 | **윈도우:** JDK 설치 때 "Set JAVA_HOME variable"을 켜고 다시 설치 (또는 시스템 환경 변수에 `JAVA_HOME` = JDK 폴더 추가) / **맥:** [부록](#부록-맥-사용자) |
| JDK 추가 시 **`The home directory does not exist`** | JRE home에 `…\bin\java.exe`(맥 `…/bin/java`)까지 넣었거나, 붙여넣을 때 공백·줄바꿈이 섞임 | JRE home은 **`bin`의 상위 폴더**까지만. 직접 입력하지 말고 **Directory…** 버튼으로 선택 |
| Tomcat 시작 시 **`기본 클래스 org.apache.catalina.startup.Bootstrap을(를) 찾거나 로드할 수 없습니다`** | ① Tomcat을 zip이 아닌 설치 파일(.exe)이나 `-src`(소스) 버전으로 받음 ② Tomcat 폴더를 한 단계 잘못 지정 ③ 이클립스의 JDK·Tomcat 등록이 꼬임 | ① `bin\bootstrap.jar`가 있는지 확인, 없으면 **Core zip**으로 다시 받기 ② `bin`, `conf`, `lib`가 바로 보이는 폴더로 지정 ③ Servers의 서버 Delete → **Run → Run Configurations…** 에서 Apache Tomcat 항목 Delete → Preferences의 Server Runtime Remove → [3-2](#3-2-jdk-등록)부터 다시 |
| Tomcat이 이클립스에서 문제인지 Tomcat 자체 문제인지 모르겠음 | | 이클립스 서버를 끄고 터미널에서 Tomcat만 단독 실행: 윈도우 `C:\dev\apache-tomcat-10.1.xx\bin\catalina.bat run` (맥 `…/bin/catalina.sh run`) → `localhost:8080`에 고양이 화면이 뜨면 Tomcat·Java는 정상 → 이클립스 설정 문제. 확인 후 **Ctrl+C로 끄기** |

### 이클립스 워크스페이스 · 프로젝트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 창 제목이 `workspace - Eclipse IDE` 가 아니라 **`backend - Eclipse IDE`** 등 | 워크스페이스가 저장소 안(`backend`)에 열려 있음 | **File → Switch Workspace → Other…** → `C:\dev\jspTeam\workspace`. 그다음 아래 행처럼 찌꺼기 정리 |
| Import에서 **Finish를 눌러도 아무 반응 없음**, 또는 엉뚱한 **`src` 프로젝트**가 생김, Import 목록에 `Folder already imported as project` | 워크스페이스가 `backend`(또는 저장소) 안에 있어서 이클립스가 폴더를 이미 프로젝트로 착각 | ① 워크스페이스를 [2번](#2-저장소-받기와-폴더-배치)처럼 분리 ② 잘못 생긴 프로젝트는 우클릭 → Delete (**"Delete project contents on disk" 체크 해제!**) ③ `backend` 안의 이클립스 파일 삭제: `cd backend && rm -rf .metadata Servers RemoteSystemsTempFiles .project .classpath .settings target src/bin src/.project src/.classpath` ④ [3-4](#3-4-프로젝트-불러오기)부터 다시 |
| 탐색기(파인더)엔 파일이 있는데 **Project Explorer가 비어 있음** | Project Explorer는 폴더가 아니라 **워크스페이스에 등록된 프로젝트**만 보여줌. 워크스페이스를 바꾸면 다시 불러와야 함 | [3-4](#3-4-프로젝트-불러오기)로 불러오기 |
| **File 메뉴에 Import가 안 보임** | (맥) 화면 맨 위 메뉴가 다른 앱(Finder 등)의 메뉴 | 이클립스 창을 한 번 클릭한 뒤 메뉴 열기. 또는 Project Explorer의 **Import projects…** 링크 |
| `Open Projects from File System`에서 **pom.xml이 선택 안 됨** | 그 창은 파일이 아니라 **폴더**를 고르는 창 | `backend` **폴더**를 고르면 이클립스가 pom.xml을 알아서 찾음. 목록에서 `backend` 줄만 체크 |
| 브랜치를 바꿨더니 **이클립스에서 파일·폴더(webapp 등)가 사라짐** | 아직 develop에 합쳐지지 않은 PR의 파일은 develop 기반 브랜치에서 안 보임 (**지워진 게 아님**) | 그 PR을 develop에 합친 뒤 `git pull origin develop` → 이클립스에서 프로젝트 우클릭 → **Refresh (F5)** |
| (참고) **새 Maven 프로젝트를 만들 때** Next 버튼이 안 눌림 | Location 칸에 붙여넣기만 해서 이클립스가 입력을 감지 못함 | 칸 끝에 스페이스를 쳤다 지우거나 Tab, 또는 Browse…로 선택 |
| (참고) 새 Maven 프로젝트 화면이 **"Specify Archetype parameters"** 로 나옴 | "Create a simple project" 체크가 풀림 | Back → **Create a simple project** 체크 → Next → **Packaging: war** 가 보이는 화면이 맞음 |

### Maven · 라이브러리

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| **`Could not transfer artifact … from/to central (https://repo.maven.apache.org/maven2)`**, `UnknownHostException`, `Missing artifact …` 가 잔뜩 | 이클립스가 라이브러리 서버에 접속 못 함 (처음 켤 때 네트워크 불안정 등). **한 번 실패하면 실패 기록이 남아 다시 시도하지 않음** (`This failure was cached`) | ① **Preferences → General → Network Connections** → Active Provider: **Direct** ② **Preferences → Maven** → **Offline 체크 해제** ③ 실패 기록 삭제: `find ~/.m2/repository -name "*.lastUpdated" -delete` ④ 이클립스 **File → Restart** ⑤ 프로젝트 우클릭 → Maven → Update Project → **"Force Update of Snapshots/Releases" 체크** |
| 위 방법으로도 안 됨 | 이클립스만 인터넷이 막힘 | 터미널에서 직접 받기: Maven 설치 후 `cd backend && mvn -U package` → `BUILD SUCCESS` 후 이클립스에서 Force Update 한 번 더 (이클립스와 터미널이 같은 `~/.m2`를 씀) |
| **`jakarta … cannot be resolved`** | 라이브러리가 프로젝트에 반영 안 됨 | 프로젝트 우클릭 → **Maven → Update Project** |
| `index.jsp`에 **`jakarta.servlet.jsp.PageContext / JspException cannot be resolved`** | JSP 라이브러리 미반영 (`pom.xml`의 `jakarta.servlet.jsp-api`) | Maven → Update Project. 계속 나오면 Tomcat 서버를 만들고 프로젝트를 Add |
| pom.xml에 **`cvc-elt.1.a`**, **`Downloading external resources is disabled`** | XML 검사기 경고 | **무시해도 됨.** 거슬리면 Preferences → XML (Wild Web Developer) → "Download external resources" 켜기 |

### 실행 · 주소 · 포트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `localhost:8080/` 은 404인데 **`localhost:8080/sanchaek/` 에서는 뜸** | Context root가 `/sanchaek` (이클립스 기본값) | [3-5](#3-5-서버-만들고-실행)의 **Context root 두 곳**을 `/`로 → Tomcat 우클릭 **Clean…** → **Restart** |
| JSP는 뜨는데 React 화면에서 **`❌ 연결 실패 (HTTP 404)`** | 위와 같음. React는 `/api/…`(루트)로 요청함 | 위와 같음 |
| JSP는 뜨는데 **`/api/…` 만 404** (Git으로 파일이 바뀐 뒤 자주 생김) | Servlet 컴파일 결과가 Tomcat에 반영 안 됨 (JSP는 Tomcat이 직접 처리하지만 Servlet은 이클립스가 컴파일해서 넘겨야 함) | **Project → Build Automatically** 체크 확인 → **Project → Clean…** → Servers의 Tomcat 우클릭 **Clean…** → **Restart** |
| **`Port 8080 required by Tomcat … is already in use`** | 다른 프로그램이 8080 사용 중. ① 터미널에서 켠 Tomcat ② **Oracle DB(XE 11g 등)** 가 8080을 씀 (수업에서 Oracle을 깔았다면 의심) | ① 그 터미널에서 Ctrl+C ② 누가 쓰는지 확인: `netstat -ano \| findstr :8080` → 마지막 숫자(PID)로 `tasklist /fi "PID eq 숫자"`. Oracle이면 SQL*Plus에서 `EXEC DBMS_XDB.SETHTTPPORT(9090);` 로 Oracle 쪽 포트를 옮기기 (Tomcat을 8081로 바꾸면 `vite.config.js`도 같이 바꿔야 해서 비추천) |
| `localhost:8080/`에 **고양이 화면**이 나옴 | 이클립스가 아니라 터미널(`startup.bat`, `catalina.sh`)로 켠 Tomcat | 터미널 Tomcat 끄고 이클립스에서 Start |

### 프론트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 5173 화면에 **`❌ 연결 실패`** (404 외의 메시지) | 이클립스 Tomcat이 꺼져 있음 | 이클립스에서 Tomcat Start → `localhost:8080/api/hello` 가 열리는지 먼저 확인 |
| `npm run dev` 에서 버전 오류 | Node가 22.12보다 낮음 | `node -v` 확인 → nodejs.org에서 LTS 다시 설치 |
| PowerShell에서 **`npm.ps1 … 이 시스템에서 스크립트를 실행할 수 없으므로`** | 윈도우 PowerShell 보안 정책 | **Git Bash**나 명령 프롬프트(cmd)에서 실행. 또는 PowerShell에서 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| 처음 실행할 때 **윈도우 방화벽 경고** | Node·Java가 네트워크를 쓰려고 함 | "개인 네트워크" 허용 |

### Git

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `git status` 에 `backend/` 처럼 **폴더만 보여서** 뭐가 올라갈지 모르겠음 | 새 폴더는 통째로 표시됨 | `git add --dry-run backend` → 실제로 올라갈 파일 목록 확인 |
| 올라갈 목록에 **`.metadata`, `Servers`, `.project`, `target`, `node_modules`** 가 보임 | 워크스페이스가 저장소 안에 있거나 `.gitignore`가 없는 브랜치 | **커밋하지 말고** 워크스페이스 위치부터 고치기 ([이클립스 표](#이클립스-워크스페이스--프로젝트) 참고). 이미 커밋했다면 팀장에게 |
| 아무것도 안 고쳤는데 **파일 전체가 바뀐 것**으로 나옴 | 맥(LF)과 윈도우(CRLF)의 줄바꿈 차이 | `.gitattributes`로 자동 처리됨. 계속 나오면 팀장에게 |
| `git push` 가 **`rejected … (fetch first)`** | 그사이 원격에 다른 커밋이 올라옴 | `git pull` 후 다시 `git push` |

---

## 부록: 맥 사용자

| 윈도우 | 맥 |
| --- | --- |
| `Window → Preferences` | `Eclipse → Settings` (⌘,) |
| Ctrl+S, Alt+F5 | ⌘S, ⌥F5 |
| `C:\dev\jspTeam\…` | `~/programming/jspTeam/…` 등 원하는 곳 |
| `bin\catalina.bat run` | `bin/catalina.sh run` |

### Homebrew로 Java를 설치한 경우 (중요)

Homebrew의 `openjdk@21`은 설치해도 **macOS 시스템에 등록되지 않습니다.** 그래서 터미널의 `java -version`은 되는데, Tomcat은 `Unable to locate a Java Runtime`을 띄웁니다. 한 번만 등록하면 됩니다.

```bash
# macOS에 JDK 등록
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
/usr/libexec/java_home -V            # 목록에 21이 보이면 성공

# JAVA_HOME 설정
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
```

이클립스의 JRE home에는 아래 둘 중 하나를 쓰면 됩니다 (같은 JDK).

| 경로 | 특징 |
| --- | --- |
| `/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home` | **추천.** `brew upgrade`로 버전이 바뀌어도 경로가 그대로 |
| `/opt/homebrew/Cellar/openjdk@21/21.0.x/libexec/openjdk.jdk/Contents/Home` | 실제 위치. 업데이트하면 버전 번호가 바뀌어 경로가 사라짐 |

> 맥의 폴더 선택 창은 `/opt` 같은 숨김 폴더가 안 보이고 `.jdk`를 하나의 파일로 취급합니다. **Directory…** 를 누른 뒤 **⌘⇧G** 로 경로를 직접 입력하세요.
