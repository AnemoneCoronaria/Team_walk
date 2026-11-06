import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Avatar from '../components/Avatar.jsx'
import Button from '../components/Button.jsx'
import Input from '../components/Input.jsx'
import { AgeGroupChips, AvatarPicker, LanguageChips, MbtiPicker, StyleChips } from '../components/ProfileFields.jsx'
import { updateMe } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { MAX_STYLES, isProfileReady, lettersToMbti, mbtiToLetters } from '../utils/profile.js'
import { showToast } from '../utils/toast.js'
import { validateNickname } from '../utils/validate.js'
import './Account.css'

// P2 프로필 · 태그 수정 (/mypage/profile) — 로그인해야 들어올 수 있음
// API: 지금 값은 GET /api/users/me (useAuth 의 user) → 저장은 PUT /api/users/me
// 저장하면 refresh() 로 헤더의 닉네임 · 아바타까지 바꾸고 P1 마이페이지로 (Figma 메모: 저장 → P1 + 토스트)
// 한 줄 소개 · 주로 걷는 동네는 2순위라 없음
export default function P2ProfileEdit() {
  const { user, loading } = useAuth()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace state={{ from: '/mypage/profile' }} />
  return <ProfileEditForm user={user} />
}

function ProfileEditForm({ user }) {
  const navigate = useNavigate()
  const { refresh } = useAuth()
  const [nickname, setNickname] = useState(user.nickname)
  const [ageGroup, setAgeGroup] = useState(user.ageGroup)
  const [avatar, setAvatar] = useState(user.avatar)
  const [styles, setStyles] = useState(user.styles ?? [])
  const [languages, setLanguages] = useState(user.languages ?? [])
  const [mbti, setMbti] = useState(mbtiToLetters(user.mbti))
  const [submitting, setSubmitting] = useState(false)

  const canSave = isProfileReady({ nickname, ageGroup, languages, mbti }) && !submitting

  async function onSubmit(e) {
    e.preventDefault()
    if (!canSave) return
    setSubmitting(true)
    try {
      await updateMe({ nickname: nickname.trim(), ageGroup, avatar, styles, languages, mbti: lettersToMbti(mbti) })
      await refresh()
      showToast('프로필을 저장했어요')
      navigate('/mypage')
    } catch (err) {
      setSubmitting(false)
      showToast(err.message, 'error')
    }
  }

  return (
    <main className="account-page">
      <form className="account-card col g20" style={{ width: 680, borderRadius: 16 }} onSubmit={onSubmit} noValidate>
        <h1 className="t-h24 c-strong">프로필 · 태그 수정</h1>

        <div className="row ai-c g16">
          <Avatar avatar={avatar} size={72} />
          <AvatarPicker value={avatar} onChange={setAvatar} size={40} />
        </div>

        <Input
          label="닉네임"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          maxLength={10}
          helper="2~10자"
          error={validateNickname(nickname)}
        />

        <div className="account-field">
          <p className="row ai-c g8">
            <span className="t-b14m c-strong">연령대</span>
            <span className="t-c12 c-muted">연령대만 공개돼요</span>
          </p>
          <AgeGroupChips value={ageGroup} onChange={setAgeGroup} />
        </div>

        <div className="account-field">
          <p className="row ai-c g8">
            <span className="t-b14m c-strong">산책 스타일</span>
            <span className="t-c12 c-muted">최대 {MAX_STYLES}개</span>
          </p>
          <StyleChips value={styles} onChange={setStyles} />
        </div>

        <div className="account-field">
          <p className="row ai-c g8">
            <span className="t-b14m c-strong">사용 언어</span>
            <span className="t-c12 c-muted">1개 이상</span>
          </p>
          <LanguageChips value={languages} onChange={setLanguages} />
        </div>

        <div className="account-field">
          <p className="row ai-c g8">
            <span className="t-b14m c-strong">MBTI</span>
            <span className="t-c12 c-muted">선택</span>
          </p>
          <MbtiPicker value={mbti} onChange={setMbti} />
        </div>

        <div className="row g12 fill">
          <Button variant="secondary" className="grow" to="/mypage">
            취소
          </Button>
          <Button type="submit" className="grow" disabled={!canSave}>
            {submitting ? '저장 중…' : '저장'}
          </Button>
        </div>
      </form>
    </main>
  )
}
