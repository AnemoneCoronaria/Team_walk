import { useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Button from './Button.jsx'
import Chip from './Chip.jsx'
import LoginRequiredModal from './LoginRequiredModal.jsx'
import Modal from './Modal.jsx'
import { verifyLandmark } from '../api/landmarkApi.js'
import { getMyStamps, updateConsents } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { BADGES, LANDMARK_ICONS } from '../constants/labels.js'
import { showToast } from '../utils/toast.js'
import './LandmarkVerify.css'

const MAX_GPX_MB = 5

// 랜드마크 GPX 인증 버튼 + 그 뒤에 뜨는 창 (R1-1 위치 정보 동의 · R2-1 인증 성공 · R1-2 인증 실패)
//   <LandmarkVerify target="옥구공원" onVerified={() => setVersion((v) => v + 1)} />
//   target: 안내 문구에 쓸 랜드마크 이름 (없으면 "랜드마크"), onVerified: 스탬프를 새로 받았을 때
// 흐름: 버튼 → GPX 파일 고르기 → (위치 정보 동의를 안 했으면 R1-1) → POST /api/landmarks/verify → R2-1 또는 R1-2
// 로그인 안 했으면 버튼이 G1 로그인 필요 창을 띄움. 사진으로 인증 · 관리자 검수(AD6)는 2순위라 없음
export default function LandmarkVerify({ target, label = '📍 업로드한 GPX로 자동 인증', variant = 'primary', onVerified }) {
  const { user, refresh } = useAuth()
  const fileRef = useRef(null)
  const [step, setStep] = useState(null) // login · consent · success · fail
  const [pending, setPending] = useState(null) // 동의 창이 떠 있는 동안 기다리는 GPX 파일
  const [result, setResult] = useState(null) // verify 응답 + 인증 뒤 스탬프 수
  const [busy, setBusy] = useState(false)
  const place = target ? `‘${target}’` : '랜드마크'

  function onClick() {
    if (!user) setStep('login')
    else fileRef.current.click()
  }

  function onPick(e) {
    const file = e.target.files[0]
    e.target.value = '' // 같은 파일을 다시 고를 수 있게
    if (!file) return
    if (!/\.gpx$/i.test(file.name)) return showToast('GPX(.gpx) 파일만 올릴 수 있어요.', 'error')
    if (file.size > MAX_GPX_MB * 1024 * 1024) return showToast(`GPX 파일은 ${MAX_GPX_MB}MB 이하여야 해요.`, 'error')
    setStep(null)
    if (!user.agreeLocation) {
      setPending(file)
      setStep('consent')
      return
    }
    verify(file)
  }

  async function verify(file) {
    setBusy(true)
    try {
      const res = await verifyLandmark(file)
      if (res.result === 'SUCCESS') {
        const stamps = await getMyStamps().catch(() => null) // 성공 창의 "3 / 8"
        setResult({ ...res, stampCount: stamps?.stampCount, totalLandmarks: stamps?.totalLandmarks })
        setStep('success')
        onVerified?.(res)
      } else {
        setResult(res)
        setStep('fail')
      }
    } catch (err) {
      if (err.code === 'CONSENT_REQUIRED') {
        setPending(file)
        setStep('consent')
      } else {
        showToast(err.message, 'error')
      }
    } finally {
      setBusy(false)
    }
  }

  async function onAgree() {
    setBusy(true)
    try {
      await updateConsents(true)
      await refresh() // user.agreeLocation 이 true 로
    } catch (err) {
      setBusy(false)
      return showToast(err.message, 'error')
    }
    const file = pending
    setPending(null)
    setStep(null)
    verify(file)
  }

  return (
    <>
      <Button variant={variant} onClick={onClick} disabled={busy}>
        {busy ? '경로 확인 중…' : label}
      </Button>
      <input ref={fileRef} type="file" accept=".gpx,application/gpx+xml" hidden onChange={onPick} />

      <LoginRequiredModal open={step === 'login'} onClose={() => setStep(null)} />
      {step === 'consent' && <ConsentModal place={place} busy={busy} onAgree={onAgree} onClose={() => setStep(null)} />}
      {step === 'success' && <SuccessModal result={result} onClose={() => setStep(null)} />}
      {step === 'fail' && (
        <FailModal
          place={place}
          result={result}
          onRetry={() => fileRef.current.click()}
          onClose={() => setStep(null)}
        />
      )}
    </>
  )
}

// R1-1 위치 정보 이용 동의 (처음 인증할 때만)
function ConsentModal({ place, busy, onAgree, onClose }) {
  const [agree, setAgree] = useState(false)
  return (
    <Modal open onClose={onClose} width={500} className="modal-left">
      <h2 className="t-h20 c-strong">🗺 GPX의 위치 정보를 사용할게요</h2>
      <p className="t-b14 c-body">업로드한 GPX 경로로 {place} 방문 여부를 확인해요.</p>
      <ul className="modal-notes">
        <li>확인 항목: GPX 경로 좌표</li>
        <li>이용 목적: 랜드마크 방문 인증</li>
        <li>보관: 인증 결과(성공 · 실패)만 저장해요</li>
      </ul>
      <label className="verify-check">
        <input type="checkbox" checked={agree} onChange={(e) => setAgree(e.target.checked)} />
        위치 기반 서비스 이용약관에 동의합니다 (필수)
      </label>
      <div className="row g12 fill">
        <Button variant="secondary" className="grow" onClick={onClose}>
          취소
        </Button>
        <Button className="grow" onClick={onAgree} disabled={!agree || busy}>
          동의하고 인증하기
        </Button>
      </div>
    </Modal>
  )
}

// R2-1 인증 성공 — 새 스탬프 · 배지, 진행률
function SuccessModal({ result, onClose }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [first, ...rest] = result.newStamps
  const badges = result.newBadges.map((b) => BADGES.find((x) => x.code === b.code) ?? { ...b, icon: '🏅' })

  function goStamps() {
    onClose()
    if (location.pathname !== '/stamps') navigate('/stamps')
  }

  return (
    <Modal open onClose={onClose}>
      <p className="verify-icon">{LANDMARK_ICONS[first.landmarkId] ?? '📍'}</p>
      <h2 className="t-h24 c-strong">
        {first.name}
        {rest.length > 0 ? ` 외 ${rest.length}곳` : ''} 인증 완료!
      </h2>
      {result.stampCount != null && (
        <p className="t-b16 c-body">
          {result.stampCount} / {result.totalLandmarks} · 스탬프가 추가됐어요
        </p>
      )}
      {badges.length > 0 && (
        <div className="row fwrap jc-c g8">
          {badges.map((b) => (
            <Chip key={b.code}>
              {b.icon} {b.name} 배지를 받았어요
            </Chip>
          ))}
        </div>
      )}
      <Button block onClick={goStamps}>
        스탬프 보러 가기
      </Button>
    </Modal>
  )
}

// R1-2 인증 실패 — 근처를 안 지났거나(NOT_NEAR), 지나는 곳을 이미 다 받았거나(ALREADY_ALL)
function FailModal({ place, result, onRetry, onClose }) {
  const already = result.reason === 'ALREADY_ALL'
  return (
    <Modal open onClose={onClose} width={500} className="modal-left">
      <h2 className="t-h20 c-strong">{already ? '이미 받은 스탬프예요' : '인증하지 못했어요'}</h2>
      {already ? (
        <p className="t-b14 c-body">
          이 경로가 지나는 {result.alreadyStamped.map((l) => `‘${l.name}’`).join(', ')}은(는) 이미 인증했어요. 다른 날 걸은
          GPX를 올려 보세요.
        </p>
      ) : (
        <>
          <p className="t-b14 c-body">
            올린 GPX 경로가 {place} 반경 300m 안을 지나지 않았어요. 다른 GPX를 올려 주세요.
          </p>
          <ul className="modal-notes">
            <li>경로가 랜드마크를 지나는 날의 GPX인지 확인해 주세요</li>
            <li>운동 앱에서 내보낸 GPX 원본을 그대로 올려 주세요</li>
          </ul>
        </>
      )}
      <div className="row g12 fill">
        <Button variant="secondary" className="grow" onClick={onClose}>
          닫기
        </Button>
        <Button
          className="grow"
          onClick={() => {
            onClose()
            onRetry()
          }}
        >
          다른 GPX 올리기
        </Button>
      </div>
    </Modal>
  )
}
