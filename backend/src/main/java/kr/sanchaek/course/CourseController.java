package kr.sanchaek.course;

import jakarta.servlet.http.HttpServletRequest;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

@RestController
public class CourseController {
    private final CourseRepository courses;
    private final CourseDownloadService downloads;
    public CourseController(CourseRepository courses, CourseDownloadService downloads) {
        this.courses = courses;
        this.downloads = downloads;
    }

    @GetMapping("/api/courses")
    public ApiResponse<PageResult<CourseRepository.Summary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String facility,
            @RequestParam(defaultValue = "distance") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(courses.list(q, area, minDistance, maxDistance, level, facility, sort, page, size));
    }

    @GetMapping("/api/courses/{id}")
    public ApiResponse<CourseRepository.Detail> detail(@PathVariable long id, @LoginUser(required = false) Long userId) {
        return ApiResponse.ok(courses.detail(id, userId));
    }

    @GetMapping("/api/courses/{id}/gpx")
    public ResponseEntity<Resource> download(@PathVariable long id, @LoginUser(required = false) Long userId,
            HttpServletRequest request) {
        return downloads.download(id, userId, request.getMethod(), request.getHeader("Range"));
    }
}
