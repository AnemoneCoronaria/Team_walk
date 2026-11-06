import { useState } from 'react'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import { getMyStamps } from '../api/userApi.js'
import { getMyMatches } from '../api/myPageApi.js'
import { useAuth } from '../auth/authContext.js'
import { MATCH_STATUS_LABELS } from '../constants/labels.js'
import { formatKm, formatMeetAt } from '../utils/format.js'
import { useAsync } from '../utils/useAsync.js'
import './MyPage.css'

const TABS = [
  ['upcoming', '참여 예정'],
  ['hosting', '내가 모집한 글'],
  ['past', '지난 동행'],
]

const EMPTY = {
  upcoming: '참여 예정인 동행이 없어요. 동행 찾기에서 함께 걸을 이웃을 찾아보세요',
  hosting: '내가 모집한 글이 없어요',
  past: '아직 함께 걸은 동행이 없어요',
}

// P1 마이페이지 · 내 동행 (/mypage) — 왼쪽 프로필 · 메뉴는 components/MyPageLayout.jsx
// API: GET /api/users/me (통계: 함께 걸은 동행 · 올린 코스 · 총 거리), GET /api/users/me/stamps (랜드마크 n / 8),
//      GET /api/users/me/matches?tab= (탭 3개 · 민서)
// 참여 취소(W5)는 동행 상세(W2)에 있어서 상세로 보냄. 지난 동행의 출석 확인 · 설문(P1-1)은 2순위
export default function P1MyPage() {
  const { user } = useAuth()
  const [tab, setTab] = useState('upcoming')
  const stamps = useAsync(() => getMyStamps(), 'p1-stamps')
  // 탭 옆 숫자를 보여 주려고 세 탭을 한 번에 불러 둠
  const lists = useAsync(() => Promise.all(TABS.map(([key]) => getMyMatches(key))), 'p1-matches')
  const tabIndex = TABS.findIndex(([key]) => key === tab)
  const items = lists.data?.[tabIndex].items ?? []

  return (
    <>
      <div className="row g12 fill">
        <Stat value={user.matchCount} label="함께 걸은 동행" />
        <Stat value={user.courseCount} label="올린 코스" />
        <Stat value={formatKm(user.totalDistanceKm)} label="총 거리" />
        <Stat value={stamps.data ? `${stamps.data.stampCount} / ${stamps.data.totalLandmarks}` : '-'} label="랜드마크" />
      </div>

      <div className="row g8">
        {TABS.map(([key, label], i) => (
          <Chip key={key} tone="neutral" selected={tab === key} onClick={() => setTab(key)}>
            {label} {lists.data?.[i].total ?? ''}
          </Chip>
        ))}
      </div>

      {lists.error && <p className="t-b14 c-danger">{lists.error.message}</p>}
      {lists.data && items.length === 0 && <p className="mypage-empty">{EMPTY[tab]}</p>}
      {items.map((m) => (
        <MyMatchRow key={m.matchId} match={m} past={tab === 'past'} />
      ))}
    </>
  )
}

function Stat({ value, label }) {
  return (
    <div className="panel mypage-stat">
      <p className="t-h24 c-green">{value}</p>
      <p className="t-b14 c-muted">{label}</p>
    </div>
  )
}

// 동행 한 줄 — 모집자면 "글 수정", 참여자면 "참여 취소", 지난 동행이면 "후기 쓰기"(R1)
// "내가 모집한 글" 탭에는 마감 · 끝난 글도 옴 → 글 수정은 W2 처럼 모집 중 · 정원 참일 때만 (그 밖은 서버가 409)
function MyMatchRow({ match: m, past }) {
  const isHost = m.role === 'HOST'
  const editable = m.status === 'OPEN' || m.status === 'FULL'
  const state =
    past || m.status === 'DONE'
      ? '끝난 동행'
      : isHost
        ? `모집자 · ${MATCH_STATUS_LABELS[m.status]} ${m.current}/${m.capacity}명`
        : '참여 확정'

  return (
    <div className="panel mypage-row col g8 fill">
      <div className="row ai-c jc-b fill">
        <p className="t-b14m c-blue">{formatMeetAt(m.meetAt)}</p>
        <Chip tone={isHost ? 'green' : 'blue'}>{isHost ? '모집자' : '참여'}</Chip>
      </div>
      <p className="t-h20 c-strong">{m.title}</p>
      <div className="row ai-c jc-b fill">
        <p className="t-b14 c-body">
          {m.course.name} · {state}
        </p>
        <div className="row g8">
          {past ? (
            m.reviewed ? (
              <Button variant="ghost" disabled>
                후기 완료
              </Button>
            ) : (
              <Button variant="ghost" to={`/courses/${m.course.courseId}/review?matchId=${m.matchId}`}>
                후기 쓰기
              </Button>
            )
          ) : isHost ? (
            editable ? (
              <Button variant="ghost" to={`/matches/${m.matchId}/edit`}>
                글 수정
              </Button>
            ) : (
              <Button variant="ghost" disabled title="마감했거나 끝난 동행은 고칠 수 없어요">
                글 수정
              </Button>
            )
          ) : (
            <Button variant="ghost" to={`/matches/${m.matchId}`}>
              참여 취소
            </Button>
          )}
          <Button variant="secondary" to={`/matches/${m.matchId}`}>
            상세 보기
          </Button>
        </div>
      </div>
    </div>
  )
}
