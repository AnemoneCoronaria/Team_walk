// 계정 · 내 정보 API
//   GET  /api/users/me        (백엔드: 정란) — 로그인 상태 확인, 헤더 아바타 · 닉네임
//   GET  /api/users/me/stamps (백엔드: 동언) — M1 스탬프 배너
//   POST /api/auth/login      (백엔드: 정란) — A1 로그인
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
 * 로그인 (A1) — 성공하면 서버가 세션을 만들고 { userId, nickname, avatar, role } 를 돌려줌
 * 아이디 · 비밀번호가 틀리면 401 LOGIN_FAILED, 정지된 계정은 403 SUSPENDED
 */
export function login(loginId, password) {
  if (USE_MOCK) return mockLogin(loginId, password)
  return request('/api/auth/login', { method: 'POST', body: { loginId, password } })
}

// 가짜 로그인: db/seed.sql 테스트 계정(비밀번호 test1234!)만 통과, banned 는 정지 계정
// 성공하면 mockGuest 를 지워서 로그인 상태로 바꿈 (가짜 데이터의 내 정보는 누구로 로그인해도 민서)
function mockLogin(loginId, password) {
  const id = loginId.trim().toLowerCase()
  if (id === 'banned' && password === MOCK_PASSWORD) {
    return mockError(403, 'SUSPENDED', '이용이 정지된 계정입니다.')
  }
  const user = USERS[LOGIN_IDS[id]]
  if (!user || password !== MOCK_PASSWORD) {
    return mockError(401, 'LOGIN_FAILED', '아이디 또는 비밀번호가 맞지 않아요.')
  }
  try {
    localStorage.removeItem('mockGuest')
  } catch {
    // 저장소를 못 쓰는 브라우저면 무시
  }
  return mockResponse({ ...user, role: id === 'admin' ? 'ADMIN' : 'USER' })
}
