import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import KakaoMap from '../components/KakaoMap.jsx'
import LoginRequiredModal from '../components/LoginRequiredModal.jsx'
import ReportModal from '../components/ReportModal.jsx'
import X1NotFound from './X1NotFound.jsx'
import { courseGpxUrl, getCourse, getCourseReviews, saveCourse, unsaveCourse } from '../api/courseApi.js'
import { getMatches } from '../api/matchApi.js'
import { useAuth } from '../auth/authContext.js'
import { showToast } from '../utils/toast.js'
import { useAsync } from '../utils/useAsync.js'
import { FACILITY_LABELS, LEVEL_LABELS, avatarSrc, targetLabels } from '../constants/labels.js'
import { formatDate, formatDuration, formatKm, formatMeetAt, formatMonthDay, formatStars } from '../utils/format.js'
import './C2CourseDetail.css'

const FACILITY_ROWS = [
  ['TOILET', '🚻'],
  ['BENCH', '🪑'],
  ['PARKING', '🅿'],
]

/**
 * C2 코스 상세
 * API: GET /api/courses/{id}, GET /api/courses/{id}/reviews?size=3,
 *      GET /api/matches?courseId={id}
 * GPX 다운로드: <a href="/api/courses/{id}/gpx"> (파일이라 백엔드를 켜야 받아짐)
 */
