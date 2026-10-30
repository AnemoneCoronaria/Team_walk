// 가짜 회원 — db/seed.sql 의 회원과 같은 번호 · 닉네임 · 아바타
// 실제 서버(VITE_USE_MOCK=false)로 바꿔도 같은 사람이 보이도록 맞춰 둠

export const USERS = {
  1: { userId: 1, nickname: '관리자', avatar: 'sogeumi' },
  2: { userId: 2, nickname: '민서', avatar: 'seagull' },
  3: { userId: 3, nickname: '정란', avatar: 'lotus' },
  4: { userId: 4, nickname: '동언', avatar: 'lighthouse' },
  5: { userId: 5, nickname: '동훈', avatar: 'dog' },
  6: { userId: 6, nickname: '승민', avatar: 'cat' },
}

// 로그인 아이디 → 회원 번호 (db/seed.sql 테스트 계정, 비밀번호는 모두 test1234!)
// 7번 banned 는 정지 테스트용이라 가짜 로그인에서도 403 SUSPENDED
export const LOGIN_IDS = { admin: 1, minseo: 2, jeongran: 3, dongeon: 4, donghun: 5, seungmin: 6, banned: 7 }
export const MOCK_PASSWORD = 'test1234!'

// GET /api/users/me 응답 (로그인한 사람 = 민서)
export const ME = {
  userId: 2,
  loginId: 'minseo',
  nickname: '민서',
  avatar: 'seagull',
  ageGroup: '20s',
  styles: ['SLOW', 'CHAT'],
  languages: ['KO', 'EN'],
  mbti: null,
  agreeLocation: true,
  role: 'USER',
  matchCount: 1,
  courseCount: 1,
  totalDistanceKm: 2.5,
  createdAt: '2026-10-22T10:00:00',
}

// GET /api/users/me/stamps 응답 (민서: 3곳)
export const MY_STAMPS = {
  stampCount: 3,
  totalLandmarks: 8,
  stamps: [
    { landmarkId: 2, name: '오이도 빨강등대', stampedAt: '2026-10-19T18:00:00' },
    { landmarkId: 3, name: '오이도 함상전망대', stampedAt: '2026-10-19T18:20:00' },
    { landmarkId: 1, name: '옥구공원', stampedAt: '2026-10-26T20:10:00' },
  ],
  badges: [
    { code: 'FIRST_WALK', name: '첫 산책', earnedAt: '2026-10-19T18:00:00' },
    { code: 'LANDMARK_3', name: '랜드마크 3곳', earnedAt: '2026-10-26T20:10:00' },
  ],
}
