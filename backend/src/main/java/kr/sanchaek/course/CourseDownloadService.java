package kr.sanchaek.course;

import java.io.IOException;
import java.io.UncheckedIOException;
import kr.sanchaek.common.FileStore;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseDownloadService {
    private final CourseRepository courses;
    private final FileStore files;

    public CourseDownloadService(CourseRepository courses, FileStore files) {
        this.courses = courses;
        this.files = files;
    }

    @Transactional
    public ResponseEntity<Resource> download(long id, Long userId, String method, String range) {
        String path = courses.gpxPath(id, userId);
        ResponseEntity<Resource> response = files.download(path, "course-" + id + ".gpx");
        if ("GET".equals(method)) {
            if (range != null) {
                try {
                    // Use the same validation as Spring's resource response converter before counting.
                    var ranges = HttpRange.parseRanges(range);
                    if (ranges.isEmpty()) {
                        throw new IllegalArgumentException("Empty range");
                    }
                    HttpRange.toResourceRegions(ranges, response.getBody());
                } catch (IllegalArgumentException e) {
                    try {
                        return ResponseEntity.status(416)
                                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + response.getBody().contentLength()).build();
                    } catch (IOException io) {
                        throw new UncheckedIOException(io);
                    }
                }
            }
            courses.incrementDownloadCount(id, userId, path);
        }
        return response;
    }
}
