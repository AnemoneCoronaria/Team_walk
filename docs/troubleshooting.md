# 막혔을 때

[← README](../README.md) · [세팅 가이드](setup.md) · [DB 안내](../db/README.md) · [개발 규칙](development.md)

증상을 찾아서 해결 순서대로 해 보세요.<br>
그래도 안 되면 **화면 캡처와 함께 팀 채팅방에** 올려 주세요.

> MySQL 설치 · 접속 · SQL 오류는 [DB 안내의 막혔을 때](../db/README.md#막혔을-때)에 따로 정리되어 있습니다.

## 목차

- [설치 · Java](#설치--java)
- [백엔드 실행 (스프링부트)](#백엔드-실행-스프링부트)
- [프론트](#프론트)
- [Git](#git)

---

## 설치 · Java

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `java -version`이 **21이 아님** (17, 1.8 등) | 예전에 깐 다른 JDK가 먼저 잡힘 | JDK 21을 설치할 때 **"Set JAVA_HOME variable"** 을 켜고 다시 설치 → **새 터미널**에서 확인. 맥은 [부록](setup.md#부록-맥-사용자) |
| VS Code 오른쪽 아래에 **JDK를 찾을 수 없다**는 알림 | VS Code가 `JAVA_HOME`을 못 읽음 | JDK 21 설치 후 **VS Code를 완전히 껐다 켜기**. 그래도 나면 알림의 버튼으로 JDK 폴더를 직접 지정 |
| 설치 경로에 한글 · 공백이 있는데 이상한 오류가 남 | Gradle 같은 자바 도구가 경로를 잘못 읽음 | `C:\dev\…` 처럼 영문 경로로 옮기기 ([세팅 가이드 1번](setup.md#1-설치-처음-한-번)) |

## 백엔드 실행 (스프링부트)

> 실행은 Gradle `bootRun` 기준입니다 ([backend/README.md](../backend/README.md#실행하기)). 새로 겪은 오류는 여기에 추가해 주세요.

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| **`Port 8080 was already in use`** | 다른 프로그램이 8080을 씀. 예전 Tomcat이 켜져 있거나, **Oracle DB(XE 11g 등)** 가 8080을 쓰는 경우가 많음 (수업에서 Oracle을 깔았다면 의심) | 누가 쓰는지 확인: `netstat -ano \| findstr :8080` → 마지막 숫자(PID)로 `tasklist /fi "PID eq 숫자"`. Oracle이면 SQL*Plus에서 `EXEC DBMS_XDB.SETHTTPPORT(9090);` 로 Oracle 쪽 포트를 옮기기 (백엔드를 8081로 바꾸면 `vite.config.js`도 같이 바꿔야 해서 비추천) |
| **`Failed to configure a DataSource: 'url' attribute is not specified`** | `application-local.properties`가 없거나 이름 · 위치가 다름 | [DB 안내 6)](../db/README.md#6-백엔드-설정-파일-만들기). 윈도우 탐색기 **보기 → 파일 확장명**을 켜고 이름 끝에 `.txt`가 붙지 않았는지 확인 |
| **`Communications link failure`** | MySQL이 꺼져 있음 | 윈도우 검색 → **서비스** → `MySQL84` → 시작. 맥은 `brew services start mysql@8.4` |
| 모든 API가 **401** 이거나 **로그인 화면 HTML**이 옴 | `spring-boot-starter-security`가 들어감 | 빼고 비밀번호용 `spring-security-crypto`만 쓰기 ([세팅 가이드 3번](setup.md#3-백엔드-스프링부트)) |
| 인터넷 예제를 붙였더니 **`javax.servlet … cannot be resolved`** | 스프링부트 2 시절 예제 | `javax.`를 `jakarta.`로 바꾸기. 스프링부트 3 · 4 예제를 찾기 |
| 인터넷 예제의 **`com.fasterxml.jackson.databind … cannot be resolved`** (`ObjectMapper` 등) | 스프링부트 4는 Jackson 3을 써서 패키지 이름이 바뀜 | `tools.jackson.databind.…` 로 바꾸기 (어노테이션 `com.fasterxml.jackson.annotation`은 그대로) |
| `build.gradle`에 **`spring-boot-starter-web`** 을 넣었더니 경고 | 스프링부트 4에서 이름이 바뀜 | `spring-boot-starter-webmvc` 로 (Initializr로 만들면 알아서 들어감) |
| 고친 코드가 반영이 안 됨 | 서버가 예전 코드로 계속 돌고 있음 | Ctrl+C로 끄고 다시 `bootRun` |
| `bootRun`이 **`80% EXECUTING`** 에서 멈춤 | 멈춘 게 아니라 서버가 켜져 있는 상태 (실제로 겪음) | 로그에 `Started SanchaekApplication`이 있으면 정상. 그대로 두고 브라우저에서 확인, 끌 때 Ctrl+C |
| VS Code ▶ Run에서 **`ClassNotFoundException: kr.sanchaek.SanchaekApplication`** | VS Code 자바 확장이 컴파일을 못 함 (Gradle 9.8과 잘 안 맞음, 맥에서 실제로 겪음) | Gradle `bootRun`으로 실행. ▶ 를 쓰려면 [backend/README.md](../backend/README.md#vs-code의--run-버튼) |
| `/api/hello`가 500, 화면에 **`Access denied for user 'sanchaek'@'localhost'`** (실제로 겪음) | MySQL에 그 계정이 없거나 설정 파일의 비밀번호가 다름 | [DB 안내 2)](../db/README.md#2-db와-팀-계정-만들기-root로-한-번만)의 계정 만들기, 또는 `ALTER USER 'sanchaek'@'localhost' IDENTIFIED BY '파일의_비밀번호';`. 설정 파일 비밀번호는 따옴표 · 공백 없이 |
| `backend`에 **`.project`, `.classpath`, `.settings`, `bin`** 이 생김 | VS Code 자바 확장이 만든 파일 (속에서 이클립스 엔진을 씀) | 그대로 두기. `.gitignore`에 있어서 안 올라감 |

## 프론트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 5173 화면에 **`❌ 연결 실패`** | 백엔드(8080)가 꺼져 있음 | 백엔드를 켜고 `localhost:8080/api/hello`가 열리는지 먼저 확인. 스프링부트 전환 전에는 실패가 정상 |
| `npm run dev` 에서 버전 오류 | Node가 22.12보다 낮음 | `node -v` 확인 → nodejs.org에서 LTS 다시 설치 |
| PowerShell에서 **`npm.ps1 … 이 시스템에서 스크립트를 실행할 수 없으므로`** | 윈도우 PowerShell 보안 정책 | **Git Bash**나 명령 프롬프트(cmd)에서 실행. 또는 PowerShell에서 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| 처음 실행할 때 **윈도우 방화벽 경고** | Node · Java가 네트워크를 쓰려고 함 | "개인 네트워크" 허용 |
| 콘솔에 **`Unexpected token '<'`** / `is not valid JSON` | JSON 대신 HTML이 옴. 대부분 주소 오타(`/api` 빠짐 등)라서 백엔드 대신 Vite가 HTML을 보낸 것 | 개발자 도구 **Network** 탭에서 그 요청의 응답을 확인. 백엔드의 `/api` 오류는 항상 `{ success: false, error }` JSON |
| 후기 사진 자리에 **엑스박스** (5173에서만, 8080에서는 보임) | `vite.config.js`에 `/files` 프록시가 없는 옛 버전 | `git pull` 후 **`npm run dev`를 껐다 켜기** (설정 파일은 다시 켜야 반영) |

## Git

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `git status`에 `backend/` 처럼 **폴더만 보여서** 뭐가 올라갈지 모르겠음 | 새 폴더는 통째로 표시됨 | `git add --dry-run backend` → 실제로 올라갈 파일 목록 확인 |
| 올라갈 목록에 **`node_modules`, `build`, `.gradle`, `bin`** 이 보임 | `.gitignore`가 없는 옛 브랜치이거나, 만들어지는 폴더 이름이 다름 | **커밋하지 말고** 팀장에게 알리기 (`.gitignore`에 추가) |
| 올라갈 목록에 **비밀번호가 든 설정 파일**이 보임 | 예시 파일이 아니라 진짜 파일을 add 함 | **절대 add 하지 않기.** `git restore --staged <파일>` → 최신 develop의 `.gitignore` 받기 |
| 아무것도 안 고쳤는데 **파일 전체가 바뀐 것**으로 나옴 | 맥(LF)과 윈도우(CRLF)의 줄바꿈 차이 | `.gitattributes`로 자동 처리됨. 계속 나오면 팀장에게 |
| `git push` 가 **`rejected … (fetch first)`** | 그사이 원격에 다른 커밋이 올라옴 | `git pull` 후 다시 `git push` |
| 브랜치를 바꿨더니 **파일 · 폴더가 사라짐** | 아직 develop에 합쳐지지 않은 PR의 파일은 develop 기반 브랜치에서 안 보임 (**지워진 게 아님**) | 그 PR을 develop에 합친 뒤 `git checkout develop` → `git pull` |
| develop에서 바로 작업했음, 커밋 메시지를 잘못 씀, 실수로 main에 push 등 | | [개발 규칙 3-6 자주 하는 실수와 되돌리기](development.md#3-6-자주-하는-실수와-되돌리기) |
