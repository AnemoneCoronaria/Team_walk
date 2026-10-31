package kr.sanchaek.review;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {
    public record Author(long userId, String nickname, String avatar) {}
    /** C2 · C4 코스 후기 목록의 한 줄 */
    public record Review(long reviewId, Author author, int rating, String content, List<String> photos,
            boolean withMatch, LocalDateTime createdAt) {}
    /** 후기 목록 data: 공통 목록 모양 + 코스 평균 별점 */
    public record ReviewPage(List<Review> items, int page, int size, long total, double avgRating) {}
    /** 후기에 붙일 동행 확인용: 코스 · 화면 상태 · 이 사람의 참여 상태 */
    public record MatchInfo(long courseId, String status, String participantStatus) {}

    private static final String COLUMNS = """
            r.review_id, r.rating, r.content, r.match_id IS NOT NULL AS with_match, r.created_at,
            u.user_id, u.nickname, u.avatar, u.status AS user_status
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ReviewRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<Review> courseReviews(long courseId, String order, boolean photoOnly, int page, int size) {
        String where = " FROM reviews r JOIN users u ON u.user_id = r.user_id "
                + "WHERE r.course_id = :courseId AND r.is_hidden = FALSE"
                + (photoOnly ? " AND EXISTS (SELECT 1 FROM review_photos p WHERE p.review_id = r.review_id)" : "");
        MapSqlParameterSource params = new MapSqlParameterSource("courseId", courseId);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, params, Long.class);
        params.addValue("size", size).addValue("offset", PageResult.offset(page, size));
        List<Review> items = jdbc.query("SELECT " + COLUMNS + where + " ORDER BY " + order
                + ", r.review_id DESC LIMIT :size OFFSET :offset", params, (rs, n) -> review(rs));
        return new PageResult<>(items, page, size, total);
    }

    public double avgRating(long courseId) {
        List<Double> rows = jdbc.queryForList("SELECT avg_rating FROM v_course_rating WHERE course_id = :id",
                new MapSqlParameterSource("id", courseId), Double.class);
        return rows.isEmpty() || rows.get(0) == null ? 0 : rows.get(0);
    }

    /** 동행이 없거나 숨겨졌으면 null */
    public MatchInfo matchInfo(long matchId, long userId) {
        List<MatchInfo> rows = jdbc.query("""
                SELECT m.course_id, s.status,
                       (SELECT p.status FROM match_participants p
                        WHERE p.match_id = m.match_id AND p.user_id = :userId) AS participant_status
                FROM matches m JOIN v_match_status s ON s.match_id = m.match_id
                WHERE m.match_id = :matchId AND m.is_hidden = FALSE
                """, new MapSqlParameterSource("matchId", matchId).addValue("userId", userId),
                (rs, n) -> new MatchInfo(rs.getLong("course_id"), rs.getString("status"),
                        rs.getString("participant_status")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean hasReviewForMatch(long userId, long matchId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM reviews WHERE user_id = :userId AND match_id = :matchId)",
                new MapSqlParameterSource("userId", userId).addValue("matchId", matchId), Boolean.class));
    }

    public long insert(long userId, long courseId, Long matchId, int rating, String content) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO reviews (user_id, course_id, match_id, rating, content)
                VALUES (:userId, :courseId, :matchId, :rating, :content)
                """, new MapSqlParameterSource("userId", userId).addValue("courseId", courseId)
                .addValue("matchId", matchId).addValue("rating", rating).addValue("content", content),
                key, new String[] {"review_id"});
        return key.getKey().longValue();
    }

    public void insertPhoto(long reviewId, String filePath, int order) {
        jdbc.update("INSERT INTO review_photos (review_id, file_path, sort_order) VALUES (:id, :path, :order)",
                new MapSqlParameterSource("id", reviewId).addValue("path", filePath).addValue("order", order));
    }

    public record CourseRef(long courseId, String name) {}
    /** P4 내 후기: 코스 후기 목록과 같은 모양 + course */
    public record MyReview(long reviewId, Author author, int rating, String content, List<String> photos,
            boolean withMatch, LocalDateTime createdAt, CourseRef course) {}

    /** 관리자가 숨긴 후기는 빼고 최신순. 코스가 지워졌어도 후기는 보여 준다 */
    public PageResult<MyReview> userReviews(long userId, int page, int size) {
        String where = " FROM reviews r JOIN users u ON u.user_id = r.user_id "
                + "JOIN courses c ON c.course_id = r.course_id WHERE r.user_id = :userId AND r.is_hidden = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, params, Long.class);
        params.addValue("size", size).addValue("offset", PageResult.offset(page, size));
        List<MyReview> items = jdbc.query("SELECT " + COLUMNS + ", c.course_id, c.name AS course_name" + where
                + " ORDER BY r.created_at DESC, r.review_id DESC LIMIT :size OFFSET :offset", params, (rs, n) -> {
                    Review r = review(rs);
                    return new MyReview(r.reviewId(), r.author(), r.rating(), r.content(), r.photos(),
                            r.withMatch(), r.createdAt(), new CourseRef(rs.getLong("course_id"),
                                    rs.getString("course_name")));
                });
        return new PageResult<>(items, page, size, total);
    }

    /** 후기 작성자. 없으면 null. 삭제 전에 행을 잠근다 */
    public Long authorForUpdate(long reviewId) {
        List<Long> rows = jdbc.queryForList("SELECT user_id FROM reviews WHERE review_id = :id FOR UPDATE",
                new MapSqlParameterSource("id", reviewId), Long.class);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<String> photoPaths(long reviewId) {
        return jdbc.queryForList("SELECT file_path FROM review_photos WHERE review_id = :id ORDER BY sort_order",
                new MapSqlParameterSource("id", reviewId), String.class);
    }

    /** 후기는 실제로 삭제 (사진 행은 ON DELETE CASCADE) */
    public void delete(long reviewId) {
        jdbc.update("DELETE FROM reviews WHERE review_id = :id", new MapSqlParameterSource("id", reviewId));
    }

    private List<String> photos(long reviewId) {
        return jdbc.queryForList("SELECT file_path FROM review_photos WHERE review_id = :id ORDER BY sort_order",
                new MapSqlParameterSource("id", reviewId), String.class)
                .stream().map(FileStore::url).toList();
    }

    Review review(ResultSet rs) throws SQLException {
        long id = rs.getLong("review_id");
        String nickname = "WITHDRAWN".equals(rs.getString("user_status")) ? "탈퇴한 회원" : rs.getString("nickname");
        return new Review(id, new Author(rs.getLong("user_id"), nickname, rs.getString("avatar")),
                rs.getInt("rating"), rs.getString("content"), photos(id), rs.getBoolean("with_match"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
