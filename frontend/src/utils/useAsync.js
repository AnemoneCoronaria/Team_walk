import { useEffect, useState } from 'react'

/**
 * API 함수를 불러서 결과를 돌려주는 훅
 *
 *   const { data, error, loading } = useAsync(() => getCourses(params), JSON.stringify(params))
 *
 * - key 가 바뀔 때마다 다시 부릅니다. (보통 파라미터를 JSON.stringify 한 값)
 * - 결과가 오기 전에는 loading = true, data = null
 * - 실패하면 error 에 ApiError (status, code, message)
 */
export function useAsync(fn, key) {
  const [state, setState] = useState({ key: null, data: null, error: null })

  useEffect(() => {
    let alive = true // 화면을 벗어난 뒤 늦게 온 응답은 무시
    fn().then(
      (data) => alive && setState({ key, data, error: null }),
      (error) => alive && setState({ key, data: null, error }),
    )
    return () => {
      alive = false
    }
    // fn 은 매번 새로 만들어지는 함수라 key 로만 다시 부름
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  const loading = state.key !== key
  return {
    data: loading ? null : state.data,
    error: loading ? null : state.error,
    loading,
  }
}
