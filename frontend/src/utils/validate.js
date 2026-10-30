// 입력값 검사 — 규칙과 문구는 API 명세 "계정 > 입력 규칙" (백엔드 SignupRequest 와 같게 맞춤)
// 통과하면 '' , 틀리면 화면에 보여 줄 오류 문구를 돌려줍니다.
//   const error = validateLoginId(loginId)   // '' 이면 통과
//   <Input … error={error} />

export function validateLoginId(value) {
  const v = value.trim()
  if (!v) return '아이디를 입력해 주세요.'
  if (!/^[A-Za-z0-9]{4,16}$/.test(v)) return '아이디는 영문 · 숫자 4~16자예요.'
  return ''
}

export function validatePassword(value) {
  if (!value) return '비밀번호를 입력해 주세요.'
  if (value.length > 64) return '비밀번호는 64자까지예요.'
  if (!/^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/.test(value)) {
    return '비밀번호는 8자 이상, 영문 · 숫자 · 특수문자를 모두 넣어 주세요.'
  }
  return ''
}

export function validatePasswordConfirm(password, confirm) {
  if (!confirm) return '비밀번호를 한 번 더 입력해 주세요.'
  if (password !== confirm) return '비밀번호가 일치하지 않습니다'
  return ''
}

export function validateNickname(value) {
  const length = value.trim().length
  if (length === 0) return '닉네임을 입력해 주세요.'
  if (length < 2 || length > 10) return '닉네임은 2~10자예요.'
  return ''
}
