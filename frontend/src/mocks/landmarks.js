// 가짜 랜드마크 — db/seed.sql 의 랜드마크 8곳과 같은 번호 · 이름 · 좌표 (인증 반경 300m)
// 응답 모양은 GET /api/landmarks 의 items 한 줄 (stamped · stampedAt 은 api/landmarkApi.js 가 붙임)
export const LANDMARKS = [
  { landmarkId: 1, name: '옥구공원', area: 'OKGU', point: [37.354517, 126.709916], radiusM: 300 },
  { landmarkId: 2, name: '오이도 빨강등대', area: 'SIHWA', point: [37.345543, 126.687492], radiusM: 300 },
  { landmarkId: 3, name: '오이도 함상전망대', area: 'SIHWA', point: [37.340575, 126.688045], radiusM: 300 },
  { landmarkId: 4, name: '오이도 선사유적공원', area: 'SIHWA', point: [37.343089, 126.696181], radiusM: 300 },
  { landmarkId: 5, name: '배곧생명공원', area: 'BAEGOT', point: [37.371322, 126.721484], radiusM: 300 },
  { landmarkId: 6, name: '배곧한울공원', area: 'BAEGOT', point: [37.353981, 126.701768], radiusM: 300 },
  { landmarkId: 7, name: '거북섬', area: 'SIHWA', point: [37.321469, 126.679738], radiusM: 300 },
  { landmarkId: 8, name: '시흥 해양생태과학관', area: 'SIHWA', point: [37.329401, 126.677035], radiusM: 300 },
]
