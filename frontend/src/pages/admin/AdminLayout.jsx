import { Navigate, NavLink, Outlet, useLocation } from 'react-router-dom'
import { USE_MOCK } from '../../api/client.js'
import { useAuth } from '../../auth/authContext.js'
import { showToast } from '../../utils/toast.js'
import './Admin.css'

// 관리자 메뉴 (to 가 없는 것은 아직 없는 화면)
const MENU = [
  { to: '/admin/reports', label: '신고 처리', id: 'AD1' },
  { to: '/admin/users', label: '회원 관리', id: 'AD2' },
  { to: '/admin/matches', label: '모집 글 관리', id: 'AD3' },
  { to: '/admin/courses', label: '코스 관리', id: 'AD4' },
]

// 관리자 화면 공통 틀 (/admin/...) — 위쪽 메뉴, 아래에 각 화면(<Outlet />)
// API 명세: 관리자 화면을 열면 GET /api/users/me 로 확인해서, 로그인 안 했거나 ADMIN 이 아니면 A1 로그인으로
// 가짜 데이터(USE_MOCK)에서는 내 정보가 늘 민서(USER)라 검사를 건너뜀
export default function AdminLayout() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return null
  if (!USE_MOCK && user?.role !== 'ADMIN') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }

  return (
    <main className="wrap admin">
      <div className="row ai-c jc-b">
        <h1 className="t-h24 c-strong">관리자</h1>
        <p className="t-c12 c-muted">{USE_MOCK ? '가짜 데이터로 보는 중 (npm run dev:mock)' : `${user.nickname} 계정으로 로그인`}</p>
      </div>
      <nav className="admin-menu">
        {MENU.map((item) =>
          item.to ? (
            <NavLink key={item.id} className="admin-menu-item" to={item.to}>
              <span className="t-c12 c-muted">{item.id}</span>
              {item.label}
            </NavLink>
          ) : (
            <button
              key={item.id}
              className="admin-menu-item"
              type="button"
              onClick={() => showToast(`${item.label} 화면은 준비 중이에요`, 'info')}
            >
              <span className="t-c12 c-muted">{item.id}</span>
              {item.label}
            </button>
          ),
        )}
      </nav>
      <Outlet />
    </main>
  )
}
