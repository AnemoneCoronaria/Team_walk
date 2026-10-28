# 백엔드 (스프링부트)

[← README](../README.md) · [세팅 가이드](../docs/setup.md) · [개발 규칙](../docs/development.md) · [DB 안내](../db/README.md)

> [!NOTE]
> **예전 JSP · Servlet 코드는 지웠습니다.** 이 폴더에 스프링부트 프로젝트가 들어옵니다.<br>
> 처음 한 번은 **민서가 아래 1 ~ 5번**으로 만들어 올립니다.<br>
> 올라온 뒤 다른 팀원은 [세팅 가이드 3번](../docs/setup.md#3-백엔드-스프링부트)만 하면 됩니다.

지금 이 폴더에 미리 들어 있는 파일은 두 개입니다.

| 파일 | 내용 |
| --- | --- |
| `src/main/java/kr/sanchaek/common/HelloController.java` | ★ 환경 확인용 테스트 API `GET /api/hello` (서버 + DB 접속 확인) |
| `src/main/resources/application-local.properties.example` | 각자 PC 설정 파일의 예시 (DB 비밀번호 · 업로드 폴더) |

## 목차

1. [Spring Initializr로 만들기](#1-spring-initializr로-만들기)
2. [저장소에 넣기](#2-저장소에-넣기)
3. [파일 두 개 손보기](#3-파일-두-개-손보기)
4. [실행해서 확인](#4-실행해서-확인)
5. [깃허브에 올리기](#5-깃허브에-올리기)
6. [만든 뒤 폴더 구조](#만든-뒤-폴더-구조)

---

## 1. Spring Initializr로 만들기

[start.spring.io](https://start.spring.io)를 열고 아래처럼 고릅니다.

| 항목 | 고를 것 |
| --- | --- |
| Project | **Gradle - Groovy** |
| Language | **Java** |
| Spring Boot | **4.1.x** (`SNAPSHOT` · `M` · `RC`가 붙지 않은 것 중 가장 위) |
| Group | `kr.sanchaek` |
| Artifact | `sanchaek` |
| Name | `sanchaek` |
| Package name | **`kr.sanchaek`** (기본값 `kr.sanchaek.sanchaek`에서 뒤쪽 `.sanchaek`을 지우기) |
| Packaging | **Jar** |
| Configuration | **Properties** |
| Java | **21** |

오른쪽 **ADD DEPENDENCIES…** 에서 검색해서 넣습니다.

| 검색어 | 고를 것 | 쓰임 |
| --- | --- | --- |
| `web` | **Spring Web** | API 만들기 (`@RestController`) |
| `jdbc` | **JDBC API** | `JdbcTemplate` (SQL을 직접 작성) |
| `mysql` | **MySQL Driver** | MySQL 접속 |
| `validation` | **Validation** | 입력값 검사 (`@NotBlank`, `@Size` 등) |
| `devtools` | Spring Boot DevTools (선택) | 코드를 저장하면 서버가 알아서 다시 시작 |

> [!WARNING]
> **넣지 않는 것**
> - **Spring Security** — 넣으면 모든 API에 기본 로그인 화면이 걸려서 React 요청이 전부 막힙니다. 로그인은 세션으로 직접 처리합니다.
> - **Spring Data JPA** — 우리는 JdbcTemplate으로 SQL을 직접 씁니다.
> - **Thymeleaf · JSP** — 화면은 전부 React입니다.

**GENERATE** → `sanchaek.zip`이 내려받아집니다.

---

## 2. 저장소에 넣기

Git Bash에서 새 브랜치를 만듭니다.

```bash
cd /c/dev/jspTeam/Team_walk
git checkout develop
git pull
git checkout -b feature/springboot-setup
```

`sanchaek.zip`을 풀면 `sanchaek` 폴더가 나옵니다.<br>
그 **안의 내용물 전부**를 `Team_walk\backend\` 안으로 옮깁니다.

- `backend\sanchaek\build.gradle`이 아니라 **`backend\build.gradle`** 이 되어야 합니다.
- 숨김 파일 `.gitignore`, `.gitattributes`도 같이 옮깁니다 (탐색기 **보기 → 숨긴 항목** 켜기).
- 이 폴더에 미리 있던 `HelloController.java`, `….example` 파일은 그대로 둡니다 (zip 안의 파일과 겹치지 않음).

---

## 3. 파일 두 개 손보기

**① `backend/build.gradle`** — `dependencies { … }` 안에 한 줄 추가

```groovy
implementation 'org.springframework.security:spring-security-crypto'
```

비밀번호를 BCrypt로 암호화하는 작은 라이브러리입니다. Spring Security 전체가 아니라서 로그인 화면이 생기지 않습니다. 버전은 스프링부트가 알아서 맞춥니다.

**② `backend/src/main/resources/application.properties`** — 내용을 아래로 바꾸기 (이 파일은 깃허브에 올라감)

```properties
spring.application.name=sanchaek
server.port=8080

# 비밀번호가 들어가는 설정은 각자 만드는 application-local.properties 에서 읽음 (깃허브에 안 올라감)
spring.config.import=optional:classpath:application-local.properties

# 파일 업로드: GPX · 사진 한 장 5MB, 후기 사진 최대 5장
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=30MB
```

---

## 4. 실행해서 확인

1. `src/main/resources/application-local.properties.example`을 같은 폴더에 복사 → 이름을 **`application-local.properties`** 로 바꾸기 → 비밀번호를 [DB 안내 2)](../db/README.md#2-db와-팀-계정-만들기-root로-한-번만)에서 정한 것으로 고치기
2. VS Code → **File → Open Folder…** → `Team_walk\backend` → 오른쪽 아래 Java 준비 표시가 끝날 때까지 기다리기 (처음엔 몇 분)
3. 왼쪽 **Spring Boot Dashboard**에서 `sanchaek` ▶ 실행 (또는 Git Bash에서 `./gradlew bootRun`)
4. 브라우저에서 `http://localhost:8080/api/hello`

```json
{"success":true,"data":{"message":"안녕하세요, 산책시루 서버입니다","serverTime":"…","javaVersion":"21…","landmarks":8}}
```

5. `frontend`에서 `npm run dev` → `http://localhost:5173` 에 **✅ 연결 성공**

`landmarks`가 `8`이면 DB까지 연결된 것입니다. 안 되면 [막혔을 때](../docs/troubleshooting.md#백엔드-실행-스프링부트)를 보세요.

---

## 5. 깃허브에 올리기

```bash
git status                     # application-local.properties, build/, .gradle/ 이 보이면 안 됨
git add backend
git commit -m "chore: 스프링부트 4.1 프로젝트 생성 (Initializr)"
git push -u origin feature/springboot-setup
```

깃허브에서 PR을 만듭니다 (**base: develop**, 리뷰어: 정란 또는 동언).<br>
합쳐지면 이 README의 1 ~ 5번은 지우고, 아래 폴더 구조만 남깁니다.

---

## 만든 뒤 폴더 구조

```
backend/
├─ build.gradle               라이브러리 목록 (고치기 전에 팀에 알리기)
├─ settings.gradle
├─ gradlew, gradlew.bat       Gradle 실행 파일 (따로 설치할 필요 없음)
└─ src/main/
   ├─ java/kr/sanchaek/
   │  ├─ SanchaekApplication.java   시작점 (Initializr가 만듦)
   │  ├─ common/       공통 코드 (민서) · ★ HelloController
   │  ├─ match/        동행 · 관리자 AD3 (민서)
   │  ├─ user/         계정 · 신고 · 관리자 AD1 · AD2 · AD4 (정란)
   │  └─ course/       코스 · GPX · 후기 · 랜드마크 (동언)
   └─ resources/
      ├─ application.properties                 공통 설정 (깃허브에 올라감)
      ├─ application-local.properties.example   각자 설정 예시
      └─ application-local.properties           각자 설정 (비밀번호, 깃허브에 안 올라감)
```
