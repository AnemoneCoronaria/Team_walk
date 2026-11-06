// 가짜 관리자 데이터 — db/seed.sql 을 넣은 직후 GET /api/admin/... 응답과 같은 값 (날짜만 고정)
// 관리자 화면에서 처리 · 정지 · 숨김을 누르면 이 배열이 바뀜 (새로고침하면 처음으로)

// AD1 신고 (GET /api/admin/reports 의 items)
export const ADMIN_REPORTS = [
  {
    reportId: 4,
    targetType: 'MATCH',
    targetId: 5,
    targetLabel: '옥구산 정상까지 빠르게 걸어요',
    reason: 'INAPPROPRIATE',
    detail: '글에 연락처를 적어 달라고 해요',
    reporter: { userId: 3, nickname: '정란' },
    targetReportCount: 1,
    status: 'DONE',
    action: 'WARN',
    adminMemo: '처음이라 경고',
    createdAt: '2026-11-06T13:00:00',
  },
  {
    reportId: 3,
    targetType: 'COURSE',
    targetId: 2,
    targetLabel: '정왕역 동네 한 바퀴',
    reason: 'WRONG_INFO',
    detail: '실제 길과 경로가 달라요',
    reporter: { userId: 6, nickname: '승민' },
    targetReportCount: 1,
    status: 'PENDING',
    action: null,
    adminMemo: null,
    createdAt: '2026-11-06T12:00:00',
  },
  {
    reportId: 1,
    targetType: 'REVIEW',
    targetId: 4,
    targetLabel: '옥구공원 순환 코스',
    reason: 'SPAM',
    detail: '광고 링크가 있어요',
    reporter: { userId: 3, nickname: '정란' },
    targetReportCount: 1,
    status: 'DONE',
    action: 'HIDE',
    adminMemo: '광고 확인, 숨김 처리',
    createdAt: '2026-11-04T15:00:00',
  },
  {
    reportId: 2,
    targetType: 'USER',
    targetId: 7,
    targetLabel: '정지회원 (banned)',
    reason: 'RUDE',
    detail: '댓글로 욕설을 했어요',
    reporter: { userId: 5, nickname: '동훈' },
    targetReportCount: 1,
    status: 'DONE',
    action: 'SUSPEND',
    adminMemo: '욕설 확인, 7일 정지',
    createdAt: '2026-11-03T15:00:00',
  },
]

// AD2 회원 (GET /api/admin/users 의 items)
export const ADMIN_USERS = [
  { userId: 1, nickname: '관리자', loginId: 'admin', ageGroup: '30s', languages: ['KO'], matchCount: 0, reportCount: 0, status: 'ACTIVE', role: 'ADMIN', warningCount: 0, suspendedUntil: null, createdAt: '2026-10-23T10:00:00' },
  { userId: 2, nickname: '민서', loginId: 'minseo', ageGroup: '20s', languages: ['EN', 'KO'], matchCount: 1, reportCount: 0, status: 'ACTIVE', role: 'USER', warningCount: 0, suspendedUntil: null, createdAt: '2026-10-23T10:00:00' },
  { userId: 3, nickname: '정란', loginId: 'jeongran', ageGroup: '20s', languages: ['KO'], matchCount: 1, reportCount: 0, status: 'ACTIVE', role: 'USER', warningCount: 0, suspendedUntil: null, createdAt: '2026-10-23T10:00:00' },
  { userId: 4, nickname: '동언', loginId: 'dongeon', ageGroup: '20s', languages: ['KO'], matchCount: 0, reportCount: 0, status: 'ACTIVE', role: 'USER', warningCount: 0, suspendedUntil: null, createdAt: '2026-10-23T10:00:00' },
  { userId: 5, nickname: '동훈', loginId: 'donghun', ageGroup: '20s', languages: ['KO', 'ZH'], matchCount: 0, reportCount: 0, status: 'ACTIVE', role: 'USER', warningCount: 1, suspendedUntil: null, createdAt: '2026-10-23T10:00:00' },
  { userId: 6, nickname: '승민', loginId: 'seungmin', ageGroup: '20s', languages: ['EN', 'KO'], matchCount: 1, reportCount: 0, status: 'ACTIVE', role: 'USER', warningCount: 0, suspendedUntil: null, createdAt: '2026-10-23T10:00:00' },
  { userId: 7, nickname: '정지회원', loginId: 'banned', ageGroup: '30s', languages: ['KO'], matchCount: 0, reportCount: 1, status: 'SUSPENDED', role: 'USER', warningCount: 0, suspendedUntil: '2026-11-11T15:00:00', createdAt: '2026-10-23T10:00:00' },
]

