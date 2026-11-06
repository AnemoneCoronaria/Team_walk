-- =====================================================================
-- 산책시루 테스트 데이터 (1주차)
-- 실행 순서: schema.sql → seed.sql   (seed 만 다시 돌리려면 schema 부터 다시)
--
-- ■ 테스트 계정 (비밀번호 전부 test1234!)
--   admin(관리자) / minseo / jeongran / dongeon / donghun / seungmin
--   banned  ← 7일 정지된 회원 (로그인하면 403 SUSPENDED 가 나와야 정상. 정지는 실행일 + 5일까지)
--   → 각자 PC(로컬) MySQL에서 테스트할 때만 쓰는 계정입니다.
--
-- ■ 코스 좌표는 임시입니다.
--   공원·역 위치를 기준으로 대충 둥글게 만든 경로라 실제 길과 다릅니다.
--   2주차에 실제 GPX로 바꿀 예정 (동언).
--
-- ■ 2주차 데이터: 랜드마크 8곳(실제 좌표), 스탬프, 후기 6개(사진 2장), 신고 3개, 동행 댓글
--   후기 사진 파일은 db/sample-uploads/ 에 있습니다. 업로드 폴더로 복사하는 방법은 db/README.md
--
-- ■ 날짜는 "실행한 날" 기준으로 계산됩니다.
--   언제 실행해도 동행 1~5번은 앞으로 있을 동행, 6번은 지난 동행(DONE)이 됩니다.
-- =====================================================================

SET NAMES utf8mb4;
USE sanchaek;

SET @today  = CURDATE();                -- 동행 날짜 기준
SET @joined = NOW() - INTERVAL 7 DAY;   -- 가입 · 약관 동의 시각

-- ---------------------------------------------------------------------
-- 회원 7명 (팀원 5명 + 관리자 + 정지된 회원)
-- ---------------------------------------------------------------------
INSERT INTO users (user_id, login_id, password_hash, nickname, avatar, age_group, mbti, role, status,
                   suspended_until, warning_count, agree_terms_at, agree_privacy_at, agree_location, agree_location_at, created_at) VALUES
    (1, 'admin',    '$2a$10$Oz4neiApHO/Ua5Ng4nl87OGA14U8fJhtAXzuoZ5Ye7puS.XEoprmm', '관리자', 'sogeumi',    '30s', NULL,   'ADMIN', 'ACTIVE', NULL, 0, @joined, @joined, FALSE, NULL,    @joined),
    (2, 'minseo',   '$2a$10$VsxXZr4hy2ACP7KYZksWu.DaKqpChdP1JF/Sw.2ois6GKMjgWDyvK', '민서',   'seagull',    '20s', NULL,   'USER',  'ACTIVE', NULL, 0, @joined, @joined, TRUE,  @joined, @joined),
    (3, 'jeongran', '$2a$10$LK8oHnWDRiF6A8Ss2DMGVeswz68omYGPW5ztr/vGefWPUsuS9nKXK', '정란',   'lotus',      '20s', 'ENFP', 'USER',  'ACTIVE', NULL, 0, @joined, @joined, FALSE, NULL,    @joined),
    (4, 'dongeon',  '$2a$10$qQJVYt75ktVRm6LDPdyP0.BrD3foUFZrqdykCK.bJc17oInQU/mL.', '동언',   'lighthouse', '20s', 'INTJ', 'USER',  'ACTIVE', NULL, 0, @joined, @joined, TRUE,  @joined, @joined),
    (5, 'donghun',  '$2a$10$emvHeJoqjVvCxUzjiZI1N.0MVDDpQTFRcRYqXrszUFfAJYfuICAhq', '동훈',   'dog',        '20s', NULL,   'USER',  'ACTIVE', NULL, 1, @joined, @joined, FALSE, NULL,    @joined),
    (6, 'seungmin', '$2a$10$k6NV2RTYoOYmfEu8ahHip.dGli821tb8VmwmcBxcL3Iq8/IwYjA56', '승민',   'cat',        '20s', 'ISFP', 'USER',  'ACTIVE', NULL, 0, @joined, @joined, FALSE, NULL,    @joined),
    (7, 'banned',   '$2a$10$Dv.A4PcihniiXZquFBc2xevnY29.EvDShkNzI5GZ3IIVX3ALNKAQO', '정지회원', 'reed',     '30s', NULL,   'USER',  'SUSPENDED', NOW() + INTERVAL 5 DAY, 0, @joined, @joined, FALSE, NULL,    @joined);

