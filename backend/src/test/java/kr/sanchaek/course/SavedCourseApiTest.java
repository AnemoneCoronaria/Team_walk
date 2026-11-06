package kr.sanchaek.course;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import kr.sanchaek.common.SessionUtil;

/** 저장한 코스 (C2 ♡, P6): 저장 · 해제 · 목록 · 공개 범위. seed: 민서가 코스 3 · 4 를 저장 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavedCourseApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static MockHttpSession user(long id) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionUtil.USER_ID, id);
        s.setAttribute(SessionUtil.ROLE, "USER");
        return s;
    }

    @BeforeEach void dates() {
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE match_id <> 6");
        jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 7 DAY WHERE match_id = 6");
    }

    private List<Integer> saved(long userId, String query) throws Exception {
        String body = mvc.perform(get("/api/users/me/saved-courses" + query).session(user(userId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.items[*].courseId");
    }

    @Test void loginRequired() throws Exception {
        mvc.perform(get("/api/users/me/saved-courses")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/courses/1/save")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/courses/1/save")).andExpect(status().isUnauthorized());
    }

    @Test void saveListUnsave() throws Exception {
        assertEquals(List.of(), saved(5, ""));
        mvc.perform(post("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
        assertEquals(List.of(1), saved(5, ""));
        mvc.perform(delete("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
        assertEquals(List.of(), saved(5, ""));
    }

    @Test void savingTwiceAndUnsavingTwiceAreFine() throws Exception {
        mvc.perform(post("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
        mvc.perform(post("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM saved_courses WHERE user_id = 5", Integer.class));
        mvc.perform(delete("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
        mvc.perform(delete("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
    }

    @Test void onlyMyList() throws Exception {
        assertEquals(2, saved(2, "").size());
        assertEquals(List.of(), saved(3, ""));
    }

    @Test void itemShapeAndOpenMatches() throws Exception {
        mvc.perform(post("/api/courses/1/save").session(user(5)));
        mvc.perform(get("/api/users/me/saved-courses").session(user(5)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("옥구공원 순환 코스"))
                .andExpect(jsonPath("$.data.items[0].level").value("NORMAL"))
                .andExpect(jsonPath("$.data.items[0].start.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].savedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].openMatches").value(1));   // 코스 1: 1번 OPEN, 5번 취소
        jdbc.update("UPDATE matches SET is_hidden = TRUE WHERE match_id = 1");
        mvc.perform(get("/api/users/me/saved-courses").session(user(5))).andExpect(jsonPath("$.data.items[0].openMatches").value(0));
    }

    @Test void sortAndValidation() throws Exception {
        jdbc.update("INSERT INTO saved_courses (user_id, course_id, created_at) VALUES (5, 1, NOW() - INTERVAL 3 DAY), (5, 2, NOW() - INTERVAL 1 DAY), (5, 3, NOW() - INTERVAL 2 DAY)");
        assertEquals(List.of(2, 3, 1), saved(5, ""));                  // 최근 저장순
        assertEquals(List.of(2, 3, 1), saved(5, "?sort=recent"));
        assertEquals(List.of(1, 3, 2), saved(5, "?sort=distance"));    // 1.43 · 2.52 · 2.69
        assertEquals(List.of(3, 1, 2), saved(5, "?sort=rating"));      // 4.5 · 4.0 · 0
        assertEquals(List.of(3), saved(5, "?page=2&size=1"));
        mvc.perform(get("/api/users/me/saved-courses?sort=xx").session(user(5))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me/saved-courses?page=0").session(user(5))).andExpect(status().isBadRequest());
    }

    @Test void cannotSaveWhatICannotSee() throws Exception {
        mvc.perform(post("/api/courses/99999/save").session(user(5))).andExpect(status().isNotFound());
        jdbc.update("UPDATE courses SET visibility = 'PRIVATE' WHERE course_id = 1");   // 코스 1 작성자는 동언
        mvc.perform(post("/api/courses/1/save").session(user(5))).andExpect(status().isNotFound());
        mvc.perform(post("/api/courses/1/save").session(user(4))).andExpect(status().isOk());   // 작성자는 가능
        jdbc.update("UPDATE courses SET visibility = 'PUBLIC', is_hidden = TRUE WHERE course_id = 1");
        mvc.perform(post("/api/courses/1/save").session(user(5))).andExpect(status().isNotFound());
        jdbc.update("UPDATE courses SET is_hidden = FALSE, is_deleted = TRUE WHERE course_id = 1");
        mvc.perform(post("/api/courses/1/save").session(user(5))).andExpect(status().isNotFound());
    }

    @Test void listFollowsCourseVisibilityAndRestores() throws Exception {
        // 정란(3)은 코스 1 로 잡힌 동행에 참여한 적이 없어서 MATCH_ONLY 도 볼 수 없음
        mvc.perform(post("/api/courses/1/save").session(user(3)));
        for (String change : List.of("visibility = 'PRIVATE'", "visibility = 'MATCH_ONLY'", "is_hidden = TRUE", "is_deleted = TRUE")) {
            jdbc.update("UPDATE courses SET " + change + " WHERE course_id = 1");
            assertEquals(List.of(), saved(3, ""), change);
            mvc.perform(get("/api/users/me/saved-courses").session(user(3))).andExpect(jsonPath("$.data.total").value(0));
            jdbc.update("UPDATE courses SET visibility = 'PUBLIC', is_hidden = FALSE, is_deleted = FALSE WHERE course_id = 1");
            assertEquals(List.of(1), saved(3, ""), change + " 되돌림");
        }
    }

    @Test void matchOnlyCourseStaysForParticipant() throws Exception {
        // 동훈(5)은 코스 1 로 잡힌 1번 동행의 참여자라 MATCH_ONLY 여도 목록에 남음
        mvc.perform(post("/api/courses/1/save").session(user(5)));
        jdbc.update("UPDATE courses SET visibility = 'MATCH_ONLY' WHERE course_id = 1");
        assertEquals(List.of(1), saved(5, ""));
    }

    @Test void unsaveWorksEvenIfCourseIsGone() throws Exception {
        mvc.perform(post("/api/courses/1/save").session(user(5)));
        jdbc.update("UPDATE courses SET is_hidden = TRUE WHERE course_id = 1");
        mvc.perform(delete("/api/courses/1/save").session(user(5))).andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM saved_courses WHERE user_id = 5", Integer.class));
    }

    @Test void courseDetailShowsSaved() throws Exception {
        mvc.perform(get("/api/courses/3").session(user(2))).andExpect(jsonPath("$.data.saved").value(true));   // seed
        mvc.perform(get("/api/courses/1").session(user(2))).andExpect(jsonPath("$.data.saved").value(false));
        mvc.perform(get("/api/courses/3").session(user(5))).andExpect(jsonPath("$.data.saved").value(false));  // 다른 사람
        mvc.perform(get("/api/courses/3")).andExpect(jsonPath("$.data.saved").value(false));                   // 비로그인
        mvc.perform(post("/api/courses/3/save").session(user(5)));
        mvc.perform(get("/api/courses/3").session(user(5))).andExpect(jsonPath("$.data.saved").value(true));
    }
}
