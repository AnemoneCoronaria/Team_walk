// 랜드마크 API (백엔드 담당: 동언, develop 에 합쳐짐) — R1 랜드마크 인증, R2 스탬프 투어
//   GET  /api/landmarks         → { items: [{ landmarkId, name, area, point, radiusM, stamped, stampedAt }] }
//                                 (로그인 안 했으면 stamped 는 모두 false)
//   POST /api/landmarks/verify  multipart: file(.gpx)
//                               → { result: SUCCESS | FAIL, reason?: NOT_NEAR | ALREADY_ALL,
//                                   newStamps: [{ landmarkId, name }], alreadyStamped, newBadges: [{ code, name }] }
//                                 실패(R1-2)도 200. 위치 정보 동의를 안 했으면 403 CONSENT_REQUIRED
// 내 스탬프 · 배지(GET /api/users/me/stamps)는 userApi.js 의 getMyStamps
import { USE_MOCK, request, mockResponse, mockError } from './client.js'
import { LANDMARKS } from '../mocks/landmarks.js'
import { ME, MY_STAMPS } from '../mocks/users.js'

export function getLandmarks() {
  if (USE_MOCK) {
    const guest = isMockGuest()
    const items = LANDMARKS.map((l) => {
      const stamp = guest ? null : MY_STAMPS.stamps.find((s) => s.landmarkId === l.landmarkId)
      return { ...l, stamped: Boolean(stamp), stampedAt: stamp?.stampedAt ?? null }
    })
    return mockResponse({ items })
  }
  return request('/api/landmarks')
}

/** GPX 로 랜드마크 인증 — 경로의 한 점이라도 랜드마크 반경(300m) 안이면 스탬프 */
export function verifyLandmark(file) {
  if (USE_MOCK) return mockVerify(file)
  const form = new FormData()
  form.append('file', file)
  return request('/api/landmarks/verify', { method: 'POST', body: form })
}

// ---------- 가짜 데이터: 브라우저에서 GPX 를 읽어 서버와 같은 규칙으로 판정 ----------

// 스탬프 수로 받는 배지 (서버 LandmarkService 와 같은 기준)
const STAMP_BADGES = [
  ['FIRST_WALK', '첫 산책', 1],
  ['LANDMARK_3', '랜드마크 3곳', 3],
  ['SIHEUNG_MASTER', '시흥 산책 마스터', LANDMARKS.length],
]

async function mockVerify(file) {
  if (!ME.agreeLocation) return mockError(403, 'CONSENT_REQUIRED', '위치 정보 이용에 동의해야 인증할 수 있어요.')
  const path = await readGpxPoints(file)
  if (path.length === 0) return mockError(400, 'INVALID_FORMAT', 'GPX 파일에서 경로를 읽을 수 없어요.')

  const ref = (l) => ({ landmarkId: l.landmarkId, name: l.name })
  const isStamped = (l) => MY_STAMPS.stamps.some((s) => s.landmarkId === l.landmarkId)
  const near = LANDMARKS.filter((l) => path.some((p) => distanceM(p, l.point) <= l.radiusM))
  const fail = (reason, alreadyStamped = []) =>
    mockResponse({ result: 'FAIL', reason, newStamps: [], alreadyStamped, newBadges: [] })
  if (near.length === 0) return fail('NOT_NEAR')
  const fresh = near.filter((l) => !isStamped(l))
  const already = near.filter(isStamped).map(ref)
  if (fresh.length === 0) return fail('ALREADY_ALL', already)

  const before = MY_STAMPS.stamps.length
  const now = localNow()
  fresh.forEach((l) => MY_STAMPS.stamps.push({ ...ref(l), stampedAt: now }))
  MY_STAMPS.stampCount = MY_STAMPS.stamps.length
  const newBadges = STAMP_BADGES.filter(([, , n]) => n > before && n <= MY_STAMPS.stampCount).map(([code, name]) => ({ code, name }))
  newBadges.forEach((b) => MY_STAMPS.badges.push({ ...b, earnedAt: now }))
  return mockResponse({ result: 'SUCCESS', newStamps: fresh.map(ref), alreadyStamped: already, newBadges })
}

// GPX 의 trkpt · rtept · wpt 좌표 → [[위도, 경도], …]
async function readGpxPoints(file) {
  const doc = new DOMParser().parseFromString(await file.text(), 'application/xml')
  return [...doc.querySelectorAll('trkpt, rtept, wpt')]
    .map((n) => [Number(n.getAttribute('lat')), Number(n.getAttribute('lon'))])
    .filter(([lat, lng]) => Number.isFinite(lat) && Number.isFinite(lng))
}

// 두 좌표 사이 거리(m) — 하버사인 공식
function distanceM([lat1, lng1], [lat2, lng2]) {
  const rad = (d) => (d * Math.PI) / 180
  const a =
    Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lng2 - lng1) / 2) ** 2
  return 2 * 6371000 * Math.asin(Math.sqrt(a))
}

// "2026-11-06T14:30:00" (한국 시간, API 날짜 형식)
function localNow() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function isMockGuest() {
  try {
    return localStorage.getItem('mockGuest') === '1'
  } catch {
    return false
  }
}
