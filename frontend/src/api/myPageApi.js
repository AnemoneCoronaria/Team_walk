// 마이페이지 API — P1 내 동행 (백엔드: 민서), P3 내가 올린 코스 (백엔드: 동언)
//   GET    /api/users/me/matches?tab=  (민서) — tab: upcoming 참여 예정 · hosting 내가 모집한 글 · past 지난 동행
//   GET    /api/users/me/courses       (동언, PR #20)
//   PUT    /api/courses/{id}           (동언, PR #20) — 코스 정보 수정 (GPX 경로는 그대로)
//   DELETE /api/courses/{id}           (동언, PR #20)
import { USE_MOCK, request, mockResponse, paginate } from './client.js'
import { ME } from '../mocks/users.js'
import { MATCHES } from '../mocks/matches.js'
import { COURSES, REVIEWS, toCourseSummary } from '../mocks/courses.js'

/**
 * 내 동행 (P1 탭) — { items, page, size, total }
 * items 한 줄 = 동행 목록(GET /api/matches)과 같은 칸 + role(HOST 모집자 · PARTICIPANT 참여),
 *              지난 동행(past)이면 reviewed(후기를 썼는지)
 */
export function getMyMatches(tab) {
  if (USE_MOCK) return mockResponse(paginate(mockMyMatches(tab), 1, 50))
  return request('/api/users/me/matches', { params: { tab, size: 50 } })
}

function mockMyMatches(tab) {
  const me = ME.userId
  const now = new Date()
  return MATCHES.filter((m) => m.participants.some((p) => p.userId === me))
    .filter((m) => {
      if (tab === 'hosting') return m.host.userId === me && m.status !== 'CANCELED'
      if (tab === 'past') return m.status === 'DONE'
      return m.status !== 'DONE' && m.status !== 'CANCELED' && new Date(m.meetAt) > now // upcoming
    })
    .sort((a, b) => (tab === 'past' ? b.meetAt.localeCompare(a.meetAt) : a.meetAt.localeCompare(b.meetAt)))
    .map((m) => {
      const copy = { ...m, role: m.host.userId === me ? 'HOST' : 'PARTICIPANT' }
      if (tab === 'past') copy.reviewed = REVIEWS.some((r) => r.matchId === m.matchId && r.author.userId === me)
      delete copy.ageGroups
      delete copy.area
      delete copy.participants
      return copy
    })
}

/** 내가 올린 코스 (P3) — { items, page, size, total }, items 한 줄 = 코스 목록 칸 + visibility */
export function getMyCourses() {
  if (USE_MOCK) {
    const mine = COURSES.filter((c) => c.isMine).map((c) => ({ ...toCourseSummary(c), visibility: c.visibility ?? 'PUBLIC' }))
    return mockResponse(paginate(mine, 1, 50))
  }
  return request('/api/users/me/courses', { params: { size: 50 } })
}

/**
 * 코스 정보 수정 (P3) — body: { name, description, level, facilities[], visibility }
 * 부분 수정: null 인 칸은 그대로 둠. 소개를 지우려면 description: ''
 */
export function updateMyCourse(courseId, body) {
  if (USE_MOCK) {
    const next = { ...body }
    if (next.description === '') next.description = null // 서버처럼 ''는 소개 지우기
    Object.assign(COURSES.find((c) => c.courseId === Number(courseId)) ?? {}, next)
    return mockResponse(null)
  }
  return request(`/api/courses/${courseId}`, { method: 'PUT', body })
}

/** 코스 삭제 (P3) — 서버는 지우지 않고 숨김 처리 (그 코스로 잡힌 동행 · 후기는 남음) */
export function deleteMyCourse(courseId) {
  if (USE_MOCK) {
    const i = COURSES.findIndex((c) => c.courseId === Number(courseId))
    if (i >= 0) COURSES.splice(i, 1)
    return mockResponse(null)
  }
  return request(`/api/courses/${courseId}`, { method: 'DELETE' })
}
