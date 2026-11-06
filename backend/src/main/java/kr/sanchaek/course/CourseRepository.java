package kr.sanchaek.course;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            double rating, long reviewCount, List<Landmark> landmarks, boolean isMine, boolean saved, LocalDateTime createdAt) {}

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

    /** 이 사람이 볼 수 있는 코스인지 (상세와 같은 기준). 아니면 404. 후기 작성 · 목록에서 사용 */
    public void checkVisible(long id, Long userId) {
        Long found = jdbc.query("SELECT c.course_id FROM courses c WHERE c.course_id=:id AND " + VISIBLE,
                accessParameters(id, userId), rs -> rs.next() ? rs.getLong(1) : null);
        if (found == null) {
            throw ApiException.notFound("코스를 찾을 수 없습니다.");
        }
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
        List<Summary> rows = jdbc.query("SELECT " + COLUMNS + BASE + where + " ORDER BY " + order
                + ", c.course_id ASC LIMIT :size OFFSET :offset", params, (rs, n) -> summary(rs, List.of()));
        Map<Long, List<String>> facilities = facilitiesOf(rows.stream().map(Summary::courseId).toList());
        List<Summary> items = rows.stream().map(s -> withFacilities(s, facilities)).toList();
        return new PageResult<>(items, page, size, total);
    }

    private static void validateFilters(String area, Double min, Double max, String level, int page, int size) {
        PageResult.validate(page, size);
        if (invalidDistance(min) || invalidDistance(max) || (min != null && max != null && min > max)) {
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

    /** 목록용: 여러 코스의 편의시설을 쿼리 한 번으로 (코스마다 facility 순) */
    private Map<Long, List<String>> facilitiesOf(List<Long> ids) {
        Map<Long, List<String>> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        jdbc.query("SELECT course_id, facility FROM course_facilities WHERE course_id IN (:ids) "
                + "ORDER BY course_id, facility", new MapSqlParameterSource("ids", ids), rs -> {
                    result.computeIfAbsent(rs.getLong("course_id"), k -> new ArrayList<>())
                            .add(rs.getString("facility"));
                });
        return result;
    }

    private static Summary summary(ResultSet rs, List<String> facilities) throws SQLException {
        return new Summary(rs.getLong("course_id"), rs.getString("name"), rs.getString("area"),
                rs.getDouble("distance_km"), rs.getInt("duration_min"), rs.getString("level"), facilities,
                List.of(rs.getDouble("start_lat"), rs.getDouble("start_lng")),
                rs.getDouble("avg_rating"), rs.getLong("review_count"));
    }

    private static Summary withFacilities(Summary s, Map<Long, List<String>> facilities) {
        return new Summary(s.courseId(), s.name(), s.area(), s.distanceKm(), s.durationMin(), s.level(),
                facilities.getOrDefault(s.courseId(), List.of()), s.start(), s.rating(), s.reviewCount());
    }

    /** P3 내가 올린 코스: 코스 목록과 같은 모양 + visibility */
    public record MyCourse(long courseId, String name, String area, double distanceKm, int durationMin,
            String level, List<String> facilities, List<Double> start, double rating, long reviewCount,
            String visibility) {}

    /** 작성자가 지운 코스와 관리자가 숨긴 코스(상세 404)는 빼고, 최신순 */
    @Transactional(readOnly = true)
    public PageResult<MyCourse> mine(long userId, int page, int size) {
        PageResult.validate(page, size);
        String where = " WHERE c.author_id=:userId AND c.is_deleted=FALSE AND c.is_hidden=FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + BASE + where, params, Long.class);
        params.addValue("size", size).addValue("offset", PageResult.offset(page, size));
        List<MyCourse> rows = jdbc.query("SELECT " + COLUMNS + ", c.visibility" + BASE + where
                + " ORDER BY c.created_at DESC, c.course_id DESC LIMIT :size OFFSET :offset", params, (rs, n) -> {
                    Summary s = summary(rs, List.of());
                    return new MyCourse(s.courseId(), s.name(), s.area(), s.distanceKm(), s.durationMin(),
                            s.level(), s.facilities(), s.start(), s.rating(), s.reviewCount(),
                            rs.getString("visibility"));
                });
        Map<Long, List<String>> facilities = facilitiesOf(rows.stream().map(MyCourse::courseId).toList());
        List<MyCourse> items = rows.stream().map(c -> new MyCourse(c.courseId(), c.name(), c.area(),
                c.distanceKm(), c.durationMin(), c.level(), facilities.getOrDefault(c.courseId(), List.of()),
                c.start(), c.rating(), c.reviewCount(), c.visibility())).toList();
        return new PageResult<>(items, page, size, total);
    }

    /** P6 저장한 코스: 코스 목록과 같은 모양 + 지금 모집 중인 동행 수, 저장한 시각 */
    public record SavedCourse(long courseId, String name, String area, double distanceKm, int durationMin,
            String level, List<String> facilities, List<Double> start, double rating, long reviewCount,
            long openMatches, LocalDateTime savedAt) {}

    /** 저장 (C2 ♡). 볼 수 없는 코스는 404, 이미 저장했으면 그대로 */
    @Transactional
    public void save(long id, long userId) {
        checkVisible(id, userId);
        jdbc.update("""
                INSERT INTO saved_courses (user_id, course_id) VALUES (:userId, :id)
                ON DUPLICATE KEY UPDATE course_id = course_id
                """, accessParameters(id, userId));
    }

    /** 저장 해제. 저장한 적이 없거나 코스가 사라졌어도 성공 (사라진 코스도 지울 수 있게 볼 수 있는지 묻지 않음) */
    public void unsave(long id, long userId) {
        jdbc.update("DELETE FROM saved_courses WHERE user_id = :userId AND course_id = :id", accessParameters(id, userId));
    }

    /** 지금 볼 수 있는 코스만 (상세와 같은 기준). 비공개 · 숨김 · 삭제되면 빠지고 다시 공개되면 돌아옴 */
    @Transactional(readOnly = true)
    public PageResult<SavedCourse> saved(long userId, String sort, int page, int size) {
        PageResult.validate(page, size);
        String order = switch (sort) {
            case "recent" -> "sc.created_at DESC, c.course_id DESC";
            case "distance" -> "c.distance_km ASC, c.course_id";
            case "rating" -> "r.avg_rating DESC, c.course_id";
            default -> throw ApiException.badRequest("정렬 값을 확인해 주세요.");
        };
        String from = BASE + " JOIN saved_courses sc ON sc.course_id = c.course_id AND sc.user_id = :userId WHERE " + VISIBLE;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        long total = jdbc.queryForObject("SELECT COUNT(*)" + from, params, Long.class);
        params.addValue("size", size).addValue("offset", PageResult.offset(page, size));
        String openMatches = "(SELECT COUNT(*) FROM matches om JOIN v_match_status os ON os.match_id = om.match_id "
                + "WHERE om.course_id = c.course_id AND om.is_hidden = FALSE AND os.status = 'OPEN')";
        List<SavedCourse> rows = jdbc.query("SELECT " + COLUMNS + ", sc.created_at AS saved_at, " + openMatches + " AS open_matches"
                + from + " ORDER BY " + order + " LIMIT :size OFFSET :offset", params, (rs, n) -> {
                    Summary s = summary(rs, List.of());
                    return new SavedCourse(s.courseId(), s.name(), s.area(), s.distanceKm(), s.durationMin(),
                            s.level(), s.facilities(), s.start(), s.rating(), s.reviewCount(),
                            rs.getLong("open_matches"), rs.getObject("saved_at", LocalDateTime.class));
                });
        Map<Long, List<String>> facilities = facilitiesOf(rows.stream().map(SavedCourse::courseId).toList());
        List<SavedCourse> items = rows.stream().map(c -> new SavedCourse(c.courseId(), c.name(), c.area(),
                c.distanceKm(), c.durationMin(), c.level(), facilities.getOrDefault(c.courseId(), List.of()),
                c.start(), c.rating(), c.reviewCount(), c.openMatches(), c.savedAt())).toList();
        return new PageResult<>(items, page, size, total);
    }

    /** 수정 · 삭제 전 확인: 없거나 지웠거나 숨긴 코스면 404, 작성자가 아니면 403. 행을 잠근다 */
    public void checkAuthor(long id, long userId) {
        List<Long> authors = jdbc.queryForList("SELECT author_id FROM courses "
                + "WHERE course_id=:id AND is_deleted=FALSE AND is_hidden=FALSE FOR UPDATE",
                new MapSqlParameterSource("id", id), Long.class);
        if (authors.isEmpty()) {
            throw ApiException.notFound("코스를 찾을 수 없습니다.");
        }
        if (authors.get(0) != userId) {
            throw ApiException.forbidden("내가 올린 코스만 수정 · 삭제할 수 있어요.");
        }
    }

    /** null 인 칸은 그대로 둔다 */
    public void update(long id, String name, String description, boolean clearDescription, String level,
            String visibility) {
        jdbc.update("""
                UPDATE courses SET
                    name = COALESCE(:name, name),
                    description = CASE WHEN :clear THEN NULL ELSE COALESCE(:description, description) END,
                    level = COALESCE(:level, level),
                    visibility = COALESCE(:visibility, visibility)
                WHERE course_id = :id
                """, new MapSqlParameterSource("id", id).addValue("name", name)
                .addValue("description", description).addValue("clear", clearDescription)
                .addValue("level", level).addValue("visibility", visibility));
    }

    /** 삭제해도 그 코스로 잡힌 동행 · 후기는 남기고, 코스만 숨긴다 */
    public void markDeleted(long id) {
        jdbc.update("UPDATE courses SET is_deleted=TRUE WHERE course_id=:id", new MapSqlParameterSource("id", id));
    }

    @Transactional(readOnly = true)
    public Detail detail(long id, Long userId) {
        String sql = "SELECT " + COLUMNS
                + ", c.description, c.path_json, c.author_id, c.created_at, u.nickname, u.avatar, "
                + "u.status AS author_status, "
                + "EXISTS (SELECT 1 FROM saved_courses sc WHERE sc.course_id = c.course_id AND sc.user_id = :userId) AS saved"
                + BASE
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
                landmarksAlong(path), userId != null && userId == authorId, rs.getBoolean("saved"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private List<Landmark> landmarksAlong(List<List<Double>> path) {
        return jdbc.query("SELECT landmark_id, name, lat, lng, radius_m FROM landmarks "
                + "ORDER BY sort_order, landmark_id", (rs, i) -> {
                    boolean nearby = Geo.passesNear(path, rs.getDouble("lat"), rs.getDouble("lng"),
                            rs.getDouble("radius_m"));
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
