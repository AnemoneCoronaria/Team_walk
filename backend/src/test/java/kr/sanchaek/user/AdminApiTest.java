package kr.sanchaek.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import kr.sanchaek.common.SessionUtil;

/** 관리자 AD1 신고 처리 · AD2 회원 관리 · AD4 코스 관리 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static MockHttpSession admin() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionUtil.USER_ID, 1L);
        s.setAttribute(SessionUtil.ROLE, "ADMIN");
        return s;
    }

    private static MockHttpSession user() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionUtil.USER_ID, 2L);
        s.setAttribute(SessionUtil.ROLE, "USER");
        return s;
    }

    private ResultActions put2(String url, String body) throws Exception {
        return mvc.perform(put(url).session(admin()).contentType("application/json").content(body));
    }

    private long newReport(String type, long targetId, String reason) {
        jdbc.update("INSERT INTO reports (reporter_id, target_type, target_id, reason) VALUES (5, ?, ?, ?) ON DUPLICATE KEY UPDATE status = 'PENDING', action = NULL",
                type, targetId, reason);
        return jdbc.queryForObject("SELECT report_id FROM reports WHERE reporter_id = 5 AND target_type = ? AND target_id = ?", Long.class, type, targetId);
    }

    @Test void adminOnly() throws Exception {
        for (String url : java.util.List.of("/api/admin/reports", "/api/admin/users", "/api/admin/courses")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
            mvc.perform(get(url).session(user())).andExpect(status().isForbidden());
            mvc.perform(get(url).session(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
        }
    }

    // ----- AD1 신고 처리 -----

    @Test void reportListFilter() throws Exception {
        mvc.perform(get("/api/admin/reports").session(admin())).andExpect(jsonPath("$.data.total").value(1));      // seed: 대기 1건
        mvc.perform(get("/api/admin/reports?status=DONE").session(admin())).andExpect(jsonPath("$.data.total").value(3));
        mvc.perform(get("/api/admin/reports?status=XX").session(admin())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/reports?page=0").session(admin())).andExpect(status().isBadRequest());
    }

    @Test void warnSuspendHideReject() throws Exception {
        int before = jdbc.queryForObject("SELECT warning_count FROM users WHERE user_id = 3", Integer.class);
        long warn = newReport("MATCH", 2, "SPAM");   // 2번 글 작성자 = 정란
        put2("/api/admin/reports/" + warn, "{\"action\":\"WARN\",\"memo\":\"  주의  \"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DONE"));
        assertEquals(before + 1, jdbc.queryForObject("SELECT warning_count FROM users WHERE user_id = 3", Integer.class));
        assertEquals("주의", jdbc.queryForObject("SELECT admin_memo FROM reports WHERE report_id = ?", String.class, warn));
        put2("/api/admin/reports/" + warn, "{\"action\":\"WARN\"}").andExpect(status().isConflict());   // 두 번 처리

        long suspend = newReport("USER", 6, "RUDE");
        put2("/api/admin/reports/" + suspend, "{\"action\":\"SUSPEND\"}").andExpect(status().isOk());
        assertEquals("SUSPENDED", jdbc.queryForObject("SELECT status FROM users WHERE user_id = 6", String.class));

        long hide = newReport("MATCH", 1, "SPAM");
        put2("/api/admin/reports/" + hide, "{\"action\":\"HIDE\"}").andExpect(status().isOk());
        mvc.perform(get("/api/matches/1")).andExpect(status().isNotFound());

        long hideReview = newReport("REVIEW", 2, "SPAM");
        put2("/api/admin/reports/" + hideReview, "{\"action\":\"HIDE\"}").andExpect(status().isOk());
        assertEquals(true, jdbc.queryForObject("SELECT is_hidden FROM reviews WHERE review_id = 2", Boolean.class));

        long reject = newReport("COURSE", 3, "DANGER");
        put2("/api/admin/reports/" + reject, "{\"action\":\"REJECT\"}").andExpect(status().isOk());
        mvc.perform(get("/api/courses/3")).andExpect(status().isOk());
    }

    @Test void processValidation() throws Exception {
        long userReport = newReport("USER", 6, "RUDE");
        put2("/api/admin/reports/" + userReport, "{\"action\":\"HIDE\"}").andExpect(status().isBadRequest());   // 사용자는 숨길 게시물이 없음
        // 실패하면 처리되지 않은 채 남음
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM reports WHERE report_id = ?", String.class, userReport));
        put2("/api/admin/reports/" + userReport, "{\"action\":\"XX\"}").andExpect(status().isBadRequest());
        put2("/api/admin/reports/" + userReport, "{}").andExpect(status().isBadRequest());
        put2("/api/admin/reports/99999", "{\"action\":\"WARN\"}").andExpect(status().isNotFound());
    }

    @Test void withdrawnTargetCannotBeWarned() throws Exception {
        long r = newReport("USER", 6, "RUDE");
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE user_id = 6");
        put2("/api/admin/reports/" + r, "{\"action\":\"WARN\"}").andExpect(status().isBadRequest());
        put2("/api/admin/reports/" + r, "{\"action\":\"SUSPEND\"}").andExpect(status().isBadRequest());
        put2("/api/admin/reports/" + r, "{\"action\":\"REJECT\"}").andExpect(status().isOk());
    }

    @Test void adminCannotBeSuspendedThroughReport() throws Exception {
        long r = newReport("COURSE", 4, "DANGER");   // 코스 4 작성자 = 관리자 본인
        put2("/api/admin/reports/" + r, "{\"action\":\"SUSPEND\"}").andExpect(status().isBadRequest());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE user_id = 1", String.class));
    }

    // ----- AD2 회원 관리 -----

    @Test void userListSearch() throws Exception {
        mvc.perform(get("/api/admin/users?q=minseo").session(admin())).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/users?q=민서").session(admin())).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/users?status=SUSPENDED").session(admin())).andExpect(jsonPath("$.data.items[0].loginId").value("banned"));
        mvc.perform(get("/api/admin/users?status=XX").session(admin())).andExpect(status().isBadRequest());
    }

    @Test void suspendAndRelease() throws Exception {
        put2("/api/admin/users/5/status", "{\"status\":\"SUSPENDED\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.suspendedUntil").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"loginId\":\"donghun\",\"password\":\"test1234!\"}"))
                .andExpect(status().isForbidden());
        put2("/api/admin/users/5/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.suspendedUntil").isEmpty());
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"loginId\":\"donghun\",\"password\":\"test1234!\"}"))
                .andExpect(status().isOk());
    }

    @Test void statusChangeLimits() throws Exception {
        put2("/api/admin/users/1/status", "{\"status\":\"SUSPENDED\"}").andExpect(status().isBadRequest());   // 자기 자신
        put2("/api/admin/users/99999/status", "{\"status\":\"SUSPENDED\"}").andExpect(status().isNotFound());
        put2("/api/admin/users/5/status", "{\"status\":\"WITHDRAWN\"}").andExpect(status().isBadRequest());
        put2("/api/admin/users/5/status", "{}").andExpect(status().isBadRequest());
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE user_id = 5");
        put2("/api/admin/users/5/status", "{\"status\":\"ACTIVE\"}").andExpect(status().isBadRequest());
    }

    @Test void otherAdminCannotBeSuspended() throws Exception {
        // AD2 화면 주석: "탈퇴 회원 · 관리자 계정은 바꿀 수 없음"
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE user_id = 3");
        put2("/api/admin/users/3/status", "{\"status\":\"SUSPENDED\"}").andExpect(status().isBadRequest());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE user_id = 3", String.class));
    }

    @Test void otherAdminCannotBeSuspendedThroughReport() throws Exception {
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE user_id = 3");
        long r = newReport("MATCH", 2, "SPAM");   // 2번 글 작성자 = 정란
        put2("/api/admin/reports/" + r, "{\"action\":\"SUSPEND\"}").andExpect(status().isBadRequest());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE user_id = 3", String.class));
    }

    // ----- AD2 임시 비밀번호 -----

    private String issue(long userId) throws Exception {
        String body = mvc.perform(post("/api/admin/users/" + userId + "/temp-password").session(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(userId))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.data.temporaryPassword");
    }

    private ResultActions login(String id, String pw) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"loginId\":\"" + id + "\",\"password\":\"" + pw + "\"}"));
    }

    @Test void tempPasswordReplacesOldAndCanBeChanged() throws Exception {
        String temp = issue(5);
        login("donghun", "test1234!").andExpect(status().isUnauthorized());
        login("donghun", temp).andExpect(status().isOk());

        // 원문이 아니라 해시로 저장
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE user_id = 5", String.class);
        assertTrue(hash.startsWith("$2"));
        assertFalse(hash.contains(temp));

        // 임시 비밀번호로 계정 설정의 비밀번호 변경 가능
        MockHttpSession donghun = new MockHttpSession();
        donghun.setAttribute(SessionUtil.USER_ID, 5L);
        donghun.setAttribute(SessionUtil.ROLE, "USER");
        mvc.perform(put("/api/users/me/password").session(donghun).contentType("application/json")
                .content("{\"currentPassword\":\"" + temp + "\",\"newPassword\":\"newpass12!\"}")).andExpect(status().isOk());
        login("donghun", temp).andExpect(status().isUnauthorized());
        login("donghun", "newpass12!").andExpect(status().isOk());
    }

    @Test void tempPasswordFollowsPasswordRuleAndIsRandom() throws Exception {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 30; i++) {
            String temp = issue(5);
            assertTrue(temp.matches("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$"), temp);   // 회원가입 규칙
            assertEquals(12, temp.length());
            assertFalse(temp.matches(".*[0O1lI].*"), temp);   // 헷갈리는 글자 없음
            seen.add(temp);
        }
        assertEquals(30, seen.size());
    }

    @Test void tempPasswordLimits() throws Exception {
        String url = "/api/admin/users/%d/temp-password";
        mvc.perform(post(url.formatted(5))).andExpect(status().isUnauthorized());
        mvc.perform(post(url.formatted(5)).session(user())).andExpect(status().isForbidden());
        mvc.perform(post(url.formatted(99999)).session(admin())).andExpect(status().isNotFound());
        mvc.perform(post(url.formatted(1)).session(admin())).andExpect(status().isBadRequest());   // 관리자 자신
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE user_id = 3");
        mvc.perform(post(url.formatted(3)).session(admin())).andExpect(status().isBadRequest());   // 다른 관리자
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE user_id = 5");
        String before = jdbc.queryForObject("SELECT password_hash FROM users WHERE user_id = 5", String.class);
        mvc.perform(post(url.formatted(5)).session(admin())).andExpect(status().isBadRequest());   // 탈퇴 회원
        assertEquals(before, jdbc.queryForObject("SELECT password_hash FROM users WHERE user_id = 5", String.class));
    }

    @Test void suspendedUserCanGetTempPasswordButStillCannotLogin() throws Exception {
        String temp = issue(7);   // banned: 정지 중
        login("banned", temp).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("SUSPENDED"));
    }

    // ----- AD4 코스 관리 -----

    @Test void courseListAndHide() throws Exception {
        mvc.perform(get("/api/admin/courses").session(admin())).andExpect(jsonPath("$.data.total").value(4));
        mvc.perform(get("/api/admin/courses?tab=REVIEW").session(admin())).andExpect(jsonPath("$.data.total").value(1));   // seed: 코스 2 신고 대기
        mvc.perform(get("/api/admin/courses?q=옥구").session(admin())).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/admin/courses?tab=XX").session(admin())).andExpect(status().isBadRequest());

        put2("/api/admin/courses/1/visibility", "{\"hidden\":true}").andExpect(status().isOk()).andExpect(jsonPath("$.data.hidden").value(true));
        mvc.perform(get("/api/courses/1")).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/courses").session(admin())).andExpect(jsonPath("$.data.total").value(4));   // 관리자에게는 보임
        put2("/api/admin/courses/1/visibility", "{\"hidden\":false}").andExpect(status().isOk());
        mvc.perform(get("/api/courses/1")).andExpect(status().isOk());

        put2("/api/admin/courses/99999/visibility", "{\"hidden\":true}").andExpect(status().isNotFound());
        put2("/api/admin/courses/1/visibility", "{}").andExpect(status().isBadRequest());
    }
}
