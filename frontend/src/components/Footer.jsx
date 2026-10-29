import './Footer.css'

// 하단 공통 (Figma M1 아래쪽). 약관 · 문의 화면은 2순위라 지금은 글자만
export default function Footer() {
  return (
    <footer className="footer">
      <div className="wrap footer-inner">
        <span>산책시루 · 산책크루(7팀) · 시흥시 협업 프로젝트</span>
        <span className="footer-links">이용약관   개인정보처리방침   문의하기</span>
      </div>
    </footer>
  )
}
