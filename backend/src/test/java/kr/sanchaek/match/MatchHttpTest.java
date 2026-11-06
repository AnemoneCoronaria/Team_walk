package kr.sanchaek.match;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import kr.sanchaek.common.SessionUtil;

/** 요청 그대로 시험: 세션 · 권한 · 입력 검증 · multipart · GPX 파일 · 오류 JSON. 업로드 폴더는 임시 폴더 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchHttpTest {
    @TempDir static Path uploads;

    @DynamicPropertySource static void uploadDir(DynamicPropertyRegistry r) {
        r.add("app.upload-dir", () -> uploads.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static final byte[] GPX = gpx();

    private static byte[] gpx() {
        try {
            return Files.readAllBytes(Path.of("../db/sample-uploads/gpx/course-2.gpx"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static MockHttpSession as(long userId, String role) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionUtil.USER_ID, userId);
        s.setAttribute(SessionUtil.ROLE, role);
        return s;
    }

    private static MockHttpSession user(long userId) {
        return as(userId, "USER");
    }

    private static final String FUTURE = LocalDateTime.now().plusDays(5).withNano(0).toString();

    private static MockMultipartHttpServletRequestBuilder form(String url, String method, String... overrides) {
        MockMultipartHttpServletRequestBuilder b = multipart(url);
        b.with(r -> {
            r.setMethod(method);
            return r;
        });
        java.util.Map<String, String[]> f = new java.util.LinkedHashMap<>();
        f.put("title", new String[] {"제목"});
        f.put("courseId", new String[] {"1"});
        f.put("meetAt", new String[] {FUTURE});
        f.put("capacity", new String[] {"3"});
        f.put("meetPlace", new String[] {"정문"});
        f.put("languages", new String[] {"KO"});
        for (String o : overrides) {
            String[] kv = o.split("=", 2);
            if (kv.length == 1 || kv[1].isEmpty()) {
                f.remove(kv[0]);
            } else {
                f.put(kv[0], kv[1].split(","));
            }
        }
        f.forEach((k, v) -> b.param(k, v));
        return b;
    }

    @BeforeEach void dates() {
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE match_id <> 6");
        jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 7 DAY WHERE match_id = 6");
    }

    private void bad(String... overrides) throws Exception {
        mvc.perform(form("/api/matches", "POST", overrides).session(user(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    // ----- 로그인 · 권한 -----

    @Test void writesRequireLogin() throws Exception {
        for (var req : java.util.List.of(form("/api/matches", "POST"), form("/api/matches/1", "PUT"))) {
            mvc.perform(req).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        }
        mvc.perform(delete("/api/matches/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/matches/1/close")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/matches/1/join")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/matches/1/join")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/matches/1/participants/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/matches/1/comments")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/matches/1/comments").contentType("application/json").content("{\"content\":\"a\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me/matches")).andExpect(status().isUnauthorized());
    }

    @Test void readsAreOpen() throws Exception {
        mvc.perform(get("/api/matches")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
        mvc.perform(get("/api/matches/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.isHost").value(false));
        mvc.perform(get("/api/matches/1/gpx")).andExpect(status().isNotFound());   // 임시 업로드 폴더에 파일이 없음
    }

    @Test void adminApiNeedsAdmin() throws Exception {
        mvc.perform(get("/api/admin/matches")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/matches").session(user(2))).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/matches/1/visibility").session(user(2)).contentType("application/json").content("{\"hidden\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/matches").session(as(1, "ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.counts.open").exists());
    }

    // ----- 탈퇴 · 정지 세션 (SessionCheckInterceptor) -----

    @Test void suspendedOrWithdrawnSessionIsLoggedOut() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED', suspended_until = NOW() + INTERVAL 1 DAY WHERE user_id = 5");
        MockHttpSession s = user(5);
        mvc.perform(post("/api/matches/2/join").session(s)).andExpect(status().isUnauthorized());
        assertTrue(s.isInvalid());

        jdbc.update("UPDATE users SET status = 'SUSPENDED', suspended_until = NULL WHERE user_id = 5");
        mvc.perform(get("/api/users/me/matches").session(user(5))).andExpect(status().isUnauthorized());

        jdbc.update("UPDATE users SET status = 'WITHDRAWN', suspended_until = NULL WHERE user_id = 5");
        mvc.perform(get("/api/users/me/matches").session(user(5))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/matches/1").session(user(5))).andExpect(status().isOk()).andExpect(jsonPath("$.data.myStatus").isEmpty());
    }

    @Test void expiredSuspensionPasses() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED', suspended_until = NOW() - INTERVAL 1 DAY WHERE user_id = 5");
        mvc.perform(get("/api/users/me/matches").session(user(5))).andExpect(status().isOk());
    }

    @Test void suspendedAdminLosesAdminAccess() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED', suspended_until = NOW() + INTERVAL 1 DAY WHERE user_id = 1");
        mvc.perform(get("/api/admin/matches").session(as(1, "ADMIN"))).andExpect(status().isUnauthorized());
    }

    // ----- 글쓰기 입력 검증 -----

    @Test void createValidation() throws Exception {
        bad("title=");
        bad("title=" + "가".repeat(31));
        bad("courseId=");
        bad("courseId=abc");
        bad("meetAt=");
        bad("meetAt=내일");
        bad("meetAt=2020-01-01T10:00:00");
        bad("capacity=");
        bad("capacity=1");
        bad("capacity=7");
        bad("capacity=abc");
        bad("meetPlace=");
        bad("meetPlace=" + "가".repeat(101));
        bad("targets=FRIENDS");
        bad("targets=ANYONE");             // 누구나는 값 없이 (아무것도 안 보냄)
        bad("targets=MBTI");               // MBTI 맞춤인데 MBTI 없음
        bad("targets=EXCHANGE,MBTI");
        bad("targets=MBTI", "mbti=ABCD");
        bad("languages=");
        bad("languages=JP");
        bad("styles=WALK");
        bad("ageGroups=10s");
        bad("description=" + "가".repeat(1001));
    }

    @Test void limitsAreInclusive() throws Exception {
        mvc.perform(form("/api/matches", "POST", "title=" + "가".repeat(30), "capacity=6", "meetPlace=" + "가".repeat(100),
                "description=" + "가".repeat(1000), "ageGroups=50s+", "styles=SLOW,CHAT", "languages=KO,ZH").session(user(2)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.matchId").isNumber());
    }

    @Test void jsonBodyIsRejectedAs400NotServerError() throws Exception {
        mvc.perform(post("/api/matches").session(user(2)).contentType("application/json").content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    // ----- GPX -----

    @Test void gpxAttachReplaceRemove() throws Exception {
        var req = form("/api/matches", "POST").file(new MockMultipartFile("gpx", "route.gpx", "application/gpx+xml", GPX)).session(user(2));
        String body = mvc.perform(req).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll("\\D", ""));
        Path saved = uploads.resolve("gpx/match-" + id + ".gpx");
        assertTrue(Files.exists(saved));

        mvc.perform(get("/api/matches/" + id)).andExpect(jsonPath("$.data.hasGpx").value(true)).andExpect(jsonPath("$.data.path").isNotEmpty());
        mvc.perform(get("/api/matches/" + id + "/gpx")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("match-" + id + ".gpx")));

        // 수정할 때 파일을 안 보내면 그대로
        mvc.perform(form("/api/matches/" + id, "PUT").session(user(2))).andExpect(status().isOk());
        mvc.perform(get("/api/matches/" + id)).andExpect(jsonPath("$.data.hasGpx").value(true));

        // removeGpx
        mvc.perform(form("/api/matches/" + id, "PUT", "removeGpx=true").session(user(2))).andExpect(status().isOk());
        mvc.perform(get("/api/matches/" + id)).andExpect(jsonPath("$.data.hasGpx").value(false));
        mvc.perform(get("/api/matches/" + id + "/gpx")).andExpect(status().isNotFound());
        assertFalse(Files.exists(saved));
    }

    @Test void badGpxSavesNothing() throws Exception {
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM matches", Integer.class);
        var req = form("/api/matches", "POST").file(new MockMultipartFile("gpx", "route.gpx", "application/gpx+xml", "not gpx".getBytes())).session(user(2));
        mvc.perform(req).andExpect(status().isBadRequest());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM matches", Integer.class));
    }

    // ----- 파라미터 · 오류 JSON -----

    @Test void listParamsAreValidated() throws Exception {
        for (String q : java.util.List.of("status=XX", "sort=abc", "page=0", "size=0", "size=101", "page=abc", "from=abc", "to=2026-13-01",
                "timeSlot=DAWN", "courseId=abc", "page=2147483647&size=100")) {
            mvc.perform(get("/api/matches?" + q)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(get("/api/matches?status=FULL&size=1&page=1&sort=recent&style=SLOW,CHAT&lang=KO&age=50s%2B")).andExpect(status().isOk());
    }

    @Test void pathAndBodyErrorsAreJson() throws Exception {
        mvc.perform(get("/api/matches/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/matches/99999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/matches/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/matches/1/join").session(user(5)).contentType("application/json").content("{깨진 JSON")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/matches/1/join").session(user(5)).contentType("application/json").content("{\"reason\":\"" + "가".repeat(201) + "\"}")).andExpect(status().isBadRequest());
    }

    @Test void leaveBodyIsOptional() throws Exception {
        mvc.perform(delete("/api/matches/1/join").session(user(5))).andExpect(status().isOk());
    }

    @Test void commentValidation() throws Exception {
        for (String body : java.util.List.of("{}", "{\"content\":\"   \"}", "{\"content\":\"" + "가".repeat(501) + "\"}", "깨진")) {
            mvc.perform(post("/api/matches/1/comments").session(user(5)).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/matches/1/comments").session(user(5)).contentType("application/json").content("{\"content\":\"" + "가".repeat(500) + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.commentId").isNumber());
    }

    @Test void adminVisibilityValidation() throws Exception {
        mvc.perform(put("/api/admin/matches/1/visibility").session(as(1, "ADMIN")).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/matches/99999/visibility").session(as(1, "ADMIN")).contentType("application/json").content("{\"hidden\":true}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/matches?status=XX").session(as(1, "ADMIN"))).andExpect(status().isBadRequest());
    }

    @Test void leftParticipantsOnlyInHostResponse() throws Exception {
        mvc.perform(get("/api/matches/6").session(user(2))).andExpect(jsonPath("$.data.leftParticipants[0].reason").value("일정이 생겼어요"));
        mvc.perform(get("/api/matches/6").session(user(3))).andExpect(jsonPath("$.data.leftParticipants").doesNotExist());
        mvc.perform(get("/api/matches/6")).andExpect(jsonPath("$.data.leftParticipants").doesNotExist());
    }
}
