#!/usr/bin/env bash
# =====================================================================
# 동행 API 확인 스크립트 (민서)
#
# 준비
#   1. application-local.properties 에 app.dev-login=true 한 줄 (개발용 로그인 켜기)
#   2. 서버 켜기 (Gradle bootRun)
#   3. backend 폴더에서:  bash scripts/match-api-test.sh
#
# 새 모집 글을 하나 만들어서 참여 · 댓글 · 마감 · 관리자 숨김까지 차례로 불러 보고
# 응답 코드가 명세대로인지 ✅ / ❌ 로 보여 줍니다. (마지막에 그 글은 모집 취소됨)
# =====================================================================

BASE="${BASE:-http://localhost:8080}"
TMP="$(mktemp -d)"
PASS=0
FAIL=0
GPX="../db/sample-uploads/gpx/course-1.gpx"

# 3일 뒤 날짜 (맥 / 리눅스 · Git Bash 둘 다)
DAY="$(date -v+3d +%F 2>/dev/null || date -d '+3 days' +%F)"

# call <이름> <기대 코드> <쿠키(없으면 -)> curl 옵션...
call() {
  local name="$1" expect="$2" who="$3"
  shift 3
  local cookie=()
  [ "$who" != "-" ] && cookie=(-b "$TMP/$who" -c "$TMP/$who")
  local code
  code="$(curl -s -o "$TMP/body" -w '%{http_code}' "${cookie[@]}" "$@")"
  if [ "$code" = "$expect" ]; then
    PASS=$((PASS + 1)); echo "✅ $name ($code)"
  else
    FAIL=$((FAIL + 1)); echo "❌ $name — 기대 $expect, 실제 $code"; echo "   $(head -c 300 "$TMP/body")"
  fi
}

login() {   # login <쿠키 이름> <userId>
  curl -s -c "$TMP/$1" "$BASE/api/dev/login?userId=$2" > "$TMP/body"
  if ! grep -q '"success":true' "$TMP/body"; then
    echo "개발용 로그인이 안 돼요. application-local.properties 에 app.dev-login=true 를 넣고 서버를 다시 켜 주세요."
    echo "응답: $(head -c 200 "$TMP/body")"
    exit 1
  fi
}

echo "== 로그인 (seed 회원: 1 admin, 2 민서, 4 동언, 5 동훈)"
login admin 1; login minseo 2; login dongeon 4; login donghun 5

echo "== 목록 · 상세"
call "동행 목록"                         200 -       "$BASE/api/matches"
call "동행 목록 필터 (lang=ZH, 전체 상태)" 200 -       "$BASE/api/matches?lang=ZH&status=ALL"
call "잘못된 필터 값 → 400"               400 -       "$BASE/api/matches?lang=JP"
call "동행 상세 1 (비로그인)"              200 -       "$BASE/api/matches/1"
call "동행 상세 1 (모집자 민서)"           200 minseo  "$BASE/api/matches/1"
grep -q '"isHost":true' "$TMP/body" && echo "   └ isHost: true 확인" || echo "   └ ⚠️ isHost 가 true 가 아님"
call "없는 글 → 404"                      404 -       "$BASE/api/matches/9999"
call "GPX 받기 (업로드 폴더에 샘플이 있어야 200)" 200 - "$BASE/api/matches/1/gpx"

echo "== 작성 (동언, GPX 첨부)"
call "로그인 없이 작성 → 401"             401 -       -X POST "$BASE/api/matches" -F title=x
call "필수 값 빠짐 → 400"                 400 dongeon -X POST "$BASE/api/matches" -F courseId=1
call "모집 글 작성 → 201"                 201 dongeon -X POST "$BASE/api/matches" \
  -F "title=스크립트 테스트 동행" -F "content=확인용" -F courseId=1 -F "meetAt=${DAY}T19:00:00" \
  -F "meetPlace=옥구공원 정문" -F capacity=3 -F target=ANYONE -F "languages=KO,ZH" -F styles=SLOW \
  -F "gpx=@$GPX"
ID="$(grep -o '"matchId":[0-9]*' "$TMP/body" | grep -o '[0-9]*')"
if [ -z "$ID" ]; then echo "새 글 번호를 못 받아서 여기서 멈춥니다."; exit 1; fi
echo "   └ 새 글 번호: $ID"
call "새 글 상세 (hasGpx)"                200 dongeon "$BASE/api/matches/$ID"
grep -q '"hasGpx":true' "$TMP/body" && echo "   └ hasGpx: true 확인" || echo "   └ ⚠️ hasGpx 가 true 가 아님"

