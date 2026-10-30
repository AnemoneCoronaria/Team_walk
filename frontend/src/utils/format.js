// 날짜 · 거리 · 시간을 화면 글자로 바꾸는 함수

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

// "2026-11-02T19:00:00" → "월 11/02 · 19:00" (Figma MatchCard 형식)
export function formatMeetAt(meetAt) {
  const d = new Date(meetAt)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${WEEKDAYS[d.getDay()]} ${mm}/${dd} · ${hh}:${mi}`
}

// "2026-09-20T10:00:00" → "2026.09.20"
export function formatDate(dateTime) {
  return dateTime.slice(0, 10).replaceAll('-', '.')
}

// "2026-10-04T21:40:00" → "10/04"
export function formatMonthDay(dateTime) {
  return `${dateTime.slice(5, 7)}/${dateTime.slice(8, 10)}`
}

// 3.2 → "3.2km"
export function formatKm(km) {
  return `${Number(km).toFixed(1)}km`
}

// 50 → "약 50분"
export function formatDuration(min) {
  return `약 ${min}분`
}

// 4 → "★★★★☆"
export function formatStars(rating) {
  const n = Math.round(rating)
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

// Date → "2026-11-07" (API 날짜 파라미터 형식, 한국 시간 기준)
export function toDateParam(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
