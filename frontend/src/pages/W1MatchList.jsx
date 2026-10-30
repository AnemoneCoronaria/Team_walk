import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import MatchCard from '../components/MatchCard.jsx'
import { getMatches } from '../api/matchApi.js'
import { useAuth } from '../auth/authContext.js'
import { useAsync } from '../utils/useAsync.js'
import { toDateParam } from '../utils/format.js'
import {
  AGE_LABELS,
  AREA_LABELS,
  LANGUAGE_LABELS,
  STYLE_LABELS,
  TARGET_LABELS,
  TIME_SLOT_LABELS,
} from '../constants/labels.js'
import './W1MatchList.css'

const PAGE_SIZE = 20
// W1 필터에 보이는 칩 (Figma W1 · 명세 값 약속)
const STYLE_CHIPS = ['SLOW', 'FAST', 'QUIET', 'CHAT', 'PHOTO']
const LANG_CHIPS = ['KO', 'ZH', 'EN', 'VI']

// 주소(?date=weekend&style=SLOW,CHAT) ↔ 필터 값
function readFilter(sp) {
  const list = (k) => (sp.get(k) ? sp.get(k).split(',') : [])
  return {
    date: sp.get('date') ?? '',
    timeSlot: sp.get('timeSlot') ?? '',
    style: list('style'),
    lang: list('lang'),
    target: sp.get('target') ?? '',
    age: sp.get('age') ?? '',
    area: sp.get('area') ?? '',
  }
}

// 날짜 칩 → from, to (명세: 오늘 = 오늘~오늘, 이번 주말 = 토~일, 직접 선택 = 그날~그날)
function dateRange(date) {
  const today = new Date()
  if (date === 'today') return { from: toDateParam(today), to: toDateParam(today) }
  if (date === 'weekend') {
    const day = today.getDay() // 0 일요일 … 6 토요일
    const sat = new Date(today)
    sat.setDate(today.getDate() + (day === 0 ? -1 : 6 - day))
    const sun = new Date(sat)
    sun.setDate(sat.getDate() + 1)
    return { from: toDateParam(day === 0 ? today : sat), to: toDateParam(sun) }
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(date)) return { from: date, to: date }
  return {}
}

function dateLabel(date) {
  if (date === 'today') return '오늘'
  if (date === 'weekend') return '이번 주말'
  return date ? `${date.slice(5, 7)}/${date.slice(8, 10)}` : ''
}

/**
 * W1 동행 찾기 (목록 + 태그 필터) · E1 결과 없음
 * API: GET /api/matches?from=&to=&timeSlot=&style=&lang=&target=&age=&area=&sort=&page=&size=
 * 왼쪽 필터는 "적용"을 눌러야 검색됨. 적용된 필터는 주소에 남아서 뒤로 가기 · 새로고침에도 유지
 */
