package kr.sanchaek.match;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.PageResult;

@Repository         // 여기는 DB 건드는 곳...
public class MatchRepository {
    // 목록 한 줄을 만드는 칸 (toSummary 가 읽음)
    private static final String SUMMARY_COLUMNS = "m.match_id, m.title, m.meet_at, m.target, m.capacity,"
            + " s.current_count, s.status,"
            + " c.course_id, c.name AS course_name, c.distance_km, c.level,"
            + " u.user_id, u.nickname, u.avatar, u.status AS user_status";
    private static final String SUMMARY_FROM = " FROM matches m"
            + " JOIN v_match_status s ON s.match_id = m.match_id"
            + " JOIN courses c ON c.course_id = m.course_id"
            + " JOIN users u ON u.user_id = m.host_id";
    // 내가 참여 중인 글 (? 자리는 내 번호)
    private static final String JOINED_BY_ME = "EXISTS (SELECT 1 FROM match_participants p"
            + " WHERE p.match_id = m.match_id AND p.user_id = ? AND p.status = 'JOINED')";

    private final JdbcTemplate jdbc;        // jdbc 사용
    private final JsonMapper jsonMapper;    // path_json(문자열)을 [[위도, 경도], ...] 목록으로 바꿀 때 사용

    // 스프링부트가 자동으로 만들어준 JdbcTemplate을 받아옴 ... DI?
    public MatchRepository(JdbcTemplate jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    // 여기서 sql 문 하드코딩
    // 동행 목록
    /*
     * 숨겨지지 않은 동행 목록을 조건(MatchSearch)에 맞게 조회합니다.
     * - 상태(status)는 matches.status 가 아니라 v_match_status 뷰의 계산 값을 씁니다. (FULL · DONE 은 뷰에만 있음)
     * - 코스(courses), 모집자(users)를 조인하고, 태그(match_tags)는 따로 읽어서 styles · languages 로 붙입니다.
     * - 값은 모두 ? 로 넘겨서 SQL 인젝션을 막습니다. (조건 문장만 이어 붙임)
     */
    public PageResult<Map<String, Object>> findMatches(MatchSearch q) {
        // 1) 조건 조립
        StringBuilder where = new StringBuilder(" WHERE m.is_hidden = FALSE AND s.status = ?");
        List<Object> args = new ArrayList<>();
        args.add(q.status());

        if (q.from() != null) {
            where.append(" AND DATE(m.meet_at) >= ?");
            args.add(q.from());
        }
        if (q.to() != null) {
            where.append(" AND DATE(m.meet_at) <= ?");
            args.add(q.to());
        }
        if (q.timeSlot() != null) {
            // 아침 5~10시, 점심 11~16시, 저녁 17~20시, 밤 21~4시
            where.append(switch (q.timeSlot()) {
                case "MORNING" -> " AND HOUR(m.meet_at) BETWEEN 5 AND 10";
                case "AFTERNOON" -> " AND HOUR(m.meet_at) BETWEEN 11 AND 16";
                case "EVENING" -> " AND HOUR(m.meet_at) BETWEEN 17 AND 20";
                case "NIGHT" -> " AND (HOUR(m.meet_at) >= 21 OR HOUR(m.meet_at) <= 4)";
                default -> throw ApiException.badRequest("timeSlot 은 MORNING · AFTERNOON · EVENING · NIGHT 중 하나예요.");
            });
        }
        addTagFilter(where, args, "STYLE", q.styles());
        addTagFilter(where, args, "LANGUAGE", q.languages());
        if (q.target() != null) {
            where.append(" AND m.target = ?");
            args.add(q.target());
        }
        if (q.age() != null) {
            // 연령대 태그가 없는 글은 "누구나"라서 통과
            where.append(" AND (NOT EXISTS (SELECT 1 FROM match_tags t WHERE t.match_id = m.match_id AND t.tag_type = 'AGE_GROUP')"
                    + " OR EXISTS (SELECT 1 FROM match_tags t WHERE t.match_id = m.match_id AND t.tag_type = 'AGE_GROUP' AND t.tag_value = ?))");
            args.add(q.age());
        }
        if (q.area() != null) {
            where.append(" AND c.area = ?");
            args.add(q.area());
        }
        if (q.courseId() != null) {
            where.append(" AND m.course_id = ?");
            args.add(q.courseId());
        }

        // 2) 전체 개수
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + SUMMARY_FROM + where, Long.class, args.toArray());

        // 3) 이번 페이지 (정렬 값은 고정된 문장만 씀)
        String orderBy = "recent".equals(q.sort()) ? " ORDER BY m.match_id DESC" : " ORDER BY m.meet_at, m.match_id";
        String sql = "SELECT " + SUMMARY_COLUMNS + SUMMARY_FROM + where + orderBy + " LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(q.size());
        pageArgs.add(PageResult.offset(q.page(), q.size()));

        List<Map<String, Object>> items = jdbc.query(sql, (rs, i) -> toSummary(rs), pageArgs.toArray());

        attachTags(items);
        return new PageResult<>(items, q.page(), q.size(), total == null ? 0 : total);
    }

