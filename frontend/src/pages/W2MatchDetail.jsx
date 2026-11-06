import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import Avatar from '../components/Avatar.jsx'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import KakaoMap from '../components/KakaoMap.jsx'
import LoginRequiredModal from '../components/LoginRequiredModal.jsx'
import Modal from '../components/Modal.jsx'
import X1NotFound from './X1NotFound.jsx'
import X2ServerError from './X2ServerError.jsx'
import {
  cancelMatch,
  closeMatch,
  getComments,
  getMatch,
  joinMatch,
  kickParticipant,
  leaveMatch,
  matchGpxUrl,
  postComment,
} from '../api/matchApi.js'
import { useAuth } from '../auth/authContext.js'
import { useAsync } from '../utils/useAsync.js'
import { showToast } from '../utils/toast.js'
import {
  AGE_LABELS,
  LANGUAGE_LABELS,
  LEVEL_LABELS,
  MATCH_STATUS_LABELS,
  STYLE_LABELS,
  TARGET_LABELS,
} from '../constants/labels.js'
import { daysLeftLabel, formatKm, formatMeetAtLong, formatMeetAtShort } from '../utils/format.js'
import './W2MatchDetail.css'

// 참여할 수 없는 이유 (상세 응답의 reason → 버튼 아래 안내. Figma W2-2 · W4-1)
const REASON_TEXT = {
  KICKED: '모집자가 내보낸 동행이라 다시 참여할 수 없어요.',
  FULL: '정원이 다 찼어요.',
  CLOSED: '모집이 마감됐어요.',
  CANCELED: '모집이 취소된 동행이에요.',
  DONE: '이미 끝난 동행이에요.',
  AGE_MISMATCH: '모집 연령대가 내 프로필의 연령대와 맞지 않아요.',
  MBTI_MISMATCH: '모집하는 MBTI가 내 프로필의 MBTI와 맞지 않아요.',
  LANGUAGE_MISMATCH: '모집 언어 중에 내 프로필의 언어가 없어요.',
}
const LEAVE_REASONS = ['일정이 생겼어요', '몸이 안 좋아요', '날씨', '기타']

/**
 * W2 동행 상세 (참여자 시점) · W4 모집자 시점 (관리 · 공지 · 댓글) — 같은 주소 /matches/:id
 * 상세 응답의 isHost 가 true 면 W4 로 보임
 * 모달: W2-1 참여 확인, W5 참여 취소, 모집 마감 · 모집 취소 · 내보내기 확인, G1 로그인 필요
 * API: GET /api/matches/{id}, POST · DELETE /api/matches/{id}/join, POST /api/matches/{id}/close,
 *      DELETE /api/matches/{id}, DELETE /api/matches/{id}/participants/{userId}, GET · POST /api/matches/{id}/comments
 * 주소에 ?join=1 이 있으면(목록 카드의 "참여하기") 참여 확인 모달을 바로 엶
 */
