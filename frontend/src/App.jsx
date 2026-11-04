import { Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/authContext.js'
import Layout from './components/Layout.jsx'
import HealthCheck from './pages/HealthCheck.jsx'
import M0Main from './pages/M0Main.jsx'
import M1Main from './pages/M1Main.jsx'
import C1CourseList from './pages/C1CourseList.jsx'
import C2CourseDetail from './pages/C2CourseDetail.jsx'
import W1MatchList from './pages/W1MatchList.jsx'
import W2MatchDetail from './pages/W2MatchDetail.jsx'
import W3MatchWrite from './pages/W3MatchWrite.jsx'
import C3CourseUpload from './pages/C3CourseUpload.jsx'
import Placeholder from './pages/Placeholder.jsx'
import A1Login from './pages/A1Login.jsx'
import Signup from './pages/Signup.jsx'
import X1NotFound from './pages/X1NotFound.jsx'
import X2ServerError from './pages/X2ServerError.jsx'

// 화면 주소 (라우터) — 공용 파일이라 고치기 전에 팀에 알리기 (README "꼭 지킬 것")
//   동훈: /, /courses, /courses/:id, /courses/new, /matches, /matches/:id, /matches/:id/edit, /matches/new
//   승민: /login, /signup, /mypage, /500, 없는 주소(*) … (/mypage 는 아직 자리만)
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
        <Route path="/courses/new" element={<C3CourseUpload />} />

        {/* 동행 */}
        <Route path="/matches" element={<W1MatchList />} />
        <Route path="/matches/new" element={<W3MatchWrite />} />
        <Route path="/matches/:matchId" element={<W2MatchDetail />} />
        <Route path="/matches/:matchId/edit" element={<W3MatchWrite />} />

        {/* 계정 · 마이페이지 (승민) */}
        <Route path="/login" element={<A1Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/mypage" element={<Placeholder id="P1" title="마이페이지" owner="승민" />} />

        {/* 오류 화면 (승민): X2 500 은 확인 · 시연용 주소, 없는 주소 → X1 404 */}
        <Route path="/500" element={<X2ServerError />} />
        <Route path="*" element={<X1NotFound />} />
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
