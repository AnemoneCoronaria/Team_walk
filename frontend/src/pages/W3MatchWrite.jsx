import { useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import Input from '../components/Input.jsx'
import LoginRequiredModal from '../components/LoginRequiredModal.jsx'
import X1NotFound from './X1NotFound.jsx'
import { getCourses, getMyCourses, parseGpx } from '../api/courseApi.js'
import { createMatch, getMatch, updateMatch } from '../api/matchApi.js'
import { useAuth } from '../auth/authContext.js'
import { useAsync } from '../utils/useAsync.js'
import { showToast } from '../utils/toast.js'
import { AGE_LABELS, LANGUAGE_LABELS, LEVEL_LABELS, STYLE_LABELS, TARGET_LABELS, toggleStyle } from '../constants/labels.js'
import { formatKm, toDateParam } from '../utils/format.js'
import './W3MatchWrite.css'

// W3 의 산책 스타일 칩 (Figma 순서 + '반려견과'. 프로필 · W1 필터와 같은 7개)
const STYLE_CHIPS = ['SLOW', 'FAST', 'QUIET', 'CHAT', 'PHOTO', 'SUNRISE_SUNSET', 'DOG'] // 프로필과 같은 7개
const MBTI_PAIRS = [
  ['E', 'I'],
  ['N', 'S'],
  ['T', 'F'],
  ['J', 'P'],
]

/**
 * W3 모집 글 쓰기 (/matches/new) · 수정 (/matches/:id/edit, 모집자만)
 * API: GET /api/courses + GET /api/users/me/courses (코스 고르기), POST /api/gpx/parse (GPX 첨부 확인),
 *      POST /api/matches (multipart) · PUT /api/matches/{id}
 * C2 "이 코스로 동행 모집하기"에서 오면 ?courseId= 로 코스가 미리 골라져 있음
 * 임시 저장은 2순위라 없음
 */
export default function W3MatchWrite() {
  const { matchId } = useParams()
  const { user, loading } = useAuth()
  const editing = Boolean(matchId)
  const existing = useAsync(() => (editing ? getMatch(matchId) : Promise.resolve(null)), `w3-${matchId}`)

  if (loading || existing.loading) return <main className="w3" />
  if (!user) return <LoginFirst />
  if (existing.error?.status === 404) return <X1NotFound />
  if (editing && existing.data && !existing.data.isHost) {
    return <Notice text="모집자만 글을 고칠 수 있어요." />
  }
  // 취소 · 끝난 글은 서버가 수정을 막음(409) → 들어올 때 미리 안내
  if (editing && ['CANCELED', 'DONE'].includes(existing.data?.status)) {
    return (
      <Notice
        text={existing.data.status === 'DONE' ? '이미 끝난 동행은 고칠 수 없어요.' : '모집을 취소한 글은 고칠 수 없어요.'}
        to={`/matches/${matchId}`}
        label="동행 상세로"
      />
    )
  }
  // key: 수정할 글이 바뀌면 입력칸을 새로 채움
  return <WriteForm key={matchId ?? 'new'} user={user} existing={existing.data} />
}

function LoginFirst() {
  const [open, setOpen] = useState(true)
  return (
    <main className="w3">
      <div className="panel w3-card ai-c">
        <h1 className="t-h24 c-strong">로그인하고 모집 글을 써 보세요</h1>
        <Button onClick={() => setOpen(true)}>로그인하기</Button>
      </div>
      <LoginRequiredModal open={open} onClose={() => setOpen(false)} />
    </main>
  )
}

function Notice({ text, to = '/matches', label = '동행 목록으로' }) {
  return (
    <main className="w3">
      <div className="panel w3-card ai-c">
        <p className="t-b16m c-strong">{text}</p>
        <Button variant="secondary" to={to}>
          {label}
        </Button>
      </div>
    </main>
  )
}

// 수정할 때 기존 값으로 채우기
function initialValues(existing, user, presetCourseId) {
  if (!existing) {
    return {
      title: '',
      courseId: presetCourseId ? Number(presetCourseId) : null,
      date: '',
      time: '',
      capacity: '4',
      meetPlace: '',
      targets: [], // 비우면 누구나
      ageGroups: [],
      mbti: ['', '', '', ''],
      styles: [],
      languages: user.languages ?? [],
      description: '',
    }
  }
  return {
    title: existing.title,
    courseId: existing.course.courseId,
    date: existing.meetAt.slice(0, 10),
    time: existing.meetAt.slice(11, 16),
    capacity: String(existing.capacity),
    meetPlace: existing.meetPlace ?? '',
    targets: existing.targets ?? [],
    ageGroups: existing.ageGroups ?? [],
    mbti: existing.mbti ? existing.mbti.split('') : ['', '', '', ''],
    styles: existing.styles,
    languages: existing.languages,
    description: existing.description ?? '',
  }
}

function WriteForm({ user, existing }) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const editing = Boolean(existing)
  const [v, setV] = useState(() => initialValues(existing, user, searchParams.get('courseId')))
  const set = (key, value) => setV((prev) => ({ ...prev, [key]: value }))
  const toggle = (key, value) =>
    setV((prev) => ({
      ...prev,
      [key]: prev[key].includes(value) ? prev[key].filter((x) => x !== value) : [...prev[key], value],
    }))

  // GPX 첨부 (선택): { file, name, distanceKm }. 수정 중 기존 첨부는 hadGpx, 떼면 removeGpx
  const [gpx, setGpx] = useState(null)
  const [removeGpx, setRemoveGpx] = useState(false)
  const hadGpx = Boolean(existing?.hasGpx) && !removeGpx
  const gpxInput = useRef(null)
  const [checkingGpx, setCheckingGpx] = useState(false)

  const [tried, setTried] = useState(false)
  const [askLogin, setAskLogin] = useState(false) // 쓰는 도중 로그인이 풀렸을 때
  const [saving, setSaving] = useState(false)

  // 코스 고르기: 전체 공개 코스 + 내가 올린 코스. 나만 보기(PRIVATE) 코스는 참여자가 볼 수 없어 뺌 (서버도 400)
  // 수정 중이면 지금 글의 코스는 그대로 둠 (글을 쓴 뒤에 나만 보기로 바뀌었을 수 있음)
  const courses = useAsync(
    () =>
      Promise.all([getCourses({ size: 50 }), getMyCourses().catch(() => ({ items: [] }))]).then(([all, mine]) => {
        const byId = new Map()
        const usable = mine.items.filter((c) => c.visibility !== 'PRIVATE' || c.courseId === existing?.course.courseId)
        ;[...usable, ...all.items].forEach((c) => byId.set(c.courseId, { ...c, mine: mine.items.some((x) => x.courseId === c.courseId) }))
        return [...byId.values()]
      }),
    'w3-courses',
  )
  const courseList = useMemo(() => courses.data ?? [], [courses.data])
  const selectedCourse =
    courseList.find((c) => c.courseId === v.courseId) ??
    (existing && existing.course.courseId === v.courseId ? existing.course : null)

  // 만나는 곳: 직접 고치기 전까지는 "코스이름 시작점"으로 자동 입력 (Figma: 시작점이 만나는 곳으로 자동 입력돼요)
  const [placeTouched, setPlaceTouched] = useState(editing)
  const meetPlace = placeTouched || !selectedCourse ? v.meetPlace : v.meetPlace || `${selectedCourse.name} 시작점`
  const errors = validate({ ...v, meetPlace })
  const show = (key) => (tried ? errors[key] : '')

  function pickCourse(c) {
    set('courseId', c.courseId)
  }

  async function onGpx(file) {
    if (!file) return
    setCheckingGpx(true)
    try {
      const parsed = await parseGpx(file)
      setGpx({ file, name: file.name, distanceKm: parsed.distanceKm })
      setRemoveGpx(false)
    } catch (err) {
      showToast(`GPX를 첨부할 수 없어요: ${err.message}`, 'error')
    } finally {
      setCheckingGpx(false)
    }
  }

  async function submit() {
    setTried(true)
    if (Object.values(errors).some(Boolean)) {
      showToast('빠진 칸을 확인해 주세요', 'error')
      return
    }
    const form = {
      title: v.title.trim(),
      courseId: v.courseId,
      meetAt: `${v.date}T${v.time}:00`,
      capacity: Number(v.capacity),
      meetPlace: meetPlace.trim(),
      targets: v.targets,
      mbti: v.targets.includes('MBTI') ? v.mbti.join('') : null,
      ageGroups: v.ageGroups,
      styles: v.styles,
      languages: v.languages,
      description: v.description.trim(),
      gpx: gpx?.file ?? null,
      removeGpx: editing && removeGpx && !gpx ? true : null,
    }
    setSaving(true)
    try {
      if (editing) {
        await updateMatch(existing.matchId, form)
        showToast('모집 글을 고쳤어요')
        navigate(`/matches/${existing.matchId}`)
      } else {
        const { matchId } = await createMatch(form)
        showToast('모집 글이 올라갔어요')
        navigate(`/matches/${matchId}`)
      }
    } catch (err) {
      setSaving(false)
      if (err.status === 401) {
        setAskLogin(true)
        return
      }
      showToast(err.message, 'error')
    }
  }

  return (
    <main className="w3">
      <div className="panel w3-card">
        <h1 className="t-h24 c-strong nowrap">{editing ? '모집 글 고치기' : '동행 모집 글 쓰기'}</h1>

        <Input
          label="제목"
          value={v.title}
          maxLength={30}
          onChange={(e) => set('title', e.target.value)}
          placeholder="예: 퇴근 후 옥구공원 한 바퀴 같이 걸어요"
          helper="최대 30자"
          error={show('title')}
        />

        <CoursePicker
          courses={courseList}
          selected={selectedCourse}
          onPick={pickCourse}
          error={show('courseId')}
        />

        {/* 이번 동행 경로 GPX 첨부 (선택) */}
        <div className="w3-attach">
          <div className="row jc-b ai-c fill">
            <p className="t-b14m c-strong">이번 동행 경로 GPX 첨부 (선택)</p>
            <Button variant="secondary" disabled={checkingGpx} onClick={() => gpxInput.current.click()}>
              {checkingGpx ? '확인하는 중…' : 'GPX 파일 선택'}
            </Button>
            <input
              ref={gpxInput}
              type="file"
              accept=".gpx,application/gpx+xml"
              hidden
              onChange={(e) => {
                onGpx(e.target.files[0])
                e.target.value = ''
              }}
            />
          </div>
          {(gpx || hadGpx) && (
            <div className="row ai-c g8">
              <Chip tone="green">✓ {gpx ? `${gpx.name} · ${formatKm(gpx.distanceKm)}` : '첨부한 GPX 있음'}</Chip>
              <button
                type="button"
                className="w3-text-btn t-c12 c-muted"
                onClick={() => (gpx ? setGpx(null) : setRemoveGpx(true))}
              >
                삭제
              </button>
            </div>
          )}
          <p className="t-c12 c-body">
            코스와 조금 다르게 걷는다면 실제 걸을 경로를 올려 주세요. 참여자는 상세 화면에서 이 GPX를 받아 워치로 따라 걸을 수
            있어요. 첨부한 GPX는 이 모집 글에만 붙고, 공개 코스로 등록되지 않아요.
          </p>
        </div>

        <div className="row g12 fill">
          <Input
            label="날짜"
            type="date"
            value={v.date}
            min={toDateParam(new Date())}
            onChange={(e) => set('date', e.target.value)}
            error={show('date')}
          />
          <Input label="시간" type="time" value={v.time} onChange={(e) => set('time', e.target.value)} error={show('time')} />
          <div className="input input-full">
            <label className="input-label" htmlFor="w3-capacity">
              모집 인원
            </label>
            <select id="w3-capacity" className="input-field" value={v.capacity} onChange={(e) => set('capacity', e.target.value)}>
              {[2, 3, 4, 5, 6].map((n) => (
                <option key={n} value={n}>
                  {n}명 (나 포함)
                </option>
              ))}
            </select>
            {editing && <p className="input-helper">지금 참여한 {existing.current}명보다 적게는 못 바꿔요</p>}
          </div>
        </div>

        <Input
          label="만나는 곳"
          value={meetPlace}
          maxLength={100}
          onChange={(e) => {
            setPlaceTouched(true)
            set('meetPlace', e.target.value)
          }}
          placeholder="예: 옥구공원 정문 주차장 앞"
          helper="공개된 장소를 권장해요"
          error={show('meetPlace')}
        />

        <Group title="모집 대상" hint="여러 개 선택 가능 · 안 고르면 누구나">
          {Object.entries(TARGET_LABELS).map(([code, label]) => (
            <Chip key={code} tone="neutral" selected={v.targets.includes(code)} onClick={() => toggle('targets', code)}>
              {label}
            </Chip>
          ))}
        </Group>

        <Group title="연령대" hint="여러 개 선택 가능">
          <Chip tone="neutral" selected={v.ageGroups.length === 0} onClick={() => set('ageGroups', [])}>
            무관
          </Chip>
          {Object.entries(AGE_LABELS).map(([code, label]) => (
            <Chip key={code} tone="neutral" selected={v.ageGroups.includes(code)} onClick={() => toggle('ageGroups', code)}>
              {label}
            </Chip>
          ))}
        </Group>

        {/* MBTI 맞춤일 때만 고를 수 있음 (아니면 흐리게) */}
        <div className={`col g8 fill ${v.targets.includes('MBTI') ? '' : 'w3-dim'}`}>
          <div className="row ai-c g8">
            <p className="t-b14m c-strong">원하는 MBTI (MBTI 맞춤일 때)</p>
            <p className="t-c12 c-muted">{v.targets.includes('MBTI') ? `${v.mbti.map((x) => x || '_').join('')} · 글자마다 하나씩` : '선택'}</p>
          </div>
          <div className="row fwrap g6">
            {MBTI_PAIRS.map((pair, i) =>
              pair.map((letter) => (
                <Chip
                  key={letter}
                  tone="neutral"
                  selected={v.mbti[i] === letter}
                  onClick={() => v.targets.includes('MBTI') && set('mbti', v.mbti.map((x, j) => (j === i ? letter : x)))}
                >
                  {letter}
                </Chip>
              )),
            )}
          </div>
          {show('mbti') && <p className="t-c12 c-danger">{show('mbti')}</p>}
        </div>
        {v.targets.includes('EXCHANGE') && (
          <div className="w3-note">‘내 · 외국인 교류’를 고르면 서로 다른 언어 태그를 가진 사람이 함께 참여하도록 안내돼요</div>
        )}

        <Group title="원하는 산책 스타일">
          {STYLE_CHIPS.map((code) => (
            <Chip key={code} tone="green" selected={v.styles.includes(code)} onClick={() => set('styles', toggleStyle(v.styles, code))}>
              {STYLE_LABELS[code]}
            </Chip>
          ))}
        </Group>

        <Group title="사용 가능한 언어" hint="1개 이상" error={show('languages')}>
          {Object.entries(LANGUAGE_LABELS).map(([code, label]) => (
            <Chip key={code} tone="blue" selected={v.languages.includes(code)} onClick={() => toggle('languages', code)}>
              {label}
            </Chip>
          ))}
        </Group>

        <div className="input input-full">
          <label className="input-label" htmlFor="w3-desc">
            소개글
          </label>
          <textarea
            id="w3-desc"
            className="input-field w3-textarea"
            value={v.description}
            maxLength={1000}
            onChange={(e) => set('description', e.target.value)}
            placeholder="함께 걷고 싶은 분께 한마디 (선택)"
          />
        </div>

        <div className="row jc-b ai-c fill">
          <Button variant="ghost" onClick={() => navigate(-1)} disabled={saving}>
            취소
          </Button>
          <Button onClick={submit} disabled={saving}>
            {saving ? '올리는 중…' : editing ? '고친 내용 저장' : '모집 글 올리기'}
          </Button>
        </div>
      </div>
      <LoginRequiredModal open={askLogin} onClose={() => setAskLogin(false)} />
    </main>
  )
}

