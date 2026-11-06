// 동행 API (백엔드 담당: 민서) — 노션 API 명세 "동행"
import { USE_MOCK, request, mockResponse, mockError, paginate } from './client.js'
import {
  MATCHES,
  MATCH_EXTRA,
  USER_PROFILE,
  PARTICIPANT_STATUS,
  MATCH_COMMENTS,
  COURSE_BRIEF,
} from '../mocks/matches.js'
import { MATCH1_PATH } from '../mocks/paths.js'
import { COURSES } from '../mocks/courses.js'
import { ME } from '../mocks/users.js'
import { parseGpxInBrowser } from '../mocks/gpx.js'

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
 * courseId: C2 "이 코스에서 모집 중인 동행" (민서가 백엔드에 추가함)
 */
export function getMatches(params = {}) {
  if (USE_MOCK) {
    return mockResponse(paginate(filterMock(params), params.page, params.size))
  }
  return request('/api/matches', { params })
}

// =====================================================================
// 2주차: 동행 상세 · 참여 · 모집 글 (W2, W2-1, W5, W3, W4)
// 응답 · 요청 모양은 민서의 feature/match-week2 브랜치 기준
// =====================================================================

/**
 * 동행 상세 — GET /api/matches/{id} (로그인 선택)
 * 목록 칸 + meetPlace, meetPoint {lat,lng}, path, hasGpx, course.durationMin, ageGroups, mbti, description,
 * host.matchCount · host.rating, participants[{ userId, nickname, avatar, languages, isHost }],
 * isHost(내가 모집자), myStatus(JOINED · LEFT · KICKED · null), joinable, reason(참여 못 하는 이유)
 * 숨김 · 없는 글은 404
 */
export function getMatch(matchId) {
  if (USE_MOCK) {
    const m = findMock(matchId)
    return m ? mockResponse(mockDetail(m)) : mockError(404, 'NOT_FOUND', '모집 글을 찾을 수 없어요.')
  }
  return request(`/api/matches/${matchId}`)
}

/** 동행 경로 GPX 받기 주소 (파일. 모집자가 첨부한 경우만 → hasGpx 가 true 일 때만 버튼을 보여 줌) */
export function matchGpxUrl(matchId) {
  return `/api/matches/${matchId}/gpx`
}

/** 참여하기 — POST /api/matches/{id}/join. 실패: 409 FULL · CLOSED · ALREADY_JOINED, 403 KICKED · *_MISMATCH */
export function joinMatch(matchId) {
  if (USE_MOCK) return mockJoin(matchId)
  return request(`/api/matches/${matchId}/join`, { method: 'POST' })
}

/** 참여 취소 — DELETE /api/matches/{id}/join, 본문 { reason } (선택) */
export function leaveMatch(matchId, reason) {
  if (USE_MOCK) return mockLeave(matchId)
  return request(`/api/matches/${matchId}/join`, { method: 'DELETE', body: reason ? { reason } : undefined })
}

/** 모집 마감 (모집자) — POST /api/matches/{id}/close */
export function closeMatch(matchId) {
  if (USE_MOCK) return mockSetStatus(matchId, 'CLOSED')
  return request(`/api/matches/${matchId}/close`, { method: 'POST' })
}

/** 모집 취소 (모집자) — DELETE /api/matches/{id} (글은 남고 상태만 CANCELED) */
export function cancelMatch(matchId) {
  if (USE_MOCK) return mockSetStatus(matchId, 'CANCELED')
  return request(`/api/matches/${matchId}`, { method: 'DELETE' })
}

/** 참여자 내보내기 (모집자) — DELETE /api/matches/{id}/participants/{userId} */
export function kickParticipant(matchId, userId) {
  if (USE_MOCK) return mockKick(matchId, userId)
  return request(`/api/matches/${matchId}/participants/${userId}`, { method: 'DELETE' })
}

