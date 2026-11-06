// 백엔드 호출 공통 코드
//
// 가짜 데이터 / 실제 서버 바꾸기
//   npm run dev       실제 API (백엔드를 켜 놓고)
//   npm run dev:mock  가짜 데이터 src/mocks (백엔드 없이). 화면 구석에 MOCK 배지가 보입니다
//   배포 빌드(npm run build)에서는 설정과 상관없이 가짜 데이터가 켜지지 않습니다
//
// 응답 형식 (API 명세 공통 규칙)
//   성공 { success: true, data }  → data 만 돌려줌
//   실패 { success: false, error: { code, message } } → ApiError 를 던짐
//   body 에 FormData 를 넣으면 multipart/form-data 로 보냄 (GPX 업로드, 모집 글 쓰기)

export const USE_MOCK = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true'

if (USE_MOCK) {
  console.warn('[MOCK MODE] 가짜 데이터로 보는 중이에요. 로그인해도 민서로 들어가요. 실제 API는 npm run dev')
}

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.status = status // HTTP 상태 코드 (서버 연결 실패는 0)
    this.code = code // NOT_FOUND, LOGIN_REQUIRED …
  }
}

// { q: '옥구', facility: ['TOILET', 'BENCH'], page: 1 } → "?q=옥구&facility=TOILET,BENCH&page=1"
// 값이 비어 있으면(undefined, null, '', []) 빼고 보냄
export function toQuery(params = {}) {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) {
      if (value.length === 0) return
      sp.set(key, value.join(','))
    } else {
      sp.set(key, String(value))
    }
  })
  const s = sp.toString()
  return s ? `?${s}` : ''
}

export async function request(path, { method = 'GET', params, body } = {}) {
  let res
  try {
    res = await fetch(path + toQuery(params), {
      method,
      credentials: 'include', // 로그인 세션 쿠키(JSESSIONID)를 같이 보냄
      // FormData(파일 업로드)는 브라우저가 multipart 형식을 알아서 붙이므로 Content-Type 을 비워 둠
      headers: body && !(body instanceof FormData) ? { 'Content-Type': 'application/json' } : undefined,
      body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결할 수 없어요.')
  }

  let json = null
  try {
    json = await res.json()
  } catch {
    // 서버가 꺼져 있으면 Vite가 JSON이 아닌 오류를 돌려줌
  }
  // 백엔드가 꺼져 있으면 Vite 프록시가 JSON 없이 500 대 오류를 돌려줌 → 알아보기 쉬운 문구로
  if (!json && res.status >= 500) {
    throw new ApiError(0, 'SERVER_DOWN', '서버에 연결할 수 없어요. 백엔드(bootRun)가 켜져 있는지 확인해 주세요.')
  }
  if (!res.ok || !json || !json.success) {
    throw new ApiError(
      res.status,
      json?.error?.code ?? 'SERVER_ERROR',
      json?.error?.message ?? `서버 오류 (HTTP ${res.status})`,
    )
  }
  return json.data
}

// ---------- 가짜 데이터용 도우미 ----------

// 실제 서버처럼 잠깐 기다렸다가 돌려줌 (로딩 화면 확인용)
export function mockResponse(data, ms = 300) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(structuredClone(data)), ms)
  })
}

export function mockError(status, code, message, ms = 300) {
  return new Promise((_, reject) => {
    setTimeout(() => reject(new ApiError(status, code, message)), ms)
  })
}

// 목록 응답 모양 { items, page, size, total }
export function paginate(items, page = 1, size = 20) {
  const p = Number(page) || 1
  const s = Number(size) || 20
  return { items: items.slice((p - 1) * s, p * s), page: p, size: s, total: items.length }
}
