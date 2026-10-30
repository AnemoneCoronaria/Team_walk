package kr.sanchaek.match;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.PageResult;

@Repository         // 여기는 DB 건드는 곳...
public class MatchRepository {
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

        String from = " FROM matches m"
                + " JOIN v_match_status s ON s.match_id = m.match_id"
                + " JOIN courses c ON c.course_id = m.course_id"
                + " JOIN users u ON u.user_id = m.host_id";

        // 2) 전체 개수
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from + where, Long.class, args.toArray());

        // 3) 이번 페이지 (정렬 값은 고정된 문장만 씀)
        String orderBy = "recent".equals(q.sort()) ? " ORDER BY m.match_id DESC" : " ORDER BY m.meet_at, m.match_id";
        String sql = "SELECT m.match_id, m.title, m.meet_at, m.target, m.capacity,"
                + " s.current_count, s.status,"
                + " c.course_id, c.name AS course_name, c.distance_km, c.level,"
                + " u.user_id, u.nickname, u.avatar"
                + from + where + orderBy + " LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(q.size());
        pageArgs.add(PageResult.offset(q.page(), q.size()));

        List<Map<String, Object>> items = jdbc.query(sql, (rs, i) -> {
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
            host.put("nickname", rs.getString("nickname"));
            host.put("avatar", rs.getString("avatar"));
            m.put("host", host);
            return m;
        }, pageArgs.toArray());

        attachTags(items);
        return new PageResult<>(items, q.page(), q.size(), total == null ? 0 : total);
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
     * - 모집자 matchCount: 모집자가 연 동행 중 끝난(DONE) 것의 수 / rating: 그 동행들에 달린 후기 평균
     */
    public Map<String, Object> findMatchById(Long matchId) {
        String sql = """
            SELECT m.match_id, m.title, m.content, m.meet_at, m.meet_place, m.capacity, m.target, m.mbti,
                   m.path_json AS match_path, m.gpx_path,
                   s.current_count, s.status,
                   c.course_id, c.name AS course_name, c.distance_km, c.level, c.duration_min,
                   c.start_lat, c.start_lng, c.path_json AS course_path,
                   u.user_id, u.nickname, u.avatar
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
            host.put("nickname", rs.getString("nickname"));
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

    /** 모집자의 진행한 동행 수 · 후기 평균 (후기가 없으면 rating 은 null) */
    @SuppressWarnings("unchecked")
    private void attachHostStats(Map<String, Object> match) {
        Map<String, Object> host = (Map<String, Object>) match.get("host");
        Object hostId = host.get("userId");
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM matches m
            JOIN v_match_status s ON s.match_id = m.match_id
            WHERE m.host_id = ? AND m.is_hidden = FALSE AND s.status = 'DONE'
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
            SELECT u.user_id, u.nickname, u.avatar, (u.user_id = m.host_id) AS is_host
            FROM match_participants p
            JOIN matches m ON m.match_id = p.match_id
            JOIN users u ON u.user_id = p.user_id
            WHERE p.match_id = ? AND p.status = 'JOINED'
            ORDER BY is_host DESC, p.joined_at, u.user_id
            """, (rs, i) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("userId", rs.getLong("user_id"));
            p.put("nickname", rs.getString("nickname"));
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

}