export default function W1MatchList() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const applied = readFilter(searchParams)
  const sort = searchParams.get('sort') ?? 'deadline'
  const page = Number(searchParams.get('page') ?? 1)

  const params = {
    ...dateRange(applied.date),
    timeSlot: applied.timeSlot,
    style: applied.style,
    lang: applied.lang,
    target: applied.target,
    age: applied.age,
    area: applied.area,
    sort,
    page,
    size: PAGE_SIZE,
  }
  const { data, error, loading } = useAsync(() => getMatches(params), JSON.stringify(params))

  // 필터 · 정렬 · 페이지를 주소에 씀 (빈 값은 뺌)
  function writeParams(filter, extra = {}) {
    const next = new URLSearchParams()
    Object.entries({ ...filter, sort: extra.sort ?? sort, page: extra.page ?? 1 }).forEach(([k, v]) => {
      const value = Array.isArray(v) ? v.join(',') : String(v ?? '')
      if (value && !(k === 'sort' && value === 'deadline') && !(k === 'page' && value === '1')) next.set(k, value)
    })
    setSearchParams(next)
  }

  const writeLink = user ? '/matches/new' : '/login' // 로그인 전이면 G1(로그인 필요) → 지금은 로그인 화면으로
  const chips = appliedChips(applied, (filter) => writeParams(filter))
  const items = data?.items ?? []
  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  // E1: 결과 없음 (필터 영역 없이 가운데 안내)
  if (!loading && !error && items.length === 0) {
    return (
      <main className="e1">
        <p className="t-display48 c-strong nowrap">🚶</p>
        <p className="t-h24 c-strong nowrap">조건에 맞는 동행이 아직 없어요</p>
        <p className="t-b16 c-body nowrap">필터를 조금 넓혀 보거나, 직접 모집 글을 올려 먼저 이웃을 초대해 보세요.</p>
        {chips.length > 0 && <div className="row g6">{chips}</div>}
        <div className="row g12">
          <button type="button" className="btn btn-secondary" onClick={() => setSearchParams({})}>
            필터 초기화
          </button>
          <Link className="btn btn-primary" to={writeLink}>
            + 직접 모집하기
          </Link>
        </div>
      </main>
    )
  }

  return (
    <main>
      <section className="w1-top">
        <div className="wrap row jc-b ai-c">
          <div className="col g4">
            <h1 className="t-h32 c-strong nowrap">동행 찾기</h1>
            <p className="t-b16 c-body nowrap">산책 스타일과 언어가 맞는 이웃의 모집 글에 참여해 보세요</p>
          </div>
          <Link className="btn btn-primary" to={writeLink}>
            + 모집 글 쓰기
          </Link>
        </div>
      </section>

      <section className="w1-body">
        <div className="wrap row g24">
          {/* key: 주소의 필터가 바뀌면(✕로 지우기 등) 왼쪽 선택 상태도 새로 맞춤 */}
          <FilterPanel
            key={searchParams.toString()}
            applied={applied}
            onApply={(filter) => writeParams(filter)}
            onReset={() => setSearchParams({})}
          />

          <div className="col g16 grow">
            <div className="row jc-b ai-c fill">
              <div className="row ai-c g6 fwrap">
                <span className="t-b14m c-muted nowrap">적용된 필터</span>
                {chips.length ? chips : <span className="t-b14 c-muted">없음</span>}
              </div>
              <select
                className="w1-sort t-b14m c-body"
                value={sort}
                onChange={(e) => writeParams(applied, { sort: e.target.value })}
              >
                <option value="deadline">마감 임박순</option>
                <option value="recent">최신순</option>
              </select>
            </div>

            {error && <p className="t-b14 c-danger">동행 목록을 불러오지 못했어요. ({error.message})</p>}
            {loading
              ? [1, 2, 3].map((i) => <div key={i} className="panel w1-skel" />)
              : items.map((m) => <MatchCard key={m.matchId} match={m} className="w1-card" />)}

            {!loading && (
              <div className="row jc-c g8 fill">
                <button
                  type="button"
                  className="chip chip-neutral"
                  disabled={page <= 1}
                  onClick={() => writeParams(applied, { page: page - 1 })}
                >
                  ‹
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
                  <button
                    key={n}
                    type="button"
                    className={`chip chip-neutral ${n === page ? 'is-selected' : ''}`}
                    onClick={() => writeParams(applied, { page: n })}
                  >
                    {n}
                  </button>
                ))}
                <button
                  type="button"
                  className="chip chip-neutral"
                  disabled={page >= totalPages}
                  onClick={() => writeParams(applied, { page: page + 1 })}
                >
                  ›
                </button>
              </div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}

