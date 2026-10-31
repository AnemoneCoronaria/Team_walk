package kr.sanchaek.course;

import jakarta.servlet.http.HttpServletRequest;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String facilities,
            @RequestParam(required = false) String visibility) {
        var form = new CourseService.NewCourseForm(name, level, description, facilities, visibility);
        return ApiResponse.ok(new Created(service.register(userId, file, form)));
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
