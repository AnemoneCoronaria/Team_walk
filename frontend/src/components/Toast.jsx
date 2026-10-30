import { useSyncExternalStore } from 'react'
import './Toast.css'

const ICONS = { success: '✓', error: '!', info: 'i' }

// 화면 아래 가운데 토스트 목록 (Figma 컴포넌트 Toast: Success · Error · Info)
// utils/toast.js 의 showToast() 가 처음 불릴 때 스스로 화면에 붙입니다. 화면에서 직접 쓸 일은 없어요.
export default function Toast({ subscribe, getSnapshot }) {
  const list = useSyncExternalStore(subscribe, getSnapshot)
  return (
    <div className="toast-list" role="status" aria-live="polite">
      {list.map((t) => (
        <div key={t.id} className={`toast toast-${t.type}`}>
          <span className="toast-icon">{ICONS[t.type]}</span>
          {t.message}
        </div>
      ))}
    </div>
  )
}
