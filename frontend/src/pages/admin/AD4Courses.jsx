import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/Button.jsx'
import Chip from '../../components/Chip.jsx'
import { changeCourseHidden, getAdminCourses } from '../../api/adminApi.js'
import { LEVEL_LABELS, VISIBILITY_LABELS } from '../../constants/labels.js'
import { formatKm } from '../../utils/format.js'
import { showToast } from '../../utils/toast.js'
import { useAsync } from '../../utils/useAsync.js'
import { AdminListState, AdminPager, AdminSearch, AdminTabs } from './AdminParts.jsx'

const TABS = [
  { value: 'ALL', label: '전체' },
  { value: 'REVIEW', label: '검토 필요' },
]

// 구분 칸 (API 명세 AD4): 대기 중 신고가 있으면 "검토 필요", 관리자가 올렸으면 "공식", 그 외 "사용자"
function KindChip({ course }) {
  if (course.pendingReports > 0) return <Chip tone="danger">검토 필요 {course.pendingReports}</Chip>
  if (course.official) return <Chip tone="blue">공식</Chip>
  return <Chip tone="neutral">사용자</Chip>
}

// AD4 코스 관리 (/admin/courses)
// API: GET /api/admin/courses?q=&tab= · PUT /api/admin/courses/{id}/visibility { hidden }
// "+ 공식 코스 등록"은 새 API 없이 관리자 계정으로 C3 코스 등록 (작성자가 ADMIN 이면 공식). 수정 버튼은 2순위
export default function AD4Courses() {
  const [tab, setTab] = useState('ALL')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(1)
  const [version, setVersion] = useState(0)
  const { data, error, loading } = useAsync(() => getAdminCourses({ q, tab, page }), `ad4-${tab}-${q}-${page}-${version}`)
  const [busyId, setBusyId] = useState(null)

  async function onToggle(course) {
    setBusyId(course.courseId)
    try {
      await changeCourseHidden(course.courseId, !course.hidden)
      showToast(course.hidden ? `다시 보이게 했어요: ${course.name}` : `숨겼어요: ${course.name}`)
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
          tabs={TABS}
          value={tab}
          onChange={(value) => {
            setTab(value)
            setPage(1)
          }}
        />
        <div className="row ai-c g12">
          <AdminSearch
            placeholder="코스 이름 · 등록자 검색"
            onSearch={(text) => {
              setQ(text)
              setPage(1)
            }}
          />
          <Button to="/courses/new">+ 공식 코스 등록</Button>
        </div>
      </div>
      {data && <p className="t-b14 c-body">{q ? `'${q}' 검색 결과 ` : ''}{data.total}개</p>}

      <AdminListState
        loading={loading}
        error={error}
        empty={data?.items.length === 0}
        emptyText={tab === 'REVIEW' ? '검토할 코스가 없어요' : '해당하는 코스가 없어요'}
      />
      {data?.items.length > 0 && (
        <div className="panel admin-table-box">
          <table className="admin-table">
            <thead>
              <tr>
                <th>코스</th>
                <th>등록자</th>
                <th>구분</th>
                <th className="num">거리</th>
                <th>난이도</th>
                <th className="num">다운로드</th>
                <th className="num">후기</th>
                <th>공개 범위</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.items.map((c) => (
                <tr key={c.courseId} className={c.hidden ? 'is-hidden' : ''}>
                  <td>
                    <div className="row ai-c g8">
                      <Link className="t-b14m c-strong" to={`/courses/${c.courseId}`}>
                        {c.name}
                      </Link>
                      {c.hidden && <Chip tone="neutral">숨김</Chip>}
                    </div>
                  </td>
                  <td className="nowrap">{c.author.nickname}</td>
                  <td>
                    <KindChip course={c} />
                  </td>
                  <td className="num">{formatKm(c.distanceKm)}</td>
                  <td className="nowrap">{LEVEL_LABELS[c.level]}</td>
                  <td className="num">{c.downloadCount}</td>
                  <td className="num">{c.reviewCount}</td>
                  <td className="nowrap">{VISIBILITY_LABELS[c.visibility]}</td>
                  <td className="actions">
                    <Button
                      variant={c.hidden ? 'secondary' : 'danger'}
                      disabled={busyId === c.courseId}
                      onClick={() => onToggle(c)}
                    >
                      {c.hidden ? '복구' : '숨김'}
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
