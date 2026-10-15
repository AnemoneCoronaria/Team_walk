-- =====================================================================
-- 산책시루 DB 스키마
-- 범위: 1주차 (계정 · 코스 · 동행) 테이블 7개
--       2주차 (후기 · 신고 · 랜드마크 · 스탬프 · 댓글) 테이블 6개
--       계산용 뷰 2개 (동행 상태, 코스 별점)
-- DB 위치: 각자 PC의 MySQL (배포 서버는 아직 계획 없음)
-- 기준: MySQL 8.4 / 문자셋 utf8mb4 (한글 · 中文 · 이모지 모두 저장)
--
-- 실행 순서: schema.sql → seed.sql  (방법은 db/README.md)
-- 주의: 실행하면 아래 테이블이 전부 지워지고 새로 만들어집니다. (데이터도 사라짐)
--       테이블을 바꿀 때는 이 파일을 고쳐서 PR을 올리고 디스코드에 알립니다.
-- =====================================================================

SET NAMES utf8mb4;
USE sanchaek;

-- 지울 때는 만든 순서의 반대로 (다른 테이블이 참조하는 것부터 지우면 오류)
DROP VIEW  IF EXISTS v_course_rating;
DROP VIEW  IF EXISTS v_match_status;
DROP TABLE IF EXISTS match_comments;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS review_photos;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS user_stamps;
DROP TABLE IF EXISTS landmarks;
DROP TABLE IF EXISTS match_participants;
DROP TABLE IF EXISTS match_tags;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS course_facilities;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS user_tags;
DROP TABLE IF EXISTS users;


