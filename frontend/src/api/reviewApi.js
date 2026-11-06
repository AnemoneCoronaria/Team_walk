// 후기 API (백엔드 담당: 동언, PR #20) — R1 후기 쓰기 · C4 후기 전체 보기
//   POST /api/reviews                 multipart: courseId, matchId(동행으로 걸었을 때만), rating, content, photos(최대 5장)
//   GET  /api/courses/{id}/reviews    ?sort=recent|rating &photo=true &page &size → { items, page, size, total, avgRating }
// C2 의 후기 미리보기는 courseApi.js 의 getCourseReviews 를 그대로 씀 (같은 주소)
import { USE_MOCK, request, mockResponse, mockError, paginate } from './client.js'
import { ME } from '../mocks/users.js'
import { COURSES, REVIEWS } from '../mocks/courses.js'

/**
 * 후기 등록 (R1) — 201 { reviewId }
 * 규칙(서버): 별점 1~5, 후기 10자 이상, 사진은 jpg · png 한 장 5MB 이하 최대 5장, 같은 동행에는 후기 1개(409 DUPLICATE)
 */
export function createReview({ courseId, matchId, rating, content, photos = [] }) {
  if (USE_MOCK) return mockCreateReview({ courseId, matchId, rating, content, photos })
  const form = new FormData()
  form.append('courseId', courseId)
  if (matchId) form.append('matchId', matchId)
  form.append('rating', rating)
  form.append('content', content)
  photos.forEach((file) => form.append('photos', file))
  return request('/api/reviews', { method: 'POST', body: form })
}

/** 코스 후기 목록 (C4) — sort: recent(최신) · rating(별점 높은 순), photo: true 면 사진 있는 후기만 */
export function getReviews(courseId, { sort = 'recent', photo = false, page = 1, size = 10 } = {}) {
  if (USE_MOCK) return mockResponse(mockReviewPage(courseId, { sort, photo, page, size }))
  return request(`/api/courses/${courseId}/reviews`, { params: { sort, photo: photo || undefined, page, size } })
}

function mockReviewPage(courseId, { sort, photo, page, size }) {
  const all = REVIEWS.filter((r) => r.courseId === Number(courseId))
  const avg = all.length ? all.reduce((s, r) => s + r.rating, 0) / all.length : 0
  const list = all
    .filter((r) => !photo || r.photos.length > 0)
    .sort((a, b) => (sort === 'rating' ? b.rating - a.rating : 0) || b.createdAt.localeCompare(a.createdAt))
    .map((r) => {
      const copy = { ...r }
      delete copy.courseId
      delete copy.matchId
      return copy
    })
  return { ...paginate(list, page, size), avgRating: Math.round(avg * 10) / 10 }
}

// 가짜 등록: REVIEWS 맨 앞에 넣고 코스 별점 · 후기 수도 다시 계산 (새로고침하면 사라짐)
function mockCreateReview({ courseId, matchId, rating, content, photos }) {
  const id = Number(courseId)
  if (matchId && REVIEWS.some((r) => r.matchId === Number(matchId) && r.author.userId === ME.userId)) {
    return mockError(409, 'DUPLICATE', '이 동행에는 이미 후기를 썼어요.')
  }
  const reviewId = Math.max(0, ...REVIEWS.map((r) => r.reviewId)) + 1
  REVIEWS.unshift({
    reviewId,
    courseId: id,
    matchId: matchId ? Number(matchId) : null,
    author: { userId: ME.userId, nickname: ME.nickname, avatar: ME.avatar },
    rating,
    content,
    photos: photos.map((file) => URL.createObjectURL(file)),
    withMatch: Boolean(matchId),
    createdAt: new Date().toISOString().slice(0, 19),
  })
  const course = COURSES.find((c) => c.courseId === id)
  if (course) {
    const mine = REVIEWS.filter((r) => r.courseId === id)
    course.reviewCount = mine.length
    course.rating = Math.round((mine.reduce((s, r) => s + r.rating, 0) / mine.length) * 10) / 10
  }
  return mockResponse({ reviewId })
}