-- 회원 스타일 · 언어 (영어 코드. SLOW 천천히, CHAT 수다, FAST 빠르게 / KO 한국어, EN English, ZH 中文)
INSERT INTO user_tags (user_id, tag_type, tag_value) VALUES
    (1, 'LANGUAGE', 'KO'),
    (2, 'STYLE', 'SLOW'),
    (2, 'STYLE', 'CHAT'),
    (2, 'LANGUAGE', 'KO'),
    (2, 'LANGUAGE', 'EN'),
    (3, 'STYLE', 'CHAT'),
    (3, 'LANGUAGE', 'KO'),
    (4, 'STYLE', 'FAST'),
    (4, 'LANGUAGE', 'KO'),
    (5, 'STYLE', 'CHAT'),
    (5, 'LANGUAGE', 'KO'),
    (5, 'LANGUAGE', 'ZH'),
    (6, 'STYLE', 'SLOW'),
    (6, 'LANGUAGE', 'KO'),
    (6, 'LANGUAGE', 'EN'),
    (7, 'LANGUAGE', 'KO');

-- ---------------------------------------------------------------------
-- 코스 4개 (지역별 1개) — 거리·시간은 경로로 계산한 값 (시속 4km 기준)
-- GPX 파일은 db/sample-uploads/gpx/ → 업로드 폴더의 gpx/ 로 복사 (다운로드가 그 파일을 보냄)
-- ---------------------------------------------------------------------
-- 1. 옥구공원 순환 코스: 1.43km, 약 21분, 좌표 25개 (옥구공원 중심 37.3545, 126.7099 둘레 약 200m. 랜드마크 '옥구공원' 300m 안)
INSERT INTO courses (course_id, author_id, name, description, area, level, distance_km, duration_min,
                     start_lat, start_lng, path_json, visibility, gpx_path, download_count, created_at) VALUES
    (1, 4, '옥구공원 순환 코스', '옥구산 자락을 따라 공원을 한 바퀴 도는 코스예요. 중간에 오르막이 조금 있어요.', 'OKGU', 'NORMAL', 1.43, 21,
     37.352720, 126.709915,
     '[[37.35272,126.709915],[37.352712,126.710646],[37.353007,126.711231],[37.35317,126.711948],[37.3536,126.712311],[37.354075,126.712404],[37.354517,126.712709],[37.355019,126.712745],[37.355397,126.712217],[37.355711,126.711718],[37.356088,126.711285],[37.356339,126.710653],[37.356295,126.709915],[37.356131,126.709263],[37.356104,126.708532],[37.355876,126.707863],[37.355388,126.707637],[37.354996,126.707217],[37.354517,126.707312],[37.354033,126.707191],[37.353618,126.707567],[37.35331,126.708094],[37.35293,126.708532],[37.352781,126.709214],[37.35272,126.709915]]',
     'PUBLIC', 'gpx/course-1.gpx', 24, @joined);

-- 2. 정왕역 동네 한 바퀴: 2.69km, 약 40분, 좌표 25개
INSERT INTO courses (course_id, author_id, name, description, area, level, distance_km, duration_min,
                     start_lat, start_lng, path_json, visibility, gpx_path, download_count, created_at) VALUES
    (2, 2, '정왕역 동네 한 바퀴', '정왕역에서 출발해 주변 동네를 가볍게 도는 평지 코스예요.', 'JEONGWANG', 'EASY', 2.69, 40,
     37.351452, 126.742790,
     '[[37.351452,126.74279],[37.351738,126.744123],[37.352264,126.745232],[37.352887,126.746139],[37.35356,126.74694],[37.354335,126.747639],[37.355253,126.748076],[37.356247,126.74804],[37.357154,126.747449],[37.357827,126.746432],[37.358242,126.745232],[37.358485,126.744016],[37.358639,126.74279],[37.358679,126.741491],[37.358488,126.740147],[37.357969,126.738947],[37.357154,126.738131],[37.356195,126.737814],[37.355253,126.737906],[37.354387,126.738216],[37.35356,126.73864],[37.352745,126.73924],[37.352018,126.740147],[37.351544,126.741383],[37.351452,126.74279]]',
     'PUBLIC', 'gpx/course-2.gpx', 3, @joined);

