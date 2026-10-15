# 막혔을 때

[← README](../README.md) · [세팅 가이드](setup.md) · [DB 안내](../db/README.md) · [개발 규칙](development.md)

지금까지 실제로 겪고 해결한 문제들입니다. 증상을 찾아서 해결 순서대로 해 보고, 그래도 안 되면 **화면 캡처와 함께 디스코드에** 올려 주세요.

> MySQL 설치 · 접속 · SQL 오류는 [db/README.md의 막혔을 때](../db/README.md#막혔을-때)에 따로 정리되어 있습니다.

## 목차

- [설치 · Java · Tomcat](#설치--java--tomcat)
- [이클립스 워크스페이스 · 프로젝트](#이클립스-워크스페이스--프로젝트)
- [Maven · 라이브러리](#maven--라이브러리)
- [실행 · 주소 · 포트](#실행--주소--포트)
- [프론트](#프론트)
- [Git](#git)

---

## 설치 · Java · Tomcat

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `java -version` 은 되는데 Tomcat이 **`Unable to locate a Java Runtime`** / `JAVA_HOME … is not defined` | 시스템이 JDK 위치를 모름 | **윈도우:** JDK 설치 때 "Set JAVA_HOME variable"을 켜고 다시 설치 (또는 시스템 환경 변수에 `JAVA_HOME` = JDK 폴더 추가) / **맥:** [부록](setup.md#부록-맥-사용자) |
| JDK 추가 시 **`The home directory does not exist`** | JRE home에 `…\bin\java.exe`(맥 `…/bin/java`)까지 넣었거나, 붙여넣을 때 공백·줄바꿈이 섞임 | JRE home은 **`bin`의 상위 폴더**까지만. 직접 입력하지 말고 **Directory…** 버튼으로 선택 |
| Tomcat 시작 시 **`기본 클래스 org.apache.catalina.startup.Bootstrap을(를) 찾거나 로드할 수 없습니다`** | ① Tomcat을 zip이 아닌 설치 파일(.exe)이나 `-src`(소스) 버전으로 받음 ② Tomcat 폴더를 한 단계 잘못 지정 ③ 이클립스의 JDK·Tomcat 등록이 꼬임 | ① `bin\bootstrap.jar`가 있는지 확인, 없으면 **Core zip**으로 다시 받기 ② `bin`, `conf`, `lib`가 바로 보이는 폴더로 지정 ③ Servers의 서버 Delete → **Run → Run Configurations…** 에서 Apache Tomcat 항목 Delete → Preferences의 Server Runtime Remove → [3-2](setup.md#3-2-jdk-등록)부터 다시 |
| Tomcat이 이클립스에서 문제인지 Tomcat 자체 문제인지 모르겠음 | | 이클립스 서버를 끄고 터미널에서 Tomcat만 단독 실행: 윈도우 `C:\dev\apache-tomcat-10.1.xx\bin\catalina.bat run` (맥 `…/bin/catalina.sh run`) → `localhost:8080`에 고양이 화면이 뜨면 Tomcat·Java는 정상 → 이클립스 설정 문제. 확인 후 **Ctrl+C로 끄기** |

## 이클립스 워크스페이스 · 프로젝트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 창 제목이 `workspace - Eclipse IDE` 가 아니라 **`backend - Eclipse IDE`** 등 | 워크스페이스가 저장소 안(`backend`)에 열려 있음 | **File → Switch Workspace → Other…** → `C:\dev\jspTeam\workspace`. 그다음 아래 행처럼 찌꺼기 정리 |
| Import에서 **Finish를 눌러도 아무 반응 없음**, 또는 엉뚱한 **`src` 프로젝트**가 생김, Import 목록에 `Folder already imported as project` | 워크스페이스가 `backend`(또는 저장소) 안에 있어서 이클립스가 폴더를 이미 프로젝트로 착각 | ① 워크스페이스를 [2번](setup.md#2-저장소-받기와-폴더-배치)처럼 분리 ② 잘못 생긴 프로젝트는 우클릭 → Delete (**"Delete project contents on disk" 체크 해제!**) ③ `backend` 안의 이클립스 파일 삭제: `cd backend && rm -rf .metadata Servers RemoteSystemsTempFiles .project .classpath .settings target src/bin src/.project src/.classpath` ④ [3-4](setup.md#3-4-프로젝트-불러오기)부터 다시 |
| 탐색기(파인더)엔 파일이 있는데 **Project Explorer가 비어 있음** | Project Explorer는 폴더가 아니라 **워크스페이스에 등록된 프로젝트**만 보여줌. 워크스페이스를 바꾸면 다시 불러와야 함 | [3-4](setup.md#3-4-프로젝트-불러오기)로 불러오기 |
| **File 메뉴에 Import가 안 보임** | (맥) 화면 맨 위 메뉴가 다른 앱(Finder 등)의 메뉴 | 이클립스 창을 한 번 클릭한 뒤 메뉴 열기. 또는 Project Explorer의 **Import projects…** 링크 |
| `Open Projects from File System`에서 **pom.xml이 선택 안 됨** | 그 창은 파일이 아니라 **폴더**를 고르는 창 | `backend` **폴더**를 고르면 이클립스가 pom.xml을 알아서 찾음. 목록에서 `backend` 줄만 체크 |
| 브랜치를 바꿨더니 **이클립스에서 파일·폴더(webapp 등)가 사라짐** | 아직 develop에 합쳐지지 않은 PR의 파일은 develop 기반 브랜치에서 안 보임 (**지워진 게 아님**) | 그 PR을 develop에 합친 뒤 `git pull origin develop` → 이클립스에서 프로젝트 우클릭 → **Refresh (F5)** |
| (참고) **새 Maven 프로젝트를 만들 때** Next 버튼이 안 눌림 | Location 칸에 붙여넣기만 해서 이클립스가 입력을 감지 못함 | 칸 끝에 스페이스를 쳤다 지우거나 Tab, 또는 Browse…로 선택 |
| (참고) 새 Maven 프로젝트 화면이 **"Specify Archetype parameters"** 로 나옴 | "Create a simple project" 체크가 풀림 | Back → **Create a simple project** 체크 → Next → **Packaging: war** 가 보이는 화면이 맞음 |

## Maven · 라이브러리

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| **`Could not transfer artifact … from/to central (https://repo.maven.apache.org/maven2)`**, `UnknownHostException`, `Missing artifact …` 가 잔뜩 | 이클립스가 라이브러리 서버에 접속 못 함 (처음 켤 때 네트워크 불안정 등). **한 번 실패하면 실패 기록이 남아 다시 시도하지 않음** (`This failure was cached`) | ① **Preferences → General → Network Connections** → Active Provider: **Direct** ② **Preferences → Maven** → **Offline 체크 해제** ③ 실패 기록 삭제: `find ~/.m2/repository -name "*.lastUpdated" -delete` ④ 이클립스 **File → Restart** ⑤ 프로젝트 우클릭 → Maven → Update Project → **"Force Update of Snapshots/Releases" 체크** |
| 위 방법으로도 안 됨 | 이클립스만 인터넷이 막힘 | 터미널에서 직접 받기: Maven 설치 후 `cd backend && mvn -U package` → `BUILD SUCCESS` 후 이클립스에서 Force Update 한 번 더 (이클립스와 터미널이 같은 `~/.m2`를 씀) |
| **`jakarta … cannot be resolved`** | 라이브러리가 프로젝트에 반영 안 됨 | 프로젝트 우클릭 → **Maven → Update Project** |
| `index.jsp`에 **`jakarta.servlet.jsp.PageContext / JspException cannot be resolved`** | JSP 라이브러리 미반영 (`pom.xml`의 `jakarta.servlet.jsp-api`) | Maven → Update Project. 계속 나오면 Tomcat 서버를 만들고 프로젝트를 Add |
| pom.xml에 **`cvc-elt.1.a`**, **`Downloading external resources is disabled`** | XML 검사기 경고 | **무시해도 됨.** 거슬리면 Preferences → XML (Wild Web Developer) → "Download external resources" 켜기 |

## 실행 · 주소 · 포트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `localhost:8080/` 은 404인데 **`localhost:8080/sanchaek/` 에서는 뜸** | Context root가 `/sanchaek` (이클립스 기본값) | [3-5](setup.md#3-5-서버-만들고-실행)의 **Context root 두 곳**을 `/`로 → Tomcat 우클릭 **Clean…** → **Restart** |
| JSP는 뜨는데 React 화면에서 **`❌ 연결 실패 (HTTP 404)`** | 위와 같음. React는 `/api/…`(루트)로 요청함 | 위와 같음 |
| JSP는 뜨는데 **`/api/…` 만 404** (Git으로 파일이 바뀐 뒤 자주 생김) | Servlet 컴파일 결과가 Tomcat에 반영 안 됨 (JSP는 Tomcat이 직접 처리하지만 Servlet은 이클립스가 컴파일해서 넘겨야 함) | **Project → Build Automatically** 체크 확인 → **Project → Clean…** → Servers의 Tomcat 우클릭 **Clean…** → **Restart** |
| **`Port 8080 required by Tomcat … is already in use`** | 다른 프로그램이 8080 사용 중. ① 터미널에서 켠 Tomcat ② **Oracle DB(XE 11g 등)** 가 8080을 씀 (수업에서 Oracle을 깔았다면 의심) | ① 그 터미널에서 Ctrl+C ② 누가 쓰는지 확인: `netstat -ano \| findstr :8080` → 마지막 숫자(PID)로 `tasklist /fi "PID eq 숫자"`. Oracle이면 SQL*Plus에서 `EXEC DBMS_XDB.SETHTTPPORT(9090);` 로 Oracle 쪽 포트를 옮기기 (Tomcat을 8081로 바꾸면 `vite.config.js`도 같이 바꿔야 해서 비추천) |
| `localhost:8080/`에 **고양이 화면**이 나옴 | 이클립스가 아니라 터미널(`startup.bat`, `catalina.sh`)로 켠 Tomcat | 터미널 Tomcat 끄고 이클립스에서 Start |

## 프론트

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 5173 화면에 **`❌ 연결 실패`** (404 외의 메시지) | 이클립스 Tomcat이 꺼져 있음 | 이클립스에서 Tomcat Start → `localhost:8080/api/hello` 가 열리는지 먼저 확인 |
| `npm run dev` 에서 버전 오류 | Node가 22.12보다 낮음 | `node -v` 확인 → nodejs.org에서 LTS 다시 설치 |
| PowerShell에서 **`npm.ps1 … 이 시스템에서 스크립트를 실행할 수 없으므로`** | 윈도우 PowerShell 보안 정책 | **Git Bash**나 명령 프롬프트(cmd)에서 실행. 또는 PowerShell에서 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| 처음 실행할 때 **윈도우 방화벽 경고** | Node·Java가 네트워크를 쓰려고 함 | "개인 네트워크" 허용 |
| 콘솔에 **`Unexpected token '<'`** / `is not valid JSON` | 백엔드가 JSON 대신 HTML 오류 페이지를 보냄. 주소가 틀렸거나(`/api` 빠짐, 오타) `ApiFilter`가 없는 옛 브랜치 | 개발자 도구 **Network** 탭에서 그 요청의 응답을 확인. 최신 develop에서는 `/api` 오류가 항상 `{ success: false, error }` JSON |
| 후기 사진 자리에 **엑스박스** (5173에서만, 8080에서는 보임) | `vite.config.js`에 `/files` 프록시가 없는 옛 버전 | `git pull` 후 **`npm run dev`를 껐다 켜기** (설정 파일은 다시 켜야 반영) |

## Git

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `git status` 에 `backend/` 처럼 **폴더만 보여서** 뭐가 올라갈지 모르겠음 | 새 폴더는 통째로 표시됨 | `git add --dry-run backend` → 실제로 올라갈 파일 목록 확인 |
| 올라갈 목록에 **`.metadata`, `Servers`, `.project`, `target`, `node_modules`** 가 보임 | 워크스페이스가 저장소 안에 있거나 `.gitignore`가 없는 브랜치 | **커밋하지 말고** 워크스페이스 위치부터 고치기 ([이클립스 표](#이클립스-워크스페이스--프로젝트) 참고). 이미 커밋했다면 팀장에게 |
| 아무것도 안 고쳤는데 **파일 전체가 바뀐 것**으로 나옴 | 맥(LF)과 윈도우(CRLF)의 줄바꿈 차이 | `.gitattributes`로 자동 처리됨. 계속 나오면 팀장에게 |
| `git push` 가 **`rejected … (fetch first)`** | 그사이 원격에 다른 커밋이 올라옴 | `git pull` 후 다시 `git push` |
| develop에서 바로 작업했음, 커밋 메시지를 잘못 씀, 실수로 main에 push 등 | | [개발 규칙 3-6 자주 하는 실수와 되돌리기](development.md#3-6-자주-하는-실수와-되돌리기) |
| `git status`에 **`db.properties`** 가 보임 | `.gitignore`가 없는 옛 브랜치이거나 이름이 다름 | **절대 add 하지 않기.** `git pull origin develop`으로 최신 `.gitignore` 받기 |
