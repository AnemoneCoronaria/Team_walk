import { useState } from 'react'
import Button from '../../components/Button.jsx'
import Chip from '../../components/Chip.jsx'
import Modal from '../../components/Modal.jsx'
import { changeUserStatus, getAdminUsers, issueTempPassword } from '../../api/adminApi.js'
import { AGE_LABELS, LANGUAGE_LABELS } from '../../constants/labels.js'
import { formatDate, formatMonthDay } from '../../utils/format.js'
import { showToast } from '../../utils/toast.js'
import { useAsync } from '../../utils/useAsync.js'
import { AdminListState, AdminPager, AdminSearch, AdminTabs } from './AdminParts.jsx'

const TABS = [
  { value: '', label: '전체' },
  { value: 'SUSPENDED', label: '이용 정지' },
  { value: 'WITHDRAWN', label: '탈퇴' },
]

// 상태 칸 (API 명세 AD2): 정지면 "7일 정지", 탈퇴면 "탈퇴", 경고가 있으면 "경고 N회", 그 외 "정상"
function StatusChip({ user }) {
  if (user.status === 'SUSPENDED') {
    return <Chip tone="danger">7일 정지{user.suspendedUntil && ` · ~${formatMonthDay(user.suspendedUntil)}`}</Chip>
  }
  if (user.status === 'WITHDRAWN') return <Chip tone="neutral">탈퇴</Chip>
  if (user.warningCount > 0) return <Chip tone="blue">경고 {user.warningCount}회</Chip>
  return <Chip>정상</Chip>
}

