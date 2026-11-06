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

  // 검색: 지금은 코스 이름 검색(C1)으로 보냄 (동행 글 검색 API는 명세에 없음)
  function onSearch(e) {
    e.preventDefault()
    const keyword = q.trim()
    navigate(keyword ? `/courses?q=${encodeURIComponent(keyword)}` : '/courses')
  }

  return (
    <header className="header">
      <div className="wrap header-inner">
        <div className="header-left">
          <Link className="header-logo" to="/">
            산책시루
          </Link>
          <nav className="header-nav">
            <NavLink to="/courses" className={({ isActive }) => (isActive && !uploading ? 'active' : '')}>
              코스 찾기
            </NavLink>
            <NavLink to="/matches">동행 찾기</NavLink>
            <NavLink to="/courses/new">경로 업로드</NavLink>
          </nav>
          <form onSubmit={onSearch}>
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
              <Link className="row ai-c g12" to="/mypage">
                <img className="avatar" src={avatarSrc(user.avatar)} alt="" />
                <span className="header-name">{user.nickname}</span>
              </Link>
            </>
          ) : (
            <>
              <Link className="btn btn-ghost" to="/login">
                로그인
              </Link>
              <Link className="btn btn-primary" to="/signup">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
