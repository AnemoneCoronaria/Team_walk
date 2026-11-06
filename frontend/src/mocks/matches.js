// 가짜 동행 데이터 — db/seed.sql 의 동행 6개 + 화면 확인용 2개
// 응답 모양은 노션 API 명세의 GET /api/matches (민서가 만든 MatchSummary 와 같은 칸)
// 날짜는 seed 처럼 "오늘 기준 며칠 뒤"로 계산해서, 언제 실행해도 모집 중으로 보임
import { USERS } from './users.js'
import { toDateParam } from '../utils/format.js'

function daysFromToday(days, time) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return `${toDateParam(d)}T${time}`
}

const COURSE_BRIEF = {
  1: { courseId: 1, name: '옥구공원 순환 코스', distanceKm: 1.43, level: 'NORMAL' },
  2: { courseId: 2, name: '정왕역 동네 한 바퀴', distanceKm: 2.69, level: 'EASY' },
  3: { courseId: 3, name: '배곧생명공원 산책 코스', distanceKm: 2.52, level: 'EASY' },
  4: { courseId: 4, name: '오이도 빨강등대 코스', distanceKm: 2.04, level: 'EASY' },
}

// ageGroups · area 는 목록 응답에는 없는 칸 — 가짜 데이터에서 필터를 흉내 내려고 넣어 둔 것 (화면에서는 안 씀)
// participants 는 카드의 참여자 아바타용 (목록 응답에는 없음. 아래 MatchCard 설명 참고)
export const MATCHES = [
  {
    matchId: 1,
    title: '퇴근 후 옥구공원 한 바퀴 같이 걸어요',
    meetAt: daysFromToday(5, '19:00:00'),
    course: COURSE_BRIEF[1],
    target: 'EXCHANGE',
    styles: ['SLOW'],
    languages: ['KO', 'ZH'],
    current: 2,
    capacity: 4,
    status: 'OPEN',
    host: USERS[2],
    ageGroups: ['20s', '30s'],
    area: 'OKGU',
    participants: [USERS[2], USERS[5]],
  },
  {
    matchId: 2,
    title: '주말 아침 배곧생명공원 산책해요',
    meetAt: daysFromToday(8, '08:00:00'),
    course: COURSE_BRIEF[3],
    target: 'ANYONE',
    styles: ['CHAT'],
    languages: ['KO'],
    current: 1,
    capacity: 6,
    status: 'OPEN',
    host: USERS[3],
    ageGroups: [],
    area: 'BAEGOT',
    participants: [USERS[3]],
  },
  {
    matchId: 3,
    title: '오이도 노을 보러 같이 가요',
    meetAt: daysFromToday(9, '16:30:00'),
    course: COURSE_BRIEF[4],
    target: 'SAME_LANGUAGE',
    styles: ['SLOW'],
    languages: ['KO'],
    current: 3,
    capacity: 3,
    status: 'FULL',
    host: USERS[4],
    ageGroups: ['20s'],
    area: 'SIHWA',
    participants: [USERS[4], USERS[2], USERS[6]],
  },
  {
    matchId: 4,
    title: '정왕역 저녁 산책 (ISFP 모여라)',
    meetAt: daysFromToday(6, '20:00:00'),
    course: COURSE_BRIEF[2],
    target: 'MBTI',
    styles: [],
    languages: ['KO'],
    current: 2,
    capacity: 4,
    status: 'CLOSED',
    host: USERS[6],
    ageGroups: [],
    area: 'JEONGWANG',
    participants: [USERS[6], USERS[3]],
  },
  {
    matchId: 5,
    title: '옥구산 정상까지 빠르게 걸어요',
    meetAt: daysFromToday(12, '07:00:00'),
    course: COURSE_BRIEF[1],
    target: 'ANYONE',
    styles: ['FAST'],
    languages: ['KO', 'ZH'],
    current: 2,
    capacity: 5,
    status: 'CANCELED',
    host: USERS[5],
    ageGroups: [],
    area: 'OKGU',
    participants: [USERS[5], USERS[4]],
  },
  {
    matchId: 6,
    title: '지난 주말 배곧 산책 모임',
    meetAt: daysFromToday(-4, '10:00:00'),
    course: COURSE_BRIEF[3],
    target: 'ANYONE',
    styles: ['CHAT'],
    languages: ['KO'],
    current: 3,
    capacity: 4,
    status: 'DONE',
    host: USERS[2],
    ageGroups: [],
    area: 'BAEGOT',
    participants: [USERS[2], USERS[3], USERS[6]],
  },
  // ----- 아래 2개는 seed.sql 에 없음: 메인 · 동행 찾기에 카드가 여러 개 보이게 넣은 화면 확인용 -----
  {
    matchId: 101,
    title: '一起散步吧! 中文 OK 🙌',
    meetAt: daysFromToday(3, '20:00:00'),
    course: COURSE_BRIEF[2],
    target: 'EXCHANGE',
    styles: ['SLOW', 'CHAT'],
    languages: ['KO', 'ZH'],
    current: 2,
    capacity: 4,
    status: 'OPEN',
    host: USERS[5],
    ageGroups: ['20s'],
    area: 'JEONGWANG',
    participants: [USERS[5], USERS[6]],
  },
  {
    matchId: 102,
    title: '오이도 빨강등대 사진 산책',
    meetAt: daysFromToday(4, '17:00:00'),
    course: COURSE_BRIEF[4],
    target: 'ANYONE',
    styles: ['PHOTO', 'SLOW'],
    languages: ['KO', 'EN'],
    current: 1,
    capacity: 4,
    status: 'OPEN',
    host: USERS[6],
    ageGroups: ['20s', '30s'],
    area: 'SIHWA',
    participants: [USERS[6]],
  },
]

