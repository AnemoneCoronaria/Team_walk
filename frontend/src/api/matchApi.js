// 동행 API (백엔드 담당: 민서) — 노션 API 명세 "동행"
import { USE_MOCK, request, mockResponse, paginate } from './client.js'
import { MATCHES } from '../mocks/matches.js'

// 시간대 계산 (명세: 아침 5~10시, 점심 11~16시, 저녁 17~20시, 밤 21~4시)
function timeSlotOf(meetAt) {
  const h = new Date(meetAt).getHours()
  if (h >= 5 && h <= 10) return 'MORNING'
  if (h >= 11 && h <= 16) return 'AFTERNOON'
  if (h >= 17 && h <= 20) return 'EVENING'
  return 'NIGHT'
}

const splitParam = (v) => (Array.isArray(v) ? v : v ? String(v).split(',') : [])

// 가짜 데이터로 서버의 필터 · 정렬을 흉내 냄
function filterMock(params) {
  const { from, to, timeSlot, target, age, area, sort = 'deadline', courseId } = params
  const status = params.status || 'OPEN'
  const styles = splitParam(params.style)
  const langs = splitParam(params.lang)

  let list = MATCHES.filter((m) => {
    const day = m.meetAt.slice(0, 10)
    if (status && m.status !== status) return false
    if (from && day < from) return false
    if (to && day > to) return false
    if (timeSlot && timeSlotOf(m.meetAt) !== timeSlot) return false
    if (styles.length && !styles.some((s) => m.styles.includes(s))) return false
    if (langs.length && !langs.some((l) => m.languages.includes(l))) return false
    if (target && m.target !== target) return false
    if (age && m.ageGroups.length && !m.ageGroups.includes(age)) return false
    if (area && m.area !== area) return false
    if (courseId && m.course.courseId !== Number(courseId)) return false
    return true
  })
  list = [...list].sort((a, b) =>
    sort === 'recent' ? b.matchId - a.matchId : a.meetAt.localeCompare(b.meetAt),
  )
  // 가짜 데이터에만 있는 칸(ageGroups, area)은 빼고 돌려줌
  return list.map((m) => {
    const copy = { ...m }
    delete copy.ageGroups
    delete copy.area
    return copy
  })
}

/**
 * 동행 목록 — GET /api/matches
 * params: from, to, timeSlot, style, lang, target, age, area, status, sort, page, size
 * 화면: W1, E1, M0(size=2), M1(size=4), C2(이 코스에서 모집 중인 동행)
 *
 * ※ courseId 는 명세에 아직 없는 파라미터입니다. C2 "이 코스에서 모집 중인 동행"에 필요해서
 *   가짜 데이터에서만 먼저 쓰고 있습니다. (민서에게 추가 요청 필요)
 */
export function getMatches(params = {}) {
  if (USE_MOCK) {
    return mockResponse(paginate(filterMock(params), params.page, params.size))
  }
  return request('/api/matches', { params })
}
