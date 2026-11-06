import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import Modal from '../components/Modal.jsx'
import { deleteReview, getMyReviews } from '../api/reviewApi.js'
import { formatMonthDay, formatStars } from '../utils/format.js'
import { showToast } from '../utils/toast.js'
import { useAsync } from '../utils/useAsync.js'
import './MyPage.css'
import './Review.css'

const PAGE_SIZE = 10

// P4 마이페이지 · 후기 · 스탬프 (/mypage/reviews) — 왼쪽 프로필 · 메뉴는 MyPageLayout
// API: GET /api/users/me/reviews · DELETE /api/reviews/{id} (동언, develop). 스탬프 · 배지 탭은 R2 화면으로
// 후기 수정(R1 수정 모드)은 API에 수정이 없어서 없음 → 지우고 다시 쓰기. 받은 평가는 2순위
export default function P4MyReviews() {
  const [page, setPage] = useState(1)
  const [version, setVersion] = useState(0) // 삭제한 뒤 다시 불러올 때 올림
  const { data, error } = useAsync(() => getMyReviews({ page, size: PAGE_SIZE }), `p4-${page}-${version}`)
  const [deleting, setDeleting] = useState(null) // 삭제 확인 창을 연 후기
  const [busy, setBusy] = useState(false)
  const lastPage = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  async function onDelete() {
    setBusy(true)
    try {
      await deleteReview(deleting.reviewId)
      showToast('후기를 삭제했어요')
      setDeleting(null)
      if (data.items.length === 1 && page > 1) setPage(page - 1) // 마지막 쪽의 마지막 후기였으면 앞 쪽으로
      setVersion((v) => v + 1)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="row g8">
        <Chip tone="neutral" selected>
          내가 쓴 후기 {data?.total ?? ''}
        </Chip>
        <Link className="chip chip-neutral" to="/stamps">
          스탬프 · 배지 →
        </Link>
      </div>

      {error && <p className="t-b14 c-danger">{error.message}</p>}
      {data?.items.length === 0 && (
        <p className="mypage-empty">아직 쓴 후기가 없어요. 동행이 끝나면 마이페이지 지난 동행에서 후기를 남겨 보세요</p>
      )}
      {data?.items.map((r) => (
        <div key={r.reviewId} className="panel mypage-row col g8 fill">
          <div className="row ai-c jc-b fill">
            <div className="row ai-c g8">
              <Link className="t-b16m c-strong" to={`/courses/${r.course.courseId}`}>
                {r.course.name}
              </Link>
              <span className="t-c12 c-muted">{formatMonthDay(r.createdAt)}</span>
              <Chip tone={r.withMatch ? 'blue' : 'neutral'}>{r.withMatch ? '동행 후기' : '혼자'}</Chip>
            </div>
            <Button variant="danger" onClick={() => setDeleting(r)}>
              삭제
            </Button>
          </div>
          <p className="t-b14m c-green">{formatStars(r.rating)}</p>
          <p className="t-b14 c-body">{r.content}</p>
          {r.photos.length > 0 && (
            <div className="row fwrap g8">
              {r.photos.map((src) => (
                <img key={src} className="review-thumb" src={src} alt="" />
              ))}
            </div>
          )}
        </div>
      ))}

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

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} className="modal-left">
        <h2 className="t-h20 c-strong">후기를 삭제할까요?</h2>
        <p className="t-b14 c-body">
          ‘{deleting?.course.name}’ 후기와 사진이 지워지고 되돌릴 수 없어요. 코스 별점에서도 빠져요.
        </p>
        <div className="row g12 fill">
          <Button variant="secondary" className="grow" onClick={() => setDeleting(null)}>
            취소
          </Button>
          <Button variant="danger" className="grow" onClick={onDelete} disabled={busy}>
            삭제
          </Button>
        </div>
      </Modal>
    </>
  )
}
