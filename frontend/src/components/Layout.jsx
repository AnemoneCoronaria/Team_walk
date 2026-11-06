import { Outlet } from 'react-router-dom'
import { USE_MOCK } from '../api/client.js'
import Header from './Header.jsx'

// 모든 화면 위에 헤더를 붙임 (화면 내용은 <Outlet /> 자리에 들어감)
export default function Layout() {
  return (
    <>
      <Header />
      <Outlet />
      {USE_MOCK && <div className="mock-badge" title="npm run dev:mock — 가짜 데이터로 보는 중">MOCK</div>}
    </>
  )
}
