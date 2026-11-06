import { Navigate, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Avatar from './Avatar.jsx'
import Button from './Button.jsx'
import Chip from './Chip.jsx'
import { logout } from '../api/userApi.js'
import { useAuth } from '../auth/authContext.js'
import { AGE_LABELS, LANGUAGE_LABELS, STYLE_LABELS } from '../constants/labels.js'
import { showToast } from '../utils/toast.js'
import './MyPageLayout.css'

// 메뉴 (to 가 없는 것은 아직 없는 화면)
const MENU = [
  { to: '/mypage', label: '내 동행', end: true },
  { to: '/mypage/courses', label: '내가 올린 코스' },
  { to: '/mypage/saved', label: '저장한 코스' },
  { to: '/mypage/reviews', label: '후기 · 스탬프' },
  { to: '/mypage/settings', label: '계정 설정' },
]

// 마이페이지 공통 틀 (Figma P1 · P3 · P5 의 왼쪽): 프로필 카드 + 메뉴, 오른쪽에 각 화면(<Outlet />)
// 로그인해야 들어올 수 있음 → 안 했으면 A1 로그인으로 (로그인하면 보던 화면으로 돌아옴)
export default function MyPageLayout() {
  const { user, loading, refresh } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  if (loading) return null
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />

  async function onLogout() {
    try {
      await logout()
    } catch {
      // 세션이 이미 끝났어도 로그인 전 상태로 보내면 됨
    }
    navigate('/') // 메인이 로그인 전 화면(M0)으로 바뀜
    await refresh()
    showToast('로그아웃했어요')
  }

  return (
    <main className="wrap mypage">
      <aside className="mypage-side">
        <div className="panel mypage-profile">
          <Avatar avatar={user.avatar} size={88} />
          <p className="t-h20 c-strong">{user.nickname}</p>
          <p className="t-b14 c-muted">{AGE_LABELS[user.ageGroup]}</p>
          <div className="row fwrap jc-c g6 fill">
            {user.styles.map((code) => (
              <Chip key={code}>{STYLE_LABELS[code]}</Chip>
            ))}
            {user.languages.map((code) => (
              <Chip key={code} tone="blue">
                {LANGUAGE_LABELS[code]}
              </Chip>
            ))}
            {user.mbti && <Chip tone="neutral">{user.mbti}</Chip>}
          </div>
          <Button variant="secondary" block to="/mypage/profile">
            프로필 · 태그 수정
          </Button>
        </div>

        <nav className="panel mypage-menu">
          {MENU.map((item) =>
            item.to ? (
              <NavLink key={item.label} className="mypage-menu-item" to={item.to} end={item.end}>
                {item.label}
              </NavLink>
            ) : (
              <button
                key={item.label}
                className="mypage-menu-item"
                type="button"
                onClick={() => showToast(`${item.label} 화면은 준비 중이에요`, 'info')}
              >
                {item.label}
              </button>
            ),
          )}
          <button className="mypage-menu-item" type="button" onClick={onLogout}>
            로그아웃
          </button>
        </nav>
      </aside>

      <section className="mypage-main">
        <Outlet />
      </section>
    </main>
  )
}
