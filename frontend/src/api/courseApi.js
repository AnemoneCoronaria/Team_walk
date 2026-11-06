// 코스 API (백엔드 담당: 동언) — 노션 API 명세 "코스·GPX", "후기"
import { USE_MOCK, request, mockResponse, mockError, paginate } from './client.js'
import { COURSES, REVIEWS, toCourseSummary } from '../mocks/courses.js'
import { ME } from '../mocks/users.js'
import { parseGpxInBrowser } from '../mocks/gpx.js'

const splitParam = (v) => (Array.isArray(v) ? v : v ? String(v).split(',') : [])

function filterMock({ q, area, minDistance, maxDistance, level, facility, sort = 'distance' }) {
  const facilities = splitParam(facility)
  let list = COURSES.filter((c) => {
    if (c.visibility && c.visibility !== 'PUBLIC') return false // 목록에는 전체 공개만 (명세)
    if (q && !c.name.includes(q)) return false
    if (area && c.area !== area) return false
    if (minDistance && c.distanceKm < Number(minDistance)) return false
    if (maxDistance && c.distanceKm > Number(maxDistance)) return false
    if (level && c.level !== level) return false
    if (facilities.length && !facilities.every((f) => c.facilities.includes(f))) return false
    return true
  })
  const compare = {
    distance: (a, b) => a.distanceKm - b.distanceKm,
    rating: (a, b) => b.rating - a.rating,
    recent: (a, b) => b.courseId - a.courseId,
  }[sort]
  list = [...list].sort(compare)
  return list.map(toCourseSummary)
}

/**
 * 코스 목록 — GET /api/courses
 * params: q, area, minDistance, maxDistance, level, facility, sort, page, size
 * 화면: C1, C5(결과 없음), L1(로딩), M1(sort=rating&size=3)
 */
export function getCourses(params = {}) {
  if (USE_MOCK) {
    return mockResponse(paginate(filterMock(params), params.page, params.size))
  }
  return request('/api/courses', { params })
}

/**
 * 코스 상세 (경로 좌표 포함) — GET /api/courses/{id}
 * 없는 코스면 404 NOT_FOUND
 */
export function getCourse(courseId) {
  if (USE_MOCK) {
    const course = COURSES.find((c) => c.courseId === Number(courseId))
    return course ? mockResponse(course) : mockError(404, 'NOT_FOUND', '코스를 찾을 수 없습니다.')
  }
  return request(`/api/courses/${courseId}`)
}

/**
 * 코스 후기 목록 — GET /api/courses/{id}/reviews
 * params: sort(recent/rating), photo, page, size (C2 상세는 size=3)
 * 응답 data 에 avgRating 이 함께 옴
 */
export function getCourseReviews(courseId, params = {}) {
  if (USE_MOCK) {
    const list = REVIEWS.filter((r) => r.courseId === Number(courseId))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map((r) => {
        const copy = { ...r }
        delete copy.courseId
        return copy
      })
    const avg = list.length ? list.reduce((s, r) => s + r.rating, 0) / list.length : 0
    return mockResponse({
      ...paginate(list, params.page, params.size ?? 10),
      avgRating: Math.round(avg * 10) / 10,
    })
  }
  return request(`/api/courses/${courseId}/reviews`, { params })
}

// GPX 다운로드 주소 (JSON이 아니라 파일 자체. <a href>에 그대로 넣음)
export function courseGpxUrl(courseId) {
  return `/api/courses/${courseId}/gpx`
}

// ---------------------------------------------------------------------
// 2주차: 코스 등록 (C3-1 ~ C3-3)
// ---------------------------------------------------------------------

/**
 * GPX 미리 분석 — POST /api/gpx/parse (로그인 필요, multipart: file)
 * 저장은 하지 않고 { distanceKm, durationMin, pointCount, area, path, start, end } 만 돌려줌
 * 오류(400): INVALID_FORMAT (GPX 아님 · 좌표 없음), TOO_LARGE (5MB 초과), TOO_SHORT (300m 미만 · 점 20개 미만)
 * 화면: C3-1 → C3-2, C3-1E, W3 GPX 첨부
 */
export function parseGpx(file) {
  if (USE_MOCK) return parseGpxInBrowser(file).then((data) => mockResponse(data, 400))
  const form = new FormData()
  form.append('file', file)
  return request('/api/gpx/parse', { method: 'POST', body: form })
}

/**
 * 코스 등록 — POST /api/courses (로그인 필요, multipart)
 * fields: { file, name, level, description, facilities: [...], visibility }
 * 응답 201 { courseId } → C2 로 이동
 */
export function createCourse({ file, name, level, description, facilities, visibility }) {
  if (USE_MOCK) return mockCreateCourse({ file, name, level, description, facilities, visibility })
  const form = new FormData()
  form.append('file', file)
  form.append('name', name)
  form.append('level', level)
  if (description) form.append('description', description)
  if (facilities.length) form.append('facilities', facilities.join(','))
  form.append('visibility', visibility)
  return request('/api/courses', { method: 'POST', body: form })
}

/** 내가 올린 코스 — GET /api/users/me/courses (W3 코스 선택에서 "내가 올린 코스") */
export function getMyCourses() {
  if (USE_MOCK) {
    const mine = COURSES.filter((c) => c.isMine).map((c) => ({ ...toCourseSummary(c), visibility: c.visibility ?? 'PUBLIC' }))
    return mockResponse(paginate(mine, 1, 50))
  }
  return request('/api/users/me/courses', { params: { size: 50 } })
}

// 가짜 등록: 브라우저에서 분석한 결과로 새 코스를 COURSES 에 추가 (새로고침하면 사라짐)
async function mockCreateCourse({ file, name, level, description, facilities, visibility }) {
  const parsed = await parseGpxInBrowser(file)
  const courseId = Math.max(...COURSES.map((c) => c.courseId)) + 1
  COURSES.push({
    courseId,
    name,
    description: description || null,
    area: parsed.area,
    distanceKm: parsed.distanceKm,
    durationMin: parsed.durationMin,
    level,
    facilities,
    path: parsed.path,
    author: { userId: ME.userId, nickname: ME.nickname, avatar: ME.avatar },
    rating: 0,
    reviewCount: 0,
    landmarks: [],
    isMine: true,
    visibility,
    createdAt: new Date().toISOString().slice(0, 19),
  })
  return mockResponse({ courseId }, 500)
}
