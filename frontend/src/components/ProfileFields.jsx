import Avatar from './Avatar.jsx'
import Chip from './Chip.jsx'
import { AGE_LABELS, AVATAR_LABELS, LANGUAGE_LABELS, STYLE_LABELS } from '../constants/labels.js'
import { MAX_STYLES, MBTI_PAIRS, NO_MBTI, isMbtiPartial } from '../utils/profile.js'
import { showToast } from '../utils/toast.js'
import './ProfileFields.css'

// 프로필 · 태그 고르는 부품 — A4 회원가입 2단계와 P2 프로필 수정이 같이 씀
// 모두 value + onChange 로 쓰고, 값은 API 코드 그대로 (seagull, 20s, SLOW, KO …)
//   <AvatarPicker value={avatar} onChange={setAvatar} />
//   <StyleChips value={styles} onChange={setStyles} />   ← 최대 3개 (넘으면 안내 토스트)
//   <MbtiPicker value={mbti} onChange={setMbti} />       ← value 는 쌍마다 고른 글자 ['E', '', 'T', '']
// 제목 줄(연령대, 산책 스타일 …)은 화면마다 문구가 달라서 화면에서 붙입니다.

/** 아바타 8종 중 하나 (size: 아바타 크기, A4 48px · P2 40px) */
export function AvatarPicker({ value, onChange, size = 48 }) {
  return (
    <div className="row fwrap g12">
      {Object.entries(AVATAR_LABELS).map(([code, label]) => (
        <button
          key={code}
          className={`avatar-pick${value === code ? ' is-selected' : ''}`}
          type="button"
          aria-pressed={value === code}
          onClick={() => onChange(code)}
        >
          <Avatar avatar={code} size={size} />
          {label}
        </button>
      ))}
    </div>
  )
}

/** 연령대 하나 */
export function AgeGroupChips({ value, onChange }) {
  return (
    <div className="row fwrap g6">
      {Object.entries(AGE_LABELS).map(([code, label]) => (
        <Chip key={code} tone="neutral" selected={value === code} onClick={() => onChange(code)}>
          {label}
        </Chip>
      ))}
    </div>
  )
}

/** 산책 스타일 여러 개 (최대 3개) */
export function StyleChips({ value, onChange }) {
  function toggle(code) {
    if (value.includes(code)) {
      onChange(value.filter((s) => s !== code))
    } else if (value.length >= MAX_STYLES) {
      showToast(`산책 스타일은 ${MAX_STYLES}개까지 고를 수 있어요.`, 'info')
    } else {
      onChange([...value, code])
    }
  }

  return (
    <div className="row fwrap g8">
      {Object.entries(STYLE_LABELS).map(([code, label]) => (
        <Chip key={code} selected={value.includes(code)} onClick={() => toggle(code)}>
          {label}
        </Chip>
      ))}
    </div>
  )
}

/** 언어 여러 개 (1개 이상인지는 화면에서 검사) */
export function LanguageChips({ value, onChange }) {
  function toggle(code) {
    onChange(value.includes(code) ? value.filter((l) => l !== code) : [...value, code])
  }

  return (
    <div className="row fwrap g8">
      {Object.entries(LANGUAGE_LABELS).map(([code, label]) => (
        <Chip key={code} tone="blue" selected={value.includes(code)} onClick={() => toggle(code)}>
          {label}
        </Chip>
      ))}
    </div>
  )
}

/** MBTI — 쌍마다 한 글자 (같은 글자를 다시 누르면 그 쌍은 해제), "선택 안 함"이면 모두 비움 */
export function MbtiPicker({ value, onChange }) {
  function pick(pairIndex, letter) {
    onChange(value.map((v, i) => (i === pairIndex ? (v === letter ? '' : letter) : v)))
  }

  return (
    <>
      <div className="row fwrap g8">
        <Chip tone="neutral" selected={value.every((v) => !v)} onClick={() => onChange(NO_MBTI)}>
          선택 안 함
        </Chip>
        {MBTI_PAIRS.map((pair, i) =>
          pair.map((letter) => (
            <Chip key={letter} tone="neutral" selected={value[i] === letter} onClick={() => pick(i, letter)}>
              {letter}
            </Chip>
          )),
        )}
      </div>
      {isMbtiPartial(value) && <p className="t-c12 c-danger">E/I · N/S · T/F · J/P 에서 하나씩, 4글자를 모두 골라 주세요</p>}
    </>
  )
}
