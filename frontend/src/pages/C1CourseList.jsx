import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import KakaoMap from '../components/KakaoMap.jsx'
import { getCourses } from '../api/courseApi.js'
import { useAsync } from '../utils/useAsync.js'
import { FACILITY_LABELS, LEVEL_LABELS } from '../constants/labels.js'
import { formatDuration, formatKm } from '../utils/format.js'
import './C1CourseList.css'

// 거리 칩 → API 파라미터 (명세: ~3km = maxDistance=3, 3~5km = min 3 & max 5, 5km~ = minDistance=5)
const DISTANCES = {
  le3: { label: '~3km', params: { maxDistance: 3 } },
  '3to5': { label: '3~5km', params: { minDistance: 3, maxDistance: 5 } },
  ge5: { label: '5km~', params: { minDistance: 5 } },
}
const LEVELS = ['EASY', 'NORMAL', 'HARD']
const FACILITIES = ['TOILET', 'BENCH', 'PARKING', 'SEA_VIEW'] // 코스 등록(C3-3)과 같은 4개

/**
 * C1 코스 찾기 (지도 + 목록) · C5 검색 결과 없음 · L1 로딩
 * API: GET /api/courses?q=&minDistance=&maxDistance=&level=&facility=&sort=distance
 * 필터는 주소(?q=옥구&dist=le3&level=EASY&facility=TOILET,BENCH)에 남겨서 뒤로 가기 · 새로고침에도 유지
 */
