import { useEffect, useRef, useState } from 'react'
import './KakaoMap.css'

/**
 * 카카오맵 컴포넌트 (지도 · 마커 · 경로 선)
 *
 * 좌표는 모두 API와 같은 [위도, 경도] 배열입니다.
 *
 *   <KakaoMap center={[37.3545, 126.7099]} />                          지도만
 *   <KakaoMap markers={[{ id: 1, position: [37.35, 126.71], label: '옥구공원' }]} fit />
 *   <KakaoMap path={course.path} showEnds fit />                       경로 선 + 출발 · 도착 점
 *
 * props
 *   center      처음 가운데 좌표 (fit 이면 무시)
 *   level       확대 수준 (작을수록 가까이. 기본 5)
 *   markers     [{ id, position, label?, onClick? }]  label 은 선택된 마커 위에 말풍선으로 보임
 *   selectedId  선택된 마커 id (크게 + 말풍선)
 *   path        경로 좌표 배열 → 초록 선
 *   showEnds    경로의 출발(초록) · 도착(빨강) 점 표시
 *   fit         마커 · 경로가 모두 보이게 자동으로 맞춤
 *   interactive false 면 끌기 · 확대 막음 (카드 미리보기용)
 *   className   크기는 여기서 정함 (예: .c1-map { width: 760px; height: 780px; })
 */

// 시흥 정왕동 근처 (아무 좌표도 없을 때 가운데)
const DEFAULT_CENTER = [37.3496, 126.7206]
const HAS_KEY = Boolean(import.meta.env.VITE_KAKAO_JS_KEY)

function sdkReady() {
  return HAS_KEY && typeof window !== 'undefined' && Boolean(window.kakao?.maps)
}

export default function KakaoMap({
  center = DEFAULT_CENTER,
  level = 5,
  markers = [],
  selectedId = null,
  path = null,
  showEnds = false,
  fit = false,
  interactive = true,
  className = '',
}) {
  const elRef = useRef(null)
  const mapRef = useRef(null)
  const [ready, setReady] = useState(false)
  const [failed] = useState(() => !sdkReady())

  // 1) 지도 만들기 (처음 한 번)
  useEffect(() => {
    if (failed) return
    const { kakao } = window
    let alive = true
    kakao.maps.load(() => {
      if (!alive || !elRef.current) return
      const map = new kakao.maps.Map(elRef.current, {
        center: new kakao.maps.LatLng(center[0], center[1]),
        level,
        draggable: interactive,
        scrollwheel: interactive,
        disableDoubleClickZoom: !interactive,
      })
      if (interactive) {
        map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.TOPRIGHT)
      }
      mapRef.current = map
      setReady(true)
    })
    return () => {
      alive = false
      mapRef.current = null
    }
    // 처음 만들 때만 center · level 을 씀 (바뀌면 아래 2번에서 처리)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [failed])

  // 2) 마커 · 경로 그리기 (값이 바뀔 때마다 지우고 다시 그림)
  useEffect(() => {
    const map = mapRef.current
    if (!ready || !map) return
    const { kakao } = window
    const toLatLng = ([lat, lng]) => new kakao.maps.LatLng(lat, lng)
    const drawn = [] // 나중에 지울 것들
    const bounds = new kakao.maps.LatLngBounds()
    let hasBounds = false

    // 경로 선
    if (path && path.length > 1) {
      const line = new kakao.maps.Polyline({
        path: path.map(toLatLng),
        strokeWeight: 5,
        strokeColor: '#2e7d5b',
        strokeOpacity: 0.9,
        strokeStyle: 'solid',
      })
      line.setMap(map)
      drawn.push(line)
      path.forEach((p) => bounds.extend(toLatLng(p)))
      hasBounds = true

      if (showEnds) {
        ;[
          [path[0], 'kmap-dot kmap-dot-start'],
          [path[path.length - 1], 'kmap-dot kmap-dot-end'],
        ].forEach(([p, cls]) => {
          const el = document.createElement('span')
          el.className = cls
          const overlay = new kakao.maps.CustomOverlay({ position: toLatLng(p), content: el, zIndex: 3 })
          overlay.setMap(map)
          drawn.push(overlay)
        })
      }
    }

    // 마커 (초록 동그라미). 선택된 마커는 크게 + 말풍선
    markers.forEach((m) => {
      const selected = m.id === selectedId
      const el = document.createElement('div')
      el.className = 'kmap-pin' + (selected ? ' is-selected' : '') + (m.onClick ? ' is-clickable' : '')
      if (selected && m.label) {
        const tip = document.createElement('div')
        tip.className = 'kmap-tip'
        tip.textContent = m.label
        el.appendChild(tip)
      }
      const dot = document.createElement('span')
      dot.className = 'kmap-pin-dot'
      el.appendChild(dot)
      if (m.onClick) el.addEventListener('click', () => m.onClick(m.id))

      const overlay = new kakao.maps.CustomOverlay({
        position: toLatLng(m.position),
        content: el,
        yAnchor: 1,
        zIndex: selected ? 5 : 2,
        clickable: true,
      })
      overlay.setMap(map)
      drawn.push(overlay)
      bounds.extend(toLatLng(m.position))
      hasBounds = true
    })

    if (fit && hasBounds) {
      if (markers.length === 1 && !path) {
        map.setLevel(level)
        map.setCenter(toLatLng(markers[0].position))
      } else {
        map.setBounds(bounds, 40, 40, 40, 40)
      }
    } else {
      map.setCenter(toLatLng(center))
    }

    return () => drawn.forEach((d) => d.setMap(null))
    // center 는 배열이라 매번 새로 만들어지므로 값으로 비교
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready, markers, selectedId, path, showEnds, fit, level, center[0], center[1]])

  if (failed) {
    return (
      <div className={`kmap kmap-fallback ${className}`}>
        {interactive && (
          <p className="t-b14m c-muted tc">
            지도를 불러오지 못했어요
            <br />
            <span className="t-c12">
              {HAS_KEY
                ? '카카오 도메인 등록 · 인터넷 연결을 확인하세요'
                : 'frontend/.env.local 에 VITE_KAKAO_JS_KEY 를 넣어 주세요'}
            </span>
          </p>
        )}
      </div>
    )
  }

  return <div ref={elRef} className={`kmap ${className}`} />
}
