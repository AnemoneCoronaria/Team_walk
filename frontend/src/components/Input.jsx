import { useId } from 'react'
import './Input.css'

// 입력창 (Figma 컴포넌트 Input: Default · Focus · Error · Disabled)
//   <Input label="아이디" value={id} onChange={(e) => setId(e.target.value)} placeholder="영문 · 숫자 4~16자" />
//   helper : 칸 아래 회색 도움말
//   error  : 빨간 오류 문구 (helper 대신 보이고 테두리도 빨강). 문구 없이 테두리만 빨갛게 하려면 error={true}
//   addon  : 칸 오른쪽에 붙일 버튼 (A3 아이디 "중복 확인")
// 나머지 속성(type, value, onChange, onBlur, maxLength …)은 <input> 에 그대로 넘어갑니다.
export default function Input({ label, helper, error, addon, className = '', ...inputProps }) {
  const id = useId()
  const message = typeof error === 'string' && error ? error : helper

  return (
    <div className={`input input-full ${className}`.trim()}>
      {label && (
        <label className="input-label" htmlFor={id}>
          {label}
        </label>
      )}
      <div className="input-row">
        <input
          id={id}
          className={`input-field${error ? ' is-error' : ''}`}
          aria-invalid={error ? true : undefined}
          {...inputProps}
        />
        {addon}
      </div>
      {message && <p className={`input-helper${error ? ' is-error' : ''}`}>{message}</p>}
    </div>
  )
}