    /**
     * 내 동행 (P1 탭). 목록과 같은 칸 + role(HOST 모집자 · PARTICIPANT 참여), past 탭은 reviewed(후기를 썼는지) 추가.
     * - upcoming: 내가 참여 중(JOINED)이고 아직 안 끝난 글 (모집자로 연 글 포함), 가까운 날짜 순
     * - hosting : 내가 연 글 중 취소하지 않은 것 (끝난 글 포함), 가까운 날짜 순
     * - past    : 내가 참여 중(JOINED)이었고 끝난(DONE) 글, 최근 날짜 순
     */
    public PageResult<Map<String, Object>> findMyMatches(long userId, String tab, int page, int size) {
        String where = " WHERE m.is_hidden = FALSE AND " + switch (tab) {
            case "hosting" -> "m.host_id = ? AND s.status <> 'CANCELED'";
            case "past" -> JOINED_BY_ME + " AND s.status = 'DONE'";
            default -> JOINED_BY_ME + " AND s.status NOT IN ('DONE', 'CANCELED')";
        };
        String orderBy = "past".equals(tab) ? " ORDER BY m.meet_at DESC, m.match_id DESC" : " ORDER BY m.meet_at, m.match_id";

        Long total = jdbc.queryForObject("SELECT COUNT(*)" + SUMMARY_FROM + where, Long.class, userId);
        List<Map<String, Object>> items = jdbc.query(
                "SELECT " + SUMMARY_COLUMNS + ", EXISTS (SELECT 1 FROM reviews r WHERE r.user_id = ? AND r.match_id = m.match_id) AS reviewed"
                        + SUMMARY_FROM + where + orderBy + " LIMIT ? OFFSET ?",
                (rs, i) -> {
                    Map<String, Object> m = toSummary(rs);
                    m.put("role", rs.getLong("user_id") == userId ? "HOST" : "PARTICIPANT");
                    if ("past".equals(tab)) {
                        m.put("reviewed", rs.getBoolean("reviewed"));
                    }
                    return m;
                }, userId, userId, size, PageResult.offset(page, size));

        attachTags(items);
        return new PageResult<>(items, page, size, total == null ? 0 : total);
    }

    /** 목록 한 줄 (GET /api/matches · 내 동행 공통). SUMMARY_COLUMNS 를 읽어서 만듦, 태그는 attachTags 로 채움 */
    private static Map<String, Object> toSummary(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("matchId", rs.getLong("match_id"));
        m.put("title", rs.getString("title"));
        m.put("meetAt", rs.getObject("meet_at", LocalDateTime.class));

        Map<String, Object> course = new LinkedHashMap<>();
        course.put("courseId", rs.getLong("course_id"));
        course.put("name", rs.getString("course_name"));
        course.put("distanceKm", rs.getBigDecimal("distance_km"));
        course.put("level", rs.getString("level"));
        m.put("course", course);

        m.put("target", rs.getString("target"));
        m.put("styles", new ArrayList<String>());       // 아래에서 태그를 읽어 채움
        m.put("languages", new ArrayList<String>());
        m.put("current", rs.getInt("current_count"));
        m.put("capacity", rs.getInt("capacity"));
        m.put("status", rs.getString("status"));

        Map<String, Object> host = new LinkedHashMap<>();
        host.put("userId", rs.getLong("user_id"));
        host.put("nickname", displayName(rs));
        host.put("avatar", rs.getString("avatar"));
        m.put("host", host);
        return m;
    }

