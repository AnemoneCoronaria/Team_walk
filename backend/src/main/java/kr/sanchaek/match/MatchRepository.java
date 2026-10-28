package kr.sanchaek.match;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 동행 SQL 모음 (matches, match_tags, match_participants, match_comments, v_match_status).
 * 화면에 보낼 상태(FULL · DONE 포함)와 현재 인원은 항상 v_match_status 뷰에서 꺼냅니다.
 */
@Repository
public class MatchRepository {

    /** 탈퇴한 회원은 닉네임 대신 "탈퇴한 회원" (API 명세 회원 탈퇴) */
    static final String NICKNAME = "CASE WHEN u.status = 'WITHDRAWN' THEN '탈퇴한 회원' ELSE u.nickname END";

    /** 목록 한 줄에 필요한 칸 (태그는 따로 한 번에 읽음) */
    private static final String SUMMARY_SELECT = """
            SELECT m.match_id, m.title, m.meet_at, m.target, m.capacity,
                   s.current_count, s.status AS display_status,
                   c.course_id, c.name AS course_name, c.distance_km, c.level,
                   u.user_id AS host_id, %s AS host_nickname, u.avatar AS host_avatar
            """.formatted(NICKNAME);

    private static final String SUMMARY_FROM = """
            FROM matches m
            JOIN v_match_status s ON s.match_id = m.match_id
            JOIN courses c        ON c.course_id = m.course_id
            JOIN users u          ON u.user_id = m.host_id
            """;

    private final JdbcTemplate jdbc;

    public MatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------
    // 목록
    // ------------------------------------------------------------------

    /**
     * 목록 조회. where 는 "m.is_hidden = FALSE AND ..." 처럼 조건만 (값은 ? 로, params 에).
     * reviewedBy 가 있으면 그 사람이 후기를 썼는지(reviewed)도 채웁니다 (내 동행 past 탭).
     */
    public List<MatchSummary> findSummaries(String where, List<Object> params, String orderBy,
            int limit, int offset, Long reviewedBy) {
        String reviewedCol = reviewedBy == null ? ""
                : ", EXISTS (SELECT 1 FROM reviews r WHERE r.match_id = m.match_id AND r.user_id = ?) AS reviewed";
        String sql = SUMMARY_SELECT + reviewedCol + " " + SUMMARY_FROM
                + " WHERE " + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";

        List<Object> args = new ArrayList<>();
        if (reviewedBy != null) {
            args.add(reviewedBy);
        }
        args.addAll(params);
        args.add(limit);
        args.add(offset);

        List<SummaryRow> rows = jdbc.query(sql, (rs, i) -> new SummaryRow(rs, reviewedBy != null), args.toArray());
        Map<Long, Tags> tags = findTags(rows.stream().map(r -> r.matchId).toList());
        return rows.stream().map(r -> r.toSummary(tags.getOrDefault(r.matchId, Tags.EMPTY))).toList();
    }

