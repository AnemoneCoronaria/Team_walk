package kr.sanchaek.course;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.PageResult;

@Repository
public class CourseRepository {
    public record Summary(long courseId, String name, String area, double distanceKm, int durationMin,
            String level, List<String> facilities, List<Double> start, double rating, long reviewCount) {}
    public record Author(long userId, String nickname, String avatar) {}
    public record Landmark(long landmarkId, String name) {}
    public record Detail(long courseId, String name, String description, String area, double distanceKm,
            int durationMin, String level, List<String> facilities, List<List<Double>> path, Author author,
            double rating, long reviewCount, List<Landmark> landmarks, boolean isMine, LocalDateTime createdAt) {}

    private final NamedParameterJdbcTemplate jdbc;
    private static final String BASE = " FROM courses c JOIN v_course_rating r ON r.course_id = c.course_id ";
    private static final String COLUMNS = """
            c.course_id, c.name, c.area, c.distance_km, c.duration_min, c.level,
            c.start_lat, c.start_lng, r.avg_rating, r.review_count
            """;
    private static final String VISIBLE = """
            c.is_deleted = FALSE AND c.is_hidden = FALSE AND (
                c.visibility = 'PUBLIC' OR c.author_id = :userId OR (
                    c.visibility = 'MATCH_ONLY' AND EXISTS (
                        SELECT 1 FROM matches m
                        JOIN match_participants p ON p.match_id = m.match_id
                        WHERE m.course_id = c.course_id AND p.user_id = :userId AND p.status = 'JOINED'
                    )
                )
            )
            """;
    static final Set<String> AREAS = Set.of("OKGU", "JEONGWANG", "BAEGOT", "SIHWA");
    static final Set<String> LEVELS = Set.of("EASY", "NORMAL", "HARD");
    static final Set<String> FACILITIES = Set.of("TOILET", "BENCH", "PARKING", "SEA_VIEW");
    static final Set<String> VISIBILITIES = Set.of("PUBLIC", "MATCH_ONLY", "PRIVATE");
    private static final Pattern PATH_POINT = Pattern.compile("\\[\\s*([-+0-9.eE]+)\\s*,\\s*([-+0-9.eE]+)\\s*\\]");

    public CourseRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static MapSqlParameterSource accessParameters(long id, Long userId) {
        return new MapSqlParameterSource("id", id).addValue("userId", userId == null ? 0L : userId);
    }

    public String gpxPath(long id, Long userId) {
        List<String> paths = jdbc.query("SELECT c.gpx_path FROM courses c WHERE c.course_id=:id AND " + VISIBLE,
                accessParameters(id, userId), (rs, n) -> rs.getString("gpx_path"));
        if (paths.isEmpty()) {
            throw ApiException.notFound("코스를 찾을 수 없습니다.");
        }
        return paths.get(0);
    }

    public void incrementDownloadCount(long id, Long userId, String path) {
        MapSqlParameterSource params = accessParameters(id, userId).addValue("path", path);
        int updated = jdbc.update("UPDATE courses c SET c.download_count = c.download_count + 1 "
                + "WHERE c.course_id = :id AND c.gpx_path = :path AND " + VISIBLE, params);
        if (updated != 1) {
            throw ApiException.notFound("코스를 찾을 수 없습니다.");
        }
    }

    public record NewCourse(long authorId, String name, String description, String area, String level,
            double distanceKm, int durationMin, double startLat, double startLng, String pathJson,
            String visibility) {}

