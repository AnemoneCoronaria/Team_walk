import { Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/authContext.js'
import Layout from './components/Layout.jsx'
import HealthCheck from './pages/HealthCheck.jsx'
import M0Main from './pages/M0Main.jsx'
import M1Main from './pages/M1Main.jsx'
import C1CourseList from './pages/C1CourseList.jsx'
import C2CourseDetail from './pages/C2CourseDetail.jsx'
import W1MatchList from './pages/W1MatchList.jsx'
import Placeholder from './pages/Placeholder.jsx'

// 화면 주소 (라우터) — 공용 파일이라 고치기 전에 팀에 알리기 (README "꼭 지킬 것")
//   동훈: /, /courses, /courses/:id, /courses/new, /matches, /matches/:id, /matches/new
//   승민: /login, /signup, /mypage … (지금은 자리만)
export default function App() {
  return (
    <Routes>
      {/* 환경 확인용 (README 5번) */}
      <Route path="/health" element={<HealthCheck />} />

      <Route element={<Layout />}>
        <Route path="/" element={<MainRoute />} />

        {/* 코스 */}
        <Route path="/courses" element={<C1CourseList />} />
        <Route path="/courses/:courseId" element={<C2CourseDetail />} />
        <Route path="/courses/:courseId/reviews" element={<Placeholder id="C4" title="코스 후기 전체 보기" owner="승민" />} />
        <Route path="/courses/new" element={<Placeholder id="C3-1 ~ C3-3" title="GPX로 코스 등록" owner="동훈 · 2주차" />} />

        {/* 동행 */}
        <Route path="/matches" element={<W1MatchList />} />
        <Route path="/matches/new" element={<Placeholder id="W3" title="모집 글 쓰기" owner="동훈 · 2주차" />} />
        <Route path="/matches/:matchId" element={<Placeholder id="W2 · W4" title="동행 상세" owner="동훈 · 2주차" />} />

        {/* 계정 · 마이페이지 (승민) */}
        <Route path="/login" element={<Placeholder id="A1" title="로그인" owner="승민" />} />
        <Route path="/signup" element={<Placeholder id="A3 ~ A5" title="회원가입" owner="승민" />} />
        <Route path="/mypage" element={<Placeholder id="P1" title="마이페이지" owner="승민" />} />

        {/* 없는 주소 → X1 404 (승민) */}
        <Route path="*" element={<Placeholder id="X1" title="페이지를 찾을 수 없어요" owner="승민" />} />
      </Route>
    </Routes>
  )
}

// 메인: 로그인 전이면 M0, 로그인했으면 M1
function MainRoute() {
  const { user, loading } = useAuth()
  if (loading) return null
  return user ? <M1Main /> : <M0Main />
}
