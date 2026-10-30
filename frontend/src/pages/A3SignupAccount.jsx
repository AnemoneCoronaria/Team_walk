import { useState } from 'react'
import Button from '../components/Button.jsx'
import Input from '../components/Input.jsx'
import { checkIdAvailable } from '../api/userApi.js'
import { showToast } from '../utils/toast.js'
import { validateLoginId, validatePassword, validatePasswordConfirm } from '../utils/validate.js'

// A3 회원가입 1단계 · 계정 정보 (Signup.jsx 안에서 보임)
// API: GET /api/users/check-id (중복 확인). 가입 API는 A4 에서 부름
// "다음 단계"는 아이디 중복 확인 · 비밀번호 규칙 · 필수 약관을 모두 통과해야 눌림 (Figma 메모)
// 이메일 · 인증번호 칸은 2순위라 만들지 않음
export default function A3SignupAccount({ initial, onNext }) {
  const [loginId, setLoginId] = useState(initial?.loginId ?? '')
  const [password, setPassword] = useState(initial?.password ?? '')
  const [confirm, setConfirm] = useState(initial?.password ?? '')
  const [passwordTouched, setPasswordTouched] = useState(Boolean(initial))
  // 중복 확인을 통과한 아이디. 아이디를 고치면 다시 확인해야 함
  const [checkedId, setCheckedId] = useState(initial && !initial.duplicate ? initial.loginId : '')
  const [idError, setIdError] = useState(initial?.duplicate ? '이미 사용 중인 아이디예요.' : '')
  const [checking, setChecking] = useState(false)
  const [agree, setAgree] = useState({
    terms: initial?.agreeTerms ?? false,
    privacy: initial?.agreePrivacy ?? false,
    location: initial?.agreeLocation ?? false,
  })

  const idOk = checkedId !== '' && checkedId === loginId.trim()
  const passwordError = passwordTouched ? validatePassword(password) : ''
  const confirmError = confirm ? validatePasswordConfirm(password, confirm) : '' // 입력하는 대로 바로 검사
  const allAgreed = agree.terms && agree.privacy && agree.location
  const canNext =
    idOk && !validatePassword(password) && !validatePasswordConfirm(password, confirm) && agree.terms && agree.privacy

  function onChangeId(e) {
    setLoginId(e.target.value)
    setIdError('')
  }

  async function onCheckId() {
    const error = validateLoginId(loginId)
    if (error) {
      setIdError(error)
      return
    }
    const id = loginId.trim()
    setChecking(true)
    try {
      const { available } = await checkIdAvailable(id)
      setCheckedId(available ? id : '')
      setIdError(available ? '' : '이미 사용 중인 아이디예요.')
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setChecking(false)
    }
  }

  function onSubmit(e) {
    e.preventDefault()
    if (!canNext) return
    onNext({
      loginId: loginId.trim(),
      password,
      agreeTerms: agree.terms,
      agreePrivacy: agree.privacy,
      agreeLocation: agree.location,
    })
  }

  return (
    <form className="account-card col g20" style={{ width: 560 }} onSubmit={onSubmit} noValidate>
      <h1 className="t-h24 c-strong">계정 정보를 입력해 주세요</h1>

      <Input
        label="아이디"
        value={loginId}
        onChange={onChangeId}
        placeholder="영문 · 숫자 4~16자"
        maxLength={16}
        autoComplete="username"
        autoFocus
        helper={idOk ? '사용 가능한 아이디입니다' : ''}
        error={idError}
        addon={
          <Button variant="secondary" onClick={onCheckId} disabled={checking}>
            중복 확인
          </Button>
        }
      />
      <Input
        label="비밀번호"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        onBlur={() => setPasswordTouched(true)}
        placeholder="영문 · 숫자 · 특수문자 포함 8자 이상"
        autoComplete="new-password"
        helper="8자 이상, 영문 · 숫자 · 특수문자 조합"
        error={passwordError}
      />
      <Input
        label="비밀번호 확인"
        type="password"
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
        placeholder="비밀번호를 한 번 더 입력"
        autoComplete="new-password"
        error={confirmError}
      />

      <div className="signup-terms">
        <label className="account-check t-b14m">
          <input
            type="checkbox"
            checked={allAgreed}
            onChange={(e) => setAgree({ terms: e.target.checked, privacy: e.target.checked, location: e.target.checked })}
          />
          전체 동의
        </label>
        <label className="account-check">
          <input type="checkbox" checked={agree.terms} onChange={(e) => setAgree({ ...agree, terms: e.target.checked })} />
          (필수) 이용약관 동의
        </label>
        <label className="account-check">
          <input type="checkbox" checked={agree.privacy} onChange={(e) => setAgree({ ...agree, privacy: e.target.checked })} />
          (필수) 개인정보 수집 · 이용 동의
        </label>
        <label className="account-check">
          <input type="checkbox" checked={agree.location} onChange={(e) => setAgree({ ...agree, location: e.target.checked })} />
          (선택) 위치 정보 이용 동의 (랜드마크 인증)
        </label>
      </div>

      <Button type="submit" block disabled={!canNext}>
        다음 단계
      </Button>
    </form>
  )
}
