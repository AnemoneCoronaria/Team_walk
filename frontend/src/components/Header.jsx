import { useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/authContext.js'
import { avatarSrc } from '../constants/labels.js'
import './Header.css'

// 상단 공통 헤더 (Figma 컴포넌트 Header: Guest / Member)
// 로그인 상태는 useAuth() (GET /api/users/me) 로 판단
export default function Header() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  // '코스 찾기'는 /courses 아래 전부지만, 코스 등록(/courses/new)은 '경로 업로드'만 켜지게
  const uploading = pathname === '/courses/new'
  const [q, setQ] = useState('')
  // 좁은 화면(태블릿 · 휴대폰)의 ☰ 메뉴: 연 화면 주소를 기억해서, 다른 화면으로 가면 저절로 닫힘
  const [menuPath, setMenuPath] = useState(null)
  const menuOpen = menuPath === pathname
  const closeMenu = () => setMenuPath(null)

  // 검색: 지금은 코스 이름 검색(C1)으로 보냄 (동행 글 검색 API는 명세에 없음)
  function onSearch(e) {
    e.preventDefault()
    const keyword = q.trim()
    closeMenu()
    navigate(keyword ? `/courses?q=${encodeURIComponent(keyword)}` : '/courses')
  }

  return (
    <header className={`header ${menuOpen ? 'is-menu-open' : ''}`.trim()}>
      <div className="wrap header-inner">
        <div className="header-left">
          <Link className="header-logo" to="/" onClick={closeMenu}>
            산책시루
          </Link>
          <nav id="header-menu" className="header-nav" onClick={closeMenu}>
            <NavLink to="/courses" className={({ isActive }) => (isActive && !uploading ? 'active' : '')}>
              코스 찾기
            </NavLink>
            <NavLink to="/matches">동행 찾기</NavLink>
            <NavLink to="/courses/new">경로 업로드</NavLink>
          </nav>
          <form className="header-search-form" onSubmit={onSearch}>
            <input
              className="header-search"
              type="search"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="🔍  코스 · 동행 글 검색"
            />
          </form>
        </div>

        <div className="header-right">
          {/* 화면 언어 선택은 2순위 (지금은 모양만) */}
          <button className="header-lang" type="button">
            🌐 한국어 ▾
          </button>
          {loading ? null : user ? (
            <>
              {/* 알림(N1)은 2순위 — 지금은 아이콘만 */}
              <button className="header-bell" type="button" aria-label="알림">
                <img src="/icons/bell.svg" alt="" />
              </button>
              <Link className="row ai-c g12" to="/mypage" onClick={closeMenu}>
                <img className="avatar" src={avatarSrc(user.avatar)} alt="" />
                <span className="header-name">{user.nickname}</span>
              </Link>
            </>
          ) : (
            <>
              <Link className="btn btn-ghost header-login" to="/login" onClick={closeMenu}>
                로그인
              </Link>
              <Link className="btn btn-primary header-signup" to="/signup" onClick={closeMenu}>
                회원가입
              </Link>
            </>
          )}
          {/* 1024px 이하에서만 보이는 메뉴 버튼 (메뉴 · 검색 · 언어를 펼침) */}
          <button
            className="header-menu-btn"
            type="button"
            aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'}
            aria-expanded={menuOpen}
            aria-controls="header-menu"
            onClick={() => setMenuPath(menuOpen ? null : pathname)}
          >
            {menuOpen ? '✕' : '☰'}
          </button>
        </div>
      </div>
    </header>
  )
}