-- ---------------------------------------------------------------------
-- 회원 (담당: 정란)
-- 탈퇴해도 행을 지우지 않고 status = 'WITHDRAWN' 으로 바꿉니다.
-- (글 · 후기의 작성자를 "탈퇴한 회원"으로 보여 주기 위해)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id            BIGINT       NOT NULL AUTO_INCREMENT,
    login_id           VARCHAR(20)  NOT NULL                  COMMENT '로그인 아이디 (중복 불가, 대소문자 구분 안 함)',
    password_hash      CHAR(60)     NOT NULL                  COMMENT 'BCrypt 해시 60자. 비밀번호 원문 저장 금지',
    nickname           VARCHAR(20)  NOT NULL,
    avatar             VARCHAR(20)  NOT NULL DEFAULT 'seagull' COMMENT 'seagull, lighthouse, lotus, reed, sogeumi, wave, dog, cat',
    age_group          VARCHAR(5)   NOT NULL                  COMMENT '20s, 30s, 40s, 50s+ (Figma A4: 20대 · 30대 · 40대 · 50대 이상)',
    mbti               CHAR(4)      NULL                      COMMENT '선택 (ENFP 같은 4글자)',
    role               VARCHAR(10)  NOT NULL DEFAULT 'USER'   COMMENT 'USER, ADMIN',
    status             VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, SUSPENDED(정지), WITHDRAWN(탈퇴)',
    agree_terms_at     DATETIME     NOT NULL                  COMMENT '이용약관 동의 시각 (필수)',
    agree_privacy_at   DATETIME     NOT NULL                  COMMENT '개인정보 수집 동의 시각 (필수)',
    agree_location     BOOLEAN      NOT NULL DEFAULT FALSE    COMMENT '위치정보 동의 (랜드마크 인증에 필요)',
    agree_location_at  DATETIME     NULL                      COMMENT '위치정보 동의 시각',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_login_id (login_id),
    CONSTRAINT chk_users_avatar    CHECK (avatar IN ('seagull','lighthouse','lotus','reed','sogeumi','wave','dog','cat')),
    CONSTRAINT chk_users_age_group CHECK (age_group IN ('20s','30s','40s','50s+')),
    CONSTRAINT chk_users_role      CHECK (role IN ('USER','ADMIN')),
    CONSTRAINT chk_users_status    CHECK (status IN ('ACTIVE','SUSPENDED','WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='회원 (담당: 정란)';


-- ---------------------------------------------------------------------
-- 회원의 산책 스타일 · 언어 (담당: 정란)
-- 값 하나당 한 줄. 예) 민서가 언어 2개면 LANGUAGE 행이 2개
-- tag_value 는 영어 코드로 저장합니다. 화면 문구(🐢 천천히, 中文 …)는 프론트가 코드에 맞춰 표시.
--   STYLE    : SLOW 천천히, FAST 빠르게, QUIET 조용히, CHAT 수다, PHOTO 사진 찍으며,
--              SUNRISE_SUNSET 일출·일몰, DOG 반려견과
--   LANGUAGE : KO 한국어, ZH 中文, EN English, VI Tiếng Việt, RU Русский
-- ---------------------------------------------------------------------
CREATE TABLE user_tags (
    user_id    BIGINT       NOT NULL,
    tag_type   VARCHAR(10)  NOT NULL COMMENT 'STYLE(산책 스타일, 최대 3개), LANGUAGE(언어)',
    tag_value  VARCHAR(30)  NOT NULL COMMENT '영어 코드 (위 목록)',
    PRIMARY KEY (user_id, tag_type, tag_value),
    KEY idx_user_tags_value (tag_type, tag_value),
    CONSTRAINT fk_user_tags_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_user_tags_value CHECK (
        (tag_type = 'STYLE'    AND tag_value IN ('SLOW','FAST','QUIET','CHAT','PHOTO','SUNRISE_SUNSET','DOG'))
     OR (tag_type = 'LANGUAGE' AND tag_value IN ('KO','ZH','EN','VI','RU')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='회원 스타일·언어 (담당: 정란)';


-- ---------------------------------------------------------------------
-- 코스 (담당: 동언)
-- 경로는 [[위도, 경도], ...] 배열을 path_json 한 칸에 통째로 저장합니다.
-- 목록 화면 지도 마커용으로 시작점만 start_lat / start_lng 에 따로 둡니다.
-- GPX 파일은 저장하지 않습니다. 다운로드할 때 path_json 으로 GPX를 만들어 보냅니다.
-- 삭제해도 행을 지우지 않고 is_deleted = TRUE (그 코스로 잡힌 동행 · 후기는 남김)
-- 관리자가 숨기면 is_hidden = TRUE (AD4 코스 관리, AD1 신고 처리)
-- ---------------------------------------------------------------------
CREATE TABLE courses (
    course_id     BIGINT        NOT NULL AUTO_INCREMENT,
    author_id     BIGINT        NOT NULL                  COMMENT '올린 사람',
    name          VARCHAR(50)   NOT NULL,
    description   TEXT          NULL,
    area          VARCHAR(20)   NOT NULL                  COMMENT 'OKGU 옥구공원, JEONGWANG 정왕동, BAEGOT 배곧, SIHWA 시화호·서해',
    level         VARCHAR(10)   NOT NULL                  COMMENT 'EASY, NORMAL, HARD',
    distance_km   DECIMAL(5,2)  NOT NULL                  COMMENT 'GPX 경로로 계산한 거리',
    duration_min  INT           NOT NULL                  COMMENT '예상 소요시간. 거리 ÷ 시속 4km 로 계산해서 저장',
    start_lat     DECIMAL(9,6)  NOT NULL                  COMMENT '시작점 위도 (목록 지도 마커용)',
    start_lng     DECIMAL(9,6)  NOT NULL                  COMMENT '시작점 경도',
    path_json     JSON          NOT NULL                  COMMENT '[[위도, 경도], ...] 전체 경로',
    visibility    VARCHAR(10)   NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC 전체 공개, PRIVATE 나만 보기',
    hide_ends     BOOLEAN       NOT NULL DEFAULT FALSE    COMMENT '출발·도착 200m 숨기기',
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE    COMMENT '작성자가 삭제한 코스 (목록·상세에서 제외)',
    is_hidden     BOOLEAN       NOT NULL DEFAULT FALSE    COMMENT '관리자가 숨긴 코스 (목록에서 제외, 상세 404)',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id),
    KEY idx_courses_area (area),
    KEY idx_courses_author (author_id),
    CONSTRAINT fk_courses_author FOREIGN KEY (author_id) REFERENCES users (user_id),
    CONSTRAINT chk_courses_area       CHECK (area IN ('OKGU','JEONGWANG','BAEGOT','SIHWA')),
    CONSTRAINT chk_courses_level      CHECK (level IN ('EASY','NORMAL','HARD')),
    CONSTRAINT chk_courses_visibility CHECK (visibility IN ('PUBLIC','PRIVATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='코스 (담당: 동언)';


-- ---------------------------------------------------------------------
-- 코스 편의시설 (담당: 동언)
-- 있음/없음만 저장 (개수는 저장하지 않음). 시설 하나당 한 줄.
-- ---------------------------------------------------------------------
CREATE TABLE course_facilities (
    course_id  BIGINT       NOT NULL,
    facility   VARCHAR(20)  NOT NULL COMMENT 'TOILET 화장실, BENCH 벤치, PARKING 주차, SEA_VIEW 바다 뷰',
    PRIMARY KEY (course_id, facility),
    KEY idx_course_facilities_facility (facility),
    CONSTRAINT fk_course_facilities_course FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT chk_course_facilities_facility CHECK (facility IN ('TOILET','BENCH','PARKING','SEA_VIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='코스 편의시설 (담당: 동언)';


-- ---------------------------------------------------------------------
-- 동행 모집 글 (담당: 민서)
-- status 에는 모집자가 직접 바꾼 상태만 저장합니다: OPEN / CLOSED(마감) / CANCELED(모집 취소)
-- FULL(정원 참) 과 DONE(끝남) 은 저장하지 않고 계산합니다 → 아래 v_match_status 뷰
-- 만나는 위치 좌표(meetPoint)도 저장하지 않고 첨부 경로 시작점, 없으면 코스 시작점을 씁니다.
-- ---------------------------------------------------------------------
CREATE TABLE matches (
    match_id    BIGINT        NOT NULL AUTO_INCREMENT,
    host_id     BIGINT        NOT NULL                 COMMENT '모집자',
    course_id   BIGINT        NOT NULL,
    title       VARCHAR(40)   NOT NULL,
    content     TEXT          NOT NULL,
    meet_at     DATETIME      NOT NULL                 COMMENT '만나는 일시 (시간대 MORNING/AFTERNOON/EVENING 은 이 값으로 계산)',
    meet_place  VARCHAR(100)  NOT NULL                 COMMENT '만남 장소 설명 (예: 옥구공원 정문 주차장 앞)',
    capacity    TINYINT       NOT NULL                 COMMENT '정원 2~10 (모집자 포함)',
    target      VARCHAR(20)   NOT NULL                 COMMENT 'ANYONE 누구나, EXCHANGE 내·외국인 교류, SAME_LANGUAGE 같은 언어끼리, MBTI MBTI 맞춤 (Figma W3)',
    mbti        CHAR(4)       NULL                     COMMENT 'target 이 MBTI 일 때만 (그 외에는 NULL)',
    path_json   JSON          NULL                     COMMENT '첨부한 GPX의 경로. NULL 이면 코스 경로를 씀 (GPX 파일은 저장 안 함)',
    status      VARCHAR(10)   NOT NULL DEFAULT 'OPEN'  COMMENT 'OPEN, CLOSED 마감, CANCELED 모집 취소 (화면에 보낼 상태는 v_match_status)',
    is_hidden   BOOLEAN       NOT NULL DEFAULT FALSE   COMMENT '관리자가 숨긴 글 → 목록 제외, 상세 404',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    KEY idx_matches_meet_at (meet_at),
    KEY idx_matches_course (course_id),
    KEY idx_matches_host (host_id),
    CONSTRAINT fk_matches_host   FOREIGN KEY (host_id)   REFERENCES users (user_id),
    CONSTRAINT fk_matches_course FOREIGN KEY (course_id) REFERENCES courses (course_id),
    CONSTRAINT chk_matches_capacity CHECK (capacity BETWEEN 2 AND 10),
    CONSTRAINT chk_matches_target   CHECK (target IN ('ANYONE','EXCHANGE','SAME_LANGUAGE','MBTI')),
    CONSTRAINT chk_matches_mbti     CHECK ((target = 'MBTI' AND mbti IS NOT NULL) OR (target <> 'MBTI' AND mbti IS NULL)),
    CONSTRAINT chk_matches_status   CHECK (status IN ('OPEN','CLOSED','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='동행 모집 글 (담당: 민서)';


-- ---------------------------------------------------------------------
-- 동행이 원하는 스타일 · 언어 · 연령대 (담당: 민서)
-- 값 하나당 한 줄. "中文(ZH) 가능한 동행만" 같은 필터에 씁니다.
-- 코드는 user_tags 와 같고, 연령대는 users.age_group 과 같은 값 (20s, 30s, 40s, 50s+)
-- ---------------------------------------------------------------------
CREATE TABLE match_tags (
    match_id   BIGINT       NOT NULL,
    tag_type   VARCHAR(10)  NOT NULL COMMENT 'STYLE, LANGUAGE, AGE_GROUP',
    tag_value  VARCHAR(30)  NOT NULL COMMENT '영어 코드 (SLOW, KO, 20s …)',
    PRIMARY KEY (match_id, tag_type, tag_value),
    KEY idx_match_tags_value (tag_type, tag_value),
    CONSTRAINT fk_match_tags_match FOREIGN KEY (match_id) REFERENCES matches (match_id) ON DELETE CASCADE,
    CONSTRAINT chk_match_tags_value CHECK (
        (tag_type = 'STYLE'     AND tag_value IN ('SLOW','FAST','QUIET','CHAT','PHOTO','SUNRISE_SUNSET','DOG'))
     OR (tag_type = 'LANGUAGE'  AND tag_value IN ('KO','ZH','EN','VI','RU'))
     OR (tag_type = 'AGE_GROUP' AND tag_value IN ('20s','30s','40s','50s+')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='동행 태그 (담당: 민서)';


-- ---------------------------------------------------------------------
-- 동행 참여자 (담당: 민서)
-- 모집자도 글을 쓸 때 자동으로 한 줄 들어갑니다 (matches.host_id 와 같은 사람).
-- 참여 인원 = status 가 JOINED 인 행 수 (저장하지 않고 셉니다).
-- 참여 취소 후 다시 참여하면 같은 행의 status 를 JOINED 로 되돌립니다.
-- KICKED(내보내짐) 는 같은 글에 다시 참여할 수 없습니다.
-- ---------------------------------------------------------------------
CREATE TABLE match_participants (
    match_id      BIGINT        NOT NULL,
    user_id       BIGINT        NOT NULL,
    status        VARCHAR(10)   NOT NULL DEFAULT 'JOINED' COMMENT 'JOINED 참여 중, LEFT 참여 취소, KICKED 내보내짐',
    leave_reason  VARCHAR(200)  NULL                      COMMENT '참여 취소 사유',
    joined_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id, user_id),
    KEY idx_match_participants_user (user_id, status),
    CONSTRAINT fk_match_participants_match FOREIGN KEY (match_id) REFERENCES matches (match_id) ON DELETE CASCADE,
    CONSTRAINT fk_match_participants_user  FOREIGN KEY (user_id)  REFERENCES users (user_id),
    CONSTRAINT chk_match_participants_status CHECK (status IN ('JOINED','LEFT','KICKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='동행 참여자 (담당: 민서)';


-- ---------------------------------------------------------------------
-- 동행 상태 계산 뷰 (담당: 민서)
-- 화면에 보낼 status 와 현재 참여 인원은 항상 이 뷰에서 꺼냅니다.
--   우선순위: CANCELED > DONE(일시 지남) > CLOSED > FULL(정원 참) > OPEN
-- 사용 예) SELECT m.*, s.current_count, s.status AS display_status
--          FROM matches m JOIN v_match_status s ON s.match_id = m.match_id
-- 끝의 COLLATE 는 지우지 마세요. 자바 드라이버 설정에 따라 s.status = 'DONE' 비교에서
-- "Illegal mix of collations" 오류가 나는 것을 막습니다.
-- ---------------------------------------------------------------------
CREATE VIEW v_match_status AS
SELECT m.match_id,
       COUNT(p.user_id) AS current_count,
       (CASE
           WHEN m.status = 'CANCELED'          THEN 'CANCELED'
           WHEN m.meet_at <= NOW()             THEN 'DONE'
           WHEN m.status = 'CLOSED'            THEN 'CLOSED'
           WHEN COUNT(p.user_id) >= m.capacity THEN 'FULL'
           ELSE 'OPEN'
       END) COLLATE utf8mb4_0900_ai_ci AS status
FROM matches m
LEFT JOIN match_participants p
       ON p.match_id = m.match_id AND p.status = 'JOINED'
GROUP BY m.match_id;


-- =====================================================================
-- 2주차 테이블
-- =====================================================================

-- ---------------------------------------------------------------------
-- 랜드마크 (담당: 동언)
-- 관리자 화면 없이 seed.sql 로 미리 넣어 둡니다 (8곳, 한국관광공사 관광정보 좌표).
-- 코스 상세의 landmarks 는 저장하지 않고 계산합니다: 코스 경로가 radius_m 안을 지나는 랜드마크.
-- ---------------------------------------------------------------------
CREATE TABLE landmarks (
    landmark_id  BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)   NOT NULL,
    area         VARCHAR(20)   NOT NULL                 COMMENT 'OKGU, JEONGWANG, BAEGOT, SIHWA (코스 지역과 같은 코드)',
    lat          DECIMAL(9,6)  NOT NULL                 COMMENT '위도',
    lng          DECIMAL(9,6)  NOT NULL                 COMMENT '경도',
    radius_m     INT           NOT NULL DEFAULT 300     COMMENT '인증 반경 (m). 경로의 한 점이라도 이 안이면 스탬프',
    description  VARCHAR(200)  NULL                     COMMENT '짧은 소개',
    sort_order   INT           NOT NULL DEFAULT 0       COMMENT '목록에 보일 순서',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (landmark_id),
    UNIQUE KEY uk_landmarks_name (name),
    CONSTRAINT chk_landmarks_area   CHECK (area IN ('OKGU','JEONGWANG','BAEGOT','SIHWA')),
    CONSTRAINT chk_landmarks_radius CHECK (radius_m BETWEEN 50 AND 1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='랜드마크 (담당: 동언)';


-- ---------------------------------------------------------------------
-- 회원이 받은 스탬프 (담당: 동언)
-- 랜드마크 하나당 한 번만 (같은 곳을 또 걸어도 alreadyStamped 로 응답).
-- 배지는 저장하지 않고 스탬프 개수로 계산합니다:
--   FIRST_STAMP 1개, WALKER 3개, SIHEUNG_MASTER 랜드마크 전부
-- ---------------------------------------------------------------------
CREATE TABLE user_stamps (
    user_id      BIGINT    NOT NULL,
    landmark_id  BIGINT    NOT NULL,
    stamped_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, landmark_id),
    KEY idx_user_stamps_landmark (landmark_id),
    CONSTRAINT fk_user_stamps_user     FOREIGN KEY (user_id)     REFERENCES users (user_id),
    CONSTRAINT fk_user_stamps_landmark FOREIGN KEY (landmark_id) REFERENCES landmarks (landmark_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='회원 스탬프 (담당: 동언)';


-- ---------------------------------------------------------------------
-- 후기 (담당: 정란)
-- 동행으로 걸었으면 match_id 를 넣습니다 (withMatch, 내 동행 past 의 reviewed 판단).
-- 같은 동행에는 후기 1개만 (uk_reviews_user_match). 동행 없이 쓴 후기(match_id NULL)는 제한 없음.
-- 작성자가 지우면 실제로 삭제합니다 (사진 행도 같이 삭제됨). 관리자가 숨기면 is_hidden = TRUE.
-- 별점 평균 · 후기 수는 저장하지 않고 v_course_rating 뷰에서 계산합니다.
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
    review_id   BIGINT    NOT NULL AUTO_INCREMENT,
    user_id     BIGINT    NOT NULL                  COMMENT '작성자',
    course_id   BIGINT    NOT NULL,
    match_id    BIGINT    NULL                      COMMENT '동행으로 걸었을 때만',
    rating      TINYINT   NOT NULL                  COMMENT '별점 1~5',
    content     TEXT      NOT NULL                  COMMENT '10자 이상',
    is_hidden   BOOLEAN   NOT NULL DEFAULT FALSE    COMMENT '관리자가 숨긴 후기 (신고 처리 HIDE) → 목록·별점에서 제외',
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_reviews_user_match (user_id, match_id),
    KEY idx_reviews_course (course_id, created_at),
    CONSTRAINT fk_reviews_user   FOREIGN KEY (user_id)   REFERENCES users (user_id),
    CONSTRAINT fk_reviews_course FOREIGN KEY (course_id) REFERENCES courses (course_id),
    CONSTRAINT fk_reviews_match  FOREIGN KEY (match_id)  REFERENCES matches (match_id),
    CONSTRAINT chk_reviews_rating  CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_content CHECK (CHAR_LENGTH(content) >= 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='후기 (담당: 정란)';


-- ---------------------------------------------------------------------
-- 후기 사진 (담당: 정란)
-- 사진 파일은 각자 PC의 업로드 폴더(예: C:/dev/jspTeam/uploads)에 저장하고,
-- DB에는 그 폴더 기준 상대 경로만 저장합니다. 예) reviews/88-1.jpg
-- 화면 주소는 /files/ + file_path → /files/reviews/88-1.jpg
-- 후기 1개당 최대 5장 (sort_order 1~5)
-- ---------------------------------------------------------------------
CREATE TABLE review_photos (
    photo_id    BIGINT        NOT NULL AUTO_INCREMENT,
    review_id   BIGINT        NOT NULL,
    file_path   VARCHAR(255)  NOT NULL                 COMMENT '업로드 폴더 기준 상대 경로',
    sort_order  TINYINT       NOT NULL DEFAULT 1       COMMENT '사진 순서 1~5',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (photo_id),
    UNIQUE KEY uk_review_photos_order (review_id, sort_order),
    CONSTRAINT fk_review_photos_review FOREIGN KEY (review_id) REFERENCES reviews (review_id) ON DELETE CASCADE,
    CONSTRAINT chk_review_photos_order CHECK (sort_order BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='후기 사진 (담당: 정란)';


-- ---------------------------------------------------------------------
-- 신고 (담당: 정란 · 관리자 AD1)
-- 신고 대상이 4종류라 외래키 대신 target_type + target_id 로 가리킵니다.
--   MATCH → matches.match_id, USER → users.user_id, COURSE → courses.course_id, REVIEW → reviews.review_id
-- 같은 사람이 같은 대상을 두 번 신고할 수 없습니다 (uk_reports_once → 409 DUPLICATE).
-- 관리자 처리: status 를 DONE 으로, action 에 결과 기록
--   HIDE    대상 숨김 (matches/courses/reviews 의 is_hidden = TRUE)
--   SUSPEND 회원 정지 (users.status = 'SUSPENDED', 대상이 글이면 그 작성자)
--   REJECT  기각 (아무것도 바꾸지 않음)
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    report_id     BIGINT        NOT NULL AUTO_INCREMENT,
    reporter_id   BIGINT        NOT NULL                  COMMENT '신고한 사람',
    target_type   VARCHAR(10)   NOT NULL                  COMMENT 'MATCH, USER, COURSE, REVIEW',
    target_id     BIGINT        NOT NULL                  COMMENT 'target_type 에 맞는 테이블의 id',
    reason        VARCHAR(20)   NOT NULL                  COMMENT 'SPAM 광고, ABUSE 욕설·비방, INAPPROPRIATE 부적절, FAKE 사실과 다름, ETC 기타',
    detail        VARCHAR(300)  NULL                      COMMENT '자세한 내용 (선택)',
    status        VARCHAR(10)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 대기, DONE 처리 완료',
    action        VARCHAR(10)   NULL                      COMMENT 'HIDE, SUSPEND, REJECT (처리 완료일 때만)',
    admin_memo    VARCHAR(500)  NULL                      COMMENT '관리자 메모',
    processed_by  BIGINT        NULL                      COMMENT '처리한 관리자',
    processed_at  DATETIME      NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_reports_once (reporter_id, target_type, target_id),
    KEY idx_reports_status (status, created_at),
    CONSTRAINT fk_reports_reporter  FOREIGN KEY (reporter_id)  REFERENCES users (user_id),
    CONSTRAINT fk_reports_processor FOREIGN KEY (processed_by) REFERENCES users (user_id),
    CONSTRAINT chk_reports_target CHECK (target_type IN ('MATCH','USER','COURSE','REVIEW')),
    CONSTRAINT chk_reports_reason CHECK (reason IN ('SPAM','ABUSE','INAPPROPRIATE','FAKE','ETC')),
    -- 주의: MySQL 검사 규칙은 결과가 NULL 이면 통과로 칩니다. 그래서 action IS NOT NULL 을 꼭 같이 씁니다.
    CONSTRAINT chk_reports_status CHECK ((status = 'PENDING' AND action IS NULL)
                                      OR (status = 'DONE' AND action IS NOT NULL AND action IN ('HIDE','SUSPEND','REJECT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='신고 (담당: 정란)';


-- ---------------------------------------------------------------------
-- 동행 댓글 (담당: 민서)
-- 참여자와 모집자만 읽고 씁니다 (서블릿에서 검사). 모집자가 is_notice = TRUE 로 쓰면 W4 상단 공지.
-- ---------------------------------------------------------------------
CREATE TABLE match_comments (
    comment_id  BIGINT        NOT NULL AUTO_INCREMENT,
    match_id    BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL                 COMMENT '작성자',
    content     VARCHAR(500)  NOT NULL,
    is_notice   BOOLEAN       NOT NULL DEFAULT FALSE   COMMENT '모집자 공지',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    KEY idx_match_comments_match (match_id, created_at),
    CONSTRAINT fk_match_comments_match FOREIGN KEY (match_id) REFERENCES matches (match_id) ON DELETE CASCADE,
    CONSTRAINT fk_match_comments_user  FOREIGN KEY (user_id)  REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='동행 댓글 (담당: 민서)';


-- ---------------------------------------------------------------------
-- 코스 별점 계산 뷰 (담당: 정란 · 동언)
-- 코스 목록 · 상세의 rating, reviewCount 와 후기 목록의 avgRating 은 이 뷰에서 꺼냅니다.
-- 관리자가 숨긴 후기는 빼고 계산합니다. 후기가 없으면 0.
-- 사용 예) SELECT c.*, r.avg_rating, r.review_count
--          FROM courses c JOIN v_course_rating r ON r.course_id = c.course_id
-- ---------------------------------------------------------------------
CREATE VIEW v_course_rating AS
SELECT c.course_id,
       COUNT(r.review_id)                 AS review_count,
       IFNULL(ROUND(AVG(r.rating), 1), 0) AS avg_rating
FROM courses c
LEFT JOIN reviews r
       ON r.course_id = c.course_id AND r.is_hidden = FALSE
GROUP BY c.course_id;
