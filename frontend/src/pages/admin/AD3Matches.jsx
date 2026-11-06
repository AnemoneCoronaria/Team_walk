import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/Button.jsx'
import Chip from '../../components/Chip.jsx'
import { changeMatchHidden, getAdminMatches } from '../../api/adminApi.js'
import { MATCH_STATUS_LABELS, targetLabels } from '../../constants/labels.js'
import { formatMeetAt } from '../../utils/format.js'
import { showToast } from '../../utils/toast.js'
import { useAsync } from '../../utils/useAsync.js'
import { AdminListState, AdminPager, AdminSearch, AdminTabs } from './AdminParts.jsx'

const EMPTY_TEXT = { OPEN: '모집 중인 글이 없어요', CLOSED: '마감된 글이 없어요', HIDDEN: '숨긴 글이 없어요' }

// 상태 칸: 숨긴 글은 "숨김", 그 외는 동행 상태 (모집 중 · 정원 참 · 마감 · 모집 취소 · 끝남)
function StatusChip({ match }) {
  if (match.hidden) return <Chip tone="neutral">숨김</Chip>
  return <Chip tone={match.status === 'OPEN' ? 'green' : 'neutral'}>{MATCH_STATUS_LABELS[match.status]}</Chip>
}

// AD3 모집 글 관리 (/admin/matches)
// API: GET /api/admin/matches?q=&status= · PUT /api/admin/matches/{id}/visibility { hidden }
// 탭: 모집 중(정원 찬 글 포함) · 마감(마감 · 취소 · 끝남) · 숨김. 숨긴 글은 상세가 404 라 제목 링크가 없음
export default function AD3Matches() {
  const [status, setStatus] = useState('OPEN')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(1)
  const [version, setVersion] = useState(0)
  const { data, error, loading } = useAsync(
    () => getAdminMatches({ q, status, page }),
    `ad3-${status}-${q}-${page}-${version}`,
  )
  const [busyId, setBusyId] = useState(null)

  const counts = data?.counts
  const tabs = [
    { value: 'OPEN', label: `모집 중${counts ? ` ${counts.open}` : ''}` },
    { value: 'CLOSED', label: `마감${counts ? ` ${counts.closed}` : ''}` },
    { value: 'HIDDEN', label: `숨김${counts ? ` ${counts.hidden}` : ''}` },
  ]

  async function onToggle(match) {
    setBusyId(match.matchId)
    try {
      await changeMatchHidden(match.matchId, !match.hidden)
      showToast(match.hidden ? `복구했어요: ${match.title}` : `숨겼어요: ${match.title}`)
      setVersion((v) => v + 1)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="col g16 fill">
      <div className="admin-toolbar">
        <AdminTabs
          tabs={tabs}
          value={status}
          onChange={(value) => {
            setStatus(value)
            setPage(1)
          }}
        />
        <AdminSearch
          placeholder="제목 · 모집자 · 코스 검색"
          onSearch={(text) => {
            setQ(text)
            setPage(1)
          }}
        />
      </div>
      {data && <p className="t-b14 c-body">{q ? `'${q}' 검색 결과 ` : ''}{data.total}개</p>}

      <AdminListState loading={loading} error={error} empty={data?.items.length === 0} emptyText={EMPTY_TEXT[status]} />
      {data?.items.length > 0 && (
        <div className="panel admin-table-box">
          <table className="admin-table">
            <thead>
              <tr>
                <th>제목</th>
                <th>모집자</th>
                <th>일시</th>
                <th>대상</th>
                <th className="num">인원</th>
                <th>상태</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.items.map((m) => (
                <tr key={m.matchId} className={m.hidden ? 'is-hidden' : ''}>
                  <td>
                    {m.hidden ? (
                      <span className="t-b14m c-strong">{m.title}</span>
                    ) : (
                      <Link className="t-b14m c-strong" to={`/matches/${m.matchId}`}>
                        {m.title}
                      </Link>
                    )}
                  </td>
                  <td className="nowrap">{m.host.nickname}</td>
                  <td className="nowrap">{formatMeetAt(m.meetAt)}</td>
                  <td className="nowrap">{targetLabels(m.targets).join(' · ')}</td>
                  <td className="num">
                    {m.current}/{m.capacity}
                  </td>
                  <td>
                    <StatusChip match={m} />
                  </td>
                  <td className="actions">
                    <Button
                      variant={m.hidden ? 'secondary' : 'danger'}
                      disabled={busyId === m.matchId}
                      onClick={() => onToggle(m)}
                    >
                      {m.hidden ? '복구' : '숨김'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <AdminPager data={data} onPage={setPage} />}
    </section>
  )
}
