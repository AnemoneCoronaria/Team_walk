import { useState } from 'react'
import Button from '../../components/Button.jsx'
import Chip from '../../components/Chip.jsx'

// 관리자 화면(AD1 · AD2 · AD3 · AD4)이 같이 쓰는 작은 부품

// 탭 줄 — tabs: [{ value, label }]
//   <AdminTabs tabs={TABS} value={status} onChange={setStatus} />
export function AdminTabs({ tabs, value, onChange }) {
  return (
    <div className="row g8">
      {tabs.map((tab) => (
        <Chip key={tab.label} tone="neutral" selected={value === tab.value} onClick={() => onChange(tab.value)}>
          {tab.label}
        </Chip>
      ))}
    </div>
  )
}

// 검색창 — 엔터나 "검색"을 누르면 onSearch(검색어)
export function AdminSearch({ placeholder, onSearch }) {
  const [text, setText] = useState('')
  return (
    <form
      className="row ai-c g8"
      onSubmit={(e) => {
        e.preventDefault()
        onSearch(text.trim())
      }}
    >
      <input
        className="input-field admin-search"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
      />
      <Button type="submit" variant="secondary">
        검색
      </Button>
    </form>
  )
}

// 페이지 이동 — data: 목록 응답 { page, size, total }
export function AdminPager({ data, onPage }) {
  const last = Math.max(1, Math.ceil(data.total / data.size))
  if (last === 1) return null
  return (
    <div className="row ai-c jc-c g12">
      <Button variant="ghost" disabled={data.page <= 1} onClick={() => onPage(data.page - 1)}>
        ← 이전
      </Button>
      <span className="t-b14 c-body">
        {data.page} / {last}
      </span>
      <Button variant="ghost" disabled={data.page >= last} onClick={() => onPage(data.page + 1)}>
        다음 →
      </Button>
    </div>
  )
}

// 목록 위 상태 줄: 불러오는 중 · 오류 · 비었음
export function AdminListState({ loading, error, empty, emptyText }) {
  if (loading) return <p className="admin-empty">불러오는 중…</p>
  if (error) return <p className="admin-empty c-danger">{error.message}</p>
  if (empty) return <p className="admin-empty">{emptyText}</p>
  return null
}
