# 세팅 가이드 (처음 한 번)

[← README](../README.md) · [DB 안내](../db/README.md) · [개발 규칙](development.md) · [막혔을 때](troubleshooting.md)

새로 온 사람이 개발 환경을 갖추는 순서입니다. 끝나면 [README의 빠른 시작](../README.md#빠른-시작) 세 화면이 모두 보여야 합니다.
**백엔드 담당**은 이어서 [DB 세팅](../db/README.md#처음-세팅-윈도우-한-번만)까지 합니다 (프론트는 2주차에).

## 목차

- [1. 설치 (처음 한 번)](#1-설치-처음-한-번)
- [2. 저장소 받기와 폴더 배치](#2-저장소-받기와-폴더-배치)
- [3. 백엔드: 이클립스 세팅 (처음 한 번)](#3-백엔드-이클립스-세팅-처음-한-번)
- [4. 프론트 세팅 (처음 한 번)](#4-프론트-세팅-처음-한-번)
- [5. 환경 확인용 테스트 코드](#5-환경-확인용-테스트-코드)
- [부록: 맥 사용자](#부록-맥-사용자)

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
> 주소가 `/sanchaek` 아래에 있으면 **JSP 페이지는 뜨는데 React에서는 전부 404**가 납니다. 그래서 개발 때부터 루트(`/`)로 맞춰 둡니다.

> **❓ `localhost:8080/`에 원래 나오던 Tomcat 고양이 화면은?**
> 이클립스는 Tomcat을 실행할 때 Tomcat 설치 폴더를 그대로 쓰지 않고, 워크스페이스 안에 따로 실행 공간을 만들어 우리 프로젝트만 올립니다.
> 그래서 이클립스로 켜면 `/`에 우리 프로젝트가 나오고, Tomcat 폴더의 `bin\startup.bat`(맥은 `bin/catalina.sh run`)으로 직접 켜면 원래 고양이 화면이 나옵니다.
> **둘 다 8080 포트를 쓰니 동시에 켜지 마세요.**

> **백엔드 담당은 여기까지 한 뒤** [DB 세팅](../db/README.md#처음-세팅-윈도우-한-번만) 1) ~ 6)을 이어서 합니다.
> 마지막 6)에서 만드는 설정 파일 **`db.properties`** 가 있어야 자바 코드가 DB에 접속합니다 (깃허브에 안 올라가는 파일이라 각자 만듦).

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