-- 3. 배곧생명공원 산책 코스: 2.52km, 약 38분, 좌표 25개
INSERT INTO courses (course_id, author_id, name, description, area, level, distance_km, duration_min,
                     start_lat, start_lng, path_json, visibility, gpx_path, download_count, created_at) VALUES
    (3, 3, '배곧생명공원 산책 코스', '넓은 공원 안을 천천히 도는 평탄한 코스예요. 아이와 함께 걷기 좋아요.', 'BAEGOT', 'EASY', 2.52, 38,
     37.371410, 126.716051,
     '[[37.37141,126.716051],[37.370804,126.716395],[37.370225,126.716867],[37.369654,126.717534],[37.369146,126.718541],[37.368813,126.719916],[37.368749,126.721479],[37.368949,126.722961],[37.369318,126.724193],[37.369754,126.7252],[37.370225,126.726091],[37.370767,126.726868],[37.37141,126.727354],[37.372106,126.727314],[37.37274,126.726656],[37.373212,126.725527],[37.373502,126.724193],[37.373672,126.722841],[37.37378,126.721479],[37.373808,126.720035],[37.373674,126.718541],[37.373311,126.717208],[37.37274,126.716302],[37.372069,126.715949],[37.37141,126.716051]]',
     'PUBLIC', 'gpx/course-3.gpx', 15, @joined);

-- 4. 오이도 빨강등대 코스: 2.04km, 약 31분, 좌표 25개
INSERT INTO courses (course_id, author_id, name, description, area, level, distance_km, duration_min,
                     start_lat, start_lng, path_json, visibility, gpx_path, download_count, created_at) VALUES
    (4, 1, '오이도 빨강등대 코스', '오이도 빨강등대에서 출발해 바다를 보며 걷는 코스예요.', 'SIHWA', 'EASY', 2.04, 31,
     37.345543, 126.687608,
     '[[37.345543,126.687608],[37.34472,126.687787],[37.343935,126.688033],[37.34316,126.688379],[37.34247,126.688903],[37.342019,126.689617],[37.341932,126.69043],[37.342203,126.6912],[37.342704,126.691841],[37.343295,126.692364],[37.343935,126.692827],[37.344671,126.693231],[37.345543,126.693484],[37.346487,126.693463],[37.347348,126.693121],[37.347988,126.692534],[37.348382,126.691841],[37.348613,126.691138],[37.348759,126.69043],[37.348798,126.689679],[37.348616,126.688903],[37.348123,126.68821],[37.347348,126.687739],[37.346438,126.687555],[37.345543,126.687608]]',
     'PUBLIC', 'gpx/course-4.gpx', 41, @joined);

-- 코스 편의시설 (있음/없음만)
INSERT INTO course_facilities (course_id, facility) VALUES
    (1, 'TOILET'),
    (1, 'BENCH'),
    (1, 'PARKING'),
    (2, 'BENCH'),
    (3, 'TOILET'),
    (3, 'BENCH'),
    (3, 'PARKING'),
    (4, 'TOILET'),
    (4, 'PARKING'),
    (4, 'SEA_VIEW');

