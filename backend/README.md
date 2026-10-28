# 백엔드 (스프링부트)

[← README](../README.md) · [세팅 가이드](../docs/setup.md) · [개발 규칙](../docs/development.md) · [DB 안내](../db/README.md)

**스프링부트 4.1 · Java 21 · Gradle** 프로젝트입니다. [Spring Initializr](https://start.spring.io)로 만들었습니다.<br>
개발 도구는 **VS Code**, 실행은 **Gradle `bootRun`** 이 팀 기준입니다.

## 목차

1. [처음 한 번 준비](#처음-한-번-준비)
2. [실행하기](#실행하기)
3. [VS Code의 ▶ Run 버튼](#vs-code의--run-버튼)
4. [폴더 구조](#폴더-구조)
5. [라이브러리 (build.gradle)](#라이브러리-buildgradle)
6. [프로젝트를 만든 설정 (참고)](#프로젝트를-만든-설정-참고)

---

## 처음 한 번 준비

1. JDK 21, VS Code 확장 3개 설치 → [세팅 가이드 1번](../docs/setup.md#1-설치-처음-한-번)
2. MySQL에 DB 만들고 `schema.sql` → `seed.sql` → `check.sql` 실행 → [DB 안내 1) ~ 4)](../db/README.md#처음-세팅-윈도우-한-번만)
3. 각자 설정 파일 만들기 → [DB 안내 6)](../db/README.md#6-백엔드-설정-파일-만들기)<br>
   `src/main/resources/application-local.properties.example`을 복사 → 이름을 `application-local.properties`로 → 비밀번호 넣기

---

## 실행하기

**방법 1. VS Code의 Gradle 버튼 (추천)**

1. VS Code에서 **File → Open Folder…** → `Team_walk/backend`
2. 왼쪽의 **코끼리 아이콘(Gradle)** → `sanchaek` → **Tasks** → **application** → **bootRun** 더블클릭

**방법 2. 터미널** (`backend` 폴더에서)

| 터미널 | 명령 |
| --- | --- |
| 맥 · Git Bash | `./gradlew bootRun` |
| 윈도우 cmd · PowerShell | `gradlew.bat bootRun` |

**켜졌는지 확인**

- 로그에 **`Started SanchaekApplication in … seconds`** 가 보이면 켜진 것입니다.
- 브라우저에서 `http://localhost:8080/api/hello`

```json
{
  "success": true,
  "data": {
    "message": "안녕하세요, 산책시루 서버입니다",
    "serverTime": "2026-10-28T11:23:21",
    "javaVersion": "21.0.12.1",
    "landmarks": 8
  }
}
```

`landmarks`가 `8`이면 DB까지 연결된 것입니다.

> [!NOTE]
> **`80% EXECUTING > :bootRun` 에서 멈춘 것처럼 보이는 건 정상입니다.**<br>
> 서버가 켜져 있는 동안 계속 그 상태이고, 100%는 서버가 꺼질 때입니다. **끌 때는 Ctrl+C.**

- 처음 실행할 때는 Gradle과 라이브러리를 내려받느라 몇 분 걸립니다.
- 서버는 **하나만** 켭니다. 두 개를 켜면 `Port 8080 was already in use`.
- **Java 코드나 설정 파일을 고쳤으면 Ctrl+C로 끄고 다시 `bootRun`** 합니다.

---

## VS Code의 ▶ Run 버튼

`SanchaekApplication.java`의 **Run** 이나 왼쪽 스프링 아이콘(Spring Boot Dashboard)의 ▶ 는 Gradle이 아니라 **VS Code 자바 확장이 직접 컴파일한 결과**로 실행합니다.<br>
지금 프로젝트의 Gradle(9.8)과 확장이 잘 맞지 않아 아래 오류가 날 수 있습니다. 실제로 맥에서 겪었고, 윈도우도 같을 수 있습니다.

```
오류: 기본 클래스 kr.sanchaek.SanchaekApplication을(를) 찾거나 로드할 수 없습니다.
원인: java.lang.ClassNotFoundException: kr.sanchaek.SanchaekApplication
```

이 오류가 나면 **Gradle `bootRun`으로 실행하면 됩니다** (위 방법 1 · 2). ▶ 를 꼭 쓰고 싶으면:

1. ⌘, (윈도우 Ctrl+,) → `gradle build server` 검색 → **Java › Gradle › Build Server: Enabled** 를 **off**
2. ⌘⇧P (윈도우 Ctrl+Shift+P) → **Java: Clean Java Language Server Workspace** → **Restart and delete**
3. 오른쪽 아래 진행 표시가 끝난 뒤 `bin/main/kr/sanchaek/SanchaekApplication.class`가 생겼는지 확인 → **Run**

> 💡 `backend` 안에 `.project`, `.classpath`, `.settings`, `bin` 이 생기는 건 VS Code 자바 확장이 만든 파일입니다 (속에서 이클립스 엔진을 씀). `.gitignore`에 있어서 깃허브에 안 올라갑니다.

---

## 폴더 구조

```
backend/
├─ build.gradle               라이브러리 목록 (고치기 전에 팀에 알리기)
├─ settings.gradle
├─ gradlew, gradlew.bat       Gradle 실행 파일 (따로 설치할 필요 없음)
├─ gradle/wrapper/            Gradle 버전 정보 (9.8.2)
├─ scripts/                 API 확인용 curl 스크립트 (match-api-test.sh)
└─ src/
   ├─ main/
   │  ├─ java/kr/sanchaek/
   │  │  ├─ SanchaekApplication.java   시작점
   │  │  ├─ common/      공통 코드 (민서) · ★ HelloController (/api/hello)
   │  │  │                 ApiResponse · ApiException · LoginUser · FileStore … (쓰는 법: docs/development.md)
   │  │  ├─ match/       동행 · 관리자 AD3 (민서) · MatchController, MatchService, MatchRepository …
   │  │  ├─ user/        계정 · 신고 · 관리자 AD1 · AD2 · AD4 (정란)
   │  │  └─ course/      코스 · GPX · 후기 · 랜드마크 (동언)
   │  └─ resources/
   │     ├─ application.properties                 공통 설정 (깃허브에 올라감)
   │     ├─ application-local.properties.example   각자 설정 예시
   │     └─ application-local.properties           각자 설정 (비밀번호, 깃허브에 안 올라감)
   └─ test/java/kr/sanchaek/
      └─ SanchaekApplicationTests.java    서버가 뜨는지만 보는 기본 테스트
```

- `user/`, `course/` 폴더는 각자 첫 API를 만들 때 생깁니다. `match/`가 예시입니다 (Controller → Service → Repository).
- **★ `HelloController`는 환경 확인용이라 지우지 않습니다.**
- `./gradlew build`(또는 `test`)는 기본 테스트가 DB 설정을 읽으므로 `application-local.properties`가 있어야 통과합니다. `bootRun`은 테스트를 돌리지 않습니다.

---

## 라이브러리 (build.gradle)

| 라이브러리 | 쓰임 |
| --- | --- |
| `spring-boot-starter-webmvc` | API 만들기 (`@RestController`). 스프링부트 3의 `starter-web`이 4에서 이름이 바뀐 것 |
| `spring-boot-starter-jdbc` | `JdbcTemplate` (SQL을 직접 작성) |
| `spring-boot-starter-validation` | 입력값 검사 (`@NotBlank`, `@Size` 등) |
| `mysql-connector-j` | MySQL 접속 |
| `spring-boot-devtools` | 개발 편의 기능 (IDE의 ▶ 로 실행할 때 저장하면 자동 재시작. `bootRun`에서는 껐다 켜기) |

**나중에 추가할 것**

- 회원가입 · 로그인을 만들 때 (정란): `dependencies { … }` 안에 아래 한 줄을 추가합니다.
  ```groovy
  implementation 'org.springframework.security:spring-security-crypto'
  ```
  비밀번호를 BCrypt로 암호화하는 작은 라이브러리입니다. **Spring Security 전체(`spring-boot-starter-security`)는 넣지 않습니다.** 넣으면 모든 API에 기본 로그인 화면이 걸려서 React 요청이 전부 막힙니다.

---

## 프로젝트를 만든 설정 (참고)

다시 만들 일은 없지만, 어떻게 만들었는지 기록입니다.

| 항목 | 값 |
| --- | --- |
| Project | Gradle - Groovy |
| Spring Boot | 4.1.2 |
| Group / Artifact / Package name | `kr.sanchaek` / `sanchaek` / `kr.sanchaek` |
| Packaging / Configuration / Java | Jar / Properties / 21 |
| Dependencies | Spring Web, JDBC API, MySQL Driver, Validation, Spring Boot DevTools |

만든 뒤 바꾼 것: `application.properties` (각자 설정 파일 읽기, 업로드 크기), `application-local.properties.example` 추가, `common/` 공통 코드 ([개발 규칙](../docs/development.md#백엔드-공통-코드)).
