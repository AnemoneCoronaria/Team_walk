package kr.sanchaek.match;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import kr.sanchaek.common.ApiException;

@Repository         // 여기는 DB 건드는 곳...
public class MatchRepository {
    private final JdbcTemplate jdbc;        // jdbc 사용

    // 스프링부트가 자동으로 만들어준 JdbcTemplate을 받아옴 ... DI?
    public MatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 여기서 sql 문 하드코딩 
    // 동행 목록 (Anemone)
    /*
     * 현재 모집 중이고 숨겨지지 않은 매칭 목록(OPEN된 것만)을 조회합니다.
     * SQL 쿼리는 matches 테이블과 v_match_status 뷰를 조인하여,
     * is_hidden = FALSE 이며 status = 'OPEN' 인 매칭만 가져옵니다.
     * 조회된 정보는 meet_at 기준 오름차순으로 정렬됩니다.
     */
    public List<Map<String, Object>> findOpenMatches() {
        String sql = """
            SELECT m.match_id AS matchId, m.title, m.meet_at AS meetAt, s.current_count AS current, m.capacity, s.status
            FROM matches m
            JOIN v_match_status s ON s.match_id = m.match_id
            WHERE m.is_hidden = FALSE AND s.status = 'OPEN'
            ORDER BY m.meet_at
            """;

        return jdbc.queryForList(sql);
    }


    // 동행 상세 (Anemone)
    /*
     * 특정 매칭 ID에 해당하는 상세 정보를 조회합니다.
     * SQL 쿼리는 matches 테이블과 v_match_status 뷰를 조인하여,
     * is_hidden = FALSE 인 매칭만 조회합니다.
     * matchId에 해당하는 행이 없을 경우 NotFoundException을 발생시킵니다.
     */
    public Map<String, Object> findMatchById(Long matchId) {
        String sql = """
            SELECT m.match_id AS matchId, 
                   m.title,
                   m.meet_at AS meetAt,
                   s.current_count AS current, 
                   m.capacity, 
                   s.status,
                   m.content AS description
            FROM matches m
            JOIN v_match_status s ON s.match_id = m.match_id
            WHERE m.is_hidden = FALSE
              AND m.match_id = ?
            """;
        // return jdbc.queryForMap(sql, matchId);

        // matchId가 없는걸 요구하면 404 error 나도록 함
        List<Map<String, Object>> rows = jdbc.queryForList(sql, matchId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("동행 찾을 수 없음");
        }

        return rows.get(0);

    }



}
