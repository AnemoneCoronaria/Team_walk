import { useState } from 'react'
import { Link } from 'react-router-dom'
import LoginRequiredModal from './LoginRequiredModal.jsx'
import { useAuth } from '../auth/authContext.js'
import { LANGUAGE_LABELS, MATCH_STATUS_LABELS, STYLE_LABELS, avatarSrc } from '../constants/labels.js'
import { formatKm, formatMeetAt } from '../utils/format.js'
import './MatchCard.css'

/**
 * 동행 모집 카드 (Figma 컴포넌트 MatchCard)
 * match: GET /api/matches 의 items 한 줄
 *
 * 참여자 아바타: 목록 응답에는 참여자 목록이 없어서 모집자 아바타만 보여 줍니다.
 *   (가짜 데이터에는 participants 를 넣어 두어 디자인처럼 여러 명이 보임)
 * 참여하기: 로그인 전이면 G1 로그인 필요 모달(승민),
 *   로그인했으면 동행 상세(W2)로 가서 참여 확인 모달(W2-1)을 띄움 (?join=1)
 */
export default function MatchCard({ match, className = '' }) {
  const { user } = useAuth()
  const [askLogin, setAskLogin] = useState(false)
  const open = match.status === 'OPEN'
  const people = match.participants ?? [match.host]

  return (
    <div className={`match-card ${className}`}>
      <div className="match-card-row">
        <p className="t-b14m c-blue nowrap">{formatMeetAt(match.meetAt)}</p>
        <span className={`chip ${open ? 'chip-green' : 'chip-neutral'}`}>
          {MATCH_STATUS_LABELS[match.status]} {match.current}/{match.capacity}
        </span>
      </div>
      <Link className="t-h20 c-strong nowrap match-card-title" to={`/matches/${match.matchId}`}>
        {match.title}
      </Link>
      <p className="t-b14 c-body nowrap">
        {match.course.name} · {formatKm(match.course.distanceKm)}
      </p>
      <div className="match-card-tags">
        {match.styles.map((s) => (
          <span key={s} className="chip chip-green">
            {STYLE_LABELS[s] ?? s}
          </span>
        ))}
        {match.languages.map((l) => (
          <span key={l} className="chip chip-blue">
            {LANGUAGE_LABELS[l] ?? l}
          </span>
        ))}
      </div>
      <div className="match-card-row">
        <div className="match-card-people">
          {people.map((p) => (
            <img key={p.userId} className="avatar" src={avatarSrc(p.avatar)} alt={p.nickname} title={p.nickname} />
          ))}
        </div>
        {!open ? (
          <span className="btn btn-disabled">{MATCH_STATUS_LABELS[match.status]}</span>
        ) : user ? (
          <Link className="btn btn-primary" to={`/matches/${match.matchId}?join=1`}>
            참여하기
          </Link>
        ) : (
          <button type="button" className="btn btn-primary" onClick={() => setAskLogin(true)}>
            참여하기
          </button>
        )}
      </div>
      <LoginRequiredModal open={askLogin} onClose={() => setAskLogin(false)} />
    </div>
  )
}
