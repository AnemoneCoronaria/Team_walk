package kr.sanchaek.landmark;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LandmarkRepository {
    /** 랜드마크 한 곳 + 이 사람의 스탬프 시각 (없으면 null) */
    public record Row(long landmarkId, String name, String area, double lat, double lng, int radiusM,
            LocalDateTime stampedAt) {}
    public record Stamp(long landmarkId, String name, LocalDateTime stampedAt) {}
    /** 끝난(DONE) 동행에 끝까지 참여한 기록 */
    public record DoneMatch(LocalDateTime meetAt, String target) {}

    private final NamedParameterJdbcTemplate jdbc;

    public LandmarkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 로그인 안 했으면 userId 0 → 스탬프 없음 */
    public List<Row> landmarks(Long userId) {
        return jdbc.query("""
                SELECT l.landmark_id, l.name, l.area, l.lat, l.lng, l.radius_m, s.stamped_at
                FROM landmarks l
                LEFT JOIN user_stamps s ON s.landmark_id = l.landmark_id AND s.user_id = :userId
                ORDER BY l.sort_order, l.landmark_id
                """, new MapSqlParameterSource("userId", userId == null ? 0L : userId),
                (rs, n) -> new Row(rs.getLong("landmark_id"), rs.getString("name"), rs.getString("area"),
                        rs.getDouble("lat"), rs.getDouble("lng"), rs.getInt("radius_m"),
                        rs.getObject("stamped_at", LocalDateTime.class)));
    }

    public long countLandmarks() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM landmarks", new MapSqlParameterSource(), Long.class);
    }

    /** 위치 정보 동의 여부. 같은 사람이 동시에 인증하지 않도록 회원 행을 잠근다. 회원이 없으면 null */
    public Boolean agreeLocationForUpdate(long userId) {
        List<Boolean> rows = jdbc.queryForList("SELECT agree_location FROM users WHERE user_id = :userId FOR UPDATE",
                new MapSqlParameterSource("userId", userId), Boolean.class);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 이미 받은 곳이면 false */
    public boolean addStamp(long userId, long landmarkId) {
        return jdbc.update("INSERT IGNORE INTO user_stamps (user_id, landmark_id) VALUES (:userId, :landmarkId)",
                new MapSqlParameterSource("userId", userId).addValue("landmarkId", landmarkId)) == 1;
    }

    /** 받은 순서대로 */
    public List<Stamp> stamps(long userId) {
        return jdbc.query("""
                SELECT s.landmark_id, l.name, s.stamped_at
                FROM user_stamps s JOIN landmarks l ON l.landmark_id = s.landmark_id
                WHERE s.user_id = :userId
                ORDER BY s.stamped_at, s.landmark_id
                """, new MapSqlParameterSource("userId", userId),
                (rs, n) -> new Stamp(rs.getLong("landmark_id"), rs.getString("name"),
                        rs.getObject("stamped_at", LocalDateTime.class)));
    }

    /** 만난 순서대로 */
    public List<DoneMatch> doneMatches(long userId) {
        return jdbc.query("""
                SELECT m.meet_at, m.target
                FROM match_participants p
                JOIN matches m        ON m.match_id = p.match_id
                JOIN v_match_status s ON s.match_id = p.match_id
                WHERE p.user_id = :userId AND p.status = 'JOINED' AND s.status = 'DONE'
                ORDER BY m.meet_at, m.match_id
                """, new MapSqlParameterSource("userId", userId),
                (rs, n) -> new DoneMatch(rs.getObject("meet_at", LocalDateTime.class), rs.getString("target")));
    }
}
