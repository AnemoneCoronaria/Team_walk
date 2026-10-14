# Team_walk — 산책시루

7팀 산책크루의 정왕동 산책 동행 서비스 **산책시루** 저장소입니다.

---

## 폴더 구조

```
Team_walk/
├─ frontend/                     React 화면 (동훈, 승민)
├─ backend/src/main/
│  ├─ java/kr/sanchaek/
│  │  ├─ common/                 공통 코드: DB 연결, 로그인 필터, 응답 형식 (민서)
│  │  ├─ match/                  동행 (민서)
│  │  ├─ user/                   계정, 후기, 신고 (정란)
│  │  └─ course/                 코스, GPX, 랜드마크 (동언)
│  ├─ resources/                 설정 파일 (DB 접속 정보 등)
│  └─ webapp/
│     ├─ WEB-INF/views/admin/    관리자 JSP
│     └─ error/                  404, 500 페이지
├─ db/                           schema.sql (테이블), seed.sql (테스트 데이터)
└─ docs/                         API 명세, ERD
```

**자기 담당 폴더에서만 작업**하면 서로 같은 파일을 고쳐서 생기는 충돌이 거의 없습니다.

---

## 개발 환경

| 항목 | 버전 |
| --- | --- |
| Java (JDK) | 17 이상 설치 (빌드 기준은 17, 21 사용해도 됨) |
| Tomcat | 10.1 |
| 빌드 | Maven |
| Node.js | 22.12 이상 |
| 프론트 도구 | (결정 예정: Vite) |
| DB | MySQL 8.4 |

> Tomcat 10.1부터는 `javax.servlet` 대신 **`jakarta.servlet`** 을 씁니다. 인터넷 예제를 복사할 때 주의하세요.
>
> JSTL 주소도 바뀌었습니다: `<%@ taglib prefix="c" uri="jakarta.tags.core" %>`

---

## IDE에서 여는 법

저장소는 하나지만, **IDE마다 다른 폴더를 엽니다.** Git(커밋, 푸시, 브랜치)은 어느 폴더를 열어도 저장소 전체 기준으로 똑같이 동작합니다.

| 담당 | IDE | 여는 폴더 |
| --- | --- | --- |
| 백엔드 | 이클립스 | `backend/` (아래 세팅 순서 참고) |
| 프론트 | VS Code / WebStorm | `frontend/` |

---

## 백엔드: 이클립스 세팅 (처음 한 번)

### 0. 폴더 배치

**워크스페이스와 저장소를 반드시 분리합니다.** 워크스페이스를 저장소나 `backend` 안에 만들면 프로젝트가 불러와지지 않습니다.

```
~/programming/jspTeam/          (예시. 원하는 위치 아무 데나)
├─ workspace/     ← 이클립스 워크스페이스 (개인 설정만 들어감)
└─ Team_walk/     ← 이 저장소 (git clone)
```

```bash
mkdir -p ~/programming/jspTeam/workspace
cd ~/programming/jspTeam
git clone https://github.com/AnemoneCoronaria/Team_walk.git
```

### 1. JDK 확인 (맥, Homebrew로 Java를 설치한 경우)

터미널에서 `java -version`은 되는데 Tomcat이 `Unable to locate a Java Runtime` 을 띄우면, Homebrew Java가 macOS에 등록되지 않은 것입니다. 한 번만 등록하면 됩니다.

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
/usr/libexec/java_home -V            # 목록에 21이 보이면 성공
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
```

### 2. 이클립스 워크스페이스 열기

- 이클립스 실행 → Workspace: `…/jspTeam/workspace` → Launch
- 이미 다른 워크스페이스라면: **File → Switch Workspace → Other…**
- **창 제목이 `workspace - Eclipse IDE`** 인지 확인 (제목 = 워크스페이스 이름)

### 3. JDK, Tomcat 등록 (워크스페이스마다 따로 저장됨)

- **Eclipse → Settings → Java → Installed JREs** → Add → Standard VM → JDK 폴더 선택 → 체크
- **Settings → Server → Runtime Environments** → Add → **Apache Tomcat v10.1** → Tomcat 압축 푼 폴더 (`bin`, `conf`, `lib`가 바로 보이는 폴더) → JRE 선택

### 4. 프로젝트 불러오기

- Project Explorer의 **Import projects…** 링크 (또는 File → Import…)
- **Maven → Existing Maven Projects** → Root Directory: `Team_walk/backend` → `/pom.xml` 체크 → Finish
- 왼쪽에 **`sanchaek`** 가 보이면 성공 → 우클릭 → Maven → **Update Project**

### 5. 실행

- Servers 탭 → 새 서버 → Tomcat v10.1 Server → `sanchaek` Add → Finish
- `sanchaek` 우클릭 → Properties → Web Project Settings → Context root가 **`/`** 인지 확인
- 서버 우클릭 → Start → `http://localhost:8080/`, `http://localhost:8080/api/hello`

