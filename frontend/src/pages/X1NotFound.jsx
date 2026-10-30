import { useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import './ErrorPage.css'

// X1 404 — 없는 주소 · 삭제된 글에 들어왔을 때
// 라우터의 path="*" 가 보여 줍니다. 상세 화면에서 API가 404 를 돌려줘도 이 화면을 쓰면 됩니다.
//   if (error?.status === 404) return <X1NotFound />
// "메인으로"는 / → 로그인 전이면 M0, 로그인했으면 M1
export default function X1NotFound() {
  const navigate = useNavigate()

  return (
    <main className="error-page">
      <p className="t-display48 c-green">404</p>
      <h1 className="t-h24 c-strong">길을 잃으셨나요? 🧭</h1>
      <p className="t-b16 c-body">요청하신 페이지가 없거나 삭제된 모집 글 · 코스예요</p>
      <div className="row g8">
        <Button variant="secondary" onClick={() => navigate(-1)}>
          이전 페이지
        </Button>
        <Button to="/">메인으로</Button>
      </div>
    </main>
  )
}