    public long count(String where, List<Object> params) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) " + SUMMARY_FROM + " WHERE " + where, Long.class,
                params.toArray());
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------
    // 상세
    // ------------------------------------------------------------------

    /** 상세에 필요한 칸. 숨긴 글이거나 없으면 null */
    public DetailRow findDetail(long matchId) {
        String sql = """
                SELECT m.match_id, m.title, m.content, m.meet_at, m.meet_place, m.capacity, m.target, m.mbti,
                       m.gpx_path, s.current_count, s.status AS display_status,
                       c.course_id, c.name AS course_name, c.distance_km, c.level, c.duration_min,
                       COALESCE(m.path_json, c.path_json) AS path,
                       COALESCE(JSON_EXTRACT(m.path_json, '$[0]'), JSON_ARRAY(c.start_lat, c.start_lng)) AS meet_point,
                       u.user_id AS host_id, %s AS host_nickname, u.avatar AS host_avatar
                FROM matches m
                JOIN v_match_status s ON s.match_id = m.match_id
                JOIN courses c        ON c.course_id = m.course_id
                JOIN users u          ON u.user_id = m.host_id
                WHERE m.match_id = ? AND m.is_hidden = FALSE
                """.formatted(NICKNAME);
        List<DetailRow> rows = jdbc.query(sql, (rs, i) -> new DetailRow(rs), matchId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Tags findTags(long matchId) {
        return findTags(List.of(matchId)).getOrDefault(matchId, Tags.EMPTY);
    }

    /** 끝난(DONE) 동행에 참여한 수 — 상세의 host.matchCount, P1 "함께 걸은 동행" */
    public int countDoneMatches(long userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM match_participants p
                JOIN v_match_status s ON s.match_id = p.match_id
                WHERE p.user_id = ? AND p.status = 'JOINED' AND s.status = 'DONE'
                """, Integer.class, userId);
        return n == null ? 0 : n;
    }

    /** 참여 중(JOINED)인 사람. 모집자가 맨 앞 */
    public List<Participant> findParticipants(long matchId, long hostId) {
        String sql = """
                SELECT u.user_id, %s AS nickname, u.avatar
                FROM match_participants p
                JOIN users u ON u.user_id = p.user_id
                WHERE p.match_id = ? AND p.status = 'JOINED'
                ORDER BY (u.user_id = ?) DESC, p.joined_at, u.user_id
                """.formatted(NICKNAME);
        record Row(long userId, String nickname, String avatar) {
        }
        List<Row> rows = jdbc.query(sql, (rs, i) -> new Row(rs.getLong("user_id"), rs.getString("nickname"),
                rs.getString("avatar")), matchId, hostId);

        Map<Long, List<String>> languages = findUserLanguages(rows.stream().map(Row::userId).toList());
        List<Participant> result = new ArrayList<>();
        for (Row r : rows) {
            result.add(new Participant(r.userId(), r.nickname(), r.avatar(),
                    languages.getOrDefault(r.userId(), List.of()), r.userId() == hostId));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 한 글의 기본 정보 (수정 · 취소 · 참여 등에서 권한 · 상태 확인용)
    // ------------------------------------------------------------------

    /** 숨긴 글이거나 없으면 null. forUpdate 면 이 글 행을 잠가서 동시에 참여하는 경우를 막음 */
    public MatchState findState(long matchId, boolean forUpdate) {
        String sql = "SELECT match_id, host_id, capacity, meet_at, status FROM matches"
                + " WHERE match_id = ? AND is_hidden = FALSE" + (forUpdate ? " FOR UPDATE" : "");
        List<MatchState> rows = jdbc.query(sql, (rs, i) -> new MatchState(
                rs.getLong("match_id"),
                rs.getLong("host_id"),
                rs.getInt("capacity"),
                rs.getObject("meet_at", LocalDateTime.class),
                rs.getString("status"),
                0), matchId);
        if (rows.isEmpty()) {
            return null;
        }
        MatchState s = rows.get(0);
        return new MatchState(s.matchId(), s.hostId(), s.capacity(), s.meetAt(), s.savedStatus(),
                countJoined(matchId));
    }

    public int countJoined(long matchId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM match_participants WHERE match_id = ? AND status = 'JOINED'",
                Integer.class, matchId);
        return n == null ? 0 : n;
    }

    /** 그 사람의 참여 상태 (JOINED, LEFT, KICKED). 참여한 적 없으면 null */
    public String findParticipantStatus(long matchId, long userId) {
        List<String> rows = jdbc.queryForList(
                "SELECT status FROM match_participants WHERE match_id = ? AND user_id = ?",
                String.class, matchId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------------
    // 작성 · 수정
    // ------------------------------------------------------------------

    /** 동행에 쓸 수 있는 코스인지: 삭제 · 숨김이 아니고, 전체 공개이거나 내가 올린 코스 */
    public boolean isUsableCourse(long courseId, long userId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM courses
                WHERE course_id = ? AND is_deleted = FALSE AND is_hidden = FALSE
                  AND (visibility = 'PUBLIC' OR author_id = ?)
                """, Integer.class, courseId, userId);
        return n != null && n > 0;
    }

    public long insert(long hostId, MatchForm f, String mbti, String pathJson) {
        String sql = """
                INSERT INTO matches (host_id, course_id, title, content, meet_at, meet_place, capacity, target, mbti, path_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, hostId);
            ps.setLong(2, f.courseId());
            ps.setString(3, f.title().trim());
            ps.setString(4, blankToNull(f.content()));
            ps.setTimestamp(5, Timestamp.valueOf(f.meetAt()));
            ps.setString(6, f.meetPlace().trim());
            ps.setInt(7, f.capacity());
            ps.setString(8, f.target());
            ps.setString(9, mbti);
            ps.setString(10, pathJson);
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    public void updateGpxPath(long matchId, String gpxPath) {
        jdbc.update("UPDATE matches SET gpx_path = ? WHERE match_id = ?", gpxPath, matchId);
    }

    public void update(long matchId, MatchForm f, String mbti) {
        jdbc.update("""
                UPDATE matches
                SET course_id = ?, title = ?, content = ?, meet_at = ?, meet_place = ?, capacity = ?, target = ?, mbti = ?
                WHERE match_id = ?
                """, f.courseId(), f.title().trim(), blankToNull(f.content()), Timestamp.valueOf(f.meetAt()),
                f.meetPlace().trim(), f.capacity(), f.target(), mbti, matchId);
    }

    /** 태그 전부 지우고 다시 넣기 */
    public void replaceTags(long matchId, List<String> ageGroups, List<String> styles, List<String> languages) {
        jdbc.update("DELETE FROM match_tags WHERE match_id = ?", matchId);
        List<Object[]> rows = new ArrayList<>();
        ageGroups.forEach(v -> rows.add(new Object[] { matchId, "AGE_GROUP", v }));
        styles.forEach(v -> rows.add(new Object[] { matchId, "STYLE", v }));
        languages.forEach(v -> rows.add(new Object[] { matchId, "LANGUAGE", v }));
        if (!rows.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (?, ?, ?)", rows);
        }
    }

    /** status 를 OPEN · CLOSED · CANCELED 로 */
    public void updateStatus(long matchId, String status) {
        jdbc.update("UPDATE matches SET status = ? WHERE match_id = ?", status, matchId);
    }

    // ------------------------------------------------------------------
    // 참여
    // ------------------------------------------------------------------

    /** 참여 (처음이면 새 줄, 취소했다가 다시면 같은 줄을 JOINED 로) */
    public void join(long matchId, long userId) {
        jdbc.update("""
                INSERT INTO match_participants (match_id, user_id, status) VALUES (?, ?, 'JOINED')
                ON DUPLICATE KEY UPDATE status = 'JOINED', leave_reason = NULL, joined_at = NOW()
                """, matchId, userId);
    }

    public void leave(long matchId, long userId, String reason) {
        jdbc.update("UPDATE match_participants SET status = 'LEFT', leave_reason = ? WHERE match_id = ? AND user_id = ?",
                blankToNull(reason), matchId, userId);
    }

    public void kick(long matchId, long userId) {
        jdbc.update("UPDATE match_participants SET status = 'KICKED' WHERE match_id = ? AND user_id = ?",
                matchId, userId);
    }

    // ------------------------------------------------------------------
    // GPX
    // ------------------------------------------------------------------

    /** [제목, 동행 GPX 경로, 코스 GPX 경로]. 숨긴 글이거나 없으면 null */
    public String[] findGpxPaths(long matchId) {
        List<String[]> rows = jdbc.query("""
                SELECT m.title, m.gpx_path, c.gpx_path AS course_gpx_path
                FROM matches m JOIN courses c ON c.course_id = m.course_id
                WHERE m.match_id = ? AND m.is_hidden = FALSE
                """, (rs, i) -> new String[] { rs.getString("title"), rs.getString("gpx_path"),
                rs.getString("course_gpx_path") }, matchId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------------
    // 댓글
    // ------------------------------------------------------------------

    public List<MatchComment> findComments(long matchId, int limit, int offset) {
        String sql = """
                SELECT mc.comment_id, mc.content, mc.is_notice, mc.created_at,
                       u.user_id, %s AS nickname, u.avatar
                FROM match_comments mc
                JOIN users u ON u.user_id = mc.user_id
                WHERE mc.match_id = ?
                ORDER BY mc.created_at, mc.comment_id
                LIMIT ? OFFSET ?
                """.formatted(NICKNAME);
        return jdbc.query(sql, (rs, i) -> new MatchComment(
                rs.getLong("comment_id"),
                new UserBrief(rs.getLong("user_id"), rs.getString("nickname"), rs.getString("avatar")),
                rs.getString("content"),
                rs.getBoolean("is_notice"),
                rs.getObject("created_at", LocalDateTime.class)), matchId, limit, offset);
    }

    public long countComments(long matchId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM match_comments WHERE match_id = ?", Long.class, matchId);
        return n == null ? 0 : n;
    }

    public long insertComment(long matchId, long userId, String content, boolean notice) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO match_comments (match_id, user_id, content, is_notice) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, matchId);
            ps.setLong(2, userId);
            ps.setString(3, content);
            ps.setBoolean(4, notice);
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    public MatchComment findComment(long commentId) {
        String sql = """
                SELECT mc.comment_id, mc.content, mc.is_notice, mc.created_at,
                       u.user_id, %s AS nickname, u.avatar
                FROM match_comments mc JOIN users u ON u.user_id = mc.user_id
                WHERE mc.comment_id = ?
                """.formatted(NICKNAME);
        return jdbc.queryForObject(sql, (rs, i) -> new MatchComment(
                rs.getLong("comment_id"),
                new UserBrief(rs.getLong("user_id"), rs.getString("nickname"), rs.getString("avatar")),
                rs.getString("content"),
                rs.getBoolean("is_notice"),
                rs.getObject("created_at", LocalDateTime.class)), commentId);
    }

    // ------------------------------------------------------------------
    // 관리자 AD3
    // ------------------------------------------------------------------

    public List<AdminMatchRow> findAdminRows(String where, List<Object> params, int limit, int offset) {
        String sql = """
                SELECT m.match_id, m.title, m.meet_at, m.target, m.capacity, m.is_hidden,
                       s.current_count, s.status AS display_status,
                       u.user_id AS host_id, %s AS host_nickname
                """.formatted(NICKNAME) + SUMMARY_FROM
                + " WHERE " + where + " ORDER BY m.created_at DESC, m.match_id DESC LIMIT ? OFFSET ?";
        List<Object> args = new ArrayList<>(params);
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql, (rs, i) -> new AdminMatchRow(
                rs.getLong("match_id"),
                rs.getString("title"),
                new AdminMatchRow.Host(rs.getLong("host_id"), rs.getString("host_nickname")),
                rs.getObject("meet_at", LocalDateTime.class),
                rs.getString("target"),
                rs.getInt("current_count"),
                rs.getInt("capacity"),
                rs.getString("display_status"),
                rs.getBoolean("is_hidden")), args.toArray());
    }

    /** 숨김 · 복구. 글이 없으면 false */
    public boolean updateHidden(long matchId, boolean hidden) {
        return jdbc.update("UPDATE matches SET is_hidden = ? WHERE match_id = ?", hidden, matchId) > 0;
    }

    // ------------------------------------------------------------------
    // 내부 도우미
    // ------------------------------------------------------------------

    /** 여러 글의 태그를 한 번에 */
    private Map<Long, Tags> findTags(Collection<Long> matchIds) {
        if (matchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Tags> result = new HashMap<>();
        jdbc.query("SELECT match_id, tag_type, tag_value FROM match_tags WHERE match_id IN (" + marks(matchIds.size())
                + ")", rs -> {
                    Tags t = result.computeIfAbsent(rs.getLong("match_id"), id -> new Tags());
                    t.add(rs.getString("tag_type"), rs.getString("tag_value"));
                }, matchIds.toArray());
        result.values().forEach(Tags::sort);
        return result;
    }

    /** 여러 사람의 언어를 한 번에 */
    private Map<Long, List<String>> findUserLanguages(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> result = new HashMap<>();
        jdbc.query("SELECT user_id, tag_value FROM user_tags WHERE tag_type = 'LANGUAGE' AND user_id IN ("
                + marks(userIds.size()) + ")", rs -> {
                    result.computeIfAbsent(rs.getLong("user_id"), id -> new ArrayList<>())
                            .add(rs.getString("tag_value"));
                }, userIds.toArray());
        result.values().forEach(list -> list.sort(MatchCodes.order(MatchCodes.LANGUAGES)));
        return result;
    }

    /** "?, ?, ?" */
    static String marks(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ------------------------------------------------------------------
    // 조회 결과를 잠깐 담는 클래스들
    // ------------------------------------------------------------------

    /** 한 글의 태그 (연령대 · 스타일 · 언어) */
    public static class Tags {
        static final Tags EMPTY = new Tags();
        final List<String> ageGroups = new ArrayList<>();
        final List<String> styles = new ArrayList<>();
        final List<String> languages = new ArrayList<>();

        void add(String type, String value) {
            switch (type) {
                case "AGE_GROUP" -> ageGroups.add(value);
                case "STYLE" -> styles.add(value);
                case "LANGUAGE" -> languages.add(value);
                default -> { }
            }
        }

        void sort() {
            ageGroups.sort(MatchCodes.order(MatchCodes.AGE_GROUPS));
            styles.sort(MatchCodes.order(MatchCodes.STYLES));
            languages.sort(MatchCodes.order(MatchCodes.LANGUAGES));
        }
    }

    /** 권한 · 상태 확인용. current 는 지금 참여 중인 인원 */
    public record MatchState(long matchId, long hostId, int capacity, LocalDateTime meetAt, String savedStatus,
            int current) {

        /** 화면에 보낼 상태 (v_match_status 뷰와 같은 순서: CANCELED > DONE > CLOSED > FULL > OPEN) */
        public String displayStatus() {
            if ("CANCELED".equals(savedStatus)) {
                return "CANCELED";
            }
            if (!meetAt.isAfter(LocalDateTime.now())) {
                return "DONE";
            }
            if ("CLOSED".equals(savedStatus)) {
                return "CLOSED";
            }
            return current >= capacity ? "FULL" : "OPEN";
        }
    }

    private static class SummaryRow {
        final long matchId;
        final String title;
        final LocalDateTime meetAt;
        final String target;
        final int capacity;
        final int current;
        final String status;
        final CourseBrief course;
        final UserBrief host;
        final Boolean reviewed;

        SummaryRow(ResultSet rs, boolean withReviewed) throws SQLException {
            matchId = rs.getLong("match_id");
            title = rs.getString("title");
            meetAt = rs.getObject("meet_at", LocalDateTime.class);
            target = rs.getString("target");
            capacity = rs.getInt("capacity");
            current = rs.getInt("current_count");
            status = rs.getString("display_status");
            course = new CourseBrief(rs.getLong("course_id"), rs.getString("course_name"),
                    rs.getDouble("distance_km"), rs.getString("level"));
            host = new UserBrief(rs.getLong("host_id"), rs.getString("host_nickname"), rs.getString("host_avatar"));
            reviewed = withReviewed ? rs.getBoolean("reviewed") : null;
        }

        MatchSummary toSummary(Tags t) {
            return new MatchSummary(matchId, title, meetAt, course, target, t.styles, t.languages,
                    current, capacity, status, host, reviewed);
        }
    }

    /** 상세 조회 결과 (태그 · 참여자는 서비스에서 붙임) */
    public static class DetailRow {
        final long matchId;
        final String title;
        final String content;
        final LocalDateTime meetAt;
        final String meetPlace;
        final int capacity;
        final String target;
        final String mbti;
        final boolean hasGpx;
        final String status;
        final CourseBrief course;
        final int durationMin;
        final String path;
        final String meetPoint;
        final long hostId;
        final String hostNickname;
        final String hostAvatar;

        DetailRow(ResultSet rs) throws SQLException {
            matchId = rs.getLong("match_id");
            title = rs.getString("title");
            content = rs.getString("content");
            meetAt = rs.getObject("meet_at", LocalDateTime.class);
            meetPlace = rs.getString("meet_place");
            capacity = rs.getInt("capacity");
            target = rs.getString("target");
            mbti = rs.getString("mbti");
            hasGpx = rs.getString("gpx_path") != null;
            status = rs.getString("display_status");
            course = new CourseBrief(rs.getLong("course_id"), rs.getString("course_name"),
                    rs.getDouble("distance_km"), rs.getString("level"));
            durationMin = rs.getInt("duration_min");
            path = rs.getString("path");
            meetPoint = rs.getString("meet_point");
            hostId = rs.getLong("host_id");
            hostNickname = rs.getString("host_nickname");
            hostAvatar = rs.getString("host_avatar");
        }
    }
}
