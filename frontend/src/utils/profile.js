// 프로필 · 태그 값 도우미 (A4 회원가입 2단계 · P2 프로필 수정이 같이 씀)
import { validateNickname } from './validate.js'

export const MAX_STYLES = 3

// MBTI 는 쌍마다 한 글자씩 골라 4글자 (예: ENFP). 화면에서는 ['E', 'N', 'F', 'P'] 처럼 글자 배열로 들고 있음
export const MBTI_PAIRS = [
  ['E', 'I'],
  ['N', 'S'],
  ['T', 'F'],
  ['J', 'P'],
]
export const NO_MBTI = ['', '', '', '']

/** API 값 → 글자 배열 ('ENFP' → ['E', 'N', 'F', 'P'], null → NO_MBTI) */
export function mbtiToLetters(mbti) {
  if (!mbti) return NO_MBTI
  const upper = mbti.toUpperCase()
  return MBTI_PAIRS.map((pair, i) => (pair.includes(upper[i]) ? upper[i] : ''))
}

/** 글자 배열 → API 값 (4글자를 다 골랐으면 'ENFP', 아니면 null) */
export function lettersToMbti(letters) {
  return letters.every(Boolean) ? letters.join('') : null
}

/** 몇 글자만 고른 상태 — 이때는 가입 · 저장 버튼을 막음 */
export function isMbtiPartial(letters) {
  const count = letters.filter(Boolean).length
  return count > 0 && count < MBTI_PAIRS.length
}

/** 보낼 수 있는 프로필인지: 닉네임 규칙, 연령대, 언어 1개 이상, MBTI 는 4글자 또는 선택 안 함 */
export function isProfileReady({ nickname, ageGroup, languages, mbti }) {
  return !validateNickname(nickname) && ageGroup !== '' && languages.length > 0 && !isMbtiPartial(mbti)
}