// AD2 회원 관리 (/admin/users)
// API: GET /api/admin/users?q=&status= · PUT /api/admin/users/{id}/status { status: SUSPENDED | ACTIVE }
//      POST /api/admin/users/{id}/temp-password (회원이 비밀번호를 잊었을 때, A1 안내 참고)
// 경고는 AD1 신고 처리에서만 줌. 탈퇴 회원 · 관리자 계정은 바꿀 수 없음
export default function AD2Users() {
  const [status, setStatus] = useState('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(1)
  const [version, setVersion] = useState(0)
  const { data, error, loading } = useAsync(() => getAdminUsers({ q, status, page }), `ad2-${status}-${q}-${page}-${version}`)
  const [suspending, setSuspending] = useState(null) // 정지 확인 창을 연 회원
  const [resetting, setResetting] = useState(null) // 임시 비밀번호 확인 창을 연 회원
  const [issued, setIssued] = useState(null) // 발급 결과 { nickname, loginId, temporaryPassword }
  const [busy, setBusy] = useState(false)

  async function onIssue() {
    setBusy(true)
    try {
      const { temporaryPassword } = await issueTempPassword(resetting.userId)
      setIssued({ nickname: resetting.nickname, loginId: resetting.loginId, temporaryPassword })
      setResetting(null)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  async function onCopy() {
    try {
      await navigator.clipboard.writeText(issued.temporaryPassword)
      showToast('복사했어요')
    } catch {
      showToast('복사하지 못했어요. 직접 적어 주세요', 'error')
    }
  }

  async function onChange(user, next) {
    setBusy(true)
    try {
      await changeUserStatus(user.userId, next)
      showToast(next === 'SUSPENDED' ? `${user.nickname}님을 7일 정지했어요` : `${user.nickname}님의 정지를 풀었어요`)
      setSuspending(null)
      setVersion((v) => v + 1)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="col g16 fill">
      <div className="admin-toolbar">
        <AdminTabs
          tabs={TABS}
          value={status}
          onChange={(value) => {
            setStatus(value)
            setPage(1)
          }}
        />
        <AdminSearch
          placeholder="닉네임 · 아이디 검색"
          onSearch={(text) => {
            setQ(text)
            setPage(1)
          }}
        />
      </div>
      {data && <p className="t-b14 c-body">{q ? `'${q}' 검색 결과 ` : ''}{data.total}명</p>}

      <AdminListState loading={loading} error={error} empty={data?.items.length === 0} emptyText="해당하는 회원이 없어요" />
      {data?.items.length > 0 && (
        <div className="panel admin-table-box">
          <table className="admin-table">
            <thead>
              <tr>
                <th>회원</th>
                <th>연령대</th>
                <th>언어</th>
                <th className="num">동행</th>
                <th className="num">신고</th>
                <th>상태</th>
                <th>가입일</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.items.map((u) => (
                <tr key={u.userId} className={u.status === 'WITHDRAWN' ? 'is-hidden' : ''}>
                  <td>
                    <div className="col">
                      <span className="t-b14m c-strong">
                        {u.nickname}
                        {u.role === 'ADMIN' && <span className="t-c12 c-green"> · 관리자</span>}
                      </span>
                      <span className="t-c12 c-muted">{u.loginId}</span>
                    </div>
                  </td>
                  <td className="nowrap">{AGE_LABELS[u.ageGroup]}</td>
                  <td>
                    <div className="row fwrap g4">
                      {u.languages.map((code) => (
                        <Chip key={code} tone="blue">
                          {LANGUAGE_LABELS[code]}
                        </Chip>
                      ))}
                    </div>
                  </td>
                  <td className="num">{u.matchCount}</td>
                  <td className={`num${u.reportCount > 0 ? ' c-danger' : ''}`}>{u.reportCount}</td>
                  <td>
                    <StatusChip user={u} />
                  </td>
                  <td className="nowrap">{formatDate(u.createdAt)}</td>
                  <td className="actions">
                    {u.role === 'ADMIN' || u.status === 'WITHDRAWN' ? (
                      <span className="t-c12 c-muted">-</span>
                    ) : (
                      <div className="row g8 admin-row-actions">
                        <Button variant="ghost" disabled={busy} onClick={() => setResetting(u)}>
                          임시 비밀번호
                        </Button>
                        {u.status === 'SUSPENDED' ? (
                          <Button variant="secondary" disabled={busy} onClick={() => onChange(u, 'ACTIVE')}>
                            정지 풀기
                          </Button>
                        ) : (
                          <Button variant="danger" disabled={busy} onClick={() => setSuspending(u)}>
                            7일 정지
                          </Button>
                        )}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <AdminPager data={data} onPage={setPage} />}

      <Modal open={Boolean(suspending)} onClose={busy ? undefined : () => setSuspending(null)}>
        {suspending && (
          <>
            <h2 className="t-h20 c-strong">{suspending.nickname}님을 7일 정지할까요?</h2>
            <p className="t-b14 c-body">정지 기간에는 로그인할 수 없어요. 7일이 지나면 자동으로 풀려요.</p>
            <div className="row g12 fill">
              <Button variant="secondary" className="grow" disabled={busy} onClick={() => setSuspending(null)}>
                취소
              </Button>
              <Button variant="danger" className="grow" disabled={busy} onClick={() => onChange(suspending, 'SUSPENDED')}>
                7일 정지
              </Button>
            </div>
          </>
        )}
      </Modal>

      <Modal open={Boolean(resetting)} onClose={busy ? undefined : () => setResetting(null)}>
        {resetting && (
          <>
            <h2 className="t-h20 c-strong">{resetting.nickname}님의 임시 비밀번호를 발급할까요?</h2>
            <p className="t-b14 c-body">지금 쓰는 비밀번호는 바로 쓸 수 없게 돼요. 본인이 요청한 것이 맞는지 먼저 확인해 주세요.</p>
            <div className="row g12 fill">
              <Button variant="secondary" className="grow" disabled={busy} onClick={() => setResetting(null)}>
                취소
              </Button>
              <Button className="grow" disabled={busy} onClick={onIssue}>
                발급하기
              </Button>
            </div>
          </>
        )}
      </Modal>

      <Modal open={Boolean(issued)} onClose={() => setIssued(null)}>
        {issued && (
          <>
            <h2 className="t-h20 c-strong">임시 비밀번호를 발급했어요</h2>
            <p className="t-b14 c-body">
              {issued.nickname}({issued.loginId})님에게 전달해 주세요. <b>이 창을 닫으면 다시 볼 수 없어요.</b>
            </p>
            <code className="admin-temp-password">{issued.temporaryPassword}</code>
            <p className="t-c12 c-muted">로그인한 뒤 계정 설정에서 비밀번호를 바꾸도록 안내해 주세요.</p>
            <div className="row g12 fill">
              <Button variant="secondary" className="grow" onClick={onCopy}>
                복사
              </Button>
              <Button className="grow" onClick={() => setIssued(null)}>
                닫기
              </Button>
            </div>
          </>
        )}
      </Modal>
    </section>
  )
}
