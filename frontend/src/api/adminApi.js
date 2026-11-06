// 관리자 API — AD1 신고 처리 · AD2 회원 관리 · AD4 코스 관리 (백엔드 · 화면: 정란), AD3 모집 글 관리 (민서)
//   GET /api/admin/reports?status=          PUT /api/admin/reports/{id}            { action, memo }
//   GET /api/admin/users?q=&status=         PUT /api/admin/users/{id}/status       { status }
//   GET /api/admin/matches?q=&status=       PUT /api/admin/matches/{id}/visibility { hidden }
//   GET /api/admin/courses?q=&tab=          PUT /api/admin/courses/{id}/visibility { hidden }
// 관리자(role ADMIN)가 아니면 서버가 401 · 403 을 돌려줌
import { USE_MOCK, request, mockResponse, mockError, paginate } from './client.js'
import { ADMIN_COURSES, ADMIN_MATCHES, ADMIN_REPORTS, ADMIN_USERS } from '../mocks/admin.js'

const PAGE_SIZE = 20

/** AD1 신고 목록 — status: PENDING(대기, 기본) · DONE(처리 완료) */
export function getAdminReports({ status = 'PENDING', page = 1 } = {}) {
  if (USE_MOCK) return mockResponse(paginate(ADMIN_REPORTS.filter((r) => r.status === status), page, PAGE_SIZE))
  return request('/api/admin/reports', { params: { status, page, size: PAGE_SIZE } })
}

/** AD1 신고 처리 — action: WARN 경고 · SUSPEND 7일 정지 · HIDE 게시물 숨김 · REJECT 문제 없음 */
export function processReport(reportId, { action, memo }) {
  if (USE_MOCK) {
    const report = ADMIN_REPORTS.find((r) => r.reportId === reportId)
    if (!report || report.status === 'DONE') return mockError(409, 'DUPLICATE', '이미 처리한 신고예요.')
    if (action === 'HIDE' && report.targetType === 'USER') {
      return mockError(400, 'INVALID_INPUT', '사용자 신고는 숨길 게시물이 없어요. 경고나 정지를 골라 주세요.')
    }
    Object.assign(report, { status: 'DONE', action, adminMemo: memo?.trim() || null })
    return mockResponse({ reportId, status: 'DONE', action })
  }
  return request(`/api/admin/reports/${reportId}`, { method: 'PUT', body: { action, memo } })
}

/** AD2 회원 목록 — q: 닉네임 · 아이디, status: ACTIVE · SUSPENDED · WITHDRAWN (비우면 전체) */
export function getAdminUsers({ q = '', status = '', page = 1 } = {}) {
  if (USE_MOCK) {
    const keyword = q.trim()
    const items = ADMIN_USERS.filter((u) => !status || u.status === status).filter(
      (u) => !keyword || u.nickname.includes(keyword) || u.loginId.includes(keyword),
    )
    return mockResponse(paginate(items, page, PAGE_SIZE))
  }
  return request('/api/admin/users', { params: { q: q.trim(), status, page, size: PAGE_SIZE } })
}

/** AD2 회원 상태 — SUSPENDED 7일 정지 · ACTIVE 정지 풀기 → { userId, status, suspendedUntil } */
export function changeUserStatus(userId, status) {
  if (USE_MOCK) {
    const user = ADMIN_USERS.find((u) => u.userId === userId)
    const until = status === 'SUSPENDED' ? new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 19) : null
    Object.assign(user, { status, suspendedUntil: until })
    return mockResponse({ userId, status, suspendedUntil: until })
  }
  return request(`/api/admin/users/${userId}/status`, { method: 'PUT', body: { status } })
}

// 가짜 데이터로 서버의 AD3 탭 나누기를 흉내 냄 (모집 중 = OPEN · FULL, 마감 = 마감 · 취소 · 끝남, 숨김 = 상태 무관)
function adminMatchTab(m) {
  if (m.hidden) return 'HIDDEN'
  return m.status === 'OPEN' || m.status === 'FULL' ? 'OPEN' : 'CLOSED'
}

/**
 * AD3 모집 글 목록 — q: 제목 · 모집자 · 코스, status: OPEN(모집 중, 기본) · CLOSED(마감) · HIDDEN(숨김)
 * → { items, page, size, total, counts: { open, closed, hidden } } (counts 는 탭 옆 개수)
 */
export function getAdminMatches({ q = '', status = 'OPEN', page = 1 } = {}) {
  if (USE_MOCK) {
    const keyword = q.trim()
    const found = ADMIN_MATCHES.filter(
      (m) => !keyword || m.title.includes(keyword) || m.host.nickname.includes(keyword) || m.courseName.includes(keyword),
    )
    const count = (tab) => found.filter((m) => adminMatchTab(m) === tab).length
    const items = found.filter((m) => adminMatchTab(m) === status)
    return mockResponse({
      ...paginate(items, page, PAGE_SIZE),
      counts: { open: count('OPEN'), closed: count('CLOSED'), hidden: count('HIDDEN') },
    })
  }
  return request('/api/admin/matches', { params: { q: q.trim(), status, page, size: PAGE_SIZE } })
}

/** AD3 모집 글 숨김(true) · 복구(false) */
export function changeMatchHidden(matchId, hidden) {
  if (USE_MOCK) {
    Object.assign(ADMIN_MATCHES.find((m) => m.matchId === matchId), { hidden })
    return mockResponse({ matchId, hidden })
  }
  return request(`/api/admin/matches/${matchId}/visibility`, { method: 'PUT', body: { hidden } })
}

/** AD4 코스 목록 — q: 코스 이름 · 등록자, tab: ALL(전체, 기본) · REVIEW(검토 필요) */
export function getAdminCourses({ q = '', tab = 'ALL', page = 1 } = {}) {
  if (USE_MOCK) {
    const keyword = q.trim()
    const items = ADMIN_COURSES.filter((c) => tab !== 'REVIEW' || c.pendingReports > 0).filter(
      (c) => !keyword || c.name.includes(keyword) || c.author.nickname.includes(keyword),
    )
    return mockResponse(paginate(items, page, PAGE_SIZE))
  }
  return request('/api/admin/courses', { params: { q: q.trim(), tab, page, size: PAGE_SIZE } })
}

/** AD4 코스 숨김(true) · 복구(false) */
export function changeCourseHidden(courseId, hidden) {
  if (USE_MOCK) {
    Object.assign(ADMIN_COURSES.find((c) => c.courseId === courseId), { hidden })
    return mockResponse({ courseId, hidden })
  }
  return request(`/api/admin/courses/${courseId}/visibility`, { method: 'PUT', body: { hidden } })
}
