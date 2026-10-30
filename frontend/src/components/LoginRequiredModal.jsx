import { useLocation } from 'react-router-dom'
import Button from './Button.jsx'
import Modal from './Modal.jsx'

// G1 로그인 필요 모달 — 로그인 안 한 사람이 참여 · 모집 글 쓰기 · 스탬프 인증을 누를 때
//   const { user } = useAuth()
//   const [askLogin, setAskLogin] = useState(false)
//   <Button onClick={() => (user ? join() : setAskLogin(true))}>참여하기</Button>
//   <LoginRequiredModal open={askLogin} onClose={() => setAskLogin(false)} />
// 로그인하면 지금 보던 화면으로 돌아옵니다 (A1 이 state.from 으로 돌려보냄)
export default function LoginRequiredModal({ open, onClose }) {
  const location = useLocation()
  const from = location.pathname + location.search

  return (
    <Modal open={open} onClose={onClose}>
      <p className="t-emoji32">🚶‍♀️🚶</p>
      <h2 className="t-h24 c-strong">로그인하고 함께 걸어요</h2>
      <p className="t-b14 c-body">동행 참여 · 모집 글 쓰기 · 스탬프 인증은 회원만 할 수 있어요</p>
      <Button block to="/login" state={{ from }}>
        로그인
      </Button>
      <Button block variant="secondary" to="/signup">
        회원가입 (1분)
      </Button>
      <p className="t-c12 c-muted">둘러보기는 로그인 없이도 가능해요</p>
    </Modal>
  )
}
