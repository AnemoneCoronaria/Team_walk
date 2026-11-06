import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import KakaoMap from '../components/KakaoMap.jsx'
import { unsaveCourse } from '../api/courseApi.js'
import { getSavedCourses } from '../api/myPageApi.js'
import { LEVEL_LABELS } from '../constants/labels.js'
import { formatKm } from '../utils/format.js'
import { showToast } from '../utils/toast.js'
import { useAsync } from '../utils/useAsync.js'
import './MyPage.css'

const SORTS = [
  ['recent', '최근 저장순'],
  ['distance', '거리 짧은 순'],
  ['rating', '별점 높은 순'],
]

// P6 마이페이지 · 저장한 코스 (/mypage/saved)
// API: GET /api/users/me/saved-courses?sort= · DELETE /api/courses/{id}/save
// 저장은 코스 상세(C2)의 ♡ 코스 저장. 카드 → C2, ♥ → 저장 해제 + 토스트
export default function P6SavedCourses() {
  const [sort, setSort] = useState('recent')
  const [version, setVersion] = useState(0) // 해제한 뒤 목록을 다시 불러올 때 올림
  const { data, error } = useAsync(() => getSavedCourses(sort), `p6-${sort}-${version}`)
  const [busyId, setBusyId] = useState(null)

  async function onUnsave(course) {
    setBusyId(course.courseId)
    try {
      await unsaveCourse(course.courseId)
      showToast(`저장을 해제했어요: ${course.name}`)
      setVersion((v) => v + 1)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <div className="section-head">
        <h1 className="t-h24 c-strong">저장한 코스 {data?.total ?? ''}</h1>
        <select className="mypage-sort t-b14m c-body" value={sort} aria-label="정렬" onChange={(e) => setSort(e.target.value)}>
          {SORTS.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="t-b14 c-danger">{error.message}</p>}
      {data?.items.length === 0 && (
        <p className="mypage-empty">
          아직 저장한 코스가 없어요. <Link className="c-green" to="/courses">코스 찾기</Link>에서 마음에 드는 코스를 저장해 보세요
        </p>
      )}
      {data?.items.map((c) => (
        <SavedCard key={c.courseId} course={c} busy={busyId === c.courseId} onUnsave={() => onUnsave(c)} />
      ))}
    </>
  )
}

function SavedCard({ course: c, busy, onUnsave }) {
  const markers = useMemo(() => [{ id: c.courseId, position: c.start }], [c])
  return (
    <div className="panel mypage-saved col g8 fill">
      <Link className="fill" to={`/courses/${c.courseId}`} aria-label={`${c.name} 상세 보기`}>
        <KakaoMap className="mypage-saved-map" markers={markers} level={5} fit interactive={false} />
      </Link>
      <div className="row ai-c jc-b fill">
        <Link className="t-b16m c-strong" to={`/courses/${c.courseId}`}>
          {c.name}
        </Link>
        <button type="button" className="mypage-heart" disabled={busy} aria-label={`${c.name} 저장 해제`} onClick={onUnsave}>
          ♥
        </button>
      </div>
      <p className="t-b14 c-body">
        {formatKm(c.distanceKm)} · {LEVEL_LABELS[c.level]} · {c.openMatches > 0 ? `모집 중 ${c.openMatches}` : '모집 없음'}
      </p>
    </div>
  )
}
