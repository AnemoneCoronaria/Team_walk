// 가짜 데이터용 GPX 분석 — 백엔드 GpxService(동언)와 같은 규칙을 브라우저에서 흉내 냄
// 실제 서버 모드(VITE_USE_MOCK=false)에서는 쓰지 않고 POST /api/gpx/parse 를 부릅니다.
//   - .gpx 가 아니거나 좌표가 없으면 400 INVALID_FORMAT
//   - 5MB 초과 400 TOO_LARGE
//   - 경로 300m 미만 또는 점 20개 미만 400 TOO_SHORT
//   - 거리: 점 사이 거리 합 (같은 trkseg 안에서만), 시간: 거리 × 15분 (시속 4km)
//   - 지역: 시작점과 가장 가까운 지역 (seed 코스 시작점 기준, 백엔드 Geo.area 와 같음)
import { ApiError } from '../api/client.js'

const MAX_FILE_SIZE = 5 * 1024 * 1024
const MIN_POINTS = 20
const MIN_DISTANCE_METERS = 300
const AREA_CENTERS = [
  ['OKGU', 37.35272, 126.709915],
  ['JEONGWANG', 37.351452, 126.74279],
  ['BAEGOT', 37.37141, 126.716051],
  ['SIHWA', 37.345543, 126.687608],
]

function meters(lat1, lon1, lat2, lon2) {
  const rad = (d) => (d * Math.PI) / 180
  const dLat = rad(lat2 - lat1)
  const dLon = rad(lon2 - lon1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2
  return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)))
}

function nearestArea(lat, lon) {
  let best = AREA_CENTERS[0]
  AREA_CENTERS.forEach((c) => {
    if (meters(lat, lon, c[1], c[2]) < meters(lat, lon, best[1], best[2])) best = c
  })
  return best[0]
}

const invalid = () => new ApiError(400, 'INVALID_FORMAT', 'GPX 파일과 좌표를 확인해 주세요.')

/** File → { distanceKm, durationMin, pointCount, area, path, start, end } (API 명세 POST /api/gpx/parse 의 data) */
export async function parseGpxInBrowser(file) {
  if (!file.name.toLowerCase().endsWith('.gpx')) throw invalid()
  if (file.size > MAX_FILE_SIZE) throw new ApiError(400, 'TOO_LARGE', '파일은 5MB 이하여야 해요.')

  const doc = new DOMParser().parseFromString(await file.text(), 'application/xml')
  if (doc.querySelector('parsererror') || doc.documentElement.localName !== 'gpx') throw invalid()

  const points = Array.from(doc.getElementsByTagNameNS('*', 'trkpt'))
  const path = []
  let distance = 0
  let prevSegment = null
  points.forEach((p) => {
    const lat = Number(p.getAttribute('lat'))
    const lon = Number(p.getAttribute('lon'))
    if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) throw invalid()
    if (path.length && prevSegment === p.parentNode) {
      const [plat, plon] = path[path.length - 1]
      distance += meters(plat, plon, lat, lon)
    }
    path.push([lat, lon])
    prevSegment = p.parentNode
  })
  if (path.length === 0) throw invalid()
  if (path.length < MIN_POINTS || distance < MIN_DISTANCE_METERS) {
    throw new ApiError(400, 'TOO_SHORT', '경로는 300m 이상이고 위치 점은 20개 이상이어야 해요.')
  }
  const start = path[0]
  return {
    distanceKm: Math.round(distance / 10) / 100,
    durationMin: Math.round((distance / 1000) * 15),
    pointCount: path.length,
    area: nearestArea(start[0], start[1]),
    path,
    start,
    end: path[path.length - 1],
  }
}
