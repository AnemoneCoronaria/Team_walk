// 태그 칩 (Figma 컴포넌트 Chip: Green · Blue · Neutral · Selected)
//   <Chip tone="blue">한국어</Chip>                                    ← 보여 주기만
//   <Chip tone="green" selected={on} onClick={toggle}>🐢 천천히</Chip>   ← onClick 을 주면 누를 수 있는 버튼
// tone: green(산책 스타일) · blue(언어) · neutral(연령대 · MBTI 등) · danger
export default function Chip({ tone = 'green', selected = false, onClick, className = '', children }) {
  const cls = ['chip', `chip-${tone}`, selected && 'is-selected', className].filter(Boolean).join(' ')

  if (!onClick) return <span className={cls}>{children}</span>
  return (
    <button className={cls} type="button" aria-pressed={selected} onClick={onClick}>
      {children}
    </button>
  )
}
