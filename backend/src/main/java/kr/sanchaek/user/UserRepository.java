package kr.sanchaek.user;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 계정 DB 접근 (users, user_tags). 담당: 정란 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 아이디가 이미 있는지 (탈퇴 회원 포함. 대소문자 구분 안 함은 DB 정렬 규칙 _ci 가 처리) */
    public boolean existsLoginId(String loginId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE login_id = ?", Integer.class, loginId);
        return count != null && count > 0;
    }

    /** 회원 + 스타일 · 언어 태그를 한 번에 저장. 하나라도 실패하면 모두 취소 */
    @Transactional
    public long insertUser(String loginId, String passwordHash, String nickname, String avatar,
            String ageGroup, String mbti, boolean agreeLocation, List<String> styles, List<String> languages) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String sql = "INSERT INTO users (login_id, password_hash, nickname, avatar, age_group, mbti, "
                   + "agree_terms_at, agree_privacy_at, agree_location, agree_location_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, loginId);
            ps.setString(2, passwordHash);
            ps.setString(3, nickname);
            ps.setString(4, avatar);
            ps.setString(5, ageGroup);
            ps.setString(6, mbti);
            ps.setObject(7, now);
            ps.setObject(8, now);
            ps.setBoolean(9, agreeLocation);
            ps.setObject(10, agreeLocation ? now : null);
            return ps;
        }, keyHolder);
        long userId = keyHolder.getKey().longValue();

        String tagSql = "INSERT INTO user_tags (user_id, tag_type, tag_value) VALUES (?, ?, ?)";
        for (String style : styles) {
            jdbc.update(tagSql, userId, "STYLE", style);
        }
        for (String language : languages) {
            jdbc.update(tagSql, userId, "LANGUAGE", language);
        }
        return userId;
    }

    /** 로그인 확인용. 없으면 null */
    public UserAccount findAccountByLoginId(String loginId) {
        String sql = "SELECT user_id, password_hash, nickname, avatar, role, status, suspended_until "
                   + "FROM users WHERE login_id = ?";
        List<UserAccount> rows = jdbc.query(sql, (rs, i) -> new UserAccount(
                rs.getLong("user_id"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getString("avatar"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getObject("suspended_until", LocalDateTime.class)), loginId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 정지 기간이 지난 회원을 정상으로 되돌림 */
    public void releaseSuspension(long userId) {
        jdbc.update("UPDATE users SET status = 'ACTIVE', suspended_until = NULL "
                  + "WHERE user_id = ? AND status = 'SUSPENDED'", userId);
    }

    /** 내 정보. 없거나 탈퇴한 회원이면 null */
    public MyInfo findMyInfo(long userId) {
        String sql = "SELECT user_id, login_id, nickname, avatar, age_group, mbti, agree_location, role, created_at "
                   + "FROM users WHERE user_id = ? AND status <> 'WITHDRAWN'";
        List<MyInfo> rows = jdbc.query(sql, (rs, i) -> {
            long id = rs.getLong("user_id");
            MatchStats stats = findMatchStats(id);
            return new MyInfo(
                    id,
                    rs.getString("login_id"),
                    rs.getString("nickname"),
                    rs.getString("avatar"),
                    rs.getString("age_group"),
                    findTags(id, "STYLE"),
                    findTags(id, "LANGUAGE"),
                    rs.getString("mbti"),
                    rs.getBoolean("agree_location"),
                    rs.getString("role"),
                    stats.matchCount(),
                    countMyCourses(id),
                    stats.totalDistanceKm(),
                    rs.getObject("created_at", LocalDateTime.class));
        }, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 회원의 스타일(STYLE) 또는 언어(LANGUAGE) 코드 목록 */
    private List<String> findTags(long userId, String tagType) {
        return jdbc.queryForList(
                "SELECT tag_value FROM user_tags WHERE user_id = ? AND tag_type = ? ORDER BY tag_value",
                String.class, userId, tagType);
    }

    private record MatchStats(int matchCount, double totalDistanceKm) {
    }

    /** 끝난(DONE) 동행에 JOINED 로 남은 횟수와 그 코스 거리의 합 */
    private MatchStats findMatchStats(long userId) {
        String sql = "SELECT COUNT(*) AS match_count, ROUND(IFNULL(SUM(c.distance_km), 0), 2) AS total_km "
                   + "FROM match_participants p "
                   + "JOIN matches m        ON m.match_id = p.match_id "
                   + "JOIN v_match_status s ON s.match_id = p.match_id "
                   + "JOIN courses c        ON c.course_id = m.course_id "
                   + "WHERE p.user_id = ? AND p.status = 'JOINED' AND s.status = 'DONE'";
        return jdbc.queryForObject(sql, (rs, i) -> new MatchStats(
                rs.getInt("match_count"),
                rs.getDouble("total_km")), userId);
    }

    /** 내가 올린 코스 수 (내가 삭제한 코스는 빼고) */
    private int countMyCourses(long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE author_id = ? AND is_deleted = FALSE", Integer.class, userId);
        return count == null ? 0 : count;
    }
}