/**
 * 모집 글 쓰기 — POST /api/matches (multipart) → 201 { matchId }
 * form: { title, courseId, meetAt("2026-11-02T19:00:00"), capacity, meetPlace, target, mbti,
 *         ageGroups[], styles[], languages[], description, gpx(File, 선택) }
 * 여러 개 고르는 값은 같은 이름으로 여러 번 보냄 (styles=SLOW, styles=CHAT) — 민서 MatchRequest 규칙
 */
export function createMatch(form) {
  if (USE_MOCK) return mockCreate(form)
  return request('/api/matches', { method: 'POST', body: toFormData(form) })
}

/** 모집 글 수정 (모집자) — PUT /api/matches/{id} (multipart, 쓰기와 같은 칸 + removeGpx) */
export function updateMatch(matchId, form) {
  if (USE_MOCK) return mockUpdate(matchId, form)
  return request(`/api/matches/${matchId}`, { method: 'PUT', body: toFormData(form) })
}

/**
 * 댓글 · 공지 — GET · POST /api/matches/{id}/comments (참여자와 모집자만, 그 외 403)
 * 쓰기: { content, isNotice } (isNotice 는 모집자만)
 * ※ 민서 일정상 3주차 API. 지금은 가짜 데이터로만 동작
 */
export function getComments(matchId) {
  if (USE_MOCK) {
    if (!canReadComments(findMock(matchId))) return mockError(403, 'FORBIDDEN', '참여자만 볼 수 있어요.')
    const list = MATCH_COMMENTS[matchId] ?? []
    return mockResponse({ items: list, page: 1, size: 50, total: list.length })
  }
  return request(`/api/matches/${matchId}/comments`, { params: { size: 50 } })
}

export function postComment(matchId, { content, isNotice = false }) {
  if (USE_MOCK) {
    const m = findMock(matchId)
    if (!canReadComments(m)) return mockError(403, 'FORBIDDEN', '참여자만 쓸 수 있어요.')
    const list = (MATCH_COMMENTS[matchId] ??= [])
    const commentId = Date.now()
    list.push({
      commentId,
      author: { userId: ME.userId, nickname: ME.nickname, avatar: ME.avatar },
      content,
      isNotice: isNotice && m.host.userId === ME.userId,
      createdAt: new Date(Date.now() + 9 * 3600 * 1000).toISOString().slice(0, 19),
    })
    return mockResponse({ commentId })
  }
  return request(`/api/matches/${matchId}/comments`, { method: 'POST', body: { content, isNotice } })
}

function toFormData(form) {
  const fd = new FormData()
  Object.entries(form).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) value.forEach((v) => fd.append(key, v))
    else fd.append(key, value)
  })
  return fd
}

// ---------- 가짜 데이터 (로그인한 사람 = ME, 민서) ----------

const findMock = (id) => MATCHES.find((m) => m.matchId === Number(id))
const statusOf = (m, userId) =>
  m.participants.some((p) => p.userId === userId) ? 'JOINED' : (PARTICIPANT_STATUS[`${m.matchId}-${userId}`] ?? null)
const canReadComments = (m) => m && statusOf(m, ME.userId) === 'JOINED'

// 참여를 막는 이유 (민서 MatchService.joinBlockReason 과 같은 순서)
function blockReason(m, userId) {
  if (m.host.userId === userId) return 'HOST'
  const my = statusOf(m, userId)
  if (my === 'JOINED') return 'ALREADY_JOINED'
  if (my === 'KICKED') return 'KICKED'
  if (m.status !== 'OPEN') return m.status
  const me = USER_PROFILE[userId] ?? { languages: [], ageGroup: null, mbti: null }
  if (m.ageGroups.length && !m.ageGroups.includes(me.ageGroup)) return 'AGE_MISMATCH'
  const extra = MATCH_EXTRA[m.matchId] ?? {}
  if (m.target === 'MBTI' && extra.mbti !== me.mbti) return 'MBTI_MISMATCH'
  if (m.target === 'SAME_LANGUAGE' && !m.languages.some((l) => me.languages.includes(l))) return 'LANGUAGE_MISMATCH'
  return null
}

