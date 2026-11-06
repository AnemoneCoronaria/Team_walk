import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/Button.jsx'
import Chip from '../../components/Chip.jsx'
import Modal from '../../components/Modal.jsx'
import { getAdminReports, processReport } from '../../api/adminApi.js'
import { formatDate } from '../../utils/format.js'
import { showToast } from '../../utils/toast.js'
import { useAsync } from '../../utils/useAsync.js'
import { AdminListState, AdminPager, AdminTabs } from './AdminParts.jsx'
import { REPORT_ACTION_LABELS, REPORT_REASON_LABELS, REPORT_TARGET_LABELS } from './adminLabels.js'

const TABS = [
  { value: 'PENDING', label: '대기' },
  { value: 'DONE', label: '처리 완료' },
]

const ACTION_TONES = { WARN: 'blue', SUSPEND: 'danger', HIDE: 'danger', REJECT: 'neutral' }

// 대상 화면으로 가는 주소 (사용자 · 후기는 따로 볼 화면이 없음)
function targetLink(r) {
  if (r.targetType === 'MATCH') return `/matches/${r.targetId}`
  if (r.targetType === 'COURSE') return `/courses/${r.targetId}`
  return null
}

// AD1 신고 처리 (/admin/reports)
// API: GET /api/admin/reports?status= · PUT /api/admin/reports/{id} { action, memo }
// "보류"는 API 를 부르지 않고 창만 닫음 → 신고가 대기로 남음 (API 명세)
export default function AD1Reports() {
  const [status, setStatus] = useState('PENDING')
  const [page, setPage] = useState(1)
  const [version, setVersion] = useState(0) // 처리한 뒤 목록을 다시 불러올 때 올림
  const { data, error, loading } = useAsync(() => getAdminReports({ status, page }), `ad1-${status}-${page}-${version}`)
  const [target, setTarget] = useState(null) // 처리 창을 연 신고

  function onTab(value) {
    setStatus(value)
    setPage(1)
  }

  return (
    <section className="col g16 fill">
      <div className="admin-toolbar">
        <AdminTabs tabs={TABS} value={status} onChange={onTab} />
        {data && <p className="t-b14 c-body">{data.total}건</p>}
      </div>

      <AdminListState
        loading={loading}
        error={error}
        empty={data?.items.length === 0}
        emptyText={status === 'PENDING' ? '처리할 신고가 없어요' : '처리한 신고가 없어요'}
      />
      {data?.items.length > 0 && (
        <div className="panel admin-table-box">
          <table className="admin-table">
            <thead>
              <tr>
                <th>접수일</th>
                <th>대상</th>
                <th>사유</th>
                <th>내용</th>
                <th>신고자</th>
                <th className="num">누적 신고</th>
                {status === 'PENDING' ? <th /> : <th>처리 결과</th>}
              </tr>
            </thead>
            <tbody>
              {data.items.map((r) => (
                <tr key={r.reportId}>
                  <td className="nowrap">{formatDate(r.createdAt)}</td>
                  <td>
                    <div className="row ai-c g8">
                      <Chip tone="neutral">{REPORT_TARGET_LABELS[r.targetType]}</Chip>
                      {targetLink(r) ? (
                        <Link className="t-b14m c-strong" to={targetLink(r)}>
                          {r.targetLabel ?? '(지워진 대상)'}
                        </Link>
                      ) : (
                        <span className="t-b14m">{r.targetLabel ?? '(지워진 대상)'}</span>
                      )}
                    </div>
                  </td>
                  <td className="nowrap">{REPORT_REASON_LABELS[r.reason]}</td>
                  <td className="admin-detail">{r.detail ?? '-'}</td>
                  <td className="nowrap">{r.reporter.nickname}</td>
                  <td className={`num${r.targetReportCount > 1 ? ' c-danger' : ''}`}>{r.targetReportCount}</td>
                  {status === 'PENDING' ? (
                    <td className="actions">
                      <Button onClick={() => setTarget(r)}>처리</Button>
                    </td>
                  ) : (
                    <td>
                      <div className="col g4">
                        <Chip tone={ACTION_TONES[r.action]}>{REPORT_ACTION_LABELS[r.action]}</Chip>
                        {r.adminMemo && <span className="t-c12 c-muted">{r.adminMemo}</span>}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <AdminPager data={data} onPage={setPage} />}

      {target && (
        <ProcessModal
          report={target}
          onClose={() => setTarget(null)}
          onDone={() => {
            setTarget(null)
            setVersion((v) => v + 1)
          }}
        />
      )}
    </section>
  )
}

// 신고 처리 창 — 경고 · 7일 정지 · 게시물 숨김 · 문제 없음 · 보류
function ProcessModal({ report, onClose, onDone }) {
  const [memo, setMemo] = useState('')
  const [busy, setBusy] = useState(false)
  const isUser = report.targetType === 'USER'

  async function onAction(action) {
    setBusy(true)
    try {
      await processReport(report.reportId, { action, memo })
      showToast(`처리했어요: ${REPORT_ACTION_LABELS[action]}`)
      onDone()
    } catch (err) {
      showToast(err.message, 'error')
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={busy ? undefined : onClose} width={520} className="admin-modal">
      <h2 className="t-h20 c-strong">신고 처리</h2>
      <dl>
        <dt>대상</dt>
        <dd>
          {REPORT_TARGET_LABELS[report.targetType]} · {report.targetLabel ?? '(지워진 대상)'}
        </dd>
        <dt>사유</dt>
        <dd>{REPORT_REASON_LABELS[report.reason]}</dd>
        <dt>내용</dt>
        <dd>{report.detail ?? '-'}</dd>
        <dt>신고자</dt>
        <dd>
          {report.reporter.nickname} · 이 대상 누적 신고 {report.targetReportCount}건
        </dd>
      </dl>
      <textarea
        className="admin-memo"
        value={memo}
        onChange={(e) => setMemo(e.target.value)}
        maxLength={500}
        placeholder="관리자 메모 (선택, 500자까지)"
        aria-label="관리자 메모"
      />
      <p className="t-c12 c-muted">
        경고 · 정지는 {isUser ? '이 회원' : '글을 쓴 회원'}에게, 숨김은 이 게시물에 적용돼요
      </p>
      <div className="admin-action-grid">
        <Button variant="secondary" disabled={busy} onClick={() => onAction('WARN')}>
          경고
        </Button>
        <Button variant="danger" disabled={busy} onClick={() => onAction('SUSPEND')}>
          7일 이용 정지
        </Button>
        <Button variant="danger" disabled={busy || isUser} onClick={() => onAction('HIDE')}>
          게시물 숨김
        </Button>
        <Button variant="secondary" disabled={busy} onClick={() => onAction('REJECT')}>
          문제 없음
        </Button>
      </div>
      <Button variant="ghost" block disabled={busy} onClick={onClose}>
        보류 (대기로 남기기)
      </Button>
    </Modal>
  )
}
