import { useEffect, useMemo, useState } from 'react'
import { Navigate, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import { getCourse } from '../api/courseApi.js'
import { createReview } from '../api/reviewApi.js'
import { useAuth } from '../auth/authContext.js'
import { showToast } from '../utils/toast.js'
import { useAsync } from '../utils/useAsync.js'
import X1NotFound from './X1NotFound.jsx'
import './Review.css'

const MAX_PHOTOS = 5
const MAX_PHOTO_MB = 5
const MIN_CONTENT = 10

// R1 산책 후기 쓰기 (/courses/:courseId/review, 동행으로 걸었으면 ?matchId=)
// 들어오는 곳: C4 "후기 쓰기", P1 지난 동행 "후기 쓰기" — 로그인해야 쓸 수 있음
// API: GET /api/courses/{id} (코스 이름), POST /api/reviews (동언, PR #20) → 등록하면 C2 로 + 토스트
// 규칙은 서버와 같게: 별점 1~5, 후기 10자 이상, 사진 jpg · png 한 장 5MB 이하 최대 5장
// 랜드마크 인증(GPX · 사진)은 3주차, 함께 걸은 동행 평가는 2순위라 아직 없음
export default function R1ReviewWrite() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <ReviewForm />
}

function ReviewForm() {
  const { courseId } = useParams()
  const [searchParams] = useSearchParams()
  const matchId = searchParams.get('matchId')
  const navigate = useNavigate()
  const course = useAsync(() => getCourse(courseId), `r1-${courseId}`)
  const [rating, setRating] = useState(0)
  const [content, setContent] = useState('')
  const [photos, setPhotos] = useState([]) // 고른 사진 File 목록
  const [submitting, setSubmitting] = useState(false)

  // 고른 사진 미리보기 주소 (화면을 벗어나거나 사진이 바뀌면 정리)
  const previews = useMemo(() => photos.map((file) => URL.createObjectURL(file)), [photos])
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews])

  const contentLength = [...content.trim()].length // 이모지도 한 글자로 셈
  const canSubmit = rating > 0 && contentLength >= MIN_CONTENT && !submitting

  if (course.error?.status === 404) return <X1NotFound />

  function onPickPhotos(e) {
    const picked = [...e.target.files]
    e.target.value = '' // 같은 사진을 다시 고를 수 있게
    const ok = picked.filter((file) => {
      if (!/\.(jpe?g|png)$/i.test(file.name)) {
        showToast('사진은 jpg · png 파일만 올릴 수 있어요.', 'error')
        return false
      }
      if (file.size > MAX_PHOTO_MB * 1024 * 1024) {
        showToast(`사진은 한 장에 ${MAX_PHOTO_MB}MB 이하여야 해요.`, 'error')
        return false
      }
      return true
    })
    const next = [...photos, ...ok]
    if (next.length > MAX_PHOTOS) showToast(`사진은 최대 ${MAX_PHOTOS}장까지 올릴 수 있어요.`, 'info')
    setPhotos(next.slice(0, MAX_PHOTOS))
  }

  async function onSubmit(e) {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    try {
      await createReview({ courseId, matchId, rating, content: content.trim(), photos })
      showToast('후기를 등록했어요')
      navigate(`/courses/${courseId}`, { replace: true })
    } catch (err) {
      setSubmitting(false)
      showToast(err.message, 'error')
    }
  }

  return (
    <main className="review-page">
      <form className="panel review-card col g20" style={{ width: 800 }} onSubmit={onSubmit} noValidate>
        <h1 className="t-h24 c-strong">오늘 산책은 어땠나요?</h1>
        <div className="row ai-c g8">
          {matchId && <Chip tone="blue">동행 후기</Chip>}
          <p className="t-b14 c-body">{course.data?.name}</p>
        </div>

        <div className="col g8">
          <p className="t-b14m c-strong">코스 별점</p>
          <StarInput value={rating} onChange={setRating} />
        </div>

        <div className="col g6 fill">
          <label className="input-label" htmlFor="r1-content">
            후기
          </label>
          <textarea
            id="r1-content"
            className="input-field review-textarea"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="코스와 동행에 대한 후기를 남겨 주세요 (다른 사용자에게 공개돼요)"
          />
          <p className={`input-helper${contentLength > 0 && contentLength < MIN_CONTENT ? ' is-error' : ''}`}>
            {contentLength}자 · {MIN_CONTENT}자 이상 써 주세요
          </p>
        </div>

        <div className="col g8 fill">
          <p className="t-b14m c-strong">사진 (최대 {MAX_PHOTOS}장)</p>
          <div className="row fwrap g8">
            {previews.map((src, i) => (
              <div key={src} className="review-photo">
                <img src={src} alt={`고른 사진 ${i + 1}`} />
                <button
                  className="review-photo-remove"
                  type="button"
                  aria-label={`사진 ${i + 1} 빼기`}
                  onClick={() => setPhotos(photos.filter((_, j) => j !== i))}
                >
                  ×
                </button>
              </div>
            ))}
            {photos.length < MAX_PHOTOS && (
              <label className="review-photo review-photo-add" aria-label="사진 추가">
                ＋
                <input type="file" accept="image/jpeg,image/png" multiple hidden onChange={onPickPhotos} />
              </label>
            )}
          </div>
        </div>

        <div className="review-actions">
          <Button variant="secondary" onClick={() => navigate(-1)} disabled={submitting}>
            나중에
          </Button>
          <Button type="submit" disabled={!canSubmit}>
            {submitting ? '등록 중…' : '후기 등록'}
          </Button>
        </div>
      </form>
    </main>
  )
}

// 별 5개 — 누른 별까지 채워짐
function StarInput({ value, onChange }) {
  return (
    <div className="review-stars" role="radiogroup" aria-label="코스 별점">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          className="review-star"
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n}점`}
          onClick={() => onChange(n)}
        >
          {n <= value ? '★' : '☆'}
        </button>
      ))}
    </div>
  )
}