export default function W2MatchDetail() {
  const { matchId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const { user } = useAuth()
  const [version, setVersion] = useState(0) // 참여 · 마감 뒤 다시 불러오기
  const { data: m, error } = useAsync(() => getMatch(matchId), `${matchId}-${version}-${user?.userId}`)
  const reload = () => setVersion((v) => v + 1)

  const [modal, setModal] = useState(null) // 'join' | 'leave' | 'close' | 'cancel' | 'login' | { kick: participant }

  if (error?.status === 404) return <X1NotFound />
  if (error) return <X2ServerError onRetry={reload} />
  if (!m) return <main className="w2" />

  // 목록 카드에서 "참여하기"로 들어왔으면 참여 확인 모달부터
  const wantsJoin = searchParams.get('join') === '1'
  const openModal = modal ?? (wantsJoin && user && m.joinable ? 'join' : null)
  function closeModal() {
    setModal(null)
    if (wantsJoin) setSearchParams({}, { replace: true })
  }

  const open = m.status === 'OPEN'
  const joined = !m.isHost && m.myStatus === 'JOINED'
  const editable = open || m.status === 'FULL' // 마감 · 취소 · 끝난 글은 수정 · 마감 불가
  const showComments = m.isHost || joined // 공지 · 댓글은 참여자와 모집자만
  const daysLeft = open || m.status === 'FULL' ? daysLeftLabel(m.meetAt) : null
  const emptySeats = Math.max(0, m.capacity - m.participants.length)
  const meetPoint = m.meetPoint ?? (m.path[0] ? { lat: m.path[0][0], lng: m.path[0][1] } : null)
  const routeUrl = meetPoint
    ? `https://map.kakao.com/link/to/${encodeURIComponent(m.meetPlace)},${meetPoint.lat},${meetPoint.lng}`
    : null

  async function run(action, message) {
    try {
      await action()
      if (message) showToast(message)
      setModal(null)
      if (wantsJoin) setSearchParams({}, { replace: true })
      reload()
    } catch (err) {
      showToast(err.message, 'error')
      setModal(null)
      reload()
    }
  }

  function share() {
    navigator.clipboard?.writeText(window.location.href.split('?')[0]).then(
      () => showToast('링크가 복사됐어요'),
      () => showToast('링크를 복사하지 못했어요', 'error'),
    )
  }

  return (
    <main className="w2">
      <div className="wrap w2-inner">
        {/* 왼쪽: 모집 글 내용 */}
        <div className="col g20 grow">
          <Link className="t-b14m c-muted nowrap" to="/matches">
            ← 동행 목록
          </Link>
          <div className="row g8">
            <Chip tone={open ? 'green' : m.status === 'CANCELED' ? 'danger' : 'neutral'}>
              {MATCH_STATUS_LABELS[m.status]} {m.current}/{m.capacity}
            </Chip>
            {daysLeft && <Chip tone="neutral">{daysLeft}</Chip>}
            {m.isHost && <Chip tone="blue">내가 모집한 글</Chip>}
          </div>
          <h1 className="t-h32 c-strong">{m.title}</h1>

          <div className="panel col g12 p20 fill">
            <InfoRow label="📅 일시">
              {formatMeetAtLong(m.meetAt)}
              {m.course.durationMin ? ` · 약 ${m.course.durationMin}분` : ''}
            </InfoRow>
            <InfoRow label="📍 만나는 곳">
              {m.meetPlace}
              {routeUrl && (
                <a className="t-b14m c-green nowrap w2-route" href={routeUrl} target="_blank" rel="noreferrer">
                  🧭 길 찾기
                </a>
              )}
            </InfoRow>
            <InfoRow label="🗺 코스">
              <Link className="w2-link" to={`/courses/${m.course.courseId}`}>
                {m.course.name} · {formatKm(m.course.distanceKm)} · {LEVEL_LABELS[m.course.level]}
              </Link>
            </InfoRow>
            <InfoRow label="👥 인원">
              {m.current} / {m.capacity}명
            </InfoRow>
          </div>

          <div className="col g10">
            <p className="t-h20 c-strong">이런 분과 걷고 싶어요</p>
            <div className="row fwrap g8">
              <Chip tone="neutral">{TARGET_LABELS[m.target]}</Chip>
              {m.mbti && <Chip tone="neutral">MBTI {m.mbti}</Chip>}
              {m.ageGroups.map((a) => (
                <Chip key={a} tone="neutral">
                  {AGE_LABELS[a]}
                </Chip>
              ))}
              {m.styles.map((s) => (
                <Chip key={s} tone="green">
                  {STYLE_LABELS[s]}
                </Chip>
              ))}
              {m.languages.map((l) => (
                <Chip key={l} tone="blue">
                  {LANGUAGE_LABELS[l]}
                </Chip>
              ))}
            </div>
          </div>
          {m.description && <p className="t-b16 c-body w2-desc">{m.description}</p>}

          {m.path.length > 0 && <KakaoMap className="w2-map" path={m.path} showEnds fit />}

          {showComments && <Comments matchId={m.matchId} isHost={m.isHost} />}
        </div>

        {/* 오른쪽: 모집자 · 참여자 · 버튼 */}
        <div className="w2-side">
          <div className="panel col g12 p24 fill">
            <p className="t-b14m c-muted">모집자</p>
            <div className="row ai-c g12">
              <Avatar avatar={m.host.avatar} size={48} />
              <div className="col g2">
                <p className="t-b16m c-strong">{m.host.nickname}</p>
                <p className="t-c12 c-muted nowrap">
                  동행 {m.host.matchCount ?? 0}회 · {m.host.rating ? `후기 ★ ${Number(m.host.rating).toFixed(1)}` : '후기 없음'}
                </p>
              </div>
            </div>
          </div>

          <div className="panel col g12 p24 fill">
            <p className="t-b14m c-muted">
              참여자 {m.current} / {m.capacity}
            </p>
            {m.participants.map((p) => (
              <div key={p.userId} className="row ai-c jc-b fill">
                <div className="row ai-c g12">
                  <Avatar avatar={p.avatar} />
                  <div className="col">
                    <p className="t-b14m c-strong nowrap">
                      {p.nickname}
                      {p.isHost ? ' (모집자)' : ''}
                      {user && p.userId === user.userId && !p.isHost ? ' (나)' : ''}
                    </p>
                    <p className="t-c12 c-muted nowrap">{p.languages.map((l) => LANGUAGE_LABELS[l]).join(' · ')}</p>
                  </div>
                </div>
                {m.isHost && !p.isHost && open && (
                  <button type="button" className="w2-text-btn t-c12b c-danger" onClick={() => setModal({ kick: p })}>
                    내보내기
                  </button>
                )}
              </div>
            ))}
            {Array.from({ length: emptySeats }, (_, i) => (
              <div key={i} className="row ai-c g12">
                <span className="w2-empty-seat">+</span>
                <p className="t-b14 c-muted">빈 자리</p>
              </div>
            ))}
          </div>

          {/* 주요 버튼 */}
          {m.isHost ? (
            <div className="row g8 fill">
              {editable ? (
                <Button variant="secondary" className="grow" to={`/matches/${m.matchId}/edit`}>
                  글 수정
                </Button>
              ) : (
                <Button variant="secondary" className="grow" disabled>
                  글 수정
                </Button>
              )}
              <Button className="grow" disabled={!editable} onClick={() => setModal('close')}>
                {m.status === 'CLOSED' ? '마감했어요' : '모집 마감'}
              </Button>
            </div>
          ) : joined ? (
            <>
              <Button block disabled>
                ✓ 참여 중이에요
              </Button>
              {m.status !== 'DONE' && (
                <Button block variant="secondary" onClick={() => setModal('leave')}>
                  참여 취소
                </Button>
              )}
            </>
          ) : !user ? (
            <Button block disabled={!open} onClick={() => setModal('login')}>
              참여하기
            </Button>
          ) : m.joinable ? (
            <Button block onClick={() => setModal('join')}>
              참여하기
            </Button>
          ) : (
            <>
              <Button block disabled>
                {m.reason === 'FULL' || m.reason === 'CLOSED' ? '모집이 마감됐어요' : '참여할 수 없어요'}
              </Button>
              <div className="w2-note w2-note-blue">{REASON_TEXT[m.reason] ?? '지금은 참여할 수 없어요.'}</div>
            </>
          )}

          {m.hasGpx && (
            <a className="btn btn-secondary btn-block" href={matchGpxUrl(m.matchId)} download>
              ⤓ 동행 경로 GPX 받기
            </a>
          )}
          <div className="row g8 fill">
            <Button variant="secondary" className="grow" onClick={share}>
              링크 공유
            </Button>
            {m.isHost ? (
              <Button variant="danger" disabled={m.status === 'CANCELED' || m.status === 'DONE'} onClick={() => setModal('cancel')}>
                모집 취소
              </Button>
            ) : (
              // 신고 모달(W6)은 승민 담당 — 3주차에 연결
              <Button variant="danger" onClick={() => showToast('신고는 준비 중이에요', 'info')}>
                신고
              </Button>
            )}
          </div>
          <div className="w2-note w2-note-blue">
            <p className="t-b14m">안전 안내</p>
            <p className="t-c12">첫 만남은 코스 시작점 같은 공개 장소에서 해요. 불편한 일이 있으면 신고해 주세요.</p>
          </div>
        </div>
      </div>

      {/* ----- 모달 ----- */}
      <JoinModal
        open={openModal === 'join'}
        match={m}
        onClose={closeModal}
        onConfirm={() => run(() => joinMatch(m.matchId), '참여가 확정됐어요')}
      />
      <LeaveModal
        open={openModal === 'leave'}
        match={m}
        onClose={closeModal}
        onConfirm={(reason) => run(() => leaveMatch(m.matchId, reason), '참여를 취소했어요')}
      />
      <ConfirmModal
        open={openModal === 'close'}
        title="모집을 마감할까요?"
        desc="마감하면 더 이상 새로 참여할 수 없어요. 지금 참여한 사람은 그대로예요."
        okLabel="모집 마감"
        onClose={closeModal}
        onConfirm={() => run(() => closeMatch(m.matchId), '모집을 마감했어요')}
      />
      <ConfirmModal
        open={openModal === 'cancel'}
        danger
        title="모집을 취소할까요?"
        desc="글은 남지만 '모집 취소'로 바뀌고, 참여자들의 내 동행에도 취소됨으로 보여요."
        okLabel="모집 취소"
        onClose={closeModal}
        onConfirm={() => run(() => cancelMatch(m.matchId), '모집을 취소했어요')}
      />
      <ConfirmModal
        open={Boolean(openModal?.kick)}
        danger
        title={`${openModal?.kick?.nickname ?? ''}님을 내보낼까요?`}
        desc="내보낸 사람은 이 동행에 다시 참여할 수 없어요."
        okLabel="내보내기"
        onClose={closeModal}
        onConfirm={() => run(() => kickParticipant(m.matchId, openModal.kick.userId), '참여자를 내보냈어요')}
      />
      <LoginRequiredModal open={openModal === 'login'} onClose={closeModal} />
    </main>
  )
}

function InfoRow({ label, children }) {
  return (
    <div className="w2-info-row">
      <p className="w2-info-key">{label}</p>
      <div className="row ai-c g12 t-b16 c-strong">{children}</div>
    </div>
  )
}

// W2-1 참여 확인
function JoinModal({ open, match, onClose, onConfirm }) {
  const [checked, setChecked] = useState(false)
  const [busy, setBusy] = useState(false)
  return (
    <Modal open={open} onClose={onClose} width={480}>
      <h2 className="t-h24 c-strong">이 동행에 참여할까요?</h2>
      <p className="t-b16 c-body">
        {formatMeetAtShort(match.meetAt)} · {match.meetPlace}
      </p>
      <div className="row fwrap jc-c g6">
        {match.styles.map((s) => (
          <Chip key={s} tone="green">
            {STYLE_LABELS[s]}
          </Chip>
        ))}
        {match.languages.map((l) => (
          <Chip key={l} tone="blue">
            {LANGUAGE_LABELS[l]}
          </Chip>
        ))}
      </div>
      <label className="row ai-c g8 t-b14 c-body w2-check">
        <input type="checkbox" checked={checked} onChange={(e) => setChecked(e.target.checked)} />
        첫 만남은 공개 장소에서 한다는 안내를 확인했어요
      </label>
      <div className="row g8 fill">
        <Button variant="secondary" className="grow" onClick={onClose}>
          취소
        </Button>
        <Button
          className="grow"
          disabled={!checked || busy}
          onClick={async () => {
            setBusy(true)
            await onConfirm()
            setBusy(false)
          }}
        >
          참여 확정
        </Button>
      </div>
    </Modal>
  )
}

// W5 참여 취소 확인
function LeaveModal({ open, match, onClose, onConfirm }) {
  const [reason, setReason] = useState(LEAVE_REASONS[0])
  const [busy, setBusy] = useState(false)
  return (
    <Modal open={open} onClose={onClose} width={480} className="w2-modal-left">
      <h2 className="t-h24 c-strong">참여를 취소할까요?</h2>
      <p className="t-b14 c-body">
        {formatMeetAtShort(match.meetAt)} · {match.course.name}
      </p>
      <p className="t-b14m c-strong">취소 사유 (모집자에게 전달돼요)</p>
      <div className="row fwrap g6">
        {LEAVE_REASONS.map((r) => (
          <Chip key={r} tone="neutral" selected={reason === r} onClick={() => setReason(r)}>
            {r}
          </Chip>
        ))}
      </div>
      <div className="w2-note w2-note-red fill">시작 3시간 전 이후 취소가 반복되면 참여가 제한될 수 있어요</div>
      <div className="row g8 fill">
        <Button variant="secondary" className="grow" onClick={onClose}>
          돌아가기
        </Button>
        <Button
          variant="danger"
          className="grow"
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            await onConfirm(reason)
            setBusy(false)
          }}
        >
          참여 취소
        </Button>
      </div>
    </Modal>
  )
}

