import { Fragment, useEffect, useState } from 'react'
import A3SignupAccount from './A3SignupAccount.jsx'
import A4SignupProfile from './A4SignupProfile.jsx'
import A5SignupDone from './A5SignupDone.jsx'
import './Account.css'

const STEPS = ['계정 정보', '프로필 · 태그', '완료']

// 회원가입 (/signup) — A3 계정 정보 → A4 프로필 · 태그 → A5 완료
// 가입 API(POST /api/users)는 A4 "가입 완료"에서 A3 + A4 값을 모아 한 번에 보냅니다.
// 그래서 A3 · A4 에서 입력한 값은 이 화면이 들고 있다가 다음 단계로 넘겨 줍니다. (이전으로 가도 그대로)
export default function Signup() {
  const [step, setStep] = useState(1)
  const [account, setAccount] = useState(null) // A3 값 { loginId, password, agreeTerms, agreePrivacy, agreeLocation }
  const [profile, setProfile] = useState(null) // A4 값 { nickname, ageGroup, avatar, styles, languages, mbti }

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [step])

  function goProfile(values) {
    setAccount(values)
    setStep(2)
  }

  // duplicate: 가입할 때 아이디가 이미 있다고 나오면 A3 로 돌아가서 다시 중복 확인
  function backToAccount(values, duplicate = false) {
    setProfile(values)
    if (duplicate) setAccount({ ...account, duplicate: true })
    setStep(1)
  }

  function finish(values) {
    setProfile(values)
    setStep(3)
  }

  return (
    <main className="account-page">
      <SignupSteps current={step} />
      {step === 1 && <A3SignupAccount initial={account} onNext={goProfile} />}
      {step === 2 && <A4SignupProfile account={account} initial={profile} onBack={backToAccount} onDone={finish} />}
      {step === 3 && <A5SignupDone profile={profile} />}
    </main>
  )
}

// 단계 표시 — 지나온 단계까지 초록
function SignupSteps({ current }) {
  return (
    <div className="signup-steps" aria-label={`회원가입 ${current} / ${STEPS.length}단계`}>
      {STEPS.map((label, i) => {
        const n = i + 1
        return (
          <Fragment key={label}>
            {n > 1 && <span className={`signup-steps-line${n <= current ? ' is-done' : ''}`} />}
            <span className={`signup-step${n <= current ? ' is-done' : ''}`} aria-current={n === current ? 'step' : undefined}>
              <span className="signup-step-num">{n}</span>
              {label}
            </span>
          </Fragment>
        )
      })}
    </div>
  )
}
