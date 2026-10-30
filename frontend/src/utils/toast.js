import { createElement } from 'react'
import { createRoot } from 'react-dom/client'
import Toast from '../components/Toast.jsx'

// 화면 아래 가운데에 3초 동안 뜨는 알림
//   import { showToast } from '../utils/toast.js'
//   showToast('저장했어요')                ← Success (기본)
//   showToast(err.message, 'error')       ← API 오류 문구를 그대로 보여 줄 때
//   showToast('준비 중이에요', 'info')
// 어느 화면에서든 함수만 부르면 됩니다. 처음 부를 때 화면 맨 아래에 알림 자리를 스스로 만들어서
// main.jsx · Layout 같은 공용 파일은 고치지 않아도 됩니다.

const SHOW_MS = 3000

let toasts = []
let nextId = 1
let mounted = false
const listeners = new Set()

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

function getSnapshot() {
  return toasts
}

function update(next) {
  toasts = next
  listeners.forEach((listener) => listener())
}

function mount() {
  if (mounted) return
  mounted = true
  const el = document.createElement('div')
  document.body.appendChild(el)
  createRoot(el).render(createElement(Toast, { subscribe, getSnapshot }))
}

export function showToast(message, type = 'success') {
  mount()
  const id = nextId++
  update([...toasts, { id, message, type }])
  setTimeout(() => update(toasts.filter((t) => t.id !== id)), SHOW_MS)
}
