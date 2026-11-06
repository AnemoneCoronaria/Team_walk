import { useState } from 'react'
import Button from './Button.jsx'
import Chip from './Chip.jsx'
import Input from './Input.jsx'
import Modal from './Modal.jsx'
import { createReport } from '../api/reportApi.js'
import { COURSE_REPORT_REASON_LABELS, REPORT_REASON_LABELS } from '../constants/labels.js'
import { showToast } from '../utils/toast.js'

const MAX_DETAIL = 300

// W6 신고하기 · C2-1 코스 신고 창 — 사유는 대상에 따라 다름 (db/schema.sql reports 약속)
//   const [reporting, setReporting] = useState(false)
//   <ReportModal open={reporting} onClose={() => setReporting(false)}
//                targetType="MATCH" targetId={match.matchId} targetLabel="하늘 · 11/11 옥구공원 동행" />
//   targetType: MATCH(동행 글) · USER(사용자) · REVIEW(후기) → W6 사유, COURSE → C2-1 사유
// 로그인한 사람만 신고할 수 있으니, 로그인 안 했으면 화면에서 LoginRequiredModal 을 먼저 띄워 주세요.
// API: POST /api/reports (정란). "이 사용자 차단하기"는 2순위라 없음
export default function ReportModal({ open, ...props }) {
  if (!open) return null
  return <ReportForm {...props} /> // 열 때마다 새로 만들어서 고른 사유 · 내용이 남지 않게
}

function ReportForm({ onClose, targetType, targetId, targetLabel }) {
  const isCourse = targetType === 'COURSE'
  const reasons = isCourse ? COURSE_REPORT_REASON_LABELS : REPORT_REASON_LABELS
  const [reason, setReason] = useState('')
  const [detail, setDetail] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    if (!reason || busy) return
    setBusy(true)
    try {
      await createReport({ targetType, targetId, reason, detail: detail.trim() })
      showToast('신고가 접수됐어요. 관리자가 확인할게요')
      onClose()
    } catch (err) {
      showToast(err.message, 'error')
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} width={520} className="modal-left">
      <form className="col g16 fill" onSubmit={onSubmit} noValidate>
        <h2 className="t-h20 c-strong">{isCourse ? '이 코스를 신고할까요?' : '신고하기'}</h2>
        <p className="t-b14 c-body">{isCourse ? targetLabel : `신고 대상: ${targetLabel}`}</p>

        <div className="col g8 fill">
          <p className="t-b14m c-strong">사유</p>
          <div className="row fwrap g8">
            {Object.entries(reasons).map(([code, text]) => (
              <Chip key={code} tone="neutral" selected={reason === code} onClick={() => setReason(code)}>
                {text}
              </Chip>
            ))}
          </div>
        </div>

        <Input
          label="자세한 내용 (선택)"
          value={detail}
          onChange={(e) => setDetail(e.target.value)}
          maxLength={MAX_DETAIL}
          placeholder={isCourse ? '어느 구간이 어떤 문제인지 적어 주세요' : '어떤 일이 있었는지 적어 주세요'}
          helper={`${detail.length} / ${MAX_DETAIL}자`}
        />

        {!isCourse && (
          <p className="t-c12 c-muted">
            신고 내용은 관리자만 확인하며, 신고자는 공개되지 않아요. 위급한 상황이면 112에 먼저 연락해 주세요.
          </p>
        )}

        <div className="row g12 fill">
          <Button variant="secondary" className="grow" onClick={onClose}>
            취소
          </Button>
          <Button variant="danger" type="submit" className="grow" disabled={!reason || busy}>
            {busy ? '접수 중…' : '신고하기'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
