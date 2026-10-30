import Button from '../components/Button.jsx'
import './ErrorPage.css'

// X2 500 — 서버 오류 (API가 5xx 를 돌려줬거나 서버에 연결이 안 될 때)
// 화면에서 불러온 데이터가 실패하면 그 자리에 이 화면을 보여 주면 됩니다.
//   const { data, error } = useAsync(…)
//   if (error && (error.status >= 500 || error.status === 0)) return <X2ServerError />
// "다시 시도"는 onRetry 를 주면 그 함수를, 없으면 페이지를 새로 불러옵니다.
// 주소 /500 으로도 볼 수 있습니다 (시연 · 확인용)
export default function X2ServerError({ onRetry }) {
  return (
    <main className="error-page">
      <p className="t-display48 c-danger">500</p>
      <h1 className="t-h24 c-strong">잠시 문제가 생겼어요 🔧</h1>
      <p className="t-b16 c-body">서버에 일시적인 오류가 있어요. 잠시 후 다시 시도해 주세요</p>
      <div className="row g8">
        <Button variant="secondary" onClick={onRetry ?? (() => window.location.reload())}>
          다시 시도
        </Button>
        <Button to="/">메인으로</Button>
      </div>
    </main>
  )
}