    public long insert(NewCourse course) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("authorId", course.authorId()).addValue("name", course.name())
                .addValue("description", course.description()).addValue("area", course.area())
                .addValue("level", course.level()).addValue("distanceKm", course.distanceKm())
                .addValue("durationMin", course.durationMin()).addValue("startLat", course.startLat())
                .addValue("startLng", course.startLng()).addValue("pathJson", course.pathJson())
                .addValue("visibility", course.visibility());
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO courses (author_id, name, description, area, level, distance_km, duration_min,
                                     start_lat, start_lng, path_json, visibility)
                VALUES (:authorId, :name, :description, :area, :level, :distanceKm, :durationMin,
                        :startLat, :startLng, :pathJson, :visibility)
                """, params, key, new String[] {"course_id"});
        return key.getKey().longValue();
    }

    public void replaceFacilities(long id, List<String> facilities) {
        jdbc.update("DELETE FROM course_facilities WHERE course_id=:id", new MapSqlParameterSource("id", id));
        for (String facility : facilities) {
            jdbc.update("INSERT INTO course_facilities (course_id, facility) VALUES (:id, :facility)",
                    new MapSqlParameterSource("id", id).addValue("facility", facility));
        }
    }

    public void setGpxPath(long id, String gpxPath) {
        jdbc.update("UPDATE courses SET gpx_path=:path WHERE course_id=:id",
                new MapSqlParameterSource("id", id).addValue("path", gpxPath));
    }

    @Transactional(readOnly = true)
    public PageResult<Summary> list(String q, String area, Double min, Double max, String level,
            String facility, String sort, int page, int size) {
        validateFilters(area, min, max, level, page, size);
        String order = switch (sort) {
            case "distance" -> "c.distance_km ASC";
            case "rating" -> "r.avg_rating DESC";
            case "recent" -> "c.created_at DESC";
            default -> throw ApiException.badRequest("정렬 값을 확인해 주세요.");
        };
        StringBuilder where = new StringBuilder(" WHERE c.is_deleted=FALSE AND c.is_hidden=FALSE AND c.visibility='PUBLIC'");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            where.append(" AND c.name LIKE :q ESCAPE '!'");
            params.addValue("q", "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        if (area != null) {
            where.append(" AND c.area=:area");
            params.addValue("area", area);
        }
        if (level != null) {
            where.append(" AND c.level=:level");
            params.addValue("level", level);
        }
        if (min != null) {
            where.append(" AND c.distance_km>=:min");
            params.addValue("min", min);
        }
        if (max != null) {
            where.append(" AND c.distance_km<=:max");
            params.addValue("max", max);
        }
        if (facility != null) {
            List<String> values = Arrays.stream(facility.split(",", -1)).map(String::trim).distinct().toList();
            if (values.isEmpty() || !FACILITIES.containsAll(values)) {
                throw ApiException.badRequest("편의시설 값을 확인해 주세요.");
            }
            where.append(" AND (SELECT COUNT(*) FROM course_facilities f WHERE f.course_id=c.course_id "
                    + "AND f.facility IN (:facilities))=:facilityCount");
            params.addValue("facilities", values).addValue("facilityCount", values.size());
        }
        long total = jdbc.queryForObject("SELECT COUNT(*)" + BASE + where, params, Long.class);
        params.addValue("size", size).addValue("offset", (page - 1) * size);
        List<Summary> items = jdbc.query("SELECT " + COLUMNS + BASE + where + " ORDER BY " + order
                + ", c.course_id ASC LIMIT :size OFFSET :offset", params, (rs, n) -> summary(rs));
        return new PageResult<>(items, page, size, total);
    }

    private static void validateFilters(String area, Double min, Double max, String level, int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE
                || invalidDistance(min) || invalidDistance(max) || (min != null && max != null && min > max)) {
            throw ApiException.badRequest("필터와 페이지 값을 확인해 주세요.");
        }
        if ((area != null && !AREAS.contains(area)) || (level != null && !LEVELS.contains(level))) {
            throw ApiException.badRequest("지역 또는 난이도를 확인해 주세요.");
        }
    }

    private static boolean invalidDistance(Double distance) {
        return distance != null && (!Double.isFinite(distance) || distance < 0);
    }

    private List<String> facilities(long id) {
        return jdbc.queryForList("SELECT facility FROM course_facilities WHERE course_id=:id ORDER BY facility",
                new MapSqlParameterSource("id", id), String.class);
    }

    private Summary summary(ResultSet rs) throws SQLException {
        long id = rs.getLong("course_id");
        return new Summary(id, rs.getString("name"), rs.getString("area"), rs.getDouble("distance_km"),
                rs.getInt("duration_min"), rs.getString("level"), facilities(id),
                List.of(rs.getDouble("start_lat"), rs.getDouble("start_lng")),
                rs.getDouble("avg_rating"), rs.getLong("review_count"));
    }

    @Transactional(readOnly = true)
    public Detail detail(long id, Long userId) {
        String sql = "SELECT " + COLUMNS
                + ", c.description, c.path_json, c.author_id, c.created_at, u.nickname, u.avatar, "
                + "u.status AS author_status" + BASE
                + " JOIN users u ON u.user_id=c.author_id WHERE c.course_id=:id AND " + VISIBLE;
        List<Detail> results = jdbc.query(sql, accessParameters(id, userId),
                (rs, n) -> mapDetail(rs, id, userId));
        if (results.isEmpty()) {
            throw ApiException.notFound("코스를 찾을 수 없습니다.");
        }
        return results.get(0);
    }

    private Detail mapDetail(ResultSet rs, long id, Long userId) throws SQLException {
        List<List<Double>> path = readPath(rs.getString("path_json"));
        long authorId = rs.getLong("author_id");
        String nickname = "WITHDRAWN".equals(rs.getString("author_status"))
                ? "탈퇴한 회원" : rs.getString("nickname");
        Author author = new Author(authorId, nickname, rs.getString("avatar"));
        return new Detail(id, rs.getString("name"), rs.getString("description"), rs.getString("area"),
                rs.getDouble("distance_km"), rs.getInt("duration_min"), rs.getString("level"),
                facilities(id), path, author, rs.getDouble("avg_rating"), rs.getLong("review_count"),
                landmarksAlong(path), userId != null && userId == authorId,
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private List<Landmark> landmarksAlong(List<List<Double>> path) {
        return jdbc.query("SELECT landmark_id, name, lat, lng, radius_m FROM landmarks "
                + "ORDER BY sort_order, landmark_id", (rs, i) -> {
                    double lat = rs.getDouble("lat");
                    double lon = rs.getDouble("lng");
                    double radius = rs.getDouble("radius_m");
                    boolean nearby = path.stream()
                            .anyMatch(point -> Geo.meters(lat, lon, point.get(0), point.get(1)) <= radius);
                    return nearby ? new Landmark(rs.getLong("landmark_id"), rs.getString("name")) : null;
                }).stream().filter(java.util.Objects::nonNull).toList();
    }

    private static List<List<Double>> readPath(String json) {
        List<List<Double>> path = new ArrayList<>();
        Matcher matcher = PATH_POINT.matcher(json);
        while (matcher.find()) {
            path.add(List.of(Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))));
        }
        if (path.isEmpty()) {
            throw new IllegalStateException("잘못된 코스 경로");
        }
        return path;
    }
}