// 입력 규칙 (민서 MatchRequest 와 같게)
function validate(v) {
  const meet = v.date && v.time ? new Date(`${v.date}T${v.time}:00`) : null
  return {
    title: !v.title.trim() ? '제목을 입력해 주세요.' : v.title.trim().length > 30 ? '제목은 30자까지예요.' : '',
    courseId: v.courseId ? '' : '코스를 골라 주세요.',
    date: v.date ? '' : '날짜를 골라 주세요.',
    time: !v.time ? '시간을 골라 주세요.' : meet && meet <= new Date() ? '지금 이후로 정해 주세요.' : '',
    meetPlace: v.meetPlace.trim() ? '' : '만나는 곳을 입력해 주세요.',
    mbti: v.targets.includes('MBTI') && v.mbti.some((x) => !x) ? 'MBTI 네 글자를 모두 골라 주세요.' : '',
    languages: v.languages.length ? '' : '언어를 1개 이상 골라 주세요.',
  }
}

function Group({ title, hint, error, children }) {
  return (
    <div className="col g8 fill">
      <div className="row ai-c g8">
        <p className="t-b14m c-strong">{title}</p>
        {hint && <p className="t-c12 c-muted">{hint}</p>}
      </div>
      <div className="row fwrap g6">{children}</div>
      {error && <p className="t-c12 c-danger">{error}</p>}
    </div>
  )
}