export default function C2CourseDetail() {
  const { courseId } = useParams()
  const { user } = useAuth()
  const [modal, setModal] = useState(null) // 'report' | 'login'
  const [savedNow, setSavedNow] = useState({}) // 이 화면에서 ♡ 를 누른 결과 (코스 번호별)
  const [saving, setSaving] = useState(false)
  const course = useAsync(() => getCourse(courseId), `course-${courseId}`)
  const reviews = useAsync(() => getCourseReviews(courseId, { size: 3 }), `reviews-${courseId}`)
  const matches = useAsync(() => getMatches({ courseId, size: 3 }), `matches-${courseId}`)

  const c = course.data
  const ends = useMemo(() => c?.path ?? null, [c])
  const isSaved = savedNow[courseId] ?? c?.saved ?? false

  async function toggleSave() {
    if (!user) {
      setModal('login')
      return
    }
    setSaving(true)
    try {
      await (isSaved ? unsaveCourse(courseId) : saveCourse(courseId))
      setSavedNow((now) => ({ ...now, [courseId]: !isSaved }))
      showToast(isSaved ? '저장을 해제했어요' : '코스를 저장했어요. 마이페이지에서 볼 수 있어요')
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setSaving(false)
    }
  }

  if (course.error?.status === 404) {
    return <X1NotFound />
  }
  if (course.error) {
    return (
      <section className="wrap col ai-c g12" style={{ padding: '120px 0' }}>
        <p className="t-b16m c-strong">코스를 불러오지 못했어요</p>
        <p className="t-b14 c-body">{course.error.message}</p>
      </section>
    )
  }
  if (!c) return <section className="c2" />

  const [startLat, startLng] = c.path[0]
  const kakaoRouteUrl = `https://map.kakao.com/link/to/${encodeURIComponent(c.name)},${startLat},${startLng}`

  return (
    <main className="c2">
      <div className="wrap c2-inner">
        {/* 왼쪽: 코스 정보 */}
        <div className="c2-main">
          <div className="c2-toprow t-b14m c-muted">
            <Link to="/courses">← 코스 찾기</Link>
            {/* 내가 올린 코스는 신고 대신 아무것도 안 보임 (서버 isMine) */}
            {!c.isMine && (
              <button type="button" className="c2-link" onClick={() => setModal(user ? 'report' : 'login')}>
                🚩 코스 신고
              </button>
            )}
          </div>
          <div className="row g8">
            <span className="chip chip-green">{LEVEL_LABELS[c.level]}</span>
            {c.facilities.includes('SEA_VIEW') && <span className="chip chip-blue">바다 뷰</span>}
            {c.landmarks.length > 0 && <span className="chip chip-blue">랜드마크 {c.landmarks.length}곳</span>}
          </div>
          <h1 className="t-h32 c-strong nowrap">{c.name}</h1>
          {c.description && <p className="t-b16 c-body">{c.description}</p>}

          <div className="c2-stats">
            <Stat value={formatKm(c.distanceKm)} label="거리" />
            <Stat value={formatDuration(c.durationMin)} label="예상 시간" />
            <Stat value={`★ ${c.reviewCount ? c.rating.toFixed(1) : '-'}`} label={`후기 ${c.reviewCount}개`} />
          </div>

          <KakaoMap className="c2-map" path={ends} showEnds fit />
          <div className="c2-legend t-c12 c-muted">
            <span>
              <i className="c2-dot c2-dot-start" /> 출발
            </span>
            <span>
              <i className="c2-dot c2-dot-end" /> 도착
            </span>
            <span className="c-green">— 경로</span>
          </div>

          <div className="panel c2-box">
            <h2 className="t-h20 c-strong nowrap">편의시설 · 랜드마크</h2>
            {FACILITY_ROWS.map(([code, icon]) => (
              <div key={code} className="row g16">
                <p className="t-b14m c-muted c2-key">
                  {icon} {FACILITY_LABELS[code]}
                </p>
                <p className={`t-b16 nowrap ${c.facilities.includes(code) ? 'c-strong' : 'c-muted'}`}>
                  {c.facilities.includes(code) ? '있음' : '없음'}
                </p>
              </div>
            ))}
            <div className="row g16">
              <p className="t-b14m c-muted c2-key">🗼 랜드마크</p>
              <p className="t-b16 c-strong nowrap">
                {c.landmarks.length ? `${c.landmarks.map((l) => l.name).join(', ')} (스탬프 인증 가능)` : '없음'}
              </p>
            </div>
            <p className="t-c12 c-muted nowrap">코스를 올린 사람이 고른 편의시설이에요 (있음/없음만)</p>
          </div>

          <div className="panel c2-box">
            <div className="section-head">
              <h2 className="t-h20 c-strong">후기 {reviews.data?.total ?? c.reviewCount}</h2>
              {/* C4 후기 전체 보기는 승민 담당 */}
              <Link className="t-b14m c-green" to={`/courses/${c.courseId}/reviews`}>
                전체 보기 →
              </Link>
            </div>
            {reviews.data?.items.length === 0 && <p className="t-b14 c-muted">아직 후기가 없어요. 첫 후기를 남겨 주세요!</p>}
            {reviews.data?.items.map((r) => (
              <div key={r.reviewId} className="row g12 fill">
                <img className="avatar" src={avatarSrc(r.author.avatar)} alt="" />
                <div className="col g4 grow">
                  <div className="row g8 nowrap">
                    <p className="t-b14m c-strong">{r.author.nickname}</p>
                    <p className="t-c12 c-muted">
                      {formatStars(r.rating)} · {formatMonthDay(r.createdAt)}
                      {r.withMatch ? ' · 동행 후기' : ''}
                    </p>
                  </div>
                  <p className="t-b14 c-body">{r.content}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 오른쪽: 버튼 · 등록한 사람 · 모집 중인 동행 */}
        <div className="c2-side">
          <div className="panel c2-box">
            <Link className="btn btn-primary btn-block" to={`/matches/new?courseId=${c.courseId}`}>
              이 코스로 동행 모집하기
            </Link>
            <a className="btn btn-secondary btn-block" href={courseGpxUrl(c.courseId)} download>
              ⤓ GPX 다운로드
            </a>
            <button type="button" className="btn btn-secondary btn-block" disabled={saving} onClick={toggleSave}>
              {isSaved ? '♥ 저장됨' : '♡ 코스 저장'}
            </button>
            <a className="btn btn-secondary btn-block" href={kakaoRouteUrl} target="_blank" rel="noreferrer">
              🧭 카카오맵 길 찾기
            </a>
            <p className="t-c12 c-muted">받은 GPX는 삼성헬스 ‘경로 목표’, 가민 코스 등으로 가져갈 수 있어요</p>
          </div>

          <div className="panel c2-box">
            <p className="t-b14m c-muted nowrap">등록한 사람</p>
            <div className="row ai-c g12">
              <img className="avatar" src={avatarSrc(c.author.avatar)} alt="" />
              <p className="t-b14 c-body nowrap">
                {c.author.nickname} · {formatDate(c.createdAt)}
              </p>
            </div>
          </div>

          <h2 className="t-h20 c-strong nowrap">이 코스에서 모집 중인 동행 {matches.data?.total ?? ''}</h2>
          {matches.data?.items.length === 0 && (
            <p className="t-b14 c-muted">아직 모집 중인 동행이 없어요. 첫 모집 글을 올려 보세요!</p>
          )}
          {matches.data?.items.map((m) => (
            <Link key={m.matchId} className="panel c2-match" to={`/matches/${m.matchId}`}>
              <p className="t-c12b c-blue nowrap">{formatMeetAt(m.meetAt)}</p>
              <p className="t-b14m c-strong">{m.title}</p>
              <div className="row g6">
                <span className="chip chip-green">
                  모집 중 {m.current}/{m.capacity}
                </span>
                <span className="chip chip-neutral">{targetLabels(m.targets).join(' · ')}</span>
              </div>
            </Link>
          ))}
        </div>
      </div>
      <LoginRequiredModal open={modal === 'login'} onClose={() => setModal(null)} />
      <ReportModal
        open={modal === 'report'}
        onClose={() => setModal(null)}
        targetType="COURSE"
        targetId={c.courseId}
        targetLabel={c.name}
      />
    </main>
  )
}

function Stat({ value, label }) {
  return (
    <div className="c2-stat">
      <p className="t-h20 c-green">{value}</p>
      <p className="t-c12 c-muted">{label}</p>
    </div>
  )
}
