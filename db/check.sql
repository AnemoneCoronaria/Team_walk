-- =====================================================================
-- 산책시루 DB 세팅 확인
-- schema.sql → seed.sql 실행 직후에 돌립니다. 5줄 모두 OK 면 성공.
-- (cmd 창에서 한글이 깨져도 읽을 수 있게 결과는 영어로 표시합니다)
-- =====================================================================

SET NAMES utf8mb4;
USE sanchaek;

SELECT 'tables 7 + view 1' AS check_item,
       IF((SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = 'sanchaek' AND table_type = 'BASE TABLE') = 7
          AND (SELECT COUNT(*) FROM information_schema.views WHERE table_schema = 'sanchaek') = 1,
          'OK', 'FAIL') AS result
UNION ALL
SELECT 'seed rows (users 7, courses 4, matches 6)',
       IF((SELECT COUNT(*) FROM users) = 7 AND (SELECT COUNT(*) FROM courses) = 4
          AND (SELECT COUNT(*) FROM matches) = 6, 'OK', 'FAIL')
UNION ALL
SELECT 'match status (OPEN,OPEN,FULL,CLOSED,CANCELED,DONE)',
       IF((SELECT GROUP_CONCAT(status ORDER BY match_id) FROM v_match_status)
          = 'OPEN,OPEN,FULL,CLOSED,CANCELED,DONE', 'OK', 'FAIL')
UNION ALL
SELECT 'korean / chinese text saved',
       IF((SELECT HEX(nickname) FROM users WHERE user_id = 2) = 'EBAFBCEC849C'
          AND (SELECT COUNT(*) FROM user_tags WHERE HEX(tag_value) = 'E4B8ADE69687') = 1, 'OK', 'FAIL')
UNION ALL
SELECT 'database charset utf8mb4',
       IF((SELECT default_character_set_name FROM information_schema.schemata
            WHERE schema_name = 'sanchaek') = 'utf8mb4', 'OK', 'FAIL');
