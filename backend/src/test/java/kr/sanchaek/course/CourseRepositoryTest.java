package kr.sanchaek.course;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import kr.sanchaek.common.ApiException;

@SpringBootTest
@Transactional
class CourseRepositoryTest {
    @Autowired CourseRepository courses;
    @Autowired JdbcTemplate jdbc;

    @Test void replacesWithdrawnAuthorNicknameWhilePreservingCourse() {
        var original = courses.detail(1, null);
        assertEquals(jdbc.queryForObject("SELECT nickname FROM users WHERE user_id=?", String.class,
                original.author().userId()), original.author().nickname());
        jdbc.update("UPDATE users SET status='WITHDRAWN' WHERE user_id=?", original.author().userId());
        var detail = courses.detail(1, null);
        assertEquals("탈퇴한 회원", detail.author().nickname());
        assertEquals(original.courseId(), detail.courseId());
        assertEquals(original.path(), detail.path());
    }

    @Test void filtersPagesAndCalculatesLandmarks() {
        var page = courses.list(null, null, null, null, null, "TOILET,BENCH", "rating", 1, 20);
        assertEquals(2, page.total());
        assertEquals(3, page.items().get(0).courseId());
        var detail = courses.detail(4, null);
        assertEquals(java.util.List.of(2L, 3L), detail.landmarks().stream().map(CourseRepository.Landmark::landmarkId).toList());
        assertEquals(4.0, courses.detail(1, null).rating());
        assertEquals(1, courses.detail(1, null).reviewCount());
    }
    @Test void visibilityHonorsOwnerAndJoinedParticipants() {
        long owner = jdbc.queryForObject("SELECT author_id FROM courses WHERE course_id=1", Long.class);
        long participant = jdbc.queryForObject("SELECT user_id FROM match_participants WHERE match_id=1 AND user_id<>? AND status='JOINED' LIMIT 1", Long.class, owner);
        jdbc.update("UPDATE courses SET visibility='PRIVATE' WHERE course_id=1");
        assertThrows(ApiException.class, () -> courses.detail(1, null));
        assertThrows(ApiException.class, () -> courses.detail(1, participant));
        assertTrue(courses.detail(1, owner).isMine());
        jdbc.update("UPDATE courses SET visibility='MATCH_ONLY' WHERE course_id=1");
        assertNotNull(courses.detail(1, participant));
        jdbc.update("UPDATE match_participants SET status='LEFT' WHERE match_id=1 AND user_id=?", participant);
        assertThrows(ApiException.class, () -> courses.detail(1, participant));
        jdbc.update("UPDATE courses SET is_hidden=TRUE WHERE course_id=1");
        assertThrows(ApiException.class, () -> courses.detail(1, owner));
    }
}