// AD4 코스 (GET /api/admin/courses 의 items)
export const ADMIN_COURSES = [
  { courseId: 2, name: '정왕역 동네 한 바퀴', author: { userId: 2, nickname: '민서' }, official: false, distanceKm: 2.69, level: 'EASY', downloadCount: 3, reviewCount: 0, visibility: 'PUBLIC', hidden: false, pendingReports: 1 },
  { courseId: 4, name: '오이도 빨강등대 코스', author: { userId: 1, nickname: '관리자' }, official: true, distanceKm: 2.04, level: 'EASY', downloadCount: 41, reviewCount: 2, visibility: 'PUBLIC', hidden: false, pendingReports: 0 },
  { courseId: 3, name: '배곧생명공원 산책 코스', author: { userId: 3, nickname: '정란' }, official: false, distanceKm: 2.52, level: 'EASY', downloadCount: 15, reviewCount: 2, visibility: 'PUBLIC', hidden: false, pendingReports: 0 },
  { courseId: 1, name: '옥구공원 순환 코스', author: { userId: 4, nickname: '동언' }, official: false, distanceKm: 1.43, level: 'NORMAL', downloadCount: 24, reviewCount: 1, visibility: 'PUBLIC', hidden: false, pendingReports: 0 },
]

// AD3 모집 글 (GET /api/admin/matches 의 items). courseName 은 가짜 데이터의 검색 흉내용 (응답에는 없음)
export const ADMIN_MATCHES = [
  { matchId: 6, title: '지난 주말 배곧 산책 모임', host: { userId: 2, nickname: '민서', avatar: 'seagull' }, courseName: '배곧생명공원 산책 코스', meetAt: '2026-11-02T10:00:00', targets: [], current: 3, capacity: 4, status: 'DONE', hidden: false },
  { matchId: 5, title: '옥구산 정상까지 빠르게 걸어요', host: { userId: 5, nickname: '동훈', avatar: 'dog' }, courseName: '옥구공원 순환 코스', meetAt: '2026-11-18T07:00:00', targets: [], current: 2, capacity: 5, status: 'CANCELED', hidden: false },
  { matchId: 4, title: '정왕역 저녁 산책 (ISFP 모여라)', host: { userId: 6, nickname: '승민', avatar: 'cat' }, courseName: '정왕역 동네 한 바퀴', meetAt: '2026-11-12T20:00:00', targets: ['MBTI'], current: 2, capacity: 4, status: 'CLOSED', hidden: false },
  { matchId: 3, title: '오이도 노을 보러 같이 가요', host: { userId: 4, nickname: '동언', avatar: 'lighthouse' }, courseName: '오이도 빨강등대 코스', meetAt: '2026-11-15T16:30:00', targets: ['SAME_LANGUAGE'], current: 3, capacity: 3, status: 'FULL', hidden: false },
  { matchId: 2, title: '주말 아침 배곧생명공원 산책해요', host: { userId: 3, nickname: '정란', avatar: 'lotus' }, courseName: '배곧생명공원 산책 코스', meetAt: '2026-11-14T08:00:00', targets: [], current: 1, capacity: 6, status: 'OPEN', hidden: false },
  { matchId: 1, title: '퇴근 후 옥구공원 한 바퀴 같이 걸어요', host: { userId: 2, nickname: '민서', avatar: 'seagull' }, courseName: '옥구공원 순환 코스', meetAt: '2026-11-11T19:00:00', targets: ['EXCHANGE'], current: 2, capacity: 4, status: 'OPEN', hidden: false },
]
