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
        return gpxOf(points);
    }

    /** 위도 0.45도(약 50km)씩 25점 → 약 1,200km */
    static MockMultipartFile longGpx() {
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            points.append("<trkpt lat='").append(27.0 + i * 0.45).append("' lon='126.709915'/>");
        }
        return gpxOf(points);
    }

    private static MockMultipartFile gpxOf(CharSequence points) {
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

    @Test void authorUpdatesOnlySentFieldsAndOthersAreForbidden() {
        long author = jdbc.queryForObject("SELECT author_id FROM courses WHERE course_id=1", Long.class);
        var before = courses.detail(1, author);
        service().update(author, 1, new CourseService.UpdateCourseRequest(null, "", "HARD", List.of("PARKING"), null));
        var after = courses.detail(1, author);
        assertEquals(before.name(), after.name());
        assertNull(after.description());
        assertEquals("HARD", after.level());
        assertEquals(List.of("PARKING"), after.facilities());
        assertEquals(before.path(), after.path());

        var forbidden = assertThrows(ApiException.class, () -> service().update(author + 1, 1,
                new CourseService.UpdateCourseRequest("남의 코스", null, null, null, null)));
        assertEquals(403, forbidden.getStatus());
        var invalid = assertThrows(ApiException.class, () -> service().update(author, 1,
                new CourseService.UpdateCourseRequest(null, null, null, null, "SECRET")));
        assertEquals(400, invalid.getStatus());
        assertEquals(404, assertThrows(ApiException.class, () -> service().delete(author, 9999)).getStatus());
    }

    @Test void deletedCourseLeavesListsAndDetailButKeepsReviewsAndMatches() {
        long author = jdbc.queryForObject("SELECT author_id FROM courses WHERE course_id=1", Long.class);
        long mineBefore = courses.mine(author, 1, 20).total();
        assertTrue(courses.mine(author, 1, 20).items().stream().anyMatch(c -> c.courseId() == 1 && c.visibility() != null));
        assertEquals(403, assertThrows(ApiException.class, () -> service().delete(author + 1, 1)).getStatus());
        long reviews = jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE course_id=1", Long.class);
        long matches = jdbc.queryForObject("SELECT COUNT(*) FROM matches WHERE course_id=1", Long.class);
        service().delete(author, 1);
        assertEquals(mineBefore - 1, courses.mine(author, 1, 20).total());
        assertThrows(ApiException.class, () -> courses.detail(1, author));
        assertEquals(404, assertThrows(ApiException.class, () -> service().delete(author, 1)).getStatus());
        assertEquals(reviews, jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE course_id=1", Long.class));
        // 명세: 코스를 지워도 그 코스로 잡힌 동행은 남김 (schema.sql courses 주석)
        assertEquals(matches, jdbc.queryForObject("SELECT COUNT(*) FROM matches WHERE course_id=1", Long.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE course_id=1", Long.class));
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

    @Test void rejectsRouteLongerThanDistanceColumn() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM courses", Long.class);
        var e = assertThrows(ApiException.class, () -> service().register(4L, longGpx(),
                new CourseService.NewCourseForm("긴 코스", "HARD", null, null, "PUBLIC")));
        assertEquals(400, e.getStatus());
        assertEquals("INVALID_INPUT", e.getCode());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM courses", Long.class));
    }

    @Test void limitsDescriptionTo20000Characters() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM courses", Long.class);
        var tooLong = "가".repeat(20001);
        var e = assertThrows(ApiException.class, () -> service().register(4L, gpx(),
                new CourseService.NewCourseForm("코스", "EASY", tooLong, null, "PUBLIC")));
        assertEquals("INVALID_INPUT", e.getCode());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM courses", Long.class));

        long author = jdbc.queryForObject("SELECT author_id FROM courses WHERE course_id=1", Long.class);
        var update = assertThrows(ApiException.class, () -> service().update(author, 1,
                new CourseService.UpdateCourseRequest(null, tooLong, null, null, null)));
        assertEquals(400, update.getStatus());
        service().update(author, 1, new CourseService.UpdateCourseRequest(null, "가".repeat(20000), null, null, null));
        assertEquals(20000, courses.detail(1, author).description().length());
    }
}
