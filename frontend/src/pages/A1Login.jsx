import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Input from '../components/Input.jsx'
import { login } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { showToast } from '../utils/toast.js'
import './Account.css'

// A1 로그인 — 실패하면 같은 화면이 A2(오류 배너 + 비밀번호 칸 빨강)로 바뀜
// API: POST /api/auth/login
// 성공하면 원래 보던 화면으로 (G1 로그인 필요 모달에서 왔으면 state.from), 없으면 메인(M1)
export default function A1Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { refresh } = useAuth()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  // 로그인 상태 유지: 세션 유지 시간은 백엔드 설정이라 지금은 모양만 (정란과 정하기)
  const [keepLogin, setKeepLogin] = useState(false)
  const [error, setError] = useState('') // A2 오류 배너 문구 (서버 메시지 그대로)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    if (!loginId.trim() || !password) {
      setError('아이디와 비밀번호를 입력해 주세요.')
      return
    }
    setSubmitting(true)
    try {
      await login(loginId.trim(), password)
      await refresh() // 헤더를 로그인 상태로
      navigate(location.state?.from ?? '/', { replace: true })
    } catch (err) {
      setError(err.message)
      setPassword('')
      setSubmitting(false)
    }
  }

  return (
    <main className="account-page">
      <form className="account-card col g20" style={{ width: 480 }} onSubmit={onSubmit} noValidate>
        <h1 className="t-h24 c-strong">다시 만나서 반가워요</h1>
        {error ? (
          <p className="account-alert" role="alert">
            {error}
          </p>
        ) : (
          <p className="t-b14 c-body">로그인하고 오늘 함께 걸을 이웃을 찾아보세요</p>
        )}

        <Input
          label="아이디"
          value={loginId}
          onChange={(e) => setLoginId(e.target.value)}
          placeholder="아이디를 입력해 주세요"
          autoComplete="username"
          autoFocus
        />
        <Input
          label="비밀번호"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호를 입력해 주세요"
          autoComplete="current-password"
          error={Boolean(error)}
        />

        <div className="row ai-c jc-b fill t-b14">
          <label className="account-check c-body">
            <input type="checkbox" checked={keepLogin} onChange={(e) => setKeepLogin(e.target.checked)} />
            로그인 상태 유지
          </label>
          {/* A6 비밀번호 찾기는 2순위 */}
          <button className="account-link c-green" type="button" onClick={() => showToast('비밀번호 찾기는 준비 중이에요', 'info')}>
            비밀번호 찾기
          </button>
        </div>

        <Button type="submit" block disabled={submitting}>
          {submitting ? '로그인 중…' : '로그인'}
        </Button>

        <p className="row jc-c g16 fill t-b14">
          <span className="c-muted">아직 회원이 아니신가요?</span>
          <Link className="c-green" to="/signup">
            회원가입 →
          </Link>
        </p>
      </form>
    </main>
  )
}
