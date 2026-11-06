// 신고 API (백엔드 담당: 정란, 3주차 · 아직 없음 → 가짜 데이터) — W6 신고하기 · C2-1 코스 신고
//   POST /api/reports  body: { targetType: MATCH | USER | REVIEW | COURSE, targetId, reason, detail? }
//   reason 코드는 constants/labels.js 의 REPORT_REASON_LABELS (코스는 COURSE_REPORT_REASON_LABELS)
//   같은 사람이 같은 대상을 두 번 신고하면 409 DUPLICATE (db/schema.sql reports 의 uk_reports_once)
import { USE_MOCK, request, mockResponse, mockError } from './client.js'

// 가짜 데이터: 이번 화면에서 신고한 대상 (새로고침하면 사라짐)
const mockReported = new Set()

export function createReport({ targetType, targetId, reason, detail }) {
  if (USE_MOCK) {
    const key = `${targetType}-${targetId}`
    if (mockReported.has(key)) return mockError(409, 'DUPLICATE', '이미 신고한 대상이에요.')
    mockReported.add(key)
    return mockResponse({ reportId: mockReported.size })
  }
  return request('/api/reports', { method: 'POST', body: { targetType, targetId, reason, detail: detail || null } })
}
