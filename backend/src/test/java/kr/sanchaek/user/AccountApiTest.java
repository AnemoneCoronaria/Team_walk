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

/** 계정: 아이디 확인, 회원가입, 로그인 · 로그아웃, 내 정보, 프로필 수정, 비밀번호 변경, 위치 동의, 탈퇴 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static MockHttpSession user(long id) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionUtil.USER_ID, id);
        s.setAttribute(SessionUtil.ROLE, "USER");
        return s;
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String body) throws Exception {
        return mvc.perform(b.contentType("application/json").content(body));
    }

    private ResultActions login(String id, String pw) throws Exception {
        return json(post("/api/auth/login"), "{\"loginId\":\"" + id + "\",\"password\":\"" + pw + "\"}");
    }

    private static String signup(String loginId, String extra) {
        return "{\"loginId\":\"" + loginId + "\",\"password\":\"abcd1234!\",\"nickname\":\"새회원\",\"agreeTerms\":true,\"agreePrivacy\":true,"
                + "\"ageGroup\":\"20s\",\"languages\":[\"KO\"]" + extra + "}";
    }

    // ----- 아이디 확인 · 회원가입 -----

    @Test void checkId() throws Exception {
        mvc.perform(get("/api/users/check-id?loginId=newuser1")).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/users/check-id?loginId=minseo")).andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/users/check-id?loginId=MINSEO")).andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/users/check-id?loginId=ab")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/check-id")).andExpect(status().isBadRequest());
    }

    @Test void signupStoresHashAndCanLogin() throws Exception {
        json(post("/api/users"), signup("NewUser1", ",\"mbti\":\"enfp\",\"styles\":[\"SLOW\",\"SLOW\"]")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.userId").isNumber());
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE login_id = 'newuser1'", String.class);
        assertTrue(hash.startsWith("$2"), "BCrypt 로 저장");
        assertEquals("ENFP", jdbc.queryForObject("SELECT mbti FROM users WHERE login_id = 'newuser1'", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM user_tags t JOIN users u ON u.user_id = t.user_id WHERE u.login_id = 'newuser1' AND tag_type = 'STYLE'", Integer.class));
        login("newuser1", "abcd1234!").andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test void signupValidation() throws Exception {
        json(post("/api/users"), signup("minseo", "")).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DUPLICATE"));
        json(post("/api/users"), signup("MINSEO", "")).andExpect(status().isConflict());
        json(post("/api/users"), signup("ab", "")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", ",\"mbti\":\"ABCD\"")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", ",\"avatar\":\"없는것\"")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", ",\"styles\":[\"SLOW\",\"FAST\",\"QUIET\",\"CHAT\"]")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", "").replace("\"agreeTerms\":true", "\"agreeTerms\":false")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", "").replace("\"languages\":[\"KO\"]", "\"languages\":[]")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", "").replace("abcd1234!", "password")).andExpect(status().isBadRequest());
        json(post("/api/users"), signup("newuser2", "").replace("새회원", "  가  ")).andExpect(status().isBadRequest());
    }

    // ----- 로그인 -----

    @Test void loginRules() throws Exception {
        login("minseo", "test1234!").andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(2)).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        login("MINSEO", "test1234!").andExpect(status().isOk());
        login("minseo", "틀림").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
        login("없는사람", "test1234!").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
        json(post("/api/auth/login"), "{}").andExpect(status().isBadRequest());
    }

    @Test void suspendedLogin() throws Exception {
        login("banned", "test1234!").andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("SUSPENDED"));
        jdbc.update("UPDATE users SET suspended_until = NOW() - INTERVAL 1 DAY WHERE login_id = 'banned'");
        login("banned", "test1234!").andExpect(status().isOk());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE login_id = 'banned'", String.class));
        jdbc.update("UPDATE users SET status = 'SUSPENDED', suspended_until = NOW() + INTERVAL 1 DAY WHERE login_id = 'banned'");
        login("banned", "틀림").andExpect(status().isUnauthorized());   // 비밀번호가 틀리면 정지 여부를 알려 주지 않음
    }

    @Test void logout() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        MockHttpSession s = user(2);
        mvc.perform(post("/api/auth/logout").session(s)).andExpect(status().isOk());
        assertTrue(s.isInvalid());
    }

    // ----- 내 정보 · 프로필 -----

    @Test void meHidesPassword() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").session(user(2))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("minseo")).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test void profileUpdateIsPartial() throws Exception {
        json(put("/api/users/me").session(user(3)), "{\"nickname\":\"새이름\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("새이름")).andExpect(jsonPath("$.data.mbti").value("ENFP"));
        json(put("/api/users/me").session(user(3)), "{\"mbti\":\"\"}").andExpect(jsonPath("$.data.mbti").isEmpty());
        json(put("/api/users/me").session(user(3)), "{\"mbti\":\"intj\"}").andExpect(jsonPath("$.data.mbti").value("INTJ"));
        json(put("/api/users/me").session(user(3)), "{\"styles\":[]}").andExpect(jsonPath("$.data.styles").isEmpty());
        json(put("/api/users/me").session(user(3)), "{\"languages\":[\"EN\",\"EN\"]}").andExpect(jsonPath("$.data.languages.length()").value(1));
        json(put("/api/users/me").session(user(3)), "{}").andExpect(status().isOk());
    }

    @Test void profileValidation() throws Exception {
        json(put("/api/users/me"), "{}").andExpect(status().isUnauthorized());
        for (String body : java.util.List.of("{\"nickname\":\"가\"}", "{\"nickname\":\"  가  \"}", "{\"nickname\":\"" + "가".repeat(11) + "\"}", "{\"languages\":[]}",
                "{\"styles\":[\"SLOW\",\"FAST\",\"QUIET\",\"CHAT\"]}", "{\"mbti\":\"ABCD\"}", "{\"ageGroup\":\"10s\"}", "{\"avatar\":\"없음\"}")) {
            json(put("/api/users/me").session(user(3)), body).andExpect(status().isBadRequest());
        }
    }

    // ----- 비밀번호 · 위치 동의 -----

    @Test void changePassword() throws Exception {
        json(put("/api/users/me/password").session(user(3)), "{\"currentPassword\":\"틀림\",\"newPassword\":\"newpass12!\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("WRONG_PASSWORD"));
        json(put("/api/users/me/password").session(user(3)), "{\"currentPassword\":\"test1234!\",\"newPassword\":\"short\"}").andExpect(status().isBadRequest());
        json(put("/api/users/me/password").session(user(3)), "{\"currentPassword\":\"test1234!\",\"newPassword\":\"newpass12!\"}").andExpect(status().isOk());
        login("jeongran", "test1234!").andExpect(status().isUnauthorized());
        login("jeongran", "newpass12!").andExpect(status().isOk());
    }

    @Test void locationConsent() throws Exception {
        json(put("/api/users/me/consents").session(user(5)), "{\"agreeLocation\":false}").andExpect(status().isOk());
        mvc.perform(get("/api/users/me").session(user(5))).andExpect(jsonPath("$.data.agreeLocation").value(false));
        assertNull(jdbc.queryForObject("SELECT agree_location_at FROM users WHERE user_id = 5", java.time.LocalDateTime.class));
        json(put("/api/users/me/consents").session(user(5)), "{\"agreeLocation\":true}").andExpect(status().isOk());
        assertNotNull(jdbc.queryForObject("SELECT agree_location_at FROM users WHERE user_id = 5", java.time.LocalDateTime.class));
        json(put("/api/users/me/consents").session(user(5)), "{}").andExpect(status().isBadRequest());
    }

    // ----- 탈퇴 -----

    @Test void withdrawRules() throws Exception {
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE match_id <> 6");
        jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 7 DAY WHERE match_id = 6");
        String ok = "{\"password\":\"test1234!\"}";
        mvc.perform(delete("/api/users/me").session(user(6)).contentType("application/json").content("{\"password\":\"틀림\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("WRONG_PASSWORD"));
        mvc.perform(delete("/api/users/me").session(user(6))).andExpect(status().isBadRequest());
        // 승민은 4번(CLOSED) 모집자, 민서는 1번(OPEN), 동언은 3번(FULL)
        for (long host : new long[] {6, 2, 4}) {
            mvc.perform(delete("/api/users/me").session(user(host)).contentType("application/json").content(ok))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("HAS_OPEN_MATCH"));
        }
        // 모집 취소하면 가능. 참여 중이던 시작 전 글에서는 빠지고, 끝난 글 기록은 남음
        jdbc.update("UPDATE matches SET status = 'CANCELED' WHERE match_id = 4");
        MockHttpSession s = user(6);
        mvc.perform(delete("/api/users/me").session(s).contentType("application/json").content(ok)).andExpect(status().isOk());
        assertTrue(s.isInvalid());
        assertEquals("WITHDRAWN", jdbc.queryForObject("SELECT status FROM users WHERE user_id = 6", String.class));
        assertEquals("LEFT", jdbc.queryForObject("SELECT status FROM match_participants WHERE match_id = 3 AND user_id = 6", String.class));
        assertEquals("회원 탈퇴", jdbc.queryForObject("SELECT leave_reason FROM match_participants WHERE match_id = 3 AND user_id = 6", String.class));
        assertEquals("JOINED", jdbc.queryForObject("SELECT status FROM match_participants WHERE match_id = 6 AND user_id = 6", String.class));
        login("seungmin", "test1234!").andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/check-id?loginId=seungmin")).andExpect(jsonPath("$.data.available").value(false));
    }

    @Test void hiddenOrFinishedMatchDoesNotBlockWithdraw() throws Exception {
        String ok = "{\"password\":\"test1234!\"}";
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE match_id = 2");
        jdbc.update("UPDATE matches SET is_hidden = TRUE WHERE match_id = 2");   // 정란의 숨긴 글
        jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 7 DAY WHERE match_id = 6");   // 끝난 글은 민서 것
        mvc.perform(delete("/api/users/me").session(user(3)).contentType("application/json").content(ok)).andExpect(status().isOk());
    }

    // ----- 신고 -----

    private ResultActions report(long user, String type, long id, String reason) throws Exception {
        return json(post("/api/reports").session(user(user)), "{\"targetType\":\"" + type + "\",\"targetId\":" + id + ",\"reason\":\"" + reason + "\"}");
    }

    @Test void reportRules() throws Exception {
        json(post("/api/reports"), "{\"targetType\":\"MATCH\",\"targetId\":1,\"reason\":\"SPAM\"}").andExpect(status().isUnauthorized());
        report(5, "MATCH", 2, "SPAM").andExpect(status().isCreated()).andExpect(jsonPath("$.data.reportId").isNumber());
        report(5, "MATCH", 2, "RUDE").andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DUPLICATE"));
        report(5, "COURSE", 2, "DANGER").andExpect(status().isCreated());
        report(5, "USER", 6, "NO_SHOW").andExpect(status().isCreated());
        report(4, "REVIEW", 1, "SPAM").andExpect(status().isCreated());

        report(5, "COURSE", 3, "SPAM").andExpect(status().isBadRequest());     // 코스 사유가 아님
        report(5, "MATCH", 3, "DANGER").andExpect(status().isBadRequest());    // 글 사유가 아님
        report(5, "MATCH", 3, "없는사유").andExpect(status().isBadRequest());
        report(5, "XX", 3, "SPAM").andExpect(status().isBadRequest());
        report(5, "MATCH", 99999, "SPAM").andExpect(status().isNotFound());
        report(5, "USER", 5, "RUDE").andExpect(status().isBadRequest());       // 자기 자신
        report(5, "MATCH", 5, "SPAM").andExpect(status().isBadRequest());      // 자기 글
    }
}
