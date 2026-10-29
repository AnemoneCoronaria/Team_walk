import { createContext, useContext } from 'react'

// 로그인 상태를 화면 어디서나 꺼내 쓰기 위한 것
//   const { user, loading } = useAuth()
//   user: GET /api/users/me 의 data (로그인 안 했으면 null)
//   loading: 처음 확인 중이면 true
// 승민이 로그인 · 로그아웃을 만들면, 로그인 성공 후 refresh() 를 불러 헤더를 바꾸면 됩니다.
export const AuthContext = createContext({ user: null, loading: true, refresh: () => {} })

export function useAuth() {
  return useContext(AuthContext)
}
