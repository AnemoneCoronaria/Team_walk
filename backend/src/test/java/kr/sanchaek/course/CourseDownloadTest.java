package kr.sanchaek.course;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import kr.sanchaek.common.ApiExceptionHandler;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.LoginUserArgumentResolver;
import kr.sanchaek.common.SessionUtil;

@SpringBootTest
@Transactional
class CourseDownloadTest {
    @Autowired CourseRepository courses;
    @Autowired JdbcTemplate jdbc;
    @TempDir Path uploadDir;

    private MockMvc mvc() {
        var downloads = new CourseDownloadService(courses, new FileStore(uploadDir.toString()));
        return MockMvcBuilders.standaloneSetup(new CourseController(courses, downloads, null))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    private int count() {
        return jdbc.queryForObject("SELECT download_count FROM courses WHERE course_id=1", Integer.class);
    }

    @Test void excludesHeadAndInvalidRangesButCountsPartialDownloads() throws Exception {
        Files.createDirectories(uploadDir.resolve("gpx"));
        Files.writeString(uploadDir.resolve("gpx/course-1.gpx"), "0123456789");
        var mvc = mvc();
        int before = count();
        mvc.perform(head("/api/courses/1/gpx")).andExpect(status().isOk());
        assertEquals(before, count());
        for (String range : java.util.List.of("bytes=100-", "bytes=5-2", "invalid", "bytes=0-8,1-9", "")) {
            mvc.perform(get("/api/courses/1/gpx").header("Range", range))
                    .andExpect(status().isRequestedRangeNotSatisfiable())
                    .andExpect(header().string("Content-Range", "bytes */10"));
        }
        assertEquals(before, count());
        mvc.perform(get("/api/courses/1/gpx").header("Range", "bytes=2-5"))
                .andExpect(status().isPartialContent()).andExpect(content().string("2345"));
        assertEquals(before + 1, count());
        mvc.perform(get("/api/courses/1/gpx").header("Range", "bytes=-2"))
                .andExpect(status().isPartialContent()).andExpect(content().string("89"));
        assertEquals(before + 2, count());
    }

    @Test void returnsOriginalFileAndIncrementsOnlySuccessfulRequests() throws Exception {
        Files.createDirectories(uploadDir.resolve("gpx"));
        byte[] original = "<gpx>원본</gpx>\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(uploadDir.resolve("gpx/course-1.gpx"), original);
        var mvc = mvc();
        int before = count();
        mvc.perform(get("/api/courses/1/gpx")).andExpect(status().isOk())
                .andExpect(content().contentType("application/gpx+xml"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("course-1.gpx")))
                .andExpect(content().bytes(original));
        assertEquals(before + 1, count());
        mvc.perform(get("/api/courses/999999/gpx")).andExpect(status().isNotFound());
        Files.delete(uploadDir.resolve("gpx/course-1.gpx"));
        mvc.perform(get("/api/courses/1/gpx")).andExpect(status().isNotFound());
        assertEquals(before + 1, count());
        jdbc.update("UPDATE courses SET gpx_path=NULL WHERE course_id=1");
        mvc.perform(get("/api/courses/1/gpx")).andExpect(status().isNotFound());
        assertEquals(before + 1, count());
    }

    @Test void respectsVisibilityAndParticipantStatus() throws Exception {
        Files.createDirectories(uploadDir.resolve("gpx"));
        Files.writeString(uploadDir.resolve("gpx/course-1.gpx"), "original");
        long owner = jdbc.queryForObject("SELECT author_id FROM courses WHERE course_id=1", Long.class);
        long participant = jdbc.queryForObject("SELECT user_id FROM match_participants WHERE match_id=1 AND user_id<>? AND status='JOINED' LIMIT 1", Long.class, owner);
        var ownerSession = new MockHttpSession(); ownerSession.setAttribute(SessionUtil.USER_ID, owner);
        var participantSession = new MockHttpSession(); participantSession.setAttribute(SessionUtil.USER_ID, participant);
        var mvc = mvc();
        int before = count();
        jdbc.update("UPDATE courses SET visibility='PRIVATE' WHERE course_id=1");
        mvc.perform(get("/api/courses/1/gpx")).andExpect(status().isNotFound());
        mvc.perform(get("/api/courses/1/gpx").session(participantSession)).andExpect(status().isNotFound());
        mvc.perform(get("/api/courses/1/gpx").session(ownerSession)).andExpect(status().isOk());
        jdbc.update("UPDATE courses SET visibility='MATCH_ONLY' WHERE course_id=1");
        mvc.perform(get("/api/courses/1/gpx").session(participantSession)).andExpect(status().isOk());
        jdbc.update("UPDATE match_participants SET status='LEFT' WHERE match_id=1 AND user_id=?", participant);
        mvc.perform(get("/api/courses/1/gpx").session(participantSession)).andExpect(status().isNotFound());
        jdbc.update("UPDATE courses SET is_hidden=TRUE WHERE course_id=1");
        mvc.perform(get("/api/courses/1/gpx").session(ownerSession)).andExpect(status().isNotFound());
        assertEquals(before + 2, count());
    }
}
