import { useEffect, useState } from 'react'

// 환경 확인 화면 (주소: /health). README 5번: 지우거나 내용을 바꾸지 마세요.
// 환경 테스트 화면: React가 뜨는지 + 백엔드 /api/hello 와 연결되는지 확인
export default function HealthCheck() {
  const [state, setState] = useState({ status: 'loading' })

  useEffect(() => {
    fetch('/api/hello', { credentials: 'include' })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then((body) => setState({ status: 'ok', data: body.data }))
      .catch((err) => setState({ status: 'error', message: err.message }))
  }, [])

  return (
    <main style={{ fontFamily: 'sans-serif', padding: 40, lineHeight: 1.6 }}>
      <h1>산책시루 프론트가 동작합니다</h1>
      <p>React: 정상 동작</p>

      <h2>백엔드 연결 (/api/hello)</h2>
      {state.status === 'loading' && <p>확인 중…</p>}
      {state.status === 'ok' && (
        <p>
          ✅ 연결 성공: {state.data.message}
          <br />
          서버 시간 {state.data.serverTime} · Java {state.data.javaVersion}
        </p>
      )}
      {state.status === 'error' && (
        <p>
          ❌ 연결 실패 ({state.message}). 백엔드(스프링부트, 8080)가 켜져 있는지,
          http://localhost:8080/api/hello 가 열리는지 확인하세요.
        </p>
      )}
    </main>
  )
}