    /** 탈퇴한 회원은 이름 대신 "탈퇴한 회원" (db/README "지우지 않습니다"). 쿼리에 nickname, user_status 칸이 있어야 함 */
    private static String displayName(ResultSet rs) throws SQLException {
        return "WITHDRAWN".equals(rs.getString("user_status")) ? "탈퇴한 회원" : rs.getString("nickname");
    }

    /** 태그 하나라도 겹치는 글만 (예: style=SLOW,CHAT) */
    private void addTagFilter(StringBuilder where, List<Object> args, String tagType, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        where.append(" AND EXISTS (SELECT 1 FROM match_tags t WHERE t.match_id = m.match_id AND t.tag_type = '")
                .append(tagType).append("' AND t.tag_value IN (")
                .append(String.join(",", values.stream().map(v -> "?").toList()))
                .append("))");
        args.addAll(values);
    }

    /** 목록의 글들에 styles · languages 를 한 번의 쿼리로 붙임 */
    @SuppressWarnings("unchecked")
    private void attachTags(List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            return;
        }
        Map<Object, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> m : items) {
            byId.put(m.get("matchId"), m);
        }
        String marks = String.join(",", items.stream().map(m -> "?").toList());
        jdbc.query("SELECT match_id, tag_type, tag_value FROM match_tags"
                + " WHERE tag_type IN ('STYLE', 'LANGUAGE') AND match_id IN (" + marks + ")"
                + " ORDER BY tag_type, tag_value", rs -> {
            Map<String, Object> m = byId.get(rs.getLong("match_id"));
            String key = "STYLE".equals(rs.getString("tag_type")) ? "styles" : "languages";
            ((List<String>) m.get(key)).add(rs.getString("tag_value"));
        }, byId.keySet().toArray());
    }


    // 동행 상세
    /*
     * 특정 매칭 ID의 상세 정보 (Figma W2 화면 기준). is_hidden = FALSE 인 글만 보여줍니다.
     * 없거나 숨겨진 글이면 404 (ApiException.notFound).
     * - 지도 경로(path): 모집자가 첨부한 경로(matches.path_json)가 있으면 그것, 없으면 코스 경로
     * - 만나는 좌표(meetPoint): 첨부 경로의 시작점, 없으면 코스 시작점 (저장하지 않고 계산)
     * - 모집자 matchCount: 끝난(DONE) 동행 중 모집자가 JOINED 인 것의 수 (db/README "모임 횟수") / rating: 모집자가 연 동행들에 달린 후기 평균
     */
    public Map<String, Object> findMatchById(Long matchId) {
        String sql = """
            SELECT m.match_id, m.title, m.content, m.meet_at, m.meet_place, m.capacity, m.target, m.mbti,
                   m.path_json AS match_path, m.gpx_path,
                   s.current_count, s.status,
                   c.course_id, c.name AS course_name, c.distance_km, c.level, c.duration_min,
                   c.start_lat, c.start_lng, c.path_json AS course_path,
                   u.user_id, u.nickname, u.avatar, u.status AS user_status
            FROM matches m
            JOIN v_match_status s ON s.match_id = m.match_id
            JOIN courses c ON c.course_id = m.course_id
            JOIN users u ON u.user_id = m.host_id
            WHERE m.is_hidden = FALSE
              AND m.match_id = ?
            """;

        // matchId가 없는걸 요구하면 404 error 나도록 함
        List<Map<String, Object>> rows = jdbc.query(sql, (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("matchId", rs.getLong("match_id"));
            m.put("title", rs.getString("title"));
            m.put("meetAt", rs.getObject("meet_at", LocalDateTime.class));
            m.put("meetPlace", rs.getString("meet_place"));

            // 경로와 만나는 좌표
            String matchPath = rs.getString("match_path");
            List<?> path = jsonMapper.readValue(matchPath != null ? matchPath : rs.getString("course_path"), List.class);
            Map<String, Object> meetPoint = new LinkedHashMap<>();
            if (matchPath != null && !path.isEmpty()) {
                List<?> first = (List<?>) path.get(0);
                meetPoint.put("lat", first.get(0));
                meetPoint.put("lng", first.get(1));
            } else {
                meetPoint.put("lat", rs.getBigDecimal("start_lat"));
                meetPoint.put("lng", rs.getBigDecimal("start_lng"));
            }
            m.put("meetPoint", meetPoint);
            m.put("path", path);
            m.put("hasGpx", rs.getString("gpx_path") != null);

            Map<String, Object> course = new LinkedHashMap<>();
            course.put("courseId", rs.getLong("course_id"));
            course.put("name", rs.getString("course_name"));
            course.put("distanceKm", rs.getBigDecimal("distance_km"));
            course.put("level", rs.getString("level"));
            course.put("durationMin", rs.getInt("duration_min"));
            m.put("course", course);

            m.put("target", rs.getString("target"));
            m.put("mbti", rs.getString("mbti"));
            m.put("styles", new ArrayList<String>());
            m.put("languages", new ArrayList<String>());
            m.put("ageGroups", new ArrayList<String>());
            m.put("description", rs.getString("content"));
            m.put("current", rs.getInt("current_count"));
            m.put("capacity", rs.getInt("capacity"));
            m.put("status", rs.getString("status"));

            Map<String, Object> host = new LinkedHashMap<>();
            host.put("userId", rs.getLong("user_id"));
            host.put("nickname", displayName(rs));
            host.put("avatar", rs.getString("avatar"));
            m.put("host", host);
            return m;
        }, matchId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("동행 찾을 수 없음");
        }
        Map<String, Object> match = rows.get(0);

        attachDetailTags(matchId, match);
        attachHostStats(match);
        match.put("participants", findParticipants(matchId));
        return match;
    }

    /**
     * 모집자가 첨부한 GPX 파일 경로 (업로드 폴더 기준, 예: gpx/match-1.gpx).
     * 없는 글 · 숨긴 글 · 첨부가 없는 글은 404. (코스 GPX 가 아니라 "동행 경로" 만 받음)
     */
    public String findGpxPath(Long matchId) {
        List<String> rows = jdbc.queryForList(
                "SELECT gpx_path FROM matches WHERE match_id = ? AND is_hidden = FALSE", String.class, matchId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("동행 찾을 수 없음");
        }
        if (rows.get(0) == null) {
            throw ApiException.notFound("모집자가 첨부한 경로 파일이 없어요.");
        }
        return rows.get(0);
    }

    /** 원하는 스타일 · 언어 · 연령대 태그 */
    @SuppressWarnings("unchecked")
    private void attachDetailTags(Long matchId, Map<String, Object> match) {
        jdbc.query("SELECT tag_type, tag_value FROM match_tags WHERE match_id = ? ORDER BY tag_type, tag_value", rs -> {
            String key = switch (rs.getString("tag_type")) {
                case "STYLE" -> "styles";
                case "LANGUAGE" -> "languages";
                default -> "ageGroups";
            };
            ((List<String>) match.get(key)).add(rs.getString("tag_value"));
        }, matchId);
    }

    /** 모집자의 모임 횟수 · 후기 평균 (후기가 없으면 rating 은 null) */
    @SuppressWarnings("unchecked")
    private void attachHostStats(Map<String, Object> match) {
        Map<String, Object> host = (Map<String, Object>) match.get("host");
        Object hostId = host.get("userId");
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM match_participants p
            JOIN v_match_status s ON s.match_id = p.match_id
            WHERE p.user_id = ? AND p.status = 'JOINED' AND s.status = 'DONE'
            """, Integer.class, hostId);
        Double rating = jdbc.queryForObject("""
            SELECT ROUND(AVG(r.rating), 1) FROM reviews r
            JOIN matches m ON m.match_id = r.match_id
            WHERE m.host_id = ? AND r.is_hidden = FALSE
            """, Double.class, hostId);
        host.put("matchCount", count == null ? 0 : count);
        host.put("rating", rating);
    }

    /** 참여 중(JOINED)인 사람들. 모집자가 맨 앞, 나머지는 참여한 순서. 언어는 회원 프로필의 LANGUAGE 태그 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findParticipants(Long matchId) {
        List<Map<String, Object>> list = jdbc.query("""
            SELECT u.user_id, u.nickname, u.avatar, u.status AS user_status, (u.user_id = m.host_id) AS is_host
            FROM match_participants p
            JOIN matches m ON m.match_id = p.match_id
            JOIN users u ON u.user_id = p.user_id
            WHERE p.match_id = ? AND p.status = 'JOINED'
            ORDER BY is_host DESC, p.joined_at, u.user_id
            """, (rs, i) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("userId", rs.getLong("user_id"));
            p.put("nickname", displayName(rs));
            p.put("avatar", rs.getString("avatar"));
            p.put("languages", new ArrayList<String>());
            p.put("isHost", rs.getBoolean("is_host"));
            return p;
        }, matchId);
        if (list.isEmpty()) {
            return list;
        }

        Map<Object, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> p : list) {
            byId.put(p.get("userId"), p);
        }
        String marks = String.join(",", list.stream().map(p -> "?").toList());
        jdbc.query("SELECT user_id, tag_value FROM user_tags WHERE tag_type = 'LANGUAGE' AND user_id IN (" + marks
                + ") ORDER BY tag_value", rs -> {
            ((List<String>) byId.get(rs.getLong("user_id")).get("languages")).add(rs.getString("tag_value"));
        }, byId.keySet().toArray());
        return list;
    }


    // ---------------------------------------------------------------------
    // 2주차: 모집 글 쓰기 · 참여 (규칙 검사는 MatchService, 여기는 SQL 만)
    // ---------------------------------------------------------------------

    /**
     * 규칙 검사용 글 정보. 없거나 숨긴 글이면 404.
     * lock 이 true 면 matches 행을 FOR UPDATE 로 잠금 → 같은 글에 동시에 참여 · 수정하는 사람은 앞 사람이 끝날 때까지 기다림
     * (db/README "참여하기는 트랜잭션으로". @Transactional 안에서만 의미가 있음)
     */
    public MatchState findState(long matchId, boolean lock) {
        String sql = "SELECT match_id, host_id, capacity, target, mbti, gpx_path FROM matches"
                + " WHERE match_id = ? AND is_hidden = FALSE" + (lock ? " FOR UPDATE" : "");
        List<Map<String, Object>> rows = jdbc.queryForList(sql, matchId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("동행 찾을 수 없음");
        }
        Map<String, Object> m = rows.get(0);
        // 잠근 "뒤에" 세야 앞 사람이 넣은 참여자까지 보임
        Map<String, Object> s = jdbc.queryForMap(
                "SELECT current_count, status FROM v_match_status WHERE match_id = ?", matchId);
        List<String> ageGroups = new ArrayList<>();
        List<String> languages = new ArrayList<>();
        jdbc.query("SELECT tag_type, tag_value FROM match_tags WHERE match_id = ? AND tag_type IN ('AGE_GROUP', 'LANGUAGE')",
                rs -> {
                    ("AGE_GROUP".equals(rs.getString("tag_type")) ? ageGroups : languages).add(rs.getString("tag_value"));
                }, matchId);
        return new MatchState(matchId, ((Number) m.get("host_id")).longValue(), ((Number) m.get("capacity")).intValue(),
                (String) m.get("target"), (String) m.get("mbti"), (String) m.get("gpx_path"),
                ((Number) s.get("current_count")).intValue(), (String) s.get("status"), ageGroups, languages);
    }

    /** 이 글에서 나의 참여 상태: JOINED · LEFT · KICKED, 한 번도 참여 안 했으면 null */
    public String findParticipantStatus(long matchId, long userId) {
        List<String> rows = jdbc.queryForList(
                "SELECT status FROM match_participants WHERE match_id = ? AND user_id = ?", String.class, matchId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 참여 조건(연령대 · MBTI · 언어) 검사용 내 프로필 */
    public Viewer findViewer(long userId) {
        Map<String, Object> u = jdbc.queryForMap("SELECT age_group, mbti FROM users WHERE user_id = ?", userId);
        List<String> languages = jdbc.queryForList(
                "SELECT tag_value FROM user_tags WHERE user_id = ? AND tag_type = 'LANGUAGE'", String.class, userId);
        return new Viewer((String) u.get("age_group"), (String) u.get("mbti"), languages);
    }

    record Viewer(String ageGroup, String mbti, List<String> languages) {
    }

    /** 모집 글에 고를 수 있는 코스: 삭제 · 숨김이 아니고, 전체 공개이거나 내가 올린 코스 (W3 "코스 검색 · 내가 올린 코스") */
    public boolean isCourseSelectable(long courseId, long userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM courses
            WHERE course_id = ? AND is_deleted = FALSE AND is_hidden = FALSE
              AND (visibility = 'PUBLIC' OR author_id = ?)
            """, Integer.class, courseId, userId);
        return count != null && count > 0;
    }

    /** 모집 글 저장 → 새 match_id. 상태는 OPEN 으로 시작 (GPX 는 번호가 정해진 뒤 updateGpx 로) */
    public long insertMatch(long hostId, MatchRequest r) {
        String sql = "INSERT INTO matches (host_id, course_id, title, content, meet_at, meet_place, capacity, target, mbti)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, hostId);
            ps.setLong(2, r.courseId());
            ps.setString(3, r.title());
            ps.setString(4, r.description());
            ps.setObject(5, r.meetAt());
            ps.setString(6, r.meetPlace());
            ps.setInt(7, r.capacity());
            ps.setString(8, r.target());
            ps.setString(9, r.mbti());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** 모집 글 수정 (상태 · 모집자 · GPX 는 그대로) */
    public void updateMatch(long matchId, MatchRequest r) {
        jdbc.update("""
            UPDATE matches SET course_id = ?, title = ?, content = ?, meet_at = ?, meet_place = ?,
                               capacity = ?, target = ?, mbti = ?
            WHERE match_id = ?
            """, r.courseId(), r.title(), r.description(), r.meetAt(), r.meetPlace(),
                r.capacity(), r.target(), r.mbti(), matchId);
    }

    /** 태그는 전부 지우고 다시 넣기 (db/README "여러 개 고르는 값은 태그 테이블") */
    public void replaceTags(long matchId, MatchRequest r) {
        jdbc.update("DELETE FROM match_tags WHERE match_id = ?", matchId);
        String sql = "INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (?, ?, ?)";
        r.styles().forEach(v -> jdbc.update(sql, matchId, "STYLE", v));
        r.languages().forEach(v -> jdbc.update(sql, matchId, "LANGUAGE", v));
        r.ageGroups().forEach(v -> jdbc.update(sql, matchId, "AGE_GROUP", v));
    }

    /** 첨부 GPX 파일 경로 · 경로 좌표 (둘 다 null 이면 첨부 떼기) */
    public void updateGpx(long matchId, String gpxPath, String pathJson) {
        jdbc.update("UPDATE matches SET gpx_path = ?, path_json = ? WHERE match_id = ?", gpxPath, pathJson, matchId);
    }

    /** 모집자가 바꾸는 상태: CLOSED(마감) · CANCELED(모집 취소) */
    public void updateStatus(long matchId, String status) {
        jdbc.update("UPDATE matches SET status = ? WHERE match_id = ?", status, matchId);
    }

    /** 참여 (모집자 자신도 글을 쓸 때 이걸로 들어감). 취소했다가 다시 참여하면 같은 행을 JOINED 로 되돌림 */
    public void upsertJoined(long matchId, long userId) {
        jdbc.update("""
            INSERT INTO match_participants (match_id, user_id) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE status = 'JOINED', leave_reason = NULL, joined_at = NOW()
            """, matchId, userId);
    }

    /** 참여 취소(LEFT, 사유 저장) · 내보내기(KICKED) */
    public void updateParticipantStatus(long matchId, long userId, String status, String leaveReason) {
        jdbc.update("UPDATE match_participants SET status = ?, leave_reason = ? WHERE match_id = ? AND user_id = ?",
                status, leaveReason, matchId, userId);
    }
}
