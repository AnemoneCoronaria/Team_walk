package kr.sanchaek.course;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;

@SpringBootTest
@Transactional
class CourseServiceTest {
    @Autowired CourseRepository courses;
    @Autowired JdbcTemplate jdbc;
    @TempDir Path uploadDir;

    static MockMultipartFile gpx() {
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            points.append("<trkpt lat='37.35272' lon='").append(126.709915 + i * 0.0002).append("'/>");
        }
        byte[] xml = ("<gpx><trk><trkseg>" + points + "</trkseg></trk></gpx>").getBytes(StandardCharsets.UTF_8);
        return new MockMultipartFile("file", "route.gpx", "application/gpx+xml", xml);
    }

    private CourseService service() {
        return new CourseService(courses, new GpxService(), new FileStore(uploadDir.toString()));
    }

    @Test void registersCourseWithSavedGpxAndParsedRoute() throws Exception {
        var file = gpx();
        long id = service().register(4L, file, new CourseService.NewCourseForm(
                " 새 산책 코스 ", "EASY", "  ", "TOILET,BENCH,TOILET", "PRIVATE"));
        var detail = courses.detail(id, 4L);
        assertEquals("새 산책 코스", detail.name());
        assertNull(detail.description());
        assertEquals("OKGU", detail.area());
        assertEquals(List.of("BENCH", "TOILET"), detail.facilities());
        assertEquals(25, detail.path().size());
        assertTrue(detail.isMine());
        assertEquals("gpx/course-" + id + ".gpx", courses.gpxPath(id, 4L));
        assertArrayEquals(file.getBytes(), Files.readAllBytes(uploadDir.resolve("gpx/course-" + id + ".gpx")));
        assertThrows(ApiException.class, () -> courses.detail(id, null));
        assertEquals("PRIVATE", jdbc.queryForObject("SELECT visibility FROM courses WHERE course_id=?", String.class, id));
    }

    @Test void rejectsInvalidInputBeforeSaving() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM courses", Long.class);
        for (var form : List.of(
                new CourseService.NewCourseForm("", "EASY", null, null, "PUBLIC"),
                new CourseService.NewCourseForm("가".repeat(21), "EASY", null, null, "PUBLIC"),
                new CourseService.NewCourseForm("코스", "VERY_HARD", null, null, "PUBLIC"),
                new CourseService.NewCourseForm("코스", "EASY", null, "POOL", "PUBLIC"),
                new CourseService.NewCourseForm("코스", "EASY", null, null, null))) {
            var e = assertThrows(ApiException.class, () -> service().register(4L, gpx(), form));
            assertEquals("INVALID_INPUT", e.getCode());
        }
        var bad = new MockMultipartFile("file", "route.gpx", "application/gpx+xml", "<gpx/>".getBytes());
        var e = assertThrows(ApiException.class, () -> service().register(4L, bad,
                new CourseService.NewCourseForm("코스", "EASY", null, null, "PUBLIC")));
        assertEquals("INVALID_FORMAT", e.getCode());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM courses", Long.class));
    }
}