export default function C1CourseList() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const dist = searchParams.get('dist') ?? ''
  const level = searchParams.get('level') ?? ''
  const facility = searchParams.get('facility') ? searchParams.get('facility').split(',') : []

  const params = { q, ...(DISTANCES[dist]?.params ?? {}), level, facility, sort: 'distance', size: 50 }
  const { data, error, loading } = useAsync(() => getCourses(params), JSON.stringify(params))
  const items = useMemo(() => data?.items ?? [], [data])

  // 지도와 목록에서 선택된 코스 (처음에는 첫 번째)
  const [picked, setPicked] = useState(null)
  const selectedId = items.some((c) => c.courseId === picked) ? picked : items[0]?.courseId

  const markers = useMemo(
    () =>
      items.map((c) => ({
        id: c.courseId,
        position: c.start,
        label: `${c.name} · ${formatKm(c.distanceKm)}`,
        onClick: setPicked,
      })),
    [items, setPicked],
  )

  // 주소의 필터 값 바꾸기 (빈 값이면 주소에서 뺌)
  function setParam(key, value) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    setSearchParams(next)
  }
  const toggleSingle = (key, current, value) => setParam(key, current === value ? '' : value)
  const toggleFacility = (f) =>
    setParam('facility', (facility.includes(f) ? facility.filter((x) => x !== f) : [...facility, f]).join(','))

  function onSearch(e) {
    e.preventDefault()
    setParam('q', new FormData(e.currentTarget).get('q').trim())
  }

  return (
    <main>
      {/* 제목 + 검색 · 필터 */}
      <section className="c1-top">
        <div className="wrap col g16">
          <h1 className="t-h24 c-strong">코스 찾기</h1>
          <div className="c1-filters">
            <form className="input c1-search" onSubmit={onSearch}>
              <label className="input-label" htmlFor="c1-q">
                검색
              </label>
              {/* key: 주소의 q 가 바뀌면(헤더 검색 등) 입력칸도 새로 채움 */}
              <input
                key={q}
                id="c1-q"
                name="q"
                className="input-field"
                defaultValue={q}
                placeholder="🔍  코스 이름 · 장소 (예: 옥구공원)"
              />
            </form>
            <FilterGroup title="거리">
              {Object.entries(DISTANCES).map(([key, d]) => (
                <ChipButton key={key} on={dist === key} onClick={() => toggleSingle('dist', dist, key)}>
                  {d.label}
                </ChipButton>
              ))}
            </FilterGroup>
            <FilterGroup title="난이도">
              {LEVELS.map((lv) => (
                <ChipButton key={lv} on={level === lv} onClick={() => toggleSingle('level', level, lv)}>
                  {LEVEL_LABELS[lv]}
                </ChipButton>
              ))}
            </FilterGroup>
            <FilterGroup title="편의시설">
              {FACILITIES.map((f) => (
                <ChipButton key={f} on={facility.includes(f)} onClick={() => toggleFacility(f)}>
                  {FACILITY_LABELS[f]}
                </ChipButton>
              ))}
            </FilterGroup>
          </div>
        </div>
      </section>

      {/* 지도 + 결과 목록 */}
      <section className="c1-body">
        <div className="wrap row g24">
          {/* 마커를 누르면 그 코스를 선택(말풍선)만 함. 상세는 목록 카드를 눌러서 */}
          <KakaoMap className="c1-map" markers={markers} selectedId={selectedId} fit />

          {loading ? (
            <SkeletonList />
          ) : error ? (
            <div className="c1-empty">
              <p className="t-emoji32">⚠️</p>
              <p className="t-b16m c-strong">코스를 불러오지 못했어요</p>
              <p className="t-b14 c-body">{error.message}</p>
            </div>
          ) : items.length === 0 ? (
            <EmptyResult q={q} dist={dist} onReset={() => setSearchParams({})} />
          ) : (
            <div className="c1-list">
              <p className="t-b14m c-muted nowrap">검색 결과 {data.total}개 · 거리순</p>
              {items.map((c) => (
                <Link
                  key={c.courseId}
                  to={`/courses/${c.courseId}`}
                  className={`c1-item ${c.courseId === selectedId ? 'is-selected' : ''}`}
                  onMouseEnter={() => setPicked(c.courseId)}
                >
                  <CourseThumb course={c} />
                  <div className="c1-item-text">
                    <p className="t-b16m c-strong">{c.name}</p>
                    <p className="t-b14 c-body">
                      {formatKm(c.distanceKm)} · {LEVEL_LABELS[c.level]} · {formatDuration(c.durationMin)}
                    </p>
                    <p className="t-c12 c-muted">
                      {c.facilities.map((f) => FACILITY_LABELS[f]).join(' · ') || '편의시설 정보 없음'}
                    </p>
                  </div>
                  {/* Figma 의 '모집 중 N' 칩은 목록 API에 값이 없어 별점으로 대신 표시 */}
                  {c.reviewCount > 0 ? (
                    <span className="chip chip-green">★ {c.rating.toFixed(1)}</span>
                  ) : (
                    <span className="chip chip-neutral">후기 없음</span>
                  )}
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>
    </main>
  )
}

function FilterGroup({ title, children }) {
  return (
    <div className="col g6">
      <p className="t-b14m c-strong nowrap">{title}</p>
      <div className="row g6">{children}</div>
    </div>
  )
}

function ChipButton({ on, onClick, children }) {
  return (
    <button type="button" className={`chip chip-neutral ${on ? 'is-selected' : ''}`} onClick={onClick}>
      {children}
    </button>
  )
}

// 목록의 작은 지도 (시작점)
function CourseThumb({ course }) {
  const markers = useMemo(() => [{ id: course.courseId, position: course.start }], [course])
  return <KakaoMap className="c1-item-thumb" markers={markers} level={6} fit interactive={false} />
}

// L1 로딩: 목록 자리에 뼈대 5개
function SkeletonList() {
  return (
    <div className="c1-list">
      <span className="skeleton" style={{ width: 140, height: 21 }} />
      {[1, 2, 3, 4, 5].map((i) => (
        <div key={i} className="c1-skel">
          <span className="skeleton noshrink" style={{ width: 96, height: 72, borderRadius: 10 }} />
          <span className="col g8">
            <span className="skeleton" style={{ width: 180, height: 12 }} />
            <span className="skeleton" style={{ width: 240, height: 12 }} />
            <span className="skeleton" style={{ width: 120, height: 12 }} />
          </span>
        </div>
      ))}
    </div>
  )
}

// C5 검색 결과 없음
function EmptyResult({ q, dist, onReset }) {
  const where = q ? `‘${q}’ 근처 ` : ''
  const range = dist ? `${DISTANCES[dist].label.replace('~', '')} ${dist === 'le3' ? '이하 ' : dist === 'ge5' ? '이상 ' : ''}` : ''
  return (
    <div className="c1-empty">
      <p className="t-emoji32 c-strong nowrap">🔍</p>
      <p className="t-b16m c-strong nowrap">
        {where || range ? `${where}${range}코스가 없어요` : '조건에 맞는 코스가 없어요'}
      </p>
      <p className="t-b14 c-body tc" style={{ width: 360 }}>
        거리 조건을 넓히거나, 직접 걸은 경로를 올려 첫 코스를 만들어 보세요
      </p>
      <div className="row g8">
        <button type="button" className="btn btn-secondary" onClick={onReset}>
          조건 초기화
        </button>
        <Link className="btn btn-primary" to="/courses/new">
          GPX로 코스 올리기
        </Link>
      </div>
    </div>
  )
}
