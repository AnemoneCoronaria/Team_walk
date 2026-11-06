// API가 주고받는 영어 코드 → 화면에 보여 줄 글자
// 값은 노션 "API 명세 (1순위)"의 "값 약속" 표 그대로입니다. 명세가 바뀌면 여기만 고치면 됩니다.
// (승민 화면에서도 같은 코드를 쓰니 같이 가져다 쓰세요)

export const STYLE_LABELS = {
  SLOW: '🐢 천천히',
  FAST: '🏃 빠르게',
  QUIET: '🤫 조용히',
  CHAT: '💬 수다',
  PHOTO: '📸 사진',
  SUNRISE_SUNSET: '🌅 일출 · 일몰',
  DOG: '🐶 반려견과',
}

export const LANGUAGE_LABELS = {
  KO: '한국어',
  ZH: '中文',
  EN: 'English',
  VI: 'Tiếng Việt',
  RU: 'Русский',
}

export const AGE_LABELS = {
  '20s': '20대',
  '30s': '30대',
  '40s': '40대',
  '50s+': '50대 이상',
}

export const LEVEL_LABELS = {
  EASY: '쉬움',
  NORMAL: '보통',
  HARD: '어려움',
}

export const AREA_LABELS = {
  OKGU: '옥구공원',
  JEONGWANG: '정왕동',
  BAEGOT: '배곧',
  SIHWA: '시화호 · 서해',
}

export const FACILITY_LABELS = {
  TOILET: '화장실',
  BENCH: '벤치',
  PARKING: '주차',
  SEA_VIEW: '바다 뷰',
}

// 코스 공개 범위 (Figma C3-3 · P3)
export const VISIBILITY_LABELS = {
  PUBLIC: '전체 공개',
  MATCH_ONLY: '동행 참여자만',
  PRIVATE: '나만 보기',
}

export const TARGET_LABELS = {
  ANYONE: '누구나',
  EXCHANGE: '내 · 외국인 교류',
  SAME_LANGUAGE: '같은 언어끼리',
  MBTI: 'MBTI 맞춤',
}

export const TIME_SLOT_LABELS = {
  MORNING: '아침',
  AFTERNOON: '점심',
  EVENING: '저녁',
  NIGHT: '밤',
}

// 동행 상태 → 카드 오른쪽 위 칩
export const MATCH_STATUS_LABELS = {
  OPEN: '모집 중',
  FULL: '정원 참',
  CLOSED: '마감',
  CANCELED: '모집 취소',
  DONE: '끝남',
}

// 랜드마크 번호 → 아이콘 (Figma R2 · R2-1, 번호는 db/seed.sql)
export const LANDMARK_ICONS = { 1: '🌲', 2: '🗼', 3: '⚓', 4: '🏺', 5: '🌳', 6: '🌊', 7: '🐢', 8: '🐠' }

// 배지 5개 (Figma R2 순서). 받는 조건은 서버가 계산 (db/schema.sql user_stamps 주석)
export const BADGES = [
  { code: 'FIRST_WALK', name: '첫 산책', icon: '🥾', hint: '랜드마크 1곳 인증' },
  { code: 'FIRST_MATCH', name: '첫 동행', icon: '🤝', hint: '동행 1번 함께 걷기' },
  { code: 'EXCHANGE_3', name: '내 · 외국인 교류 3회', icon: '🌏', hint: '교류 동행 3번' },
  { code: 'LANDMARK_3', name: '랜드마크 3곳', icon: '🗺', hint: '랜드마크 3곳 인증' },
  { code: 'SIHEUNG_MASTER', name: '시흥 산책 마스터', icon: '🏅', hint: '랜드마크 8곳 모두 인증' },
]

// 신고 사유 — 동행 글 · 사용자 · 후기 (W6)
export const REPORT_REASON_LABELS = {
  NO_SHOW: '노쇼',
  RUDE: '불쾌한 언행',
  INAPPROPRIATE: '부적절한 글 · 사진',
  SPAM: '광고 · 홍보',
  SAFETY: '안전 위협',
  ETC: '기타',
}

// 신고 사유 — 코스 (C2-1)
export const COURSE_REPORT_REASON_LABELS = {
  PRIVATE_AREA: '사유지 · 출입 금지 구역',
  DANGER: '위험한 구간',
  WRONG_INFO: '경로 정보가 실제와 다름',
  HOME_EXPOSED: '개인 주거지 노출',
  ETC: '기타',
}

// 아바타 코드 → 이름 (Figma Avatar 8종, A4에서 고르는 순서)
export const AVATAR_LABELS = {
  seagull: '갈매기',
  lighthouse: '등대',
  lotus: '연꽃',
  reed: '갈대',
  sogeumi: '소금이',
  wave: '파도',
  dog: '강아지',
  cat: '고양이',
}

// 아바타 코드 → 이미지 주소 (public/avatars/)
export function avatarSrc(avatar) {
  return `/avatars/${avatar || 'seagull'}.svg`
}
