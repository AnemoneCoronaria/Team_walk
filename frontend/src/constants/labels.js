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
