import { useMemo, useState } from 'react'
import Button from '../components/Button.jsx'
import KakaoMap from '../components/KakaoMap.jsx'
import LandmarkVerify from '../components/LandmarkVerify.jsx'
import { getLandmarks } from '../api/landmarkApi.js'
import { getMyStamps } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { BADGES, LANDMARK_ICONS } from '../constants/labels.js'
import { useAsync } from '../utils/useAsync.js'
import './Stamp.css'

// R2 시흥 랜드마크 스탬프 투어 (/stamps) — 로그인 안 해도 볼 수 있고, 인증은 로그인해야 함
// API: GET /api/landmarks (8곳 · 내 인증 여부), GET /api/users/me/stamps (배지, 로그인했을 때만) — 동언, develop
// 들어오는 곳: P4 "스탬프 · 배지", M1 스탬프 배너, R2-1 "스탬프 보러 가기"
export default function R2StampTour() {
  const { user } = useAuth()
  const [version, setVersion] = useState(0) // 인증해서 스탬프를 받으면 올려서 다시 불러옴
  const [selectedId, setSelectedId] = useState(null)
  const who = user?.userId ?? 'guest'
  const landmarks = useAsync(() => getLandmarks(), `r2-landmarks-${who}-${version}`)
  const stamps = useAsync(() => (user ? getMyStamps() : Promise.resolve(null)), `r2-stamps-${who}-${version}`)

  const items = useMemo(() => landmarks.data?.items ?? [], [landmarks.data])
  const markers = useMemo(
    () => items.map((l) => ({ id: l.landmarkId, position: l.point, label: l.name, onClick: () => setSelectedId(l.landmarkId) })),
    [items],
  )
  const total = items.length
  const count = items.filter((l) => l.stamped).length
  const earned = new Set((stamps.data?.badges ?? []).map((b) => b.code))

  return (
    <main className="wrap stamp-page">
      <section className="panel stamp-hero">
        <div className="col g8 grow">
          <h1 className="t-h32 c-strong">시흥 랜드마크 스탬프 투어</h1>
          <p className="t-b16 c-body">시흥의 대표 장소 {total || 8}곳을 걷고 인증해 배지를 모아 보세요</p>
          <p className="t-h24 c-green">
            {user ? `${count} / ${total} 인증 완료` : '로그인하면 내 스탬프가 보여요'}
          </p>
          {user && total > 0 && (
            <>
              <div className="stamp-progress" aria-hidden="true">
                <span style={{ width: `${(count / total) * 100}%` }} />
              </div>
              <p className="t-b14 c-muted">
                {count < total ? `${total - count}곳 더 모으면 ‘시흥 산책 마스터’ 배지` : '시흥 산책 마스터 달성! 🏅'}
              </p>
            </>
          )}
        </div>
        <LandmarkVerify label="📍 GPX로 랜드마크 인증" onVerified={() => setVersion((v) => v + 1)} />
      </section>

      <div className="stamp-body">
        <KakaoMap className="stamp-map" markers={markers} selectedId={selectedId} fit />
        <div className="stamp-list">
          {landmarks.error && <p className="t-b14 c-danger">{landmarks.error.message}</p>}
          {items.map((l) => (
            <button
              key={l.landmarkId}
              className={`panel stamp-card${l.stamped ? ' is-stamped' : ''}${selectedId === l.landmarkId ? ' is-selected' : ''}`}
              type="button"
              onClick={() => setSelectedId(l.landmarkId)}
            >
              <span className="stamp-card-icon">{LANDMARK_ICONS[l.landmarkId] ?? '📍'}</span>
              <span className="col g2">
                <span className="t-b16m c-strong">{l.name}</span>
                <span className={`t-c12 ${l.stamped ? 'c-green' : 'c-muted'}`}>
                  {l.stamped ? `인증 완료 · ${Number(l.stampedAt.slice(5, 7))}월` : '아직 인증 전'}
                </span>
              </span>
            </button>
          ))}
        </div>
      </div>

      <section className="col g16 fill">
        <h2 className="t-h20 c-strong">내 배지</h2>
        <div className="stamp-badges">
          {BADGES.map((b) => (
            <div key={b.code} className={`panel stamp-badge-card${earned.has(b.code) ? '' : ' is-locked'}`}>
              <span className="stamp-badge-icon">{b.icon}</span>
              <p className="t-b14m c-strong">{b.name}</p>
              <p className="t-c12 c-muted">{earned.has(b.code) ? '받았어요' : b.hint}</p>
            </div>
          ))}
        </div>
      </section>

      <div className="row ai-c jc-b fill">
        <p className="t-c12 c-muted">※ 앱 안의 스탬프 · 배지로 제공 · 만보시루 포인트 연계는 시흥시에 향후 제안</p>
        <Button variant="secondary" to="/courses">
          랜드마크 지나는 코스 찾기 →
        </Button>
      </div>
    </main>
  )
}
