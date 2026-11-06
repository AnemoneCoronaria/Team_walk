import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Input from '../components/Input.jsx'
import Modal from '../components/Modal.jsx'
import { changePassword, withdraw } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { showToast } from '../utils/toast.js'
import { validatePassword, validatePasswordConfirm } from '../utils/validate.js'
import './Account.css'
import './MyPage.css'

// P5 마이페이지 · 계정 설정 (/mypage/settings) + 비밀번호 변경 창 + P5-1 회원 탈퇴 확인 창
// API: PUT /api/users/me/password · DELETE /api/users/me (정란, develop)
// 탈퇴는 지우지 않음 (db/README "지우지 않습니다"): 회원은 WITHDRAWN, 쓴 코스 · 글 · 후기는 "탈퇴한 회원"으로 남고
// 참여 예정 동행만 자동 취소. 아이디는 다시 쓸 수 없음. 내가 연 동행이 진행 중이면 409 HAS_OPEN_MATCH
// 이메일 · 알림 · 공개 범위 · 화면 언어 · 차단 목록은 2순위라 없음 (Figma 메모)
export default function P5AccountSettings() {
  const { user } = useAuth()
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [withdrawOpen, setWithdrawOpen] = useState(false)

  return (
    <>
      <h1 className="t-h24 c-strong">계정 설정</h1>

      <div className="panel mypage-box fill">
        <h2 className="t-h20 c-strong">계정</h2>
        <div className="mypage-setting">
          <p className="row ai-c g16">
            <span className="mypage-setting-label">아이디</span>
            <span className="t-b16 c-strong">{user.loginId}</span>
          </p>
        </div>
        <div className="mypage-setting">
          <p className="row ai-c g16">
            <span className="mypage-setting-label">비밀번호</span>
            <span className="t-b16 c-strong">••••••••</span>
          </p>
          <Button variant="secondary" onClick={() => setPasswordOpen(true)}>
            변경
          </Button>
        </div>
      </div>

      <div className="mypage-danger fill">
        <p className="t-b14 c-muted">회원 탈퇴 — 올린 코스 · 모집 글 · 후기는 ‘탈퇴한 회원’으로 남아요</p>
        <Button variant="danger" onClick={() => setWithdrawOpen(true)}>
          회원 탈퇴
        </Button>
      </div>

      {/* 창을 닫으면 입력한 값이 지워지도록 열 때만 만듦 */}
      {passwordOpen && <PasswordModal onClose={() => setPasswordOpen(false)} />}
      {withdrawOpen && <WithdrawModal onClose={() => setWithdrawOpen(false)} />}
    </>
  )
}

// 비밀번호 변경 창 — 새 비밀번호 규칙은 회원가입(A3)과 같음
function PasswordModal({ onClose }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [nextTouched, setNextTouched] = useState(false)
  const [error, setError] = useState('') // 서버 오류 (지금 비밀번호가 틀림 등)
  const [busy, setBusy] = useState(false)

  const sameAsNow = next !== '' && next === current
  const nextError = nextTouched
    ? validatePassword(next) || (sameAsNow ? '지금 비밀번호와 다른 비밀번호로 바꿔 주세요.' : '')
    : ''
  const confirmError = confirm ? validatePasswordConfirm(next, confirm) : ''
  const canSave = current !== '' && !validatePassword(next) && !sameAsNow && !validatePasswordConfirm(next, confirm) && !busy

  async function onSubmit(e) {
    e.preventDefault()
    if (!canSave) return
    setBusy(true)
    try {
      await changePassword(current, next)
      showToast('비밀번호를 바꿨어요')
      onClose()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} width={480} className="mypage-modal">
      <form className="col g16 fill" onSubmit={onSubmit} noValidate>
        <h2 className="t-h20 c-strong">비밀번호 변경</h2>
        {error && (
          <p className="account-alert" role="alert">
            {error}
          </p>
        )}
        <Input
          label="지금 비밀번호"
          type="password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          autoComplete="current-password"
          autoFocus
        />
        <Input
          label="새 비밀번호"
          type="password"
          value={next}
          onChange={(e) => setNext(e.target.value)}
          onBlur={() => setNextTouched(true)}
          placeholder="영문 · 숫자 · 특수문자 포함 8자 이상"
          autoComplete="new-password"
          helper="8자 이상, 영문 · 숫자 · 특수문자 조합"
          error={nextError}
        />
        <Input
          label="새 비밀번호 확인"
          type="password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          autoComplete="new-password"
          error={confirmError}
        />
        <div className="row g12 fill">
          <Button variant="secondary" className="grow" onClick={onClose}>
            취소
          </Button>
          <Button type="submit" className="grow" disabled={!canSave}>
            {busy ? '바꾸는 중…' : '변경'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

// P5-1 회원 탈퇴 확인 창 — 비밀번호를 한 번 더 받고, 탈퇴하면 로그아웃 + 메인(M0)
function WithdrawModal({ onClose }) {
  const navigate = useNavigate()
  const { user, refresh } = useAuth()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    if (!password || busy) return
    setBusy(true)
    try {
      await withdraw(password)
    } catch (err) {
      setError(err.message)
      setBusy(false)
      return
    }
    navigate('/', { replace: true })
    await refresh() // 헤더 · 메인을 로그인 전 상태로
    showToast('탈퇴가 끝났어요. 그동안 함께 걸어 주셔서 고마워요')
  }

  return (
    <Modal open onClose={onClose} width={480} className="mypage-modal">
      <form className="col g16 fill" onSubmit={onSubmit} noValidate>
        <h2 className="t-h20 c-strong">정말 탈퇴하시겠어요?</h2>
        <ul className="mypage-notice">
          <li>참여 예정인 동행은 자동으로 취소돼요</li>
          <li>내가 모집 중이거나 마감한 동행이 있으면 먼저 모집을 취소해 주세요</li>
          <li>올린 코스 {user.courseCount}개 · 모집 글 · 후기는 지워지지 않고 ‘탈퇴한 회원’으로 남아요</li>
          <li>같은 아이디로 다시 가입할 수 없어요</li>
        </ul>
        <Input
          label="비밀번호 확인"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="지금 비밀번호를 입력해 주세요"
          autoComplete="current-password"
          autoFocus
          error={error}
        />
        <div className="row g12 fill">
          <Button variant="secondary" className="grow" onClick={onClose}>
            취소
          </Button>
          <Button variant="danger" type="submit" className="grow" disabled={!password || busy}>
            {busy ? '탈퇴하는 중…' : '탈퇴하기'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
