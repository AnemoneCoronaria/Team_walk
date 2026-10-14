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
| Java | (결정 예정: 17 또는 21) |
| Tomcat | 10.1 |
| 빌드 | Maven |
| Node.js | 22 이상 |
| 프론트 도구 | (결정 예정: Vite) |
| DB | MySQL 8.4 |

> Tomcat 10.1부터는 `javax.servlet` 대신 **`jakarta.servlet`** 을 씁니다. 인터넷 예제를 복사할 때 주의하세요.

---

## IDE에서 여는 법

저장소는 하나지만, **IDE마다 다른 폴더를 엽니다.** Git(커밋, 푸시, 브랜치)은 어느 폴더를 열어도 저장소 전체 기준으로 똑같이 동작합니다.

| 담당 | IDE | 여는 폴더 | 방법 |
| --- | --- | --- | --- |
| 백엔드 | 이클립스 | `backend/` | File → Import → **Existing Maven Projects** → `backend` 폴더 선택 |
| 프론트 | VS Code / WebStorm | `frontend/` | 폴더 열기로 `frontend` 선택 |

> `backend/pom.xml`, `frontend` 기본 설정은 버전이 확정되면 올라갑니다.

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
