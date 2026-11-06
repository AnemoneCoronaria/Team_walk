import { Link } from 'react-router-dom'
import MatchCard from '../components/MatchCard.jsx'
import { getMatches } from '../api/matchApi.js'
import { useAsync } from '../utils/useAsync.js'
import './Main.css'

const STEPS = [
  ['태그로 프로필 만들기', '산책 스타일 · 언어 · 연령대 선택'],
  ['맞는 동행 찾기', '모집 글에 참여하거나 직접 모집'],
  ['함께 걷기', '코스 시작점에서 만나 산책'],
  ['후기 · 스탬프', 'GPX 공유, 랜드마크 인증'],
]

// M0 메인 (로그인 전) — API: GET /api/matches?size=2
export default function M0Main() {
  const { data, error } = useAsync(() => getMatches({ size: 2 }), 'm0')

  return (
    <main>
      <section className="m0-hero">
        <span className="chip chip-blue">시흥 정왕동 · 배곧 · 내 · 외국인 누구나</span>
        <h1 className="t-display48 c-strong tc nowrap">
          혼자 걷기 아쉬울 때,
          <br />
          나와 맞는 동행을 찾아요
        </h1>
        <p className="t-b16 c-body tc nowrap">
          산책 스타일 · 언어 · 연령대 태그로 이웃을 만나고, 시흥의 바다 뷰 코스와 랜드마크를 함께 걸어요.
        </p>
        <div className="row g12">
          <Link className="btn btn-primary" to="/signup">
            3초 만에 시작하기
          </Link>
          <Link className="btn btn-secondary" to="/courses">
            코스 먼저 둘러보기
          </Link>
        </div>
      </section>

      <section className="m0-steps">
        <div className="wrap col g24">
          <h2 className="t-h24 c-strong">이렇게 이용해요</h2>
          <div className="row g20 fill">
            {STEPS.map(([title, desc], i) => (
              <div key={title} className="panel col g8 p24 grow">
                <p className="t-h24 c-green">{i + 1}</p>
                <p className="t-b16m c-strong">{title}</p>
                <p className="t-b14 c-body">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="m0-preview">
        <div className="wrap col g20">
          <div className="section-head">
            <h2 className="t-h24 c-strong">지금 모집 중인 동행 미리보기</h2>
            <p className="t-b14m c-muted">로그인하면 참여할 수 있어요</p>
          </div>
          <div className="main-match-list">
            {data?.items.map((m) => (
              <MatchCard key={m.matchId} match={m} />
            ))}
            {error && <p className="t-b14 c-muted">모집 글을 불러오지 못했어요. ({error.message})</p>}
          </div>
        </div>
      </section>
    </main>
  )
}
