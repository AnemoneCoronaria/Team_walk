import Avatar from '../components/Avatar.jsx'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import { LANGUAGE_LABELS, STYLE_LABELS } from '../constants/labels.js'

// A5 회원가입 · 완료 (Signup.jsx 안에서 보임) — 아바타, 환영 문구, A4 에서 고른 태그 요약
export default function A5SignupDone({ profile }) {
  return (
    <section className="account-card col ai-c g20" style={{ width: 560 }}>
      <Avatar avatar={profile.avatar} size={80} />
      <h1 className="t-h24 c-strong">환영해요, 산책러 {profile.nickname}님!</h1>
      <p className="t-b16 c-body">이제 나와 맞는 동행을 찾아 함께 걸어 보세요</p>
      <div className="row fwrap jc-c g8">
        {profile.styles.map((code) => (
          <Chip key={code}>{STYLE_LABELS[code]}</Chip>
        ))}
        {profile.languages.map((code) => (
          <Chip key={code} tone="blue">
            {LANGUAGE_LABELS[code]}
          </Chip>
        ))}
      </div>
      <div className="row g12 fill">
        <Button variant="secondary" className="grow" to="/courses">
          코스 둘러보기
        </Button>
        <Button className="grow" to="/matches">
          동행 찾으러 가기
        </Button>
      </div>
    </section>
  )
}
