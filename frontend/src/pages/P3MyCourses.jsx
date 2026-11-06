import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import Input from '../components/Input.jsx'
import Modal from '../components/Modal.jsx'
import { courseGpxUrl, getCourse } from '../api/courseApi.js'
import { deleteMyCourse, getMyCourses, updateMyCourse } from '../api/myPageApi.js'
import { useAuth } from '../auth/authContext.js'
import { FACILITY_LABELS, LEVEL_LABELS, VISIBILITY_LABELS } from '../constants/labels.js'
import { formatDuration, formatKm } from '../utils/format.js'
import { showToast } from '../utils/toast.js'
import { useAsync } from '../utils/useAsync.js'
import './MyPage.css'

const VISIBILITY_TONES = { PUBLIC: 'green', MATCH_ONLY: 'blue', PRIVATE: 'neutral' }

// P3 마이페이지 · 내가 올린 코스 (/mypage/courses)
// API: GET /api/users/me/courses · PUT /api/courses/{id} (정보 수정) · DELETE /api/courses/{id} (동언, PR #20)
// Figma 메모의 "수정 → C3-3 수정 모드"는 동훈 화면이라, 여기서는 C3-3 과 같은 칸
// (이름 · 난이도 · 소개 · 편의시설 · 공개 범위)을 창으로 고침. GPX 경로는 바꾸지 않음
export default function P3MyCourses() {
  const { refresh } = useAuth()
  const [version, setVersion] = useState(0) // 수정 · 삭제한 뒤 목록을 다시 불러올 때 올림
  const { data, error } = useAsync(() => getMyCourses(), `p3-${version}`)
  const [editing, setEditing] = useState(null) // 수정 창을 연 코스
  const [deleting, setDeleting] = useState(null) // 삭제 확인 창을 연 코스
  const [busy, setBusy] = useState(false)

  function reload() {
    setVersion((v) => v + 1)
    refresh() // 왼쪽 · P1 의 "올린 코스" 수도 바뀜
  }

  async function onDelete() {
    setBusy(true)
    try {
      await deleteMyCourse(deleting.courseId)
      showToast('코스를 삭제했어요')
      setDeleting(null)
      reload()
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="section-head">
        <h1 className="t-h24 c-strong">내가 올린 코스 {data?.total ?? ''}</h1>
        <Button to="/courses/new">+ GPX로 코스 올리기</Button>
      </div>

      {error && <p className="t-b14 c-danger">{error.message}</p>}
      {data?.items.length === 0 && <p className="mypage-empty">아직 올린 코스가 없어요. GPX 파일로 첫 코스를 올려 보세요</p>}
      {data?.items.map((c) => (
        <div key={c.courseId} className="panel mypage-course fill">
          <CourseThumb />
          <div className="col g4 grow">
            <div className="row ai-c g8">
              <Link className="t-b16m c-strong" to={`/courses/${c.courseId}`}>
                {c.name}
              </Link>
              <Chip tone={VISIBILITY_TONES[c.visibility]}>{VISIBILITY_LABELS[c.visibility]}</Chip>
            </div>
            <p className="t-b14 c-body">
              {formatKm(c.distanceKm)} · {LEVEL_LABELS[c.level]} · {formatDuration(c.durationMin)}
            </p>
            <p className="t-c12 c-muted">
              ★ {c.reviewCount ? c.rating.toFixed(1) : '-'} · 후기 {c.reviewCount}개
            </p>
          </div>
          <div className="row ai-c g8 noshrink">
            <Button variant="ghost" onClick={() => setEditing(c)}>
              수정
            </Button>
            <a className="btn btn-ghost" href={courseGpxUrl(c.courseId)} download>
              GPX ↓
            </a>
            <Button variant="danger" onClick={() => setDeleting(c)}>
              삭제
            </Button>
          </div>
        </div>
      ))}

      {editing && (
        <CourseEditModal
          course={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            reload()
          }}
        />
      )}

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} className="mypage-modal">
        <h2 className="t-h20 c-strong">코스를 삭제할까요?</h2>
        <p className="t-b14 c-body">
          ‘{deleting?.name}’ 코스가 목록에서 사라져요. 이 코스로 잡힌 동행과 후기는 그대로 남아요.
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

// 목록 썸네일 (Figma P3 의 경로 그림. 목록 API에는 경로가 없어서 모양만)
function CourseThumb() {
  return (
    <svg className="mypage-thumb" width="92" height="64" viewBox="0 0 92 64" aria-hidden="true">
      <rect width="92" height="64" rx="10" style={{ fill: 'var(--map-land)' }} />
      <path d="M0 50 Q46 40 92 48 V54 Q92 64 82 64 H10 Q0 64 0 54 Z" style={{ fill: 'var(--brand-blue-soft)' }} />
      <ellipse cx="56" cy="22" rx="18" ry="12" style={{ fill: 'var(--brand-green-soft)' }} />
      <path
        d="M10 46 C28 24 52 36 82 14"
        style={{ fill: 'none', stroke: 'var(--brand-green)', strokeWidth: 3, strokeLinecap: 'round' }}
      />
    </svg>
  )
}

// 코스 정보 수정 창 — 소개(description)는 목록에 없어서 코스 상세(GET /api/courses/{id})에서 가져옴
function CourseEditModal({ course, onClose, onSaved }) {
  const detail = useAsync(() => getCourse(course.courseId), `p3-edit-${course.courseId}`)

  return (
    <Modal open onClose={onClose} width={560} className="mypage-modal">
      <h2 className="t-h20 c-strong">코스 정보 수정</h2>
      {detail.loading && <p className="t-b14 c-muted">불러오는 중…</p>}
      {detail.error && <p className="t-b14 c-danger">{detail.error.message}</p>}
      {detail.data && <CourseEditForm course={course} detail={detail.data} onClose={onClose} onSaved={onSaved} />}
    </Modal>
  )
}

function CourseEditForm({ course, detail, onClose, onSaved }) {
  const [name, setName] = useState(detail.name)
  const [level, setLevel] = useState(detail.level)
  const [description, setDescription] = useState(detail.description ?? '')
  const [facilities, setFacilities] = useState(detail.facilities ?? [])
  const [visibility, setVisibility] = useState(course.visibility)
  const [busy, setBusy] = useState(false)
  const nameError = name.trim() ? '' : '코스 이름을 입력해 주세요.'

  function toggleFacility(code) {
    setFacilities(facilities.includes(code) ? facilities.filter((f) => f !== code) : [...facilities, code])
  }

  async function onSubmit(e) {
    e.preventDefault()
    if (nameError || busy) return
    setBusy(true)
    try {
      await updateMyCourse(course.courseId, {
        name: name.trim(),
        description: description.trim(), // 비우면 ''로 보내야 지워짐 (null 은 "그대로 둠")
        level,
        facilities,
        visibility,
      })
      showToast('코스 정보를 저장했어요')
      onSaved()
    } catch (err) {
      showToast(err.message, 'error')
      setBusy(false)
    }
  }

  return (
    <form className="col g16 fill" onSubmit={onSubmit} noValidate>
      <Input label="코스 이름" value={name} onChange={(e) => setName(e.target.value)} maxLength={20} helper="최대 20자" error={nameError} />

      <div className="col g8 fill">
        <p className="t-b14m c-strong">난이도</p>
        <div className="row fwrap g6">
          {Object.entries(LEVEL_LABELS).map(([code, label]) => (
            <Chip key={code} tone="neutral" selected={level === code} onClick={() => setLevel(code)}>
              {label}
            </Chip>
          ))}
        </div>
      </div>

      <div className="col g8 fill">
        <label className="input-label" htmlFor="p3-description">
          소개 (선택)
        </label>
        <textarea
          id="p3-description"
          className="input-field mypage-textarea"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="코스의 특징 · 주의할 점을 적어 주세요"
        />
      </div>

      <div className="col g8 fill">
        <p className="t-b14m c-strong">편의시설</p>
        <div className="row fwrap g6">
          {Object.entries(FACILITY_LABELS).map(([code, label]) => (
            <Chip key={code} tone="neutral" selected={facilities.includes(code)} onClick={() => toggleFacility(code)}>
              {label}
            </Chip>
          ))}
        </div>
      </div>

      <div className="col g8 fill">
        <p className="t-b14m c-strong">공개 범위</p>
        <div className="row fwrap g6">
          {Object.entries(VISIBILITY_LABELS).map(([code, label]) => (
            <Chip key={code} tone="neutral" selected={visibility === code} onClick={() => setVisibility(code)}>
              {label}
            </Chip>
          ))}
        </div>
      </div>

      <div className="row g12 fill">
        <Button variant="secondary" className="grow" onClick={onClose}>
          취소
        </Button>
        <Button type="submit" className="grow" disabled={Boolean(nameError) || busy}>
          {busy ? '저장 중…' : '저장'}
        </Button>
      </div>
    </form>
  )
}