// ---------------------------------------------------------------------
// 2주차: 동행 상세 (GET /api/matches/{id}) 에만 있는 칸 — db/seed.sql 의 matches 와 같은 내용
// 응답 모양은 민서의 feature/match-week2 브랜치 MatchService.detail 기준
//   (meetPoint 는 { lat, lng }, 소개글 칸 이름은 description)
// ---------------------------------------------------------------------
export const MATCH_EXTRA = {
  1: { meetPlace: '옥구공원 정문 주차장 앞', description: '퇴근하고 가볍게 걸을 분 구해요! 천천히 이야기하면서 걸어요. 欢迎说中文的朋友！', mbti: null, hasGpx: true },
  2: { meetPlace: '배곧생명공원 주차장 입구', description: '토요일 아침에 상쾌하게 걸어요. 누구나 환영해요.', mbti: null, hasGpx: false },
  3: { meetPlace: '오이도 빨강등대 앞', description: '해 질 무렵 빨강등대 앞에서 만나요. 사진도 같이 찍어요.', mbti: null, hasGpx: false },
  4: { meetPlace: '정왕역 앞', description: '조용히 걷는 걸 좋아하는 분이면 좋겠어요.', mbti: 'ISFP', hasGpx: false },
  5: { meetPlace: '옥구공원 정문', description: '운동 삼아 빠르게 걸을 분! 아침 7시에 만나요.', mbti: null, hasGpx: false },
  6: { meetPlace: '배곧생명공원 주차장 입구', description: '다녀왔어요! 다음에도 같이 걸어요.', mbti: null, hasGpx: false },
  101: { meetPlace: '정왕역 2번 출구 앞', description: '中文聊天也可以！한국어 연습하고 싶은 분도 환영해요.', mbti: null, hasGpx: false },
  102: { meetPlace: '오이도 빨강등대 앞', description: '노을 질 때 사진 찍으면서 천천히 걸어요. 카메라 없어도 괜찮아요.', mbti: null, hasGpx: false },
}

// 회원의 언어 · 연령대 · MBTI (seed 의 user_tags · users) — 참여자 칸과 참여 조건 검사에 씀
export const USER_PROFILE = {
  1: { languages: ['KO'], ageGroup: '30s', mbti: null },
  2: { languages: ['KO', 'EN'], ageGroup: '20s', mbti: null },
  3: { languages: ['KO'], ageGroup: '20s', mbti: 'ENFP' },
  4: { languages: ['KO'], ageGroup: '20s', mbti: 'INTJ' },
  5: { languages: ['KO', 'ZH'], ageGroup: '20s', mbti: null },
  6: { languages: ['KO', 'EN'], ageGroup: '20s', mbti: 'ISFP' },
}

// 참여 상태 (seed 의 match_participants 중 JOINED 가 아닌 사람). 키: `${matchId}-${userId}`
export const PARTICIPANT_STATUS = {
  '2-6': 'KICKED',
  '6-5': 'LEFT',
}

// 참여 취소한 사람과 사유 (seed 의 match_participants 중 LEFT, 모집자 화면용)
export const MATCH_LEFT = {
  6: [{ userId: 5, nickname: '동훈', avatar: 'dog', reason: '일정이 생겼어요', leftAt: daysFromToday(-6, '12:00:00') }],
}

// 댓글 (seed 의 match_comments)
export const MATCH_COMMENTS = {
  1: [
    { commentId: 1, author: USERS[2], content: '정문 주차장 앞 벤치에서 7시에 만나요!', isNotice: true, createdAt: daysFromToday(-1, '21:10:00') },
    { commentId: 2, author: USERS[5], content: '저 中文 조금 할 수 있어요. 같이 연습해요!', isNotice: false, createdAt: daysFromToday(-1, '22:10:00') },
    { commentId: 3, author: USERS[2], content: '좋아요! 편하게 오세요 :)', isNotice: false, createdAt: daysFromToday(-1, '23:10:00') },
  ],
  3: [{ commentId: 4, author: USERS[6], content: '사진 찍기 좋은 자리 알아 올게요', isNotice: false, createdAt: daysFromToday(-1, '14:00:00') }],
}

export { daysFromToday, COURSE_BRIEF }
