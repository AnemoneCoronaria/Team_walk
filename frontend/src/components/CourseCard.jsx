import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import KakaoMap from './KakaoMap.jsx'
import { FACILITY_LABELS, LEVEL_LABELS } from '../constants/labels.js'
import { formatDuration, formatKm } from '../utils/format.js'
import './CourseCard.css'

/**
 * 코스 카드 (Figma 컴포넌트 CourseCard) — M1 "이번 주 추천 코스"
 * course: GET /api/courses 의 items 한 줄 (경로 없이 시작점 start 만 있음)
 * 위쪽 지도는 시작점을 보여 주는 작은 카카오맵 (끌기 · 확대 안 됨)
 */
export default function CourseCard({ course, className = '' }) {
  const markers = useMemo(() => [{ id: course.courseId, position: course.start }], [course])

  return (
    <Link className={`course-card ${className}`} to={`/courses/${course.courseId}`}>
      <KakaoMap className="course-card-map" markers={markers} level={5} fit interactive={false} />
      <p className="t-h20 c-strong nowrap">{course.name}</p>
      <p className="t-b14 c-body nowrap">
        {formatKm(course.distanceKm)} · {LEVEL_LABELS[course.level]} · {formatDuration(course.durationMin)}
      </p>
      <div className="course-card-chips">
        {course.facilities.map((f) => (
          <span key={f} className={`chip ${f === 'SEA_VIEW' ? 'chip-blue' : 'chip-neutral'}`}>
            {FACILITY_LABELS[f]}
          </span>
        ))}
      </div>
    </Link>
  )
}