const JOIN_ERRORS = {
  HOST: [409, 'ALREADY_JOINED', '이미 참여 중인 동행이에요.'],
  ALREADY_JOINED: [409, 'ALREADY_JOINED', '이미 참여 중인 동행이에요.'],
  KICKED: [403, 'KICKED', '모집자가 내보낸 동행에는 다시 참여할 수 없어요.'],
  FULL: [409, 'FULL', '정원이 다 찼어요.'],
  CLOSED: [409, 'CLOSED', '모집이 마감된 동행이에요.'],
  CANCELED: [409, 'CANCELED', '모집이 취소된 동행이에요.'],
  DONE: [409, 'DONE', '이미 끝난 동행이에요.'],
  AGE_MISMATCH: [403, 'AGE_MISMATCH', '모집 연령대가 내 연령대와 맞지 않아요.'],
  MBTI_MISMATCH: [403, 'MBTI_MISMATCH', '모집하는 MBTI 와 내 MBTI 가 맞지 않아요.'],
  LANGUAGE_MISMATCH: [403, 'LANGUAGE_MISMATCH', '모집 언어 중에 내가 쓰는 언어가 없어요.'],
}

function mockDetail(m) {
  const extra = MATCH_EXTRA[m.matchId] ?? {}
  const course = COURSES.find((c) => c.courseId === m.course.courseId)
  const path = extra.path ?? (m.matchId === 1 ? MATCH1_PATH : course?.path ?? [])
  const reason = blockReason(m, ME.userId)
  const copy = { ...m }
  delete copy.area
  return {
    ...copy,
    meetPlace: extra.meetPlace,
    meetPoint: path.length ? { lat: path[0][0], lng: path[0][1] } : null,
    path,
    hasGpx: Boolean(extra.hasGpx),
    course: { ...m.course, durationMin: course?.durationMin ?? 0 },
    mbti: extra.mbti ?? null,
    description: extra.description ?? null,
    host: { ...m.host, matchCount: 3, rating: 4.5 },
    participants: m.participants.map((p) => ({
      ...p,
      languages: USER_PROFILE[p.userId]?.languages ?? [],
      isHost: p.userId === m.host.userId,
    })),
    isHost: m.host.userId === ME.userId,
    myStatus: m.host.userId === ME.userId ? 'JOINED' : statusOf(m, ME.userId),
    joinable: reason === null,
    reason,
  }
}

function mockJoin(matchId) {
  const m = findMock(matchId)
  if (!m) return mockError(404, 'NOT_FOUND', '모집 글을 찾을 수 없어요.')
  const reason = blockReason(m, ME.userId)
  if (reason) return mockError(...JOIN_ERRORS[reason])
  m.participants.push({ userId: ME.userId, nickname: ME.nickname, avatar: ME.avatar })
  delete PARTICIPANT_STATUS[`${m.matchId}-${ME.userId}`]
  m.current += 1
  if (m.current >= m.capacity) m.status = 'FULL'
  return mockResponse(null)
}

function removeParticipant(m, userId, status) {
  m.participants = m.participants.filter((p) => p.userId !== userId)
  PARTICIPANT_STATUS[`${m.matchId}-${userId}`] = status
  m.current -= 1
  if (m.status === 'FULL') m.status = 'OPEN' // 정원이 찼던 글은 다시 모집 중
}

function mockLeave(matchId) {
  const m = findMock(matchId)
  if (!m) return mockError(404, 'NOT_FOUND', '모집 글을 찾을 수 없어요.')
  if (m.host.userId === ME.userId) {
    return mockError(409, 'HOST_CANNOT_LEAVE', '모집자는 참여를 취소할 수 없어요. 모집 취소를 이용해 주세요.')
  }
  if (statusOf(m, ME.userId) !== 'JOINED') return mockError(409, 'NOT_JOINED', '참여 중인 동행이 아니에요.')
  removeParticipant(m, ME.userId, 'LEFT')
  return mockResponse(null)
}

