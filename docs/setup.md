# 세팅 가이드 (처음 한 번)

[← README](../README.md) · [DB 안내](../db/README.md) · [개발 규칙](development.md) · [막혔을 때](troubleshooting.md)

새로 온 사람이 개발 환경을 갖추는 순서입니다.<br>
끝나면 [README의 빠른 시작](../README.md#-빠른-시작) 세 가지가 모두 보여야 합니다.

> [!NOTE]
> **백엔드는 VS Code + 스프링부트 4.1, 실행은 Gradle `bootRun`입니다.** (이클립스 · Tomcat · JSP는 쓰지 않음)<br>
> **백엔드 담당**은 1 · 2번 → [DB 세팅](../db/README.md#처음-세팅-윈도우-한-번만) → 3번, **프론트 담당**은 1 · 2 · 4번을 하면 됩니다.

## 목차

1. [설치 (처음 한 번)](#1-설치-처음-한-번)
2. [저장소 받기와 폴더 배치](#2-저장소-받기와-폴더-배치)
3. [백엔드: 스프링부트](#3-백엔드-스프링부트)
4. [프론트 세팅 (처음 한 번)](#4-프론트-세팅-처음-한-번)
5. [환경 확인용 테스트 코드](#5-환경-확인용-테스트-코드)
6. [부록: 맥 사용자](#부록-맥-사용자)

---

## 1. 설치 (처음 한 번)

> 팀원 대부분이 **윈도우**라 윈도우 기준으로 씁니다. 맥은 [부록](#부록-맥-사용자)을 보세요.
>
> 💡 **한글 · 공백이 없는 경로에 설치하세요.** 예: `C:\dev\…`<br>
> `C:\Users\김동언\바탕 화면\…` 같은 경로는 Gradle 같은 자바 도구에서 알 수 없는 오류를 일으키기 쉽습니다.

| # | 설치 | 누가 | 방법 | 확인 |
| --- | --- | --- | --- | --- |
| 1 | **Git** | 모두 | [git-scm.com](https://git-scm.com) → Windows 설치 파일. 옵션은 기본값 그대로 (**Git Bash**가 같이 설치됨) | `git --version` |
| 2 | **Node.js** | 모두 | [nodejs.org](https://nodejs.org) → **LTS** 설치 파일 | `node -v` → `v22.12` 이상 |
| 3 | **VS Code** | 모두 | [code.visualstudio.com](https://code.visualstudio.com) (프론트는 WebStorm도 가능) | |
| 4 | **JDK 21** | 백엔드 | [adoptium.net](https://adoptium.net) → **Temurin 21 (LTS)** → Windows x64 **.msi**.<br>설치 중 **"Set JAVA_HOME variable"** 항목을 눌러 **"Will be installed on local hard drive"** 로 바꾸기 (기본값은 꺼져 있음) | 새 터미널에서 `java -version` → `21…` |
| 5 | **MySQL 8.4** + DB 화면 도구 | 백엔드 (프론트는 2주차) | [DB 안내 1)](../db/README.md#1-mysql-84-설치). 화면 도구는 윈도우 **MySQL Workbench**, 맥 **DataGrip** | |

**VS Code 확장 프로그램**

왼쪽의 네모 4개 아이콘(Extensions) → 이름 검색 → **Install**

| 확장 | 누가 | 쓰임 |
| --- | --- | --- |
| **ESLint** | 프론트 | 코드 검사 (`npm run lint`와 같은 규칙) |
| **Extension Pack for Java** (Microsoft) | 백엔드 | 자바 실행 · 디버그 · 자동 완성 |
| **Spring Boot Extension Pack** (VMware) | 백엔드 | 스프링부트 실행 버튼 (Spring Boot Dashboard) |
| **Gradle for Java** (Microsoft) | 백엔드 | Gradle 작업 목록 |

> 💡 **터미널은 Git Bash를 쓰세요.** 문서의 명령어(`cp`, `rm -rf` 등)가 그대로 동작합니다.<br>
> 시작 메뉴 → "Git Bash". VS Code 안에서는 **터미널 → 새 터미널** → 오른쪽 위 **∨** → **Git Bash**.

---

## 2. 저장소 받기와 폴더 배치

```
C:\dev\jspTeam\
├─ Team_walk\     ← 이 저장소 (팀 코드)
└─ uploads\       ← 업로드 폴더 (후기 사진 · GPX, 각자 PC에만) → DB 안내 5)
```

Git Bash에서:

```bash
mkdir -p /c/dev/jspTeam
cd /c/dev/jspTeam
git clone https://github.com/AnemoneCoronaria/Team_walk.git
cd Team_walk
git checkout develop
```

VS Code에서는 **File → Open Folder…** 로 엽니다.<br>
프론트 담당은 `Team_walk\frontend`, 백엔드 담당은 `Team_walk\backend` 폴더를 열면 됩니다. (저장소 전체를 열어도 됩니다)

---

## 3. 백엔드: 스프링부트

자세한 내용(실행 방법, ▶ Run 오류, 폴더 구조)은 **[backend/README.md](../backend/README.md)** 에 있습니다.

| 항목 | 값 |
| --- | --- |
| 스프링부트 | **4.1** (Spring Initializr 기본값. 3.5는 2026년 6월에 무료 지원이 끝나서 Initializr에서 고를 수 없음) |
| 빌드 | Gradle (프로젝트에 들어 있는 `gradlew`를 쓰므로 따로 설치하지 않음) |
| Java | 21 |
| 라이브러리 | Spring Web, JDBC API(JdbcTemplate), MySQL Driver, Validation, DevTools (비밀번호용 `spring-security-crypto`는 로그인 만들 때 추가) |
| 포트 | **8080** (프론트가 `/api` · `/files` 요청을 여기로 보냄) |
| 설정 파일 | `application.properties` 공통 (깃허브 O)<br>`application-local.properties` 각자 · 비밀번호 (깃허브 X) |

> ⚠️ **Spring Security(`spring-boot-starter-security`)는 넣지 않습니다.**<br>
> 넣으면 모든 API에 기본 로그인 화면이 걸려서 React 요청이 전부 막힙니다. 로그인은 세션으로 직접 처리합니다 ([API 명세](../README.md#-문서-안내) "공통 규칙").

**처음 실행 (팀원 모두 같은 순서)**

1. 최신 develop 받기: `git checkout develop` → `git pull`
2. 각자 설정 파일 만들기 → [DB 안내 6)](../db/README.md#6-백엔드-설정-파일-만들기)<br>
   `backend/src/main/resources/application-local.properties.example`을 복사 → 이름을 `application-local.properties`로 → 비밀번호 고치기
3. VS Code → **File → Open Folder…** → `Team_walk\backend`
4. 왼쪽 **코끼리 아이콘(Gradle)** → `sanchaek` → Tasks → application → **bootRun** 더블클릭<br>
   (터미널이면 Git Bash `./gradlew bootRun`, cmd · PowerShell `gradlew.bat bootRun`. 처음엔 몇 분 걸림)
5. 로그에 `Started SanchaekApplication`이 뜨면 브라우저에서 `http://localhost:8080/api/hello` → `"landmarks":8` 이 보이면 서버와 DB 모두 성공

> [!NOTE]
> - `80% EXECUTING`에서 멈춘 것처럼 보여도 **정상**입니다 (서버가 켜져 있는 상태). 끌 때는 Ctrl+C.
> - VS Code의 ▶ Run 버튼은 `ClassNotFoundException`이 날 수 있습니다. 그럴 땐 위처럼 Gradle `bootRun`으로 실행하세요 → [backend/README.md](../backend/README.md#vs-code의--run-버튼)

> [!TIP]
> 인터넷 예제는 대부분 **스프링부트 3** 기준입니다. 거의 그대로 되지만 이것만 다릅니다.
> - `spring-boot-starter-web` → 4에서는 **`spring-boot-starter-webmvc`** (Initializr가 알아서 넣어 줌)
> - JSON 라이브러리(Jackson) 패키지: `com.fasterxml.jackson.databind` → **`tools.jackson.databind`** (`@JsonIgnore` 같은 어노테이션은 그대로)
> - `javax.`로 시작하는 예제는 스프링부트 2 시절 것 → `jakarta.`로 바꾸기

---

## 4. 프론트 세팅 (처음 한 번)

```bash
cd /c/dev/jspTeam/Team_walk/frontend
npm install        # 처음 한 번, 그리고 package.json이 바뀌었을 때마다
npm run dev        # 개발 서버 시작 → http://localhost:5173
```

- 끌 때는 터미널에서 **Ctrl+C**
- 화면에 ✅ 연결 성공이 뜨려면 **백엔드(8080)가 켜져 있어야** 합니다.<br>
  백엔드를 안 켰으면 ❌ 연결 실패가 나오는 게 정상입니다.

| 명령 | 하는 일 |
| --- | --- |
| `npm install` | `package.json`에 적힌 라이브러리를 `node_modules/`에 설치 (깃허브엔 안 올라감) |
| `npm run dev` | 개발 서버. 파일을 저장하면 화면이 바로 바뀜 |
| `npm run lint` | 코드 검사 (ESLint). PR 올리기 전에 한 번 |
| `npm run build` | 빌드가 되는지 확인 → `dist/` (배포는 하지 않음) |

### 프론트와 백엔드는 이렇게 연결됩니다

```
브라우저 → localhost:5173 (Vite, React 화면 · 관리자 화면)
              │  /api/… · /files/… 요청만
              ▼
           localhost:8080 (스프링부트)
```

- 이 연결은 `frontend/vite.config.js`의 **`proxy`** 설정이 해 줍니다.
- 같은 주소(5173)에서 오는 것처럼 보이기 때문에 **CORS 오류가 없고, 로그인 세션 쿠키도 그대로 동작**합니다.
- 그래서 프론트 코드에서는 항상 `fetch('/api/…')` 처럼 **주소 앞부분 없이** 씁니다. (`http://localhost:8080/api/…` ❌)

---

## 5. 환경 확인용 테스트 코드

새로 온 사람이 세팅이 제대로 됐는지 확인하는 기준입니다. **지우거나 내용을 바꾸지 마세요.**

| 파일 | 주소 | 확인하는 것 |
| --- | --- | --- |
| `backend/src/main/java/kr/sanchaek/common/HelloController.java` | `localhost:8080/api/hello` | 서버 실행, DB 접속(`landmarks: 8`), 공통 응답 형식 `{ success, data }` |
| `frontend/src/App.jsx` | `localhost:5173/` | React 동작 + 프록시를 거쳐 백엔드 `/api/hello`에 연결되는지 |

- 나중에 실제 메인 화면을 만들면서 `App.jsx`가 바뀌면, 이 테스트 화면은 `src/pages/HealthCheck.jsx` 같은 별도 화면으로 옮겨서 유지합니다.

---

## 부록: 맥 사용자

| 윈도우 | 맥 |
| --- | --- |
| `C:\dev\jspTeam\…` | `~/dev/jspTeam/…` 등 원하는 곳 |
| Git Bash | 기본 터미널 (zsh) |
| Ctrl+C (터미널), Ctrl+S | ⌃C (터미널), ⌘S |

### Homebrew로 Java를 설치한 경우 (중요)

Homebrew의 `openjdk@21`은 설치해도 **macOS 시스템에 등록되지 않습니다.**<br>
그래서 터미널의 `java -version`은 되는데, VS Code나 Gradle이 JDK를 못 찾을 수 있습니다. 한 번만 등록하면 됩니다.

```bash
# macOS에 JDK 등록
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
/usr/libexec/java_home -V            # 목록에 21이 보이면 성공

# JAVA_HOME 설정
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
```

설정한 뒤에는 **VS Code를 완전히 껐다 켜야** 새 `JAVA_HOME`을 읽습니다.
