import { Navigate, Route, Routes } from 'react-router-dom'
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
import A1Login from './pages/A1Login.jsx'
import Signup from './pages/Signup.jsx'
import X1NotFound from './pages/X1NotFound.jsx'
import X2ServerError from './pages/X2ServerError.jsx'
import P2ProfileEdit from './pages/P2ProfileEdit.jsx'
import MyPageLayout from './components/MyPageLayout.jsx'
import P1MyPage from './pages/P1MyPage.jsx'
import P3MyCourses from './pages/P3MyCourses.jsx'
import P5AccountSettings from './pages/P5AccountSettings.jsx'
import R1ReviewWrite from './pages/R1ReviewWrite.jsx'
import C4CourseReviews from './pages/C4CourseReviews.jsx'
import AdminLayout from './pages/admin/AdminLayout.jsx'
import AD1Reports from './pages/admin/AD1Reports.jsx'
import AD2Users from './pages/admin/AD2Users.jsx'
import AD3Matches from './pages/admin/AD3Matches.jsx'
import AD4Courses from './pages/admin/AD4Courses.jsx'
import P4MyReviews from './pages/P4MyReviews.jsx'
import P6SavedCourses from './pages/P6SavedCourses.jsx'
import R2StampTour from './pages/R2StampTour.jsx'

// 화면 주소 (라우터) — 공용 파일이라 고치기 전에 팀에 알리기 (README "꼭 지킬 것")
//   동훈: /, /courses, /courses/:id, /courses/new, /matches, /matches/:id, /matches/:id/edit, /matches/new
//   승민: /login, /signup, /mypage, /stamps, /500, 없는 주소(*) …
//   정란: /admin/reports (AD1), /admin/users (AD2), /admin/courses (AD4) · 민서: /admin/matches (AD3)
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
        <Route path="/courses/:courseId/reviews" element={<C4CourseReviews />} />
        <Route path="/courses/new" element={<C3CourseUpload />} />

        {/* 동행 */}
        <Route path="/matches" element={<W1MatchList />} />
        <Route path="/matches/new" element={<W3MatchWrite />} />
        <Route path="/matches/:matchId" element={<W2MatchDetail />} />
        <Route path="/matches/:matchId/edit" element={<W3MatchWrite />} />

        {/* 계정 · 마이페이지 (승민) */}
        <Route path="/login" element={<A1Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/mypage" element={<MyPageLayout />}>
          <Route index element={<P1MyPage />} />
          <Route path="courses" element={<P3MyCourses />} />
          <Route path="saved" element={<P6SavedCourses />} />
          <Route path="reviews" element={<P4MyReviews />} />
          <Route path="settings" element={<P5AccountSettings />} />
        </Route>
        <Route path="/mypage/profile" element={<P2ProfileEdit />} />

        {/* 후기 쓰기 (승민) — C4 · P1 지난 동행에서 들어옴 */}
        <Route path="/courses/:courseId/review" element={<R1ReviewWrite />} />
        {/* 랜드마크 스탬프 투어 (승민) — M1 배너 · P4 · 인증 성공 창에서 들어옴 */}
        <Route path="/stamps" element={<R2StampTour />} />

        {/* 관리자 (정란 AD1 · AD2 · AD4, 민서 AD3) — 관리자 계정이 아니면 A1 로그인으로 */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/reports" replace />} />
          <Route path="reports" element={<AD1Reports />} />
          <Route path="users" element={<AD2Users />} />
          <Route path="matches" element={<AD3Matches />} />
          <Route path="courses" element={<AD4Courses />} />
        </Route>

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