### 막혔을 때

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| Import에서 Finish를 눌러도 반응 없음, 엉뚱한 `src` 프로젝트가 생김 | 워크스페이스가 `backend`(또는 저장소) 안에 있음 | 0번처럼 분리. `backend` 안의 `.metadata .project .classpath .settings target` 삭제 후 다시 4번 |
| `Unable to locate a Java Runtime` | Homebrew Java가 macOS에 미등록 | 1번 |
| JDK 추가할 때 `The home directory does not exist` | JRE home에 `…/bin/java`까지 넣었거나, 붙여넣을 때 공백이 섞임 | JRE home은 **`…/Contents/Home` 폴더**까지만. **Directory…** → ⌘⇧G로 경로 입력 → 열기 |
| `Bootstrap을(를) 찾거나 로드할 수 없습니다` | 이클립스의 Tomcat/JRE 등록이 꼬임 | 서버·Runtime 삭제 후 3번부터 다시 |
| `Could not transfer artifact … repo.maven.apache.org` | 라이브러리 다운로드 실패가 캐시됨 | `find ~/.m2/repository -name "*.lastUpdated" -delete` → 이클립스 재시작 → Maven → Update Project (**Force Update** 체크) |
| `jakarta … cannot be resolved` | 라이브러리 미반영 | Maven → Update Project |
| `8080/`에서 404 | Context root가 `/`가 아님 | 5번의 Context root 확인 후 서버에서 Remove → 다시 Add |
| pom.xml의 `cvc-elt.1.a`, `Downloading external resources is disabled` | XML 검사기 경고 | 무시해도 됨 |

> 이클립스 설정 파일(`.project`, `.classpath`, `.settings`, `.metadata`)과 `target/`은 `.gitignore`에 들어 있어서 커밋되지 않습니다. 커밋 전에 `git status`로 한 번 확인하세요.

---

## 브랜치

```
main       제출·발표용. 직접 올리지 않음
develop    통합 브랜치. 각자 작업을 PR로 여기에 합침
feature/…  각자 작업용. develop에서 만들어서 씀
```

브랜치 이름은 `feature/화면ID-기능` 으로 짓습니다.

- `feature/A1-login`
- `feature/C1-course-map`
- `feature/W2-match-detail`

---

## 작업 순서

### 처음 한 번

```bash
git clone https://github.com/AnemoneCoronaria/Team_walk.git
cd Team_walk
git checkout develop
```

### 기능 하나 작업할 때마다

```bash
# 1. 최신 develop 받기
git checkout develop
git pull

# 2. 내 작업 브랜치 만들기
git checkout -b feature/W2-match-detail

# 3. 작업하고 커밋 (작게, 자주)
git add .
git commit -m "feat: 동행 상세 API 추가"

# 4. GitHub에 올리기
git push -u origin feature/W2-match-detail
```

5. GitHub에서 **Pull Request** 만들기: `feature/…` → `develop`
6. 같은 파트 다른 사람이 확인하고 합치기 (백엔드끼리, 프론트끼리)
7. 합쳐지면 다시 1번부터

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

- **비밀번호, API 키는 절대 커밋하지 않기** (DB 비밀번호, 카카오 키 등). 디스코드 비공개 채널로 공유합니다.
- **같이 쓰는 파일은 고치기 전에 디스코드에 알리기**: `db/schema.sql`, 프론트 라우터, `web.xml`
- **작업 시작 전에 항상 `git pull`**
