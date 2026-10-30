import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import './Modal.css'

// 화면 가운데 뜨는 창 + 뒤의 어두운 배경 (Figma G1 · W2-1 · W5 · W6 모달 모양)
//   const [open, setOpen] = useState(false)
//   <Modal open={open} onClose={() => setOpen(false)}>…내용…</Modal>
// - 배경을 누르거나 Esc 를 누르면 onClose 가 불립니다.
// - 기본 폭은 Figma G1 의 440px. 다른 폭은 width={520} 처럼 넘기면 됩니다.
// - 안의 내용은 가운데 정렬 · 세로 간격 16px. 왼쪽 정렬이 필요하면 className 으로 덮어쓰기
export default function Modal({ open, onClose, width = 440, className = '', children }) {
  useEffect(() => {
    if (!open) return
    function onKeyDown(e) {
      if (e.key === 'Escape') onClose?.()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open, onClose])

  if (!open) return null
  return createPortal(
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className={`modal ${className}`.trim()}
        style={{ width }}
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>,
    document.body,
  )
}
