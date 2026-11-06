import { useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Chip from '../components/Chip.jsx'
import Input from '../components/Input.jsx'
import KakaoMap from '../components/KakaoMap.jsx'
import LoginRequiredModal from '../components/LoginRequiredModal.jsx'
import { createCourse, parseGpx } from '../api/courseApi.js'
import { useAuth } from '../auth/authContext.js'
import { AREA_LABELS, FACILITY_LABELS, LEVEL_LABELS } from '../constants/labels.js'
import { formatKm } from '../utils/format.js'
import { showToast } from '../utils/toast.js'
import './C3CourseUpload.css'

const STEPS = ['GPX 파일 올리기', '경로 확인', '코스 정보 입력']
const VISIBILITY = { PUBLIC: '전체 공개', MATCH_ONLY: '동행 참여자에게만', PRIVATE: '나만 보기' }
// 편의시설은 명세 값 약속 4개 (Figma 의 '반려견 OK' 는 명세에 없어서 뺌)
const FACILITIES = ['TOILET', 'BENCH', 'PARKING', 'SEA_VIEW']

/**
 * C3-1 ~ C3-3 GPX로 코스 등록 (+ C3-1E 업로드 오류) — 주소 /courses/new 하나에서 단계만 바뀜
 * API: POST /api/gpx/parse (C3-1, 저장 안 함) → POST /api/courses (C3-3, 같은 파일 + 입력값)
 * 둘 다 로그인 필요 → 로그인 전이면 파일을 고를 때 G1 로그인 필요 모달
 */
export default function C3CourseUpload() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [file, setFile] = useState(null)
  const [parsed, setParsed] = useState(null)
  const [error, setError] = useState(null) // { fileName, message } → C3-1E
  const [parsing, setParsing] = useState(false)
  const [askLogin, setAskLogin] = useState(false)

  async function onFile(f) {
    if (!f) return
    if (!user) {
      setAskLogin(true)
      return
    }
    setParsing(true)
    try {
      const data = await parseGpx(f)
      setFile(f)
      setParsed(data)
      setError(null)
      setStep(2)
    } catch (err) {
      if (err.status === 401) {
        setAskLogin(true) // 로그인이 풀렸으면 G1
        return
      }
      setError({ fileName: f.name, message: errorText(err) })
      showToast(errorText(err), 'error')
    } finally {
      setParsing(false)
    }
  }

  function reset() {
    setFile(null)
    setParsed(null)
    setError(null)
    setStep(1)
  }

  return (
    <main className="c3">
      <Steps current={step} />
      {step === 1 && <StepFile error={error} parsing={parsing} onFile={onFile} />}
      {step === 2 && <StepCheck file={file} parsed={parsed} onBack={reset} onNext={() => setStep(3)} />}
      {step === 3 && (
        <StepInfo
          onBack={() => setStep(2)}
          onLoginRequired={() => setAskLogin(true)}
          onSubmit={async (info) => {
            const { courseId } = await createCourse({ file, ...info })
            showToast('코스가 등록됐어요')
            navigate(`/courses/${courseId}`)
          }}
        />
      )}
      <LoginRequiredModal open={askLogin} onClose={() => setAskLogin(false)} />
    </main>
  )
}

// API 오류 코드 → 화면 문구 (C3-1E)
function errorText(err) {
  if (err.code === 'INVALID_FORMAT') return 'GPX 파일이 아니에요'
  if (err.code === 'TOO_LARGE') return '5MB를 넘어요'
  if (err.code === 'TOO_SHORT') return '경로가 너무 짧아요 (300m · 위치 점 20개 이상)'
  if (err.code === 'LOGIN_REQUIRED') return '로그인이 필요해요'
  return err.message
}

function Steps({ current }) {
  return (
    <div className="c3-steps" aria-label={`코스 등록 ${current} / 3단계`}>
      {STEPS.map((label, i) => {
        const n = i + 1
        const on = n <= current ? ' is-on' : ''
        return (
          <span key={label} className="row ai-c g8">
            {n > 1 && <span className={`c3-step-line${on}`} />}
            <span className={`c3-step-num${on}`}>{n}</span>
            <span className={`c3-step-label${on}`}>{label}</span>
          </span>
        )
      })}
    </div>
  )
}

// C3-1 파일 올리기 · C3-1E 오류
function StepFile({ error, parsing, onFile }) {
  const inputRef = useRef(null)
  const [over, setOver] = useState(false)

  return (
    <div className="panel c3-card">
      <h1 className="t-h24 c-strong nowrap">워치 · 폰에서 기록한 경로를 올려 주세요</h1>
      <div
        className={`c3-drop${over ? ' is-over' : ''}${error ? ' is-error' : ''}`}
        onDragOver={(e) => {
          e.preventDefault()
          setOver(true)
        }}
        onDragLeave={() => setOver(false)}
        onDrop={(e) => {
          e.preventDefault()
          setOver(false)
          onFile(e.dataTransfer.files[0])
        }}
      >
        <p className="c3-drop-icon">⤒</p>
        <p className={`t-b16m nowrap ${error ? 'c-danger' : 'c-strong'}`}>
          {parsing
            ? 'GPX 파일을 읽는 중…'
            : error
              ? `${error.fileName} — ${error.message}`
              : 'GPX 파일을 여기로 끌어다 놓으세요'}
        </p>
        <Button variant="secondary" disabled={parsing} onClick={() => inputRef.current.click()}>
          파일 선택
        </Button>
        <p className="t-c12 c-muted">.gpx · 최대 5MB</p>
        <input
          ref={inputRef}
          type="file"
          accept=".gpx,application/gpx+xml"
          hidden
          onChange={(e) => {
            onFile(e.target.files[0])
            e.target.value = '' // 같은 파일을 다시 골라도 동작하게
          }}
        />
      </div>

      {error && (
        <div className="c3-box c3-box-red">
          <p className="t-b14m">업로드할 수 없는 경우</p>
          <p>· 형식 오류: .gpx가 아닌 파일 (.fit, .tcx 등) → 워치 앱에서 GPX로 다시 내보내기</p>
          <p>· 용량 초과: 5MB 초과 → 긴 기록은 구간을 나눠 내보내기</p>
          <p>· 경로가 너무 짧음: 300m 미만 또는 위치 점 20개 미만</p>
        </div>
      )}
      <div className="c3-box c3-box-blue">
        <p className="t-b14m">GPX 파일 받는 법</p>
        <p>· 갤럭시 워치 · 삼성헬스: 운동 기록 → 더보기 → GPX 파일로 내보내기</p>
        <p>· 애플워치: ‘GPX Export’ 같은 앱으로 건강 앱의 운동 경로 내보내기</p>
        <p>· 가민: Garmin Connect 웹 → 활동 → 더보기 → Export to GPX</p>
      </div>
    </div>
  )
}

