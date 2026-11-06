package kr.sanchaek.course;

import jakarta.servlet.http.HttpServletRequest;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
public class CourseController {
    public record Created(long courseId) {}

    private final CourseRepository courses;
    private final CourseDownloadService downloads;
    private final CourseService service;
    public CourseController(CourseRepository courses, CourseDownloadService downloads, CourseService service) {
        this.courses = courses;
        this.downloads = downloads;
        this.service = service;
    }

    @PostMapping("/api/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Created> register(@LoginUser Long userId, @RequestPart("file") MultipartFile file,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "facilities", required = false) String facilities,
            @RequestParam(name = "visibility", required = false) String visibility) {
        var form = new CourseService.NewCourseForm(name, level, description, facilities, visibility);
        return ApiResponse.ok(new Created(service.register(userId, file, form)));
    }

    @PutMapping("/api/courses/{id}")
    public ApiResponse<Void> update(@PathVariable("id") long id, @LoginUser Long userId,
            @RequestBody CourseService.UpdateCourseRequest request) {
        service.update(userId, id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api/courses/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") long id, @LoginUser Long userId) {
        service.delete(userId, id);
        return ApiResponse.ok();
    }

    // 코스 저장 (C2 ♡). 이미 저장했으면 그대로
    @PostMapping("/api/courses/{id}/save")
    public ApiResponse<Void> save(@PathVariable("id") long id, @LoginUser Long userId) {
        courses.save(id, userId);
        return ApiResponse.ok();
    }

    // 저장 해제 (C2 · P6 ♥)
    @DeleteMapping("/api/courses/{id}/save")
    public ApiResponse<Void> unsave(@PathVariable("id") long id, @LoginUser Long userId) {
        courses.unsave(id, userId);
        return ApiResponse.ok();
    }

    // 저장한 코스 (P6). sort: recent 최근 저장순(기본) · distance 거리 · rating 별점
    @GetMapping("/api/users/me/saved-courses")
    public ApiResponse<PageResult<CourseRepository.SavedCourse>> saved(@LoginUser Long userId,
            @RequestParam(name = "sort", defaultValue = "recent") String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(courses.saved(userId, sort, page, size));
    }

    @GetMapping("/api/users/me/courses")
    public ApiResponse<PageResult<CourseRepository.MyCourse>> mine(@LoginUser Long userId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(courses.mine(userId, page, size));
    }

    @GetMapping("/api/courses")
    public ApiResponse<PageResult<CourseRepository.Summary>> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "minDistance", required = false) Double minDistance,
            @RequestParam(name = "maxDistance", required = false) Double maxDistance,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "facility", required = false) String facility,
            @RequestParam(name = "sort", defaultValue = "distance") String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(courses.list(q, area, minDistance, maxDistance, level, facility, sort, page, size));
    }

    @GetMapping("/api/courses/{id}")
    public ApiResponse<CourseRepository.Detail> detail(@PathVariable("id") long id, @LoginUser(required = false) Long userId) {
        return ApiResponse.ok(courses.detail(id, userId));
    }

    @GetMapping("/api/courses/{id}/gpx")
    public ResponseEntity<Resource> download(@PathVariable("id") long id, @LoginUser(required = false) Long userId,
            HttpServletRequest request) {
        return downloads.download(id, userId, request.getMethod(), request.getHeader("Range"));
    }
}