-- ---------------------------------------------------------------------
-- 동행 6개 (화면에 보이는 상태가 골고루 나오게)
--   1 OPEN 2/4 (GPX 첨부: gpx/match-1.gpx)     2 OPEN 1/6 (승민 내보내짐)   3 FULL 3/3
--   4 CLOSED 2/4             5 CANCELED                  6 DONE (지난 동행, 동훈 참여 취소)
--   ※ FULL · DONE 은 저장하지 않습니다. v_match_status 뷰가 계산합니다.
-- ---------------------------------------------------------------------
INSERT INTO matches (match_id, host_id, course_id, title, content, meet_at, meet_place,
                     capacity, target, mbti, path_json, gpx_path, status, created_at) VALUES
    (1, 2, 1, '퇴근 후 옥구공원 한 바퀴 같이 걸어요', '퇴근하고 가볍게 걸을 분 구해요! 천천히 이야기하면서 걸어요. 欢迎说中文的朋友！',
     TIMESTAMP(@today + INTERVAL 5 DAY, '19:00:00'), '옥구공원 정문 주차장 앞',
     4, 'EXCHANGE', NULL,
     '[[37.35272,126.709915],[37.352712,126.710646],[37.353007,126.711231],[37.35317,126.711948],[37.3536,126.712311],[37.354075,126.712404],[37.354517,126.712709],[37.355019,126.712745],[37.355397,126.712217],[37.355711,126.711718],[37.356088,126.711285],[37.356339,126.710653],[37.356295,126.709915]]',
     'gpx/match-1.gpx', 'OPEN', NOW() - INTERVAL 1 DAY),
    (2, 3, 3, '주말 아침 배곧생명공원 산책해요', '토요일 아침에 상쾌하게 걸어요. 누구나 환영해요.',
     TIMESTAMP(@today + INTERVAL 8 DAY, '08:00:00'), '배곧생명공원 주차장 입구',
     6, 'ANYONE', NULL, NULL, NULL, 'OPEN', NOW() - INTERVAL 1 DAY),
    (3, 4, 4, '오이도 노을 보러 같이 가요', '해 질 무렵 빨강등대 앞에서 만나요. 사진도 같이 찍어요.',
     TIMESTAMP(@today + INTERVAL 9 DAY, '16:30:00'), '오이도 빨강등대 앞',
     3, 'SAME_LANGUAGE', NULL, NULL, NULL, 'OPEN', NOW() - INTERVAL 1 DAY),
    (4, 6, 2, '정왕역 저녁 산책 (ISFP 모여라)', '조용히 걷는 걸 좋아하는 분이면 좋겠어요.',
     TIMESTAMP(@today + INTERVAL 6 DAY, '20:00:00'), '정왕역 앞',
     4, 'MBTI', 'ISFP', NULL, NULL, 'CLOSED', NOW() - INTERVAL 2 HOUR),
    (5, 5, 1, '옥구산 정상까지 빠르게 걸어요', '운동 삼아 빠르게 걸을 분! 아침 7시에 만나요.',
     TIMESTAMP(@today + INTERVAL 12 DAY, '07:00:00'), '옥구공원 정문',
     5, 'ANYONE', NULL, NULL, NULL, 'CANCELED', NOW() - INTERVAL 1 HOUR),
    (6, 2, 3, '지난 주말 배곧 산책 모임', '다녀왔어요! 다음에도 같이 걸어요.',
     TIMESTAMP(@today - INTERVAL 4 DAY, '10:00:00'), '배곧생명공원 주차장 입구',
     4, 'ANYONE', NULL, NULL, NULL, 'OPEN', NOW() - INTERVAL 7 DAY);

-- 동행 태그 (원하는 스타일 · 언어 · 연령대, 영어 코드)
INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES
    (1, 'STYLE', 'SLOW'),
    (1, 'LANGUAGE', 'KO'),
    (1, 'LANGUAGE', 'ZH'),
    (1, 'AGE_GROUP', '20s'),
    (1, 'AGE_GROUP', '30s'),
    (2, 'STYLE', 'CHAT'),
    (2, 'LANGUAGE', 'KO'),
    (3, 'STYLE', 'SLOW'),
    (3, 'LANGUAGE', 'KO'),
    (3, 'AGE_GROUP', '20s'),
    (4, 'LANGUAGE', 'KO'),
    (5, 'STYLE', 'FAST'),
    (5, 'LANGUAGE', 'KO'),
    (5, 'LANGUAGE', 'ZH'),
    (6, 'STYLE', 'CHAT'),
    (6, 'LANGUAGE', 'KO');