echo "== 참여 (정원 3)"
call "동훈 참여 → 2/3"                    200 donghun -X POST "$BASE/api/matches/$ID/join"
call "동훈 또 참여 → 409 DUPLICATE"       409 donghun -X POST "$BASE/api/matches/$ID/join"
call "민서 참여 → 3/3 FULL"               200 minseo  -X POST "$BASE/api/matches/$ID/join"
grep -q '"status":"FULL"' "$TMP/body" && echo "   └ status: FULL 확인" || echo "   └ ⚠️ FULL 이 아님"
call "관리자 참여 → 409 FULL"             409 admin   -X POST "$BASE/api/matches/$ID/join"
call "동훈 참여 취소"                      200 donghun -X DELETE "$BASE/api/matches/$ID/join" \
  -H 'Content-Type: application/json' -d '{"reason":"일정이 생겼어요"}'
call "모집자 참여 취소 → 400"              400 dongeon -X DELETE "$BASE/api/matches/$ID/join"

echo "== 댓글"
call "참여자 아닌 사람 댓글 보기 → 403"     403 admin   "$BASE/api/matches/$ID/comments"
call "모집자 공지 쓰기 → 201"              201 dongeon -X POST "$BASE/api/matches/$ID/comments" \
  -H 'Content-Type: application/json' -d '{"content":"정문 앞에서 만나요","isNotice":true}'
call "참여자 댓글 목록"                    200 minseo  "$BASE/api/matches/$ID/comments"

echo "== 내보내기 · 수정 · 마감"
call "모집자 아닌 사람이 내보내기 → 403"    403 minseo  -X DELETE "$BASE/api/matches/$ID/participants/2"
call "민서 내보내기"                       200 dongeon -X DELETE "$BASE/api/matches/$ID/participants/2"
call "내보내진 민서 다시 참여 → 403"        403 minseo  -X POST "$BASE/api/matches/$ID/join"
call "모집자 아닌 사람이 수정 → 403"        403 donghun -X PUT "$BASE/api/matches/$ID" -H 'Content-Type: application/json' \
  -d "{\"title\":\"수정\",\"courseId\":1,\"meetAt\":\"${DAY}T20:00:00\",\"meetPlace\":\"정문\",\"capacity\":4,\"target\":\"ANYONE\",\"languages\":[\"KO\"]}"
call "모집자 수정 (정원 4)"                200 dongeon -X PUT "$BASE/api/matches/$ID" -H 'Content-Type: application/json' \
  -d "{\"title\":\"스크립트 테스트 (수정)\",\"courseId\":1,\"meetAt\":\"${DAY}T20:00:00\",\"meetPlace\":\"정문\",\"capacity\":4,\"target\":\"MBTI\",\"mbti\":\"enfp\",\"languages\":[\"KO\",\"EN\"],\"styles\":[\"CHAT\"]}"
call "모집 마감"                           200 dongeon -X POST "$BASE/api/matches/$ID/close"
call "마감된 글 참여 → 409 CLOSED"          409 donghun -X POST "$BASE/api/matches/$ID/join"
call "내 동행 (모집한)"                     200 dongeon "$BASE/api/users/me/matches?tab=hosted"
grep -q "\"matchId\":$ID" "$TMP/body" && echo "   └ 새 글이 목록에 있음" || echo "   └ ⚠️ 새 글이 목록에 없음"
call "내 동행 (로그인 없이) → 401"          401 -       "$BASE/api/users/me/matches"

echo "== 관리자 AD3"
call "일반 회원이 관리자 API → 403"         403 donghun "$BASE/api/admin/matches"
call "관리자 목록"                          200 admin   "$BASE/api/admin/matches?status=CLOSED"
call "숨김"                                200 admin   -X PUT "$BASE/api/admin/matches/$ID/visibility" \
  -H 'Content-Type: application/json' -d '{"hidden":true}'
call "숨긴 글 상세 → 404"                   404 -       "$BASE/api/matches/$ID"
call "복구"                                200 admin   -X PUT "$BASE/api/admin/matches/$ID/visibility" \
  -H 'Content-Type: application/json' -d '{"hidden":false}'

echo "== 정리"
call "모집 취소 (CANCELED)"                 200 dongeon -X DELETE "$BASE/api/matches/$ID"
call "취소된 글 다시 취소 → 409"            409 dongeon -X DELETE "$BASE/api/matches/$ID"

rm -rf "$TMP"
echo
echo "결과: ✅ $PASS개  ❌ $FAIL개"
[ "$FAIL" -eq 0 ]