// C3-2 경로 확인 (C3-1 분석 결과만 씀, API 추가 호출 없음)
function StepCheck({ file, parsed, onBack, onNext }) {
  return (
    <div className="panel c3-card">
      <h1 className="t-h24 c-strong nowrap">경로를 확인해 주세요</h1>
      <div className="row ai-c g8">
        <Chip tone="green">✓ {file.name}</Chip>
        <p className="t-b14 c-muted nowrap">
          업로드 완료 · 포인트 {parsed.pointCount.toLocaleString()}개 · {AREA_LABELS[parsed.area]} 근처
        </p>
      </div>
      <KakaoMap className="c3-map" path={parsed.path} showEnds fit />
      <div className="c3-stats">
        <div className="c3-stat">
          <p className="t-h20 c-green">{formatKm(parsed.distanceKm)}</p>
          <p className="t-c12 c-muted">거리 (자동 계산)</p>
        </div>
        <div className="c3-stat">
          <p className="t-h20 c-green">{parsed.durationMin}분</p>
          <p className="t-c12 c-muted">예상 시간 (시속 4km)</p>
        </div>
      </div>
      <div className="c3-buttons">
        <Button variant="secondary" onClick={onBack}>
          다른 파일 선택
        </Button>
        <Button onClick={onNext}>다음</Button>
      </div>
    </div>
  )
}

// C3-3 코스 정보 입력
function StepInfo({ onBack, onSubmit, onLoginRequired }) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [level, setLevel] = useState('')
  const [facilities, setFacilities] = useState([])
  const [visibility, setVisibility] = useState('PUBLIC')
  const [touched, setTouched] = useState(false)
  const [saving, setSaving] = useState(false)

  const nameError = useMemo(() => {
    const n = name.trim()
    if (!n) return '코스 이름을 입력해 주세요.'
    if (n.length > 20) return '코스 이름은 20자까지예요.'
    return ''
  }, [name])
  const levelError = level ? '' : '난이도를 골라 주세요.'

  async function submit() {
    setTouched(true)
    if (nameError || levelError) return
    setSaving(true)
    try {
      await onSubmit({ name: name.trim(), description: description.trim(), level, facilities, visibility })
    } catch (err) {
      setSaving(false)
      if (err.status === 401) {
        onLoginRequired() // 입력한 값은 그대로 두고 로그인 모달만
        return
      }
      showToast(err.message, 'error')
    }
  }

  const toggleFacility = (f) =>
    setFacilities((list) => (list.includes(f) ? list.filter((x) => x !== f) : [...list, f]))

  return (
    <div className="panel c3-card">
      <h1 className="t-h24 c-strong nowrap">코스 정보를 입력해 주세요</h1>
      <Input
        label="코스 이름"
        value={name}
        maxLength={20}
        onChange={(e) => setName(e.target.value)}
        placeholder="예: 옥구공원 순환 코스"
        helper="최대 20자"
        error={touched && nameError}
      />
      <Input
        label="한 줄 소개"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        placeholder="예: 해질녘 바다가 보이는 평탄한 길"
      />
      <Group title="난이도" error={touched && levelError}>
        {Object.entries(LEVEL_LABELS).map(([code, label]) => (
          <Chip key={code} tone="neutral" selected={level === code} onClick={() => setLevel(code)}>
            {label}
          </Chip>
        ))}
      </Group>
      <Group title="편의시설 (있는 것만 선택)">
        {FACILITIES.map((f) => (
          <Chip key={f} tone="neutral" selected={facilities.includes(f)} onClick={() => toggleFacility(f)}>
            {FACILITY_LABELS[f]}
          </Chip>
        ))}
      </Group>
      <Group title="공개 범위">
        {Object.entries(VISIBILITY).map(([code, label]) => (
          <Chip key={code} tone="neutral" selected={visibility === code} onClick={() => setVisibility(code)}>
            {label}
          </Chip>
        ))}
      </Group>
      <div className="c3-buttons">
        <Button variant="secondary" onClick={onBack} disabled={saving}>
          이전
        </Button>
        <Button onClick={submit} disabled={saving}>
          {saving ? '등록하는 중…' : '코스 등록하기'}
        </Button>
      </div>
    </div>
  )
}

function Group({ title, error, children }) {
  return (
    <div className="col g8 fill">
      <p className="t-b14m c-strong">{title}</p>
      <div className="row fwrap g6">{children}</div>
      {error && <p className="t-c12 c-danger">{error}</p>}
    </div>
  )
}
