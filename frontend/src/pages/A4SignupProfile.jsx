import { useState } from 'react'
import Button from '../components/Button.jsx'
import Input from '../components/Input.jsx'
import { AgeGroupChips, AvatarPicker, LanguageChips, MbtiPicker, StyleChips } from '../components/ProfileFields.jsx'
import { login, signup } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { MAX_STYLES, NO_MBTI, isProfileReady, lettersToMbti } from '../utils/profile.js'
import { showToast } from '../utils/toast.js'
import { validateNickname } from '../utils/validate.js'

// A4 회원가입 2단계 · 프로필 · 태그 (Signup.jsx 안에서 보임)
// API: POST /api/users (A3 계정 정보 + 여기 값을 한 번에) → 이어서 POST /api/auth/login
//      (가입 API는 로그인까지 해 주지 않아서, 같은 아이디 · 비밀번호로 바로 로그인)
// 고르는 부품(아바타 · 칩 · MBTI)은 P2 프로필 수정과 같이 씀 → components/ProfileFields.jsx
export default function A4SignupProfile({ account, initial, onBack, onDone }) {
  const { refresh } = useAuth()
  const [nickname, setNickname] = useState(initial?.nickname ?? '')
  const [nicknameTouched, setNicknameTouched] = useState(Boolean(initial))
  const [ageGroup, setAgeGroup] = useState(initial?.ageGroup ?? '')
  const [avatar, setAvatar] = useState(initial?.avatar ?? 'seagull')
  const [styles, setStyles] = useState(initial?.styles ?? [])
  const [languages, setLanguages] = useState(initial?.languages ?? [])
  const [mbti, setMbti] = useState(initial?.mbti ?? NO_MBTI) // 쌍마다 고른 글자 (예: ['E', '', 'T', ''])
  const [submitting, setSubmitting] = useState(false)

  const nicknameError = nicknameTouched ? validateNickname(nickname) : ''
  const canSubmit = isProfileReady({ nickname, ageGroup, languages, mbti }) && !submitting

  // 지금 입력한 값 (이전 단계로 갈 때 들고 가고, A5 에서 보여 줌)
  function values() {
    return { nickname: nickname.trim(), ageGroup, avatar, styles, languages, mbti }
  }

  async function onSubmit(e) {
    e.preventDefault()
    if (!canSubmit) return
    const profile = values()
    setSubmitting(true)
    try {
      await signup({
        ...account,
        nickname: profile.nickname,
        ageGroup,
        avatar,
        styles,
        languages,
        mbti: lettersToMbti(mbti),
      })
    } catch (err) {
      setSubmitting(false)
      showToast(err.message, 'error')
      // 그사이 다른 사람이 같은 아이디로 가입함 → A3 로 돌아가서 다시 중복 확인
      if (err.code === 'DUPLICATE') onBack(profile, true)
      return
    }

    try {
      await login(account.loginId, account.password)
      await refresh() // 헤더를 로그인 상태로
    } catch {
      // 로그인이 안 돼도 가입은 끝났으니 A5 는 보여 줌 (헤더의 로그인 버튼으로 다시 로그인하면 됨)
    }
    onDone(profile)
  }

  return (
    <form className="account-card col g20" style={{ width: 640 }} onSubmit={onSubmit} noValidate>
      <h1 className="t-h24 c-strong">나와 맞는 동행을 찾기 위한 프로필</h1>
      <p className="t-b14 c-body">실제 사진 대신 아바타를 써요. 태그는 나중에 마이페이지에서 바꿀 수 있어요</p>

      <Input
        label="닉네임"
        value={nickname}
        onChange={(e) => setNickname(e.target.value)}
        onBlur={() => setNicknameTouched(true)}
        placeholder="2~10자"
        maxLength={10}
        autoFocus
        helper="다른 사람에게 보이는 이름"
        error={nicknameError}
      />

      <div className="col g8 fill">
        <p className="row ai-c g8">
          <span className="t-b14m c-strong">연령대</span>
          <span className="t-c12 c-muted">나이는 공개되지 않고 연령대만 보여요</span>
        </p>
        <AgeGroupChips value={ageGroup} onChange={setAgeGroup} />
      </div>

      <div className="account-field">
        <p className="t-b14m c-strong">아바타 캐릭터 (시흥 테마)</p>
        <AvatarPicker value={avatar} onChange={setAvatar} />
      </div>

      <div className="account-field">
        <p className="t-b14m c-strong">산책 스타일 (최대 {MAX_STYLES}개)</p>
        <StyleChips value={styles} onChange={setStyles} />
      </div>

      <div className="account-field">
        <p className="row ai-c g8">
          <span className="t-b14m c-strong">함께 쓰고 싶은 언어</span>
          <span className="t-c12 c-muted">1개 이상</span>
        </p>
        <LanguageChips value={languages} onChange={setLanguages} />
      </div>

      <div className="account-field">
        <p className="t-b14m c-strong">MBTI (선택 · 재미용)</p>
        <MbtiPicker value={mbti} onChange={setMbti} />
      </div>

      <p className="signup-note">언어 태그는 내 · 외국인 주민이 서로 편한 언어로 만날 수 있게 도와줘요</p>

      <div className="row g12 fill">
        <Button variant="secondary" className="grow" onClick={() => onBack(values())} disabled={submitting}>
          이전
        </Button>
        <Button type="submit" className="grow" disabled={!canSubmit}>
          {submitting ? '가입 중…' : '가입 완료'}
        </Button>
      </div>
    </form>
  )
}
