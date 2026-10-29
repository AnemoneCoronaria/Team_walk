// 메인 · 헤더에서 쓰는 내 정보 API
//   GET /api/users/me        (백엔드: 정란) — 로그인 상태 확인, 헤더 아바타 · 닉네임
//   GET /api/users/me/stamps (백엔드: 동언) — M1 스탬프 배너
// 로그인 · 회원가입 등 나머지 계정 API는 승민 담당 화면에서 이 파일에 추가하면 됩니다.
import { USE_MOCK, request, mockResponse, mockError } from './client.js'
import { ME, MY_STAMPS } from '../mocks/users.js'

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
