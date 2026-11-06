-- =====================================================================
-- 모집 대상을 칸(matches.target)에서 태그(match_tags TARGET)로 옮기기
-- 데이터를 지우지 않고 DB 구조만 새 코드에 맞춥니다.
--
-- 누가:   PR #33 이전의 DB 를 쓰고 있고, 내 데이터를 지키고 싶은 사람
--         (시험 데이터만 있으면 schema.sql → seed.sql 다시 실행이 더 간단합니다)
-- 언제:   git pull 로 PR #33 이후 코드를 받은 뒤 한 번만
-- 실행 뒤: 백엔드를 다시 시작합니다
--
-- 무엇을 바꾸나:
--   1. match_tags 가 TARGET 값을 받을 수 있게 규칙을 고칩니다
--   2. matches.target 값을 match_tags 로 옮겨 적습니다 (ANYONE 은 태그 없음 = 누구나)
--   3. 필요 없어진 matches.target 칸과 규칙 2개를 지웁니다
--
-- 두 번 실행하면: 2번에서 Unknown column 'target' 오류가 나고 멈춥니다. 데이터는 그대로입니다.
-- =====================================================================

USE sanchaek;

ALTER TABLE match_tags DROP CHECK chk_match_tags_value;
ALTER TABLE match_tags
  MODIFY tag_type VARCHAR(10) NOT NULL COMMENT 'STYLE, LANGUAGE, AGE_GROUP, TARGET',
  ADD CONSTRAINT chk_match_tags_value CHECK (
    (tag_type = 'STYLE'     AND tag_value IN ('SLOW','FAST','QUIET','CHAT','PHOTO','SUNRISE_SUNSET','DOG'))
 OR (tag_type = 'LANGUAGE'  AND tag_value IN ('KO','ZH','EN','VI','RU'))
 OR (tag_type = 'AGE_GROUP' AND tag_value IN ('20s','30s','40s','50s+'))
 OR (tag_type = 'TARGET'    AND tag_value IN ('EXCHANGE','SAME_LANGUAGE','MBTI')));

INSERT INTO match_tags (match_id, tag_type, tag_value)
  SELECT match_id, 'TARGET', target FROM matches WHERE target <> 'ANYONE';

ALTER TABLE matches DROP CHECK chk_matches_mbti, DROP CHECK chk_matches_target;
ALTER TABLE matches
  DROP COLUMN target,
  MODIFY mbti CHAR(4) NULL COMMENT '모집 대상에 MBTI 맞춤(match_tags TARGET = MBTI)이 있을 때만 (그 외에는 NULL)';
