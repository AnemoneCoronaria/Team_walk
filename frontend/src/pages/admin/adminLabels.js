// 관리자 화면에서만 쓰는 코드 → 화면 글자 (값은 API 명세 "신고하기" · "관리자" 표 그대로)

export const REPORT_TARGET_LABELS = {
  MATCH: '동행 글',
  USER: '사용자',
  COURSE: '코스',
  REVIEW: '후기',
}

// W6 (동행 글 · 사용자 · 후기) 사유와 C2-1 (코스) 사유
export const REPORT_REASON_LABELS = {
  NO_SHOW: '노쇼',
  RUDE: '불쾌한 언행',
  INAPPROPRIATE: '부적절한 글 · 사진',
  SPAM: '광고 · 홍보',
  SAFETY: '안전 위협',
  PRIVATE_AREA: '사유지 · 출입 금지',
  DANGER: '위험한 구간',
  WRONG_INFO: '경로 정보가 다름',
  HOME_EXPOSED: '주거지 노출',
  ETC: '기타',
}

export const REPORT_ACTION_LABELS = {
  WARN: '경고',
  SUSPEND: '7일 이용 정지',
  HIDE: '게시물 숨김',
  REJECT: '문제 없음',
}
