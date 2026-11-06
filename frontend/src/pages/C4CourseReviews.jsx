import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Avatar from '../components/Avatar.jsx'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import LoginRequiredModal from '../components/LoginRequiredModal.jsx'
import ReportModal from '../components/ReportModal.jsx'
import { getCourse } from '../api/courseApi.js'
import { getReviews } from '../api/reviewApi.js'
import { useAuth } from '../auth/authContext.js'
import { formatMonthDay, formatStars } from '../utils/format.js'
import { useAsync } from '../utils/useAsync.js'
import X1NotFound from './X1NotFound.jsx'
import './Review.css'

// 정렬 칩: photo 는 정렬이 아니라 "사진 있는 후기만" (API 의 photo=true)
const SORTS = [
  ['recent', '최신순'],
  ['rating', '별점 높은 순'],
  ['photo', '사진 후기'],
]
const PAGE_SIZE = 10

// C4 코스 후기 전체 보기 (/courses/:courseId/reviews) — C2 "전체 보기 →"에서 들어옴
// API: GET /api/courses/{id} (코스 이름), GET /api/courses/{id}/reviews?sort=&photo=&page=&size= (동언, PR #20)
// 별점 분포 · 자주 나온 말 · 도움돼요는 API에 없어서 평균 별점 · 후기 수만 보여 줌. 신고는 W6 신고 창 (후기 대상)
export default function C4CourseReviews() {
  const { courseId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [sort, setSort] = useState('recent')
  const [page, setPage] = useState(1)
  const [askLogin, setAskLogin] = useState(false)
  const [reporting, setReporting] = useState(null) // 신고 창을 연 후기
  const course = useAsync(() => getCourse(courseId), `c4-course-${courseId}`)
  const params = {
    sort: sort === 'photo' ? 'recent' : sort,
    photo: sort === 'photo',
    page,
    size: PAGE_SIZE,
  }
  const reviews = useAsync(() => getReviews(courseId, params), `c4-${courseId}-${JSON.stringify(params)}`)
  const data = reviews.data
  const lastPage = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  if (course.error?.status === 404) return <X1NotFound />

  function onSort(key) {
    setSort(key)
    setPage(1)
  }

  // 후기 쓰기 · 신고는 로그인한 사람만 → 아니면 G1 로그인 필요 창
  function onWrite() {
    if (user) navigate(`/courses/${courseId}/review`)
    else setAskLogin(true)
  }

  function onReport(review) {
    if (user) setReporting(review)
    else setAskLogin(true)
  }

  return (
    <main className="wrap review-list-page col g20">
      <Link className="t-b16m c-body" to={`/courses/${courseId}`}>
        ← {course.data?.name ?? '코스'}
      </Link>

      <div className="section-head">
        <div className="row ai-c g12">
          <h1 className="t-h24 c-strong">후기 {data?.total ?? ''}</h1>
          {data && data.avgRating > 0 && <p className="t-h20 c-green">★ {data.avgRating.toFixed(1)}</p>}
        </div>
        <Button onClick={onWrite}>후기 쓰기</Button>
      </div>

      <div className="row g8">
        {SORTS.map(([key, label]) => (
          <Chip key={key} tone="neutral" selected={sort === key} onClick={() => onSort(key)}>
            {label}
          </Chip>
        ))}
      </div>

      {reviews.error && <p className="t-b14 c-danger">{reviews.error.message}</p>}
      {data && data.items.length === 0 && (
        <p className="t-b14 c-muted">{sort === 'photo' ? '사진이 있는 후기가 아직 없어요.' : '아직 후기가 없어요. 첫 후기를 남겨 주세요!'}</p>
      )}
      {data && data.items.length > 0 && (
        <div className="panel review-list">
          {data.items.map((r) => (
            <ReviewItem key={r.reviewId} review={r} onReport={() => onReport(r)} />
          ))}
        </div>
      )}

      {lastPage > 1 && (
        <div className="review-pager">
          <Button variant="ghost" onClick={() => setPage(page - 1)} disabled={page <= 1}>
            ← 이전
          </Button>
          <p className="t-b14m c-body">
            {page} / {lastPage}
          </p>
          <Button variant="ghost" onClick={() => setPage(page + 1)} disabled={page >= lastPage}>
            다음 →
          </Button>
        </div>
      )}

      <LoginRequiredModal open={askLogin} onClose={() => setAskLogin(false)} />
      <ReportModal
        open={Boolean(reporting)}
        onClose={() => setReporting(null)}
        targetType="REVIEW"
        targetId={reporting?.reviewId}
        targetLabel={`${reporting?.author.nickname}님의 후기`}
      />
    </main>
  )
}

function ReviewItem({ review: r, onReport }) {
  return (
    <div className="review-item">
      <Avatar avatar={r.author.avatar} />
      <div className="col g6 grow">
        <div className="row ai-c jc-b fill">
          <p className="row ai-c g8">
            <span className="t-b14m c-strong">{r.author.nickname}</span>
            <span className="t-c12 c-muted">
              {formatStars(r.rating)} · {formatMonthDay(r.createdAt)} · {r.withMatch ? '동행 후기' : '혼자'}
            </span>
          </p>
          <button className="review-report t-c12 c-muted" type="button" onClick={onReport}>
            신고
          </button>
        </div>
        <p className="t-b14 c-body">{r.content}</p>
        {r.photos.length > 0 && (
          <div className="row fwrap g8">
            {r.photos.map((src) => (
              <img key={src} className="review-thumb" src={src} alt="" />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