-- 동행 참여자 (각 글의 첫 줄이 모집자)
INSERT INTO match_participants (match_id, user_id, status, leave_reason, joined_at) VALUES
    (1, 2, 'JOINED', NULL, NOW() - INTERVAL 1 DAY),
    (1, 5, 'JOINED', NULL, NOW() - INTERVAL 20 HOUR),
    (2, 3, 'JOINED', NULL, NOW() - INTERVAL 1 DAY),
    (2, 6, 'KICKED', NULL, NOW() - INTERVAL 20 HOUR),
    (3, 4, 'JOINED', NULL, NOW() - INTERVAL 1 DAY),
    (3, 2, 'JOINED', NULL, NOW() - INTERVAL 20 HOUR),
    (3, 6, 'JOINED', NULL, NOW() - INTERVAL 18 HOUR),
    (4, 6, 'JOINED', NULL, NOW() - INTERVAL 2 HOUR),
    (4, 3, 'JOINED', NULL, NOW() - INTERVAL 1 HOUR),
    (5, 5, 'JOINED', NULL, NOW() - INTERVAL 1 HOUR),
    (5, 4, 'JOINED', NULL, NOW() - INTERVAL 30 MINUTE),
    (6, 2, 'JOINED', NULL, NOW() - INTERVAL 7 DAY),
    (6, 3, 'JOINED', NULL, NOW() - INTERVAL 6 DAY),
    (6, 6, 'JOINED', NULL, NOW() - INTERVAL 6 DAY),
    (6, 5, 'LEFT', '일정이 생겼어요', NOW() - INTERVAL 6 DAY);


-- =====================================================================
-- 2주차 데이터
-- =====================================================================

-- ---------------------------------------------------------------------
-- 랜드마크 8곳 — 좌표는 한국관광공사 관광정보(TourAPI)를 옮긴 자료 기준
--   (옥구공원 · 배곧생명공원은 지도 서비스 표시 좌표)
-- 테스트 코스와의 거리: 코스1 → 옥구공원 189m / 코스3 → 배곧생명공원 273m
--   코스4 → 빨강등대 10m, 함상전망대 212m (선사유적공원은 311m 라 반경 밖: 경계 테스트용)
-- ---------------------------------------------------------------------
INSERT INTO landmarks (landmark_id, name, area, lat, lng, radius_m, description, sort_order) VALUES
    (1, '옥구공원',             'OKGU',   37.354517, 126.709916, 300, '옥구산을 품은 정왕동 대표 공원', 1),
    (2, '오이도 빨강등대',       'SIHWA',  37.345543, 126.687492, 300, '오이도 바다를 상징하는 빨간 등대', 2),
    (3, '오이도 함상전망대',     'SIHWA',  37.340575, 126.688045, 300, '배 모양 전망대에서 보는 서해', 3),
    (4, '오이도 선사유적공원',   'SIHWA',  37.343089, 126.696181, 300, '선사시대 조개더미 유적과 오이도박물관', 4),
    (5, '배곧생명공원',         'BAEGOT', 37.371322, 126.721484, 300, '배곧신도시 한가운데 넓은 공원', 5),
    (6, '배곧한울공원',         'BAEGOT', 37.353981, 126.701768, 300, '해수체험장과 해안 산책로', 6),
    (7, '거북섬',              'SIHWA',  37.321469, 126.679738, 300, '시화호 해양레저 단지', 7),
    (8, '시흥 해양생태과학관',   'SIHWA',  37.329401, 126.677035, 300, '시화호 갯벌과 바다 생태 전시', 8);

-- 저장한 코스: 민서가 코스 3 · 4 저장 (P6)
INSERT INTO saved_courses (user_id, course_id, created_at) VALUES
    (2, 3, NOW() - INTERVAL 2 DAY),
    (2, 4, NOW() - INTERVAL 1 DAY);

-- 스탬프: 민서 3곳 → 배지 첫 산책 · 랜드마크 3곳 / 동언 1곳 → 첫 산책
--   (지난 동행 6번에 참여한 민서 · 정란 · 승민은 '첫 동행' 배지도 받음)
INSERT INTO user_stamps (user_id, landmark_id, stamped_at) VALUES
    (2, 2, NOW() - INTERVAL 10 DAY),
    (2, 3, NOW() - INTERVAL 10 DAY),
    (2, 1, NOW() - INTERVAL 3 DAY),
    (4, 5, NOW() - INTERVAL 4 DAY);

