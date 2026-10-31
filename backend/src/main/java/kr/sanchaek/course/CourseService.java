package kr.sanchaek.course;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** 코스 등록 (C3-3). GPX 파일은 업로드 폴더에 그대로 저장하고, 한 번 읽은 결과를 DB에 저장한다. */
@Service
public class CourseService {
    private static final int MAX_NAME_LENGTH = 20;

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
        GpxService.Result route = gpx.parse(file);

        long courseId = courses.insert(new CourseRepository.NewCourse(userId, name, blankToNull(form.description()),
                Geo.area(route.start().get(0), route.start().get(1)), level, route.distanceKm(),
                route.durationMin(), route.start().get(0), route.start().get(1), toJson(route.path()),
                visibility));
        courses.replaceFacilities(courseId, facilities);

        String gpxPath = "gpx/course-" + courseId + ".gpx";
        files.save(file, gpxPath);
        deleteIfRolledBack(gpxPath);
        courses.setGpxPath(courseId, gpxPath);
        return courseId;
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

    /** DB 저장이 취소되면(롤백) 방금 저장한 파일도 지운다 */
    private void deleteIfRolledBack(String path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    files.delete(path);
                }
            }
        });
    }
}
