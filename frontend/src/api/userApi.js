// 계정 · 내 정보 API
//   GET  /api/users/me        (백엔드: 정란) — 로그인 상태 확인, 헤더 아바타 · 닉네임
//   GET  /api/users/me/stamps (백엔드: 동언) — M1 스탬프 배너
//   POST /api/auth/login      (백엔드: 정란) — A1 로그인
//   GET  /api/users/check-id  (백엔드: 정란) — A3 아이디 중복 확인
//   POST /api/users           (백엔드: 정란) — A4 회원가입 (A3 + A4 값을 한 번에)
//   PUT  /api/users/me        (백엔드: 정란) — P2 프로필 · 태그 수정
//   PUT  /api/users/me/password · DELETE /api/users/me (정란) — P5 비밀번호 변경 · P5-1 탈퇴
//   POST /api/auth/logout     (백엔드: 정란) — 마이페이지 메뉴의 로그아웃
//   PUT  /api/users/me/consents (정란) — R1-1 위치 정보 이용 동의
// 로그인 · 회원가입 등 나머지 계정 API는 승민 담당 화면에서 이 파일에 추가하면 됩니다.
import { USE_MOCK, request, mockResponse, mockError } from './client.js'
import { ME, MY_STAMPS, USERS, LOGIN_IDS, MOCK_PASSWORD } from '../mocks/users.js'

// 가짜 데이터에서 "로그인 전" 화면(M0)을 보고 싶을 때:
//   브라우저 개발자 도구 콘솔에서 localStorage.setItem('mockGuest', '1') 후 새로고침
//   다시 로그인 상태로: localStorage.removeItem('mockGuest')
function isMockGuest() {
  try {
    return localStorage.getItem('mockGuest') === '1'
  } catch {
    return false
  }
}

/** 내 정보 — 로그인 안 했으면 401 LOGIN_REQUIRED */
export function getMe() {
  if (USE_MOCK) {
    return isMockGuest()
      ? mockError(401, 'LOGIN_REQUIRED', '로그인이 필요합니다.', 100)
      : mockResponse(ME, 100)
  }
  return request('/api/users/me')
}

/** 내 스탬프 · 배지 */
export function getMyStamps() {
  if (USE_MOCK) return mockResponse(MY_STAMPS)
  return request('/api/users/me/stamps')
}

/**
 * 내 프로필 · 태그 수정 (P2) — PUT /api/users/me (정란), body: { nickname, ageGroup, avatar, styles[], languages[], mbti }
 * 부분 수정: null 인 칸은 "안 보낸 칸"이라 그대로 둠. MBTI 를 지우려면(선택 안 함) mbti: ''
 * 응답 모양에 기대지 않고, 저장한 뒤 화면에서 refresh() 로 GET /api/users/me 를 다시 불러옴
 */
export function updateMe(body) {
  if (USE_MOCK) {
    const next = { ...body }
    if (next.mbti === '') next.mbti = null // 서버처럼 ''는 MBTI 지우기
    Object.assign(ME, next) // 가짜 데이터: 새로고침하면 처음으로 돌아감
    return mockResponse(null)
  }
  return request('/api/users/me', { method: 'PUT', body })
}

/**
 * 비밀번호 변경 (P5) — 지금 비밀번호가 틀리면 400
 */
export function changePassword(currentPassword, newPassword) {
  if (USE_MOCK) {
    return currentPassword === MOCK_PASSWORD
      ? mockResponse(null)
      : mockError(400, 'WRONG_PASSWORD', '지금 비밀번호가 맞지 않아요.')
  }
  return request('/api/users/me/password', { method: 'PUT', body: { currentPassword, newPassword } })
}

/** 회원 탈퇴 (P5-1) — 비밀번호를 한 번 더 확인. 성공하면 서버가 세션도 끝냄 */
export function withdraw(password) {
  if (USE_MOCK) {
    if (password !== MOCK_PASSWORD) return mockError(400, 'WRONG_PASSWORD', '비밀번호가 맞지 않아요.')
    setMockGuest()
    return mockResponse(null)
  }
  return request('/api/users/me', { method: 'DELETE', body: { password } })
}

/** 로그아웃 (마이페이지 메뉴) */
export function logout() {
  if (USE_MOCK) {
    setMockGuest()
    return mockResponse(null, 100)
  }
  return request('/api/auth/logout', { method: 'POST' })
}

