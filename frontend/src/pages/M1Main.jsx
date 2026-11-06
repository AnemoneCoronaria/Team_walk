import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import KakaoMap from '../components/KakaoMap.jsx'
import MatchCard from '../components/MatchCard.jsx'
import CourseCard from '../components/CourseCard.jsx'
import Footer from '../components/Footer.jsx'
import { getMatches } from '../api/matchApi.js'
import { getCourses } from '../api/courseApi.js'
import { getMyStamps } from '../api/userApi.js'
import { useAsync } from '../utils/useAsync.js'
import { formatKm } from '../utils/format.js'
import './Main.css'

// "지금 모집 중인 동행" 위의 빠른 필터 칩 (Figma M1)
const QUICK_FILTERS = [
  { label: '전체', params: {}, tone: 'green' },
  { label: '🐢 천천히', params: { style: 'SLOW' }, tone: 'green' },
  { label: '💬 수다', params: { style: 'CHAT' }, tone: 'green' },
  { label: '한국어', params: { lang: 'KO' }, tone: 'blue' },
  { label: '中文', params: { lang: 'ZH' }, tone: 'blue' },
  { label: 'English', params: { lang: 'EN' }, tone: 'blue' },
]

// 스탬프 배너의 배지 4개 (인증한 수만큼 색이 켜짐)
const BADGE_ICONS = ['🗼', '🌾', '🌊', '🌲']

/**
 * M1 메인 (로그인 후)
 * API: GET /api/matches?size=4, GET /api/courses?sort=rating&size=3, GET /api/users/me/stamps
 * 숫자 3개 = 동행 목록 total, 코스 목록 total, 스탬프 totalLandmarks (명세 "메인 화면")
 */
export default function M1Main() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState(0)
  const filterParams = QUICK_FILTERS[filter].params

  const matches = useAsync(() => getMatches({ ...filterParams, size: 4 }), `m1-${filter}`)
  const allMatches = useAsync(() => getMatches({ size: 1 }), 'm1-total') // 모집 중 전체 수
  const courses = useAsync(() => getCourses({ sort: 'rating', size: 3 }), 'm1-courses')
  const allCourses = useAsync(() => getCourses({ size: 50 }), 'm1-map') // 지도 마커 + 전체 수
  const stamps = useAsync(() => getMyStamps(), 'm1-stamps')

  // 지도: 코스 시작점 마커 (누르면 코스 상세로)
  const markers = useMemo(
    () =>
      (allCourses.data?.items ?? []).map((c) => ({
        id: c.courseId,
        position: c.start,
        label: `${c.name} · ${formatKm(c.distanceKm)}`,
        onClick: (id) => navigate(`/courses/${id}`),
      })),
    [allCourses.data, navigate],
  )

  const stampCount = stamps.data?.stampCount ?? 0

  return (
    <main>
      {/* 히어로: 소개 + 지도 */}
      <section className="m1-hero">
        <div className="wrap m1-hero-inner">
          <div className="m1-hero-text">
            <span className="chip chip-blue">시흥 정왕동 · 배곧</span>
            <h1 className="t-display48 c-strong nowrap">
              나와 맞는 동행을 찾아
              <br />
              시흥을 함께 걷다
            </h1>
            <p className="t-b16 c-body m1-hero-desc">
              산책 스타일과 언어 태그로 이웃을 만나고, 시흥의 바다 뷰 코스를 함께 걸어요.
            </p>
            <div className="row g12">
              <Link className="btn btn-primary" to="/matches">
                동행 찾기
              </Link>
              <Link className="btn btn-secondary" to="/courses">
                코스 둘러보기
              </Link>
            </div>
            <div className="m1-hero-stats">
              <Stat value={allMatches.data?.total} label="모집 중인 동행" />
              <Stat value={allCourses.data?.total} label="등록된 코스" />
              <Stat value={stamps.data?.totalLandmarks} label="인증 랜드마크" />
            </div>
          </div>
          <KakaoMap className="m1-map" markers={markers} fit />
        </div>
      </section>

      {/* 지금 모집 중인 동행 */}
      <section className="m1-matches">
        <div className="wrap col g24">
          <div className="section-head">
            <h2 className="t-h24 c-strong">지금 모집 중인 동행</h2>
            <Link className="t-b14m c-green" to="/matches">
              전체 보기 →
            </Link>
          </div>
          <div className="row g8">
            {QUICK_FILTERS.map((f, i) => (
              <button
                key={f.label}
                type="button"
                className={`chip chip-${f.tone} ${filter === i ? 'is-selected' : ''}`}
                onClick={() => setFilter(i)}
              >
                {f.label}
              </button>
            ))}
          </div>
          <div className="main-match-list">
            {matches.data?.items.map((m) => (
              <MatchCard key={m.matchId} match={m} />
            ))}
            {matches.error && <p className="t-b14 c-muted">모집 글을 불러오지 못했어요. ({matches.error.message})</p>}
            {matches.data?.items.length === 0 && (
              <p className="t-b14 c-muted">조건에 맞는 모집 글이 아직 없어요.</p>
            )}
          </div>
        </div>
      </section>

      {/* 이번 주 추천 코스 (별점순 3개) */}
      <section className="m1-courses">
        <div className="wrap col g24">
          <div className="section-head">
            <h2 className="t-h24 c-strong">이번 주 추천 코스</h2>
            <Link className="t-b14m c-green" to="/courses">
              코스 전체 보기 →
            </Link>
          </div>
          <div className="m1-course-list">
            {courses.data?.items.map((c) => (
              <CourseCard key={c.courseId} course={c} />
            ))}
            {courses.error && <p className="t-b14 c-muted">코스를 불러오지 못했어요. ({courses.error.message})</p>}
          </div>
        </div>
      </section>

      {/* 시흥 랜드마크 스탬프 배너 → 스탬프 화면(R2, 승민)으로 */}
      <section className="m1-stamp">
        <div className="wrap">
          <Link className="m1-stamp-banner" to="/stamps">
            <div className="col g8 nowrap">
              <p className="t-h20 c-green">시흥 랜드마크 스탬프</p>
              <p className="t-b14 c-body">오이도 빨강등대 · 옥구공원 · 배곧한울공원에서 인증하고 배지를 모아보세요</p>
            </div>
            <div className="row g12">
              {BADGE_ICONS.map((icon, i) => (
                <span key={icon} className={`stamp-badge ${i < stampCount ? '' : 'is-locked'}`}>
                  {icon}
                </span>
              ))}
            </div>
          </Link>
        </div>
      </section>

      <Footer />
    </main>
  )
}

function Stat({ value, label }) {
  return (
    <div className="col g2">
      <p className="t-h24 c-green">{value ?? '-'}</p>
      <p className="t-b14 c-muted">{label}</p>
    </div>
  )
}