-- ---------------------------------------------------------------------
-- 후기 6개
--   코스3: 민서 5점(사진 2장) · 정란 4점 — 둘 다 지난 동행 6번으로 걸음 (승민은 아직 안 씀 → reviewed false)
--   코스1: 동훈 4점(中文 후기) · 승민 2점(신고되어 관리자가 숨김 → 목록·별점에서 빠짐)
--   코스4: 동언 3점 · 정란 3점(동행 없이) / 코스2: 후기 없음
--   → 별점: 코스3 4.5(2개), 코스1 4.0(1개), 코스4 3.0(2개), 코스2 0(0개)
-- ---------------------------------------------------------------------
INSERT INTO reviews (review_id, user_id, course_id, match_id, rating, content, is_hidden, created_at) VALUES
    (1, 2, 3, 6,    5, '다 같이 이야기하면서 걸으니 금방이었어요. 공원이 넓고 평탄해요!', FALSE, NOW() - INTERVAL 3 DAY),
    (2, 3, 3, 6,    4, '벤치가 많아서 쉬어 가기 좋았어요. 화장실도 깨끗해요.',       FALSE, NOW() - INTERVAL 3 DAY),
    (3, 5, 1, NULL, 4, '风景很好，很适合晚上散步！오르막이 조금 있어요.',             FALSE, NOW() - INTERVAL 2 DAY),
    (4, 6, 1, NULL, 2, '광고 링크가 들어간 후기라고 가정한 테스트 데이터입니다.',      TRUE,  NOW() - INTERVAL 2 DAY),
    (5, 4, 4, NULL, 3, '바다 보면서 걷기 좋은데 주말엔 사람이 많아요.',               FALSE, NOW() - INTERVAL 1 DAY);

-- 6번째 후기: 정란이 코스4 를 동행 없이 (동행 없이 쓴 후기는 같은 사람이 여러 개 써도 됨)
INSERT INTO reviews (review_id, user_id, course_id, match_id, rating, content, created_at) VALUES
    (6, 3, 4, NULL, 3, '노을 질 때 다시 와 보고 싶은 코스예요.', NOW() - INTERVAL 1 DAY);

-- 후기 사진 (파일은 db/sample-uploads/reviews/ → 업로드 폴더의 reviews/ 로 복사)
INSERT INTO review_photos (review_id, file_path, sort_order) VALUES
    (1, 'reviews/1-1.jpg', 1),
    (1, 'reviews/1-2.jpg', 2);

-- ---------------------------------------------------------------------
-- 신고 4개 (관리자 AD1 화면 테스트용)
--   1 정란 → 후기 4번  : 처리 완료, HIDE (그래서 후기 4번이 숨겨져 있음)
--   2 동훈 → 회원 7번  : 처리 완료, SUSPEND (그래서 banned 계정이 7일 정지 상태)
--   3 승민 → 코스 2번  : 대기 중 (AD1 목록에 보여야 함)
--   4 정란 → 동행 5번  : 처리 완료, WARN (그래서 모집자 동훈의 경고 1회)
-- ---------------------------------------------------------------------
INSERT INTO reports (report_id, reporter_id, target_type, target_id, reason, detail,
                     status, action, admin_memo, processed_by, processed_at, created_at) VALUES
    (1, 3, 'REVIEW', 4, 'SPAM',       '광고 링크가 있어요',       'DONE',    'HIDE',    '광고 확인, 숨김 처리',  1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY),
    (2, 5, 'USER',   7, 'RUDE',       '댓글로 욕설을 했어요',     'DONE',    'SUSPEND', '욕설 확인, 7일 정지',   1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY),
    (3, 6, 'COURSE', 2, 'WRONG_INFO', '실제 길과 경로가 달라요',  'PENDING', NULL,      NULL,                  NULL, NULL,                  NOW() - INTERVAL 3 HOUR),
    (4, 3, 'MATCH',  5, 'INAPPROPRIATE', '글에 연락처를 적어 달라고 해요', 'DONE', 'WARN', '처음이라 경고',    1, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 2 HOUR);

-- ---------------------------------------------------------------------
-- 동행 댓글 — 1번 동행: 모집자 공지 1 + 댓글 2 / 3번 동행: 댓글 1
-- ---------------------------------------------------------------------
INSERT INTO match_comments (match_id, user_id, content, is_notice, created_at) VALUES
    (1, 2, '정문 주차장 앞 벤치에서 7시에 만나요!',   TRUE,  NOW() - INTERVAL 20 HOUR),
    (1, 5, '저 中文 조금 할 수 있어요. 같이 연습해요!', FALSE, NOW() - INTERVAL 19 HOUR),
    (1, 2, '좋아요! 편하게 오세요 :)',                FALSE, NOW() - INTERVAL 18 HOUR),
    (3, 6, '사진 찍기 좋은 자리 알아 올게요',          FALSE, NOW() - INTERVAL 10 HOUR);