/**
 * 위치 정보 이용 동의 (R1-1) — 랜드마크 인증 전에 한 번. 동의하면 GET /api/users/me 의 agreeLocation 이 true
 */
export function updateConsents(agreeLocation) {
  if (USE_MOCK) {
    ME.agreeLocation = agreeLocation
    return mockResponse(null)
  }
  return request('/api/users/me/consents', { method: 'PUT', body: { agreeLocation } })
}

// 가짜 데이터: 로그아웃 · 탈퇴하면 "로그인 전" 상태로 (다시 로그인하면 풀림)
function setMockGuest() {
  try {
    localStorage.setItem('mockGuest', '1')
  } catch {
    // 저장소를 못 쓰는 브라우저면 무시
  }
}

/**
 * 로그인 (A1) — 성공하면 서버가 세션을 만들고 { userId, nickname, avatar, role } 를 돌려줌
 * 아이디 · 비밀번호가 틀리면 401 LOGIN_FAILED, 정지된 계정은 403 SUSPENDED
 */
export function login(loginId, password) {
  if (USE_MOCK) return mockLogin(loginId, password)
  return request('/api/auth/login', { method: 'POST', body: { loginId, password } })
}

/** 아이디 중복 확인 (A3) — { available: true/false }. 형식이 틀리면 400 */
export function checkIdAvailable(loginId) {
  if (USE_MOCK) {
    const id = loginId.trim().toLowerCase()
    return mockResponse({ available: !isMockTaken(id) })
  }
  return request('/api/users/check-id', { params: { loginId: loginId.trim() } })
}

/**
 * 회원가입 (A4 "가입 완료") — 201 { userId }
 * body: { loginId, password, agreeTerms, agreePrivacy, agreeLocation,
 *         nickname, ageGroup, avatar, styles[], languages[], mbti }
 * 이미 있는 아이디면 409 DUPLICATE, 규칙에 안 맞으면 400 INVALID_INPUT
 * 가입만 하고 로그인은 하지 않음 → 화면에서 login() 을 이어서 부름
 */
export function signup(body) {
  if (USE_MOCK) {
    const id = body.loginId.toLowerCase()
    if (isMockTaken(id)) {
      return mockError(409, 'DUPLICATE', '이미 사용 중인 아이디예요.')
    }
    mockSignedUp[id] = { password: body.password, nickname: body.nickname, avatar: body.avatar }
    return mockResponse({ userId: 100 })
  }
  return request('/api/users', { method: 'POST', body })
}

// 가짜 회원가입으로 만든 계정 (새로고침하면 사라짐)
const mockSignedUp = {}

function isMockTaken(id) {
  return Object.hasOwn(LOGIN_IDS, id) || Object.hasOwn(mockSignedUp, id)
}

// 가짜 로그인: db/seed.sql 테스트 계정(비밀번호 test1234!)과 방금 가짜로 가입한 계정만 통과, banned 는 정지 계정
// 성공하면 mockGuest 를 지워서 로그인 상태로 바꿈 (가짜 데이터의 내 정보는 누구로 로그인해도 민서)
function mockLogin(loginId, password) {
  const id = loginId.trim().toLowerCase()
  if (id === 'banned' && password === MOCK_PASSWORD) {
    return mockError(403, 'SUSPENDED', '이용이 정지된 계정입니다.')
  }
  const seedUser = Object.hasOwn(LOGIN_IDS, id) ? USERS[LOGIN_IDS[id]] : null
  const newUser = Object.hasOwn(mockSignedUp, id) ? mockSignedUp[id] : null
  let result = null
  if (seedUser && password === MOCK_PASSWORD) {
    result = { ...seedUser, role: id === 'admin' ? 'ADMIN' : 'USER' }
  } else if (newUser && password === newUser.password) {
    result = { userId: 100, nickname: newUser.nickname, avatar: newUser.avatar, role: 'USER' }
  }
  if (!result) {
    return mockError(401, 'LOGIN_FAILED', '아이디 또는 비밀번호가 맞지 않아요.')
  }
  try {
    localStorage.removeItem('mockGuest')
  } catch {
    // 저장소를 못 쓰는 브라우저면 무시
  }
  return mockResponse(result)
}
