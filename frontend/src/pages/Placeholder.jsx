import { Link } from 'react-router-dom'

// 아직 만들지 않은 화면 자리 (주소 이동이 되는지 확인용). 담당자가 실제 화면으로 바꾸면 지우면 됩니다.
export default function Placeholder({ id, title, owner }) {
  return (
    <section className="wrap col ai-c g12" style={{ padding: '120px 0 160px' }}>
      <p className="t-c12b c-muted">{id}</p>
      <h1 className="t-h24 c-strong">{title}</h1>
      <p className="t-b14 c-body">{owner} · 아직 준비 중인 화면이에요</p>
      <Link className="btn btn-secondary" to="/">
        메인으로
      </Link>
    </section>
  )
}