// 왼쪽 필터 (적용을 누르기 전까지는 이 안에서만 바뀜)
function FilterPanel({ applied, onApply, onReset }) {
  const [draft, setDraft] = useState(applied)
  const [pickDate, setPickDate] = useState(/^\d{4}/.test(applied.date))

  const single = (key, value) => setDraft((d) => ({ ...d, [key]: d[key] === value ? '' : value }))
  const multi = (key, value) =>
    setDraft((d) => ({ ...d, [key]: d[key].includes(value) ? d[key].filter((v) => v !== value) : [...d[key], value] }))

  function pickPreset(value) {
    setPickDate(false)
    single('date', value)
  }
  function togglePickDate() {
    setPickDate(!pickDate)
    setDraft((d) => ({ ...d, date: '' }))
  }

  return (
    <div className="panel col g24 w1-filter">
      <p className="t-h20 c-strong">필터</p>

      <Group title="날짜">
        <Chip on={draft.date === 'today'} onClick={() => pickPreset('today')}>
          오늘
        </Chip>
        <Chip on={draft.date === 'weekend'} onClick={() => pickPreset('weekend')}>
          이번 주말
        </Chip>
        <Chip on={pickDate} onClick={togglePickDate}>
          직접 선택
        </Chip>
        {pickDate && (
          <input
            type="date"
            className="input-field w1-date"
            value={/^\d{4}/.test(draft.date) ? draft.date : ''}
            min={toDateParam(new Date())}
            onChange={(e) => setDraft((d) => ({ ...d, date: e.target.value }))}
          />
        )}
      </Group>

      <Group title="시간대">
        {Object.entries(TIME_SLOT_LABELS).map(([code, label]) => (
          <Chip key={code} on={draft.timeSlot === code} onClick={() => single('timeSlot', code)}>
            {label}
          </Chip>
        ))}
      </Group>

      <Group title="산책 스타일">
        {STYLE_CHIPS.map((code) => (
          <Chip key={code} tone="green" on={draft.style.includes(code)} onClick={() => multi('style', code)}>
            {STYLE_LABELS[code]}
          </Chip>
        ))}
      </Group>

      <Group title="언어">
        {LANG_CHIPS.map((code) => (
          <Chip key={code} tone="blue" on={draft.lang.includes(code)} onClick={() => multi('lang', code)}>
            {LANGUAGE_LABELS[code]}
          </Chip>
        ))}
      </Group>

      <Group title="모집 대상">
        {Object.entries(TARGET_LABELS).map(([code, label]) => (
          <Chip key={code} on={draft.target === code} onClick={() => single('target', code)}>
            {label}
          </Chip>
        ))}
      </Group>

      <Group title="연령대">
        {Object.entries(AGE_LABELS).map(([code, label]) => (
          <Chip key={code} on={draft.age === code} onClick={() => single('age', code)}>
            {label}
          </Chip>
        ))}
      </Group>

      <Group title="지역">
        {Object.entries(AREA_LABELS).map(([code, label]) => (
          <Chip key={code} on={draft.area === code} onClick={() => single('area', code)}>
            {label}
          </Chip>
        ))}
      </Group>

      <div className="row g8 fill">
        <button type="button" className="btn btn-ghost" onClick={onReset}>
          초기화
        </button>
        <button type="button" className="btn btn-primary grow" onClick={() => onApply(draft)}>
          적용
        </button>
      </div>
    </div>
  )
}

function Group({ title, children }) {
  return (
    <div className="col g8 fill">
      <p className="t-b14m c-strong">{title}</p>
      <div className="row fwrap g6 fill w1-chips">{children}</div>
    </div>
  )
}

function Chip({ on, onClick, tone = 'neutral', children }) {
  return (
    <button type="button" className={`chip chip-${tone} ${on ? 'is-selected' : ''}`} onClick={onClick}>
      {children}
    </button>
  )
}

// "적용된 필터" 칩들 (✕ 누르면 그 조건만 지움)
function appliedChips(filter, write) {
  const chips = []
  const remove = (key, value) => () =>
    write({ ...filter, [key]: Array.isArray(filter[key]) ? filter[key].filter((v) => v !== value) : '' })
  const add = (key, value, label, tone = 'neutral') =>
    chips.push(
      <button key={`${key}-${value}`} type="button" className={`chip chip-${tone}`} onClick={remove(key, value)}>
        {label} ✕
      </button>,
    )

  if (filter.date) add('date', filter.date, dateLabel(filter.date))
  if (filter.timeSlot) add('timeSlot', filter.timeSlot, TIME_SLOT_LABELS[filter.timeSlot])
  filter.style.forEach((s) => add('style', s, STYLE_LABELS[s] ?? s, 'green'))
  filter.lang.forEach((l) => add('lang', l, LANGUAGE_LABELS[l] ?? l, 'blue'))
  if (filter.target) add('target', filter.target, TARGET_LABELS[filter.target])
  if (filter.age) add('age', filter.age, AGE_LABELS[filter.age])
  if (filter.area) add('area', filter.area, AREA_LABELS[filter.area])
  return chips
}