// 모집 마감 · 모집 취소 · 내보내기 확인 (Figma 에는 따로 없어서 W5 모양을 따름)
function ConfirmModal({ open, title, desc, okLabel, danger = false, onClose, onConfirm }) {
  const [busy, setBusy] = useState(false)
  return (
    <Modal open={open} onClose={onClose} width={440}>
      <h2 className="t-h24 c-strong">{title}</h2>
      <p className="t-b14 c-body">{desc}</p>
      <div className="row g8 fill">
        <Button variant="secondary" className="grow" onClick={onClose}>
          돌아가기
        </Button>
        <Button
          variant={danger ? 'danger' : 'primary'}
          className="grow"
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            await onConfirm()
            setBusy(false)
          }}
        >
          {okLabel}
        </Button>
      </div>
    </Modal>
  )
}

// W4 공지 · 댓글 (참여자 · 모집자만). 댓글 API 는 민서 일정상 3주차 → 서버에 없으면 안내만
function Comments({ matchId, isHost }) {
  const [version, setVersion] = useState(0)
  const { data, error } = useAsync(() => getComments(matchId), `${matchId}-${version}`)
  const [text, setText] = useState('')
  const [asNotice, setAsNotice] = useState(false)
  const [busy, setBusy] = useState(false)

  const items = data?.items ?? []
  const notices = items.filter((c) => c.isNotice)
  const comments = items.filter((c) => !c.isNotice)

  async function submit(e) {
    e.preventDefault()
    const content = text.trim()
    if (!content) return
    setBusy(true)
    try {
      await postComment(matchId, { content, isNotice: isHost && asNotice })
      setText('')
      setAsNotice(false)
      setVersion((v) => v + 1)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="panel col g14 p24 fill">
      <div className="section-head">
        <p className="t-h20 c-strong">공지 · 댓글 {data ? items.length : ''}</p>
        <p className="t-c12 c-muted">참여자에게만 보여요</p>
      </div>
      {error && <p className="t-b14 c-muted">댓글을 불러오지 못했어요. ({error.message})</p>}
      {notices.map((c) => (
        <div key={c.commentId} className="w2-notice">
          <p className="t-c12b c-green">📌 모집자 공지</p>
          <p className="t-b14 c-strong">{c.content}</p>
        </div>
      ))}
      {comments.map((c) => (
        <div key={c.commentId} className="row g10 fill">
          <Avatar avatar={c.author.avatar} />
          <div className="col g2 grow">
            <div className="row g8">
              <p className="t-b14m c-strong">{c.author.nickname}</p>
              <p className="t-c12 c-muted">{c.createdAt.slice(5, 16).replace('-', '/').replace('T', ' ')}</p>
            </div>
            <p className="t-b14 c-body">{c.content}</p>
          </div>
        </div>
      ))}
      {data && items.length === 0 && <p className="t-b14 c-muted">아직 댓글이 없어요. 만날 곳 · 준비물을 이야기해 보세요.</p>}
      <form className="row ai-c g8 fill" onSubmit={submit}>
        <input
          className="input-field grow"
          value={text}
          maxLength={300}
          onChange={(e) => setText(e.target.value)}
          placeholder="참여자에게 메시지 남기기"
          aria-label="댓글"
        />
        <Button type="submit" disabled={busy || !text.trim()}>
          등록
        </Button>
      </form>
      {isHost && (
        <label className="row ai-c g6 t-b14 c-body w2-check">
          <input type="checkbox" checked={asNotice} onChange={(e) => setAsNotice(e.target.checked)} />
          📌 공지로 올리기
        </label>
      )}
    </div>
  )
}