// 코스 고르기: 검색칸 + 아래로 펼쳐지는 목록
function CoursePicker({ courses, selected, onPick, error }) {
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const list = courses.filter((c) => !q.trim() || c.name.includes(q.trim()))

  return (
    <div className="input input-full w3-picker">
      <label className="input-label" htmlFor="w3-course">
        코스 선택
      </label>
      <input
        id="w3-course"
        className={`input-field${error ? ' is-error' : ''}`}
        value={open ? q : selected ? `${selected.name} · ${formatKm(selected.distanceKm)} · ${LEVEL_LABELS[selected.level]}` : q}
        onFocus={() => {
          setOpen(true)
          setQ('')
        }}
        onBlur={() => setTimeout(() => setOpen(false), 150)} // 목록 클릭이 먼저 되도록 잠깐 기다림
        onChange={(e) => setQ(e.target.value)}
        placeholder="🔍 코스 검색 · 내가 올린 코스에서 선택"
        autoComplete="off"
      />
      {open && (
        <div className="w3-picker-list">
          {list.length === 0 && <p className="t-b14 c-muted w3-picker-empty">맞는 코스가 없어요</p>}
          {list.map((c) => (
            <button
              key={c.courseId}
              type="button"
              className="w3-picker-item"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => {
                onPick(c)
                setOpen(false)
              }}
            >
              <span className="t-b14m c-strong">{c.name}</span>
              <span className="t-c12 c-muted">
                {formatKm(c.distanceKm)} · {LEVEL_LABELS[c.level]}
                {c.mine ? ' · 내가 올린 코스' : ''}
              </span>
            </button>
          ))}
        </div>
      )}
      <p className={`input-helper${error ? ' is-error' : ''}`}>{error || '선택한 코스의 시작점이 만나는 곳으로 자동 입력돼요'}</p>
    </div>
  )
}
