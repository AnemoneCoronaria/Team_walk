import { useCallback, useEffect, useState } from 'react'
import { AuthContext } from './authContext.js'
import { getMe } from '../api/userApi.js'

// 앱을 처음 열 때 GET /api/users/me 로 로그인 상태를 확인해서 아래 화면들에 나눠 줌
export default function AuthProvider({ children }) {
  const [state, setState] = useState({ user: null, loading: true })

  const refresh = useCallback(() => {
    return getMe().then(
      (user) => setState({ user, loading: false }),
      () => setState({ user: null, loading: false }), // 401 이면 로그인 안 한 상태
    )
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return <AuthContext.Provider value={{ ...state, refresh }}>{children}</AuthContext.Provider>
}
