# 개발 규칙

[← README](../README.md) · [세팅 가이드](setup.md) · [DB 안내](../db/README.md) · [막혔을 때](troubleshooting.md)

작업할 때마다 보는 문서입니다. API · 화면 만드는 법, 브랜치 · PR 규칙, Git 명령어, 커밋 메시지가 있습니다.

## 목차

- [1. 개발 방법](#1-개발-방법)
- [2. 브랜치·PR 규칙](#2-브랜치pr-규칙)
- [3. 작업 순서 (Git 명령어)](#3-작업-순서-git-명령어)
- [4. 커밋 메시지](#4-커밋-메시지)

---

## 1. 개발 방법

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

### DB

자세한 내용은 **[db/README.md](../db/README.md)** 에 있습니다. 여기서는 핵심만 적습니다.

- 1주차 테이블 7개(회원 · 코스 · 동행), 2주차 테이블 6개(후기 · 신고 · 랜드마크 · 스탬프 · 댓글), 계산용 뷰 2개(동행 상태, 코스 별점)가 있습니다. DB는 각자 PC의 MySQL입니다.
- **세팅:** MySQL 8.4 설치 → DB `sanchaek`, 계정 `sanchaek` 만들기 → `schema.sql` → `seed.sql` 실행 → `check.sql`로 확인 (7줄 모두 OK). 후기 사진을 다룰 때는 `C:\dev\jspTeam\uploads` 폴더도 만듭니다. 팀 비밀번호는 디스코드 공지.
- **테스트 계정:** `admin`, `minseo`, `jeongran`, `dongeon`, `donghun`, `seungmin`, 정지 테스트용 `banned` (비밀번호 전부 `test1234!`)
- **꼭 지킬 약속:** 지우지 않고 상태를 바꾸기 (후기만 예외), 동행 인원 · 상태는 `v_match_status`, 별점은 `v_course_rating` 뷰에서 꺼내기, GPX 파일은 저장하지 않고 좌표만, 후기 사진은 업로드 폴더에 두고 DB엔 경로만 → [설계 약속](../db/README.md#설계-약속-꼭-읽기)
- **테이블을 바꿀 때:** 디스코드에 먼저 알리고 → `schema.sql` 수정 → PR(리뷰어 민서) → 합쳐지면 모두 `schema.sql` · `seed.sql` 다시 실행. 자기 PC에서만 `ALTER TABLE` 하지 않기.

---

## 2. 브랜치·PR 규칙

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

## 3. 작업 순서 (Git 명령어)

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

## 4. 커밋 메시지

| 머리말 | 언제 | 예시 |
| --- | --- | --- |
| `feat:` | 기능 추가 | `feat: 코스 목록 API 추가` |
| `fix:` | 버그 수정 | `fix: 로그인 한글 깨짐 수정` |
| `docs:` | 문서 | `docs: API 명세 수정` |
| `style:` | 화면·코드 모양만 변경 | `style: 버튼 간격 조정` |
| `refactor:` | 동작은 같고 구조만 개선 | `refactor: DB 연결 코드 정리` |
| `chore:` | 설정, 폴더 등 기타 | `chore: .gitignore 추가` |