function mockSetStatus(matchId, status) {
  const m = findMock(matchId)
  if (!m) return mockError(404, 'NOT_FOUND', '모집 글을 찾을 수 없어요.')
  if (m.host.userId !== ME.userId) return mockError(403, 'FORBIDDEN', '모집자만 할 수 있어요.')
  m.status = status
  return mockResponse(null)
}

function mockKick(matchId, userId) {
  const m = findMock(matchId)
  if (!m) return mockError(404, 'NOT_FOUND', '모집 글을 찾을 수 없어요.')
  if (m.host.userId !== ME.userId) return mockError(403, 'FORBIDDEN', '모집자만 할 수 있어요.')
  if (statusOf(m, Number(userId)) !== 'JOINED') return mockError(409, 'NOT_JOINED', '참여 중인 사람이 아니에요.')
  removeParticipant(m, Number(userId), 'KICKED')
  return mockResponse(null)
}

// 글 쓰기 · 수정: 첨부 GPX 는 브라우저에서 분석해서 경로로 씀
async function gpxPath(file) {
  return file ? (await parseGpxInBrowser(file)).path : null
}

function courseBrief(courseId) {
  const c = COURSES.find((x) => x.courseId === Number(courseId))
  return COURSE_BRIEF[courseId] ?? { courseId: c.courseId, name: c.name, distanceKm: c.distanceKm, level: c.level }
}

async function mockCreate(form) {
  const matchId = Math.max(...MATCHES.map((m) => m.matchId)) + 1
  const course = courseBrief(form.courseId)
  const host = { userId: ME.userId, nickname: ME.nickname, avatar: ME.avatar }
  MATCHES.push({
    matchId,
    title: form.title,
    meetAt: form.meetAt,
    course,
    target: form.target,
    styles: form.styles,
    languages: form.languages,
    current: 1,
    capacity: Number(form.capacity),
    status: 'OPEN',
    host,
    ageGroups: form.ageGroups,
    area: COURSES.find((c) => c.courseId === course.courseId)?.area,
    participants: [host],
  })
  const path = await gpxPath(form.gpx)
  MATCH_EXTRA[matchId] = {
    meetPlace: form.meetPlace,
    description: form.description || null,
    mbti: form.target === 'MBTI' ? form.mbti : null,
    hasGpx: Boolean(path),
    path: path ?? undefined,
  }
  return mockResponse({ matchId }, 500)
}

async function mockUpdate(matchId, form) {
  const m = findMock(matchId)
  if (!m) return mockError(404, 'NOT_FOUND', '모집 글을 찾을 수 없어요.')
  if (m.host.userId !== ME.userId) return mockError(403, 'FORBIDDEN', '모집자만 할 수 있어요.')
  if (Number(form.capacity) < m.current) {
    return mockError(409, 'CAPACITY_TOO_SMALL', `이미 ${m.current}명이 참여 중이라 그보다 적게 바꿀 수 없어요.`)
  }
  Object.assign(m, {
    title: form.title,
    meetAt: form.meetAt,
    course: courseBrief(form.courseId),
    target: form.target,
    styles: form.styles,
    languages: form.languages,
    capacity: Number(form.capacity),
    ageGroups: form.ageGroups,
  })
  if (m.status === 'FULL' && m.current < m.capacity) m.status = 'OPEN'
  const extra = (MATCH_EXTRA[matchId] ??= {})
  const path = await gpxPath(form.gpx)
  Object.assign(extra, {
    meetPlace: form.meetPlace,
    description: form.description || null,
    mbti: form.target === 'MBTI' ? form.mbti : null,
  })
  if (path) Object.assign(extra, { hasGpx: true, path })
  if (form.removeGpx) Object.assign(extra, { hasGpx: false, path: undefined })
  return mockResponse(null, 500)
}

