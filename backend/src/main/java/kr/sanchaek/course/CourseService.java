package kr.sanchaek.course;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 코스 등록 (C3-3) · 내 코스 수정 · 삭제 (P3). GPX 파일은 업로드 폴더에 그대로 저장하고, 한 번 읽은 결과를 DB에 저장한다. */
@Service
public class CourseService {
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MAX_DESCRIPTION_LENGTH = 20000;
    private static final int TEXT_MAX_BYTES = 65_535; // description 은 TEXT 컬럼
    /** courses.distance_km 이 DECIMAL(5,2) */
    private static final double MAX_DISTANCE_KM = 999.99;

    private final CourseRepository courses;
    private final GpxService gpx;
    private final FileStore files;

    public CourseService(CourseRepository courses, GpxService gpx, FileStore files) {
        this.courses = courses;
        this.gpx = gpx;
        this.files = files;
    }

    public record NewCourseForm(String name, String level, String description, String facilities,
            String visibility) {}

    @Transactional
    public long register(long userId, MultipartFile file, NewCourseForm form) {
        String name = validName(form.name());
        String level = validLevel(form.level());
        String visibility = validVisibility(form.visibility());
        List<String> facilities = parseFacilities(form.facilities());
        String description = validDescription(form.description());
        GpxService.Result route = gpx.parse(file);
        if (route.distanceKm() > MAX_DISTANCE_KM) {
            throw ApiException.badRequest("코스 거리는 999.99km 이하여야 해요.");
        }

        long courseId = courses.insert(new CourseRepository.NewCourse(userId, name, description,
                route.area(), level, route.distanceKm(), route.durationMin(), route.start().get(0),
                route.start().get(1), toJson(route.path()), visibility));
        courses.replaceFacilities(courseId, facilities);

        String gpxPath = "gpx/course-" + courseId + ".gpx";
        files.saveInTransaction(file, gpxPath);
        courses.setGpxPath(courseId, gpxPath);
        return courseId;
    }

    /** PUT /api/courses/{id} 본문. 보낸 칸만 바뀐다 (경로는 못 바꿈) */
    public record UpdateCourseRequest(String name, String description, String level, List<String> facilities,
            String visibility) {}

    @Transactional
    public void update(long userId, long courseId, UpdateCourseRequest request) {
        if (request == null) {
            throw ApiException.badRequest("요청 값을 확인해 주세요.");
        }
        courses.checkAuthor(courseId, userId);
        String name = request.name() == null ? null : validName(request.name());
        String level = request.level() == null ? null : validLevel(request.level());
        String visibility = request.visibility() == null ? null : validVisibility(request.visibility());
        List<String> facilities = request.facilities() == null ? null : validFacilities(request.facilities());
        String description = validDescription(request.description());
        boolean clearDescription = request.description() != null && description == null;
        courses.update(courseId, name, description, clearDescription, level, visibility);
        if (facilities != null) {
            courses.replaceFacilities(courseId, facilities);
        }
    }

    @Transactional
    public void delete(long userId, long courseId) {
        courses.checkAuthor(courseId, userId);
        courses.markDeleted(courseId);
    }

    static String validName(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > MAX_NAME_LENGTH) {
            throw ApiException.badRequest("코스 이름은 1~20자로 입력해 주세요.");
        }
        return trimmed;
    }

    static String validLevel(String level) {
        if (level == null || !CourseRepository.LEVELS.contains(level)) {
            throw ApiException.badRequest("난이도를 확인해 주세요.");
        }
        return level;
    }

    static String validVisibility(String visibility) {
        if (visibility == null || !CourseRepository.VISIBILITIES.contains(visibility)) {
            throw ApiException.badRequest("공개 범위를 확인해 주세요.");
        }
        return visibility;
    }

    /** 앞뒤 공백을 지우고, 비어 있으면 null. 20000자 또는 TEXT 컬럼 한도(65,535바이트)를 넘으면 400 */
    static String validDescription(String description) {
        String value = blankToNull(description);
        if (value != null && (value.codePointCount(0, value.length()) > MAX_DESCRIPTION_LENGTH
                || value.getBytes(StandardCharsets.UTF_8).length > TEXT_MAX_BYTES)) {
            throw ApiException.badRequest("코스 소개는 20000자 이하로 입력해 주세요.");
        }
        return value;
    }

    /** "TOILET,BENCH" → [BENCH, TOILET]. 비어 있으면 빈 목록 */
    static List<String> parseFacilities(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return validFacilities(Arrays.asList(value.split(",", -1)));
    }

    static List<String> validFacilities(List<String> values) {
        List<String> facilities = values.stream().map(v -> v == null ? "" : v.strip()).distinct().sorted().toList();
        if (!CourseRepository.FACILITIES.containsAll(facilities)) {
            throw ApiException.badRequest("편의시설 값을 확인해 주세요.");
        }
        return facilities;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String toJson(List<List<Double>> path) {
        return path.stream().map(p -> "[" + p.get(0) + "," + p.get(1) + "]")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
