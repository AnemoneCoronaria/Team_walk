import { AVATAR_LABELS, avatarSrc } from '../constants/labels.js'

// 아바타 (Figma 컴포넌트 Avatar: 시흥 테마 캐릭터 8종)
//   <Avatar avatar={user.avatar} />          기본 40px
//   <Avatar avatar="cat" size={80} />
// avatar 는 API 코드 (seagull, lighthouse, lotus, reed, sogeumi, wave, dog, cat)
export default function Avatar({ avatar, size = 40, className = '' }) {
  return (
    <img
      className={`avatar ${className}`.trim()}
      src={avatarSrc(avatar)}
      alt={AVATAR_LABELS[avatar] ?? ''}
      style={{ width: size, height: size, borderRadius: size / 2 }}
    />
  )
}
