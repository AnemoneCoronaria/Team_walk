import { Link } from 'react-router-dom'

// 기본 버튼 (Figma 컴포넌트 Button: Primary · Secondary · Ghost · Danger · Disabled)
//   <Button onClick={save}>저장</Button>
//   <Button variant="secondary" to="/courses">코스 둘러보기</Button>   ← to 를 주면 링크로 이동
//   <Button block disabled={!ok}>다음 단계</Button>                   ← block = 가로로 꽉 채우기
// variant: primary(주요 동작) · secondary(보조) · ghost(글자만) · danger(신고 · 삭제)
// disabled 를 주면 Figma 의 Disabled 모양이 됩니다. 모양은 index.css 의 .btn 클래스를 그대로 씁니다.
export default function Button({ variant = 'primary', block = false, to, type = 'button', className = '', children, ...rest }) {
  const cls = ['btn', `btn-${variant}`, block && 'btn-block', className].filter(Boolean).join(' ')

  if (to) {
    return (
      <Link className={cls} to={to} {...rest}>
        {children}
      </Link>
    )
  }
  return (
    <button className={cls} type={type} {...rest}>
      {children}
    </button>
  )
}
