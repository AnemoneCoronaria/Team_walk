package kr.sanchaek.match;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import kr.sanchaek.common.ApiException;

/** 동행 쓰기 규칙: 작성 · 수정 · 취소 · 마감 · 참여 · 참여 취소 · 내보내기. 시드 날짜와 상관없게 날짜를 먼저 맞춤 (끝나면 되돌림) */
@SpringBootTest
@Transactional
class MatchRulesTest {
    private static final long ADMIN = 1, MINSEO = 2, JEONGRAN = 3, DONGEON = 4, DONGHUN = 5, SEUNGMIN = 6;

    @Autowired MatchService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void matchesAreInTheFuture() {
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE match_id <> 6");
        jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 7 DAY WHERE match_id = 6");
    }

    private static MatchRequest form(List<String> targets, String mbti, int capacity, long courseId) {
        return new MatchRequest("  같이 걸어요  ", courseId, LocalDateTime.now().plusDays(3), capacity, " 정문 ", targets, mbti,
                List.of("20s", "20s"), List.of("SLOW", "CHAT"), List.of("KO"), "   ", null);
    }

    private static MatchRequest form() {
        return form(List.of(), null, 4, 1);
    }

    private static ApiException fail(Runnable action) {
        return assertThrows(ApiException.class, action::run);
    }

    private static void assertFails(int status, String code, Runnable action) {
        ApiException e = fail(action);
        assertEquals(status, e.getStatus(), e.getMessage());
        assertEquals(code, e.getCode(), e.getMessage());
    }

    private String statusOf(long matchId) {
        return jdbc.queryForObject("SELECT status FROM v_match_status WHERE match_id = ?", String.class, matchId);
    }

    private String participant(long matchId, long userId) {
        List<String> rows = jdbc.queryForList(
                "SELECT status FROM match_participants WHERE match_id = ? AND user_id = ?", String.class, matchId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ----- 작성 -----

    @Test void createStoresEverythingAndHostJoins() {
        long id = service.create(JEONGRAN, form(), null);
        Map<String, Object> d = service.detail(id, JEONGRAN);
        assertEquals("같이 걸어요", d.get("title"));
        assertEquals("정문", d.get("meetPlace"));
        assertNull(d.get("description"));             // 공백만 있으면 비움
        assertEquals(List.of("20s"), d.get("ageGroups"));   // 중복 제거
        assertEquals(List.of("CHAT", "SLOW"), d.get("styles"));
        assertEquals("OPEN", d.get("status"));
        assertEquals(1, d.get("current"));
        assertEquals(true, d.get("isHost"));
        assertEquals(false, d.get("hasGpx"));
        assertEquals("JOINED", participant(id, JEONGRAN));
    }

    @Test void mbtiIsUpperCasedAndOnlyKeptForMbtiTarget() {
        assertEquals("ISFP", service.detail(service.create(MINSEO, form(List.of("MBTI"), "isfp", 4, 1), null), MINSEO).get("mbti"));
        assertNull(service.detail(service.create(MINSEO, form(List.of(), "ISFP", 4, 1), null), MINSEO).get("mbti"));
        assertEquals(400, fail(() -> service.create(MINSEO, form(List.of("MBTI"), null, 4, 1), null)).getStatus());
    }

    @Test void courseMustBeSelectable() {
        assertEquals(400, fail(() -> service.create(MINSEO, form(List.of(), null, 4, 9999), null)).getStatus());
        jdbc.update("UPDATE courses SET is_hidden = TRUE WHERE course_id = 1");
        assertEquals(400, fail(() -> service.create(MINSEO, form(), null)).getStatus());
        jdbc.update("UPDATE courses SET is_hidden = FALSE, is_deleted = TRUE WHERE course_id = 1");
        assertEquals(400, fail(() -> service.create(MINSEO, form(), null)).getStatus());
    }

    // ----- 상태별 허용 표 (MatchService 맨 위 주석과 같음) -----

    /** 2번 글(모집자 정란)을 원하는 상태로. 동훈이 참여 중, 정원은 FULL 이면 2 */
    private void set2To(String status) {
        jdbc.update("INSERT INTO match_participants (match_id, user_id) VALUES (2, ?)", DONGHUN);
        switch (status) {
            case "FULL" -> jdbc.update("UPDATE matches SET capacity = 2 WHERE match_id = 2");
            case "CLOSED", "CANCELED" -> jdbc.update("UPDATE matches SET status = ? WHERE match_id = 2", status);
            case "DONE" -> jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 1 DAY WHERE match_id = 2");
            default -> { }
        }
        assertEquals(status, statusOf(2));
    }

    private boolean allowed(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ApiException e) {
            assertEquals(409, e.getStatus(), e.getMessage());
            assertTrue(List.of("FULL", "CLOSED", "CANCELED", "DONE", "ALREADY_CLOSED").contains(e.getCode()), e.getCode());
            return false;
        }
    }

    private void assertRow(String status, boolean join, boolean leave, boolean edit, boolean close, boolean kick) {
        MatchRequest same = new MatchRequest("제목", 3L, LocalDateTime.now().plusDays(3), 6, "정문", List.of(), null,
                null, null, List.of("KO"), null, null);
        // 각 동작을 저장점 없이 시험하면 서로 영향을 주므로, 상태를 바꾸는 동작은 원래대로 돌려 놓음
        set2To(status);
        String stored = jdbc.queryForObject("SELECT status FROM matches WHERE match_id = 2", String.class);
        Integer capacity = jdbc.queryForObject("SELECT capacity FROM matches WHERE match_id = 2", Integer.class);
        var meetAt = jdbc.queryForObject("SELECT meet_at FROM matches WHERE match_id = 2", LocalDateTime.class);
        Runnable reset = () -> {
            jdbc.update("UPDATE matches SET status = ?, capacity = ?, meet_at = ? WHERE match_id = 2", stored, capacity, meetAt);
            jdbc.update("UPDATE match_participants SET status = 'JOINED' WHERE match_id = 2 AND user_id = ?", DONGHUN);
            jdbc.update("DELETE FROM match_participants WHERE match_id = 2 AND user_id = ?", ADMIN);
        };

        assertEquals(join, allowed(() -> service.join(2, ADMIN)), status + " 참여");
        reset.run();
        assertEquals(leave, allowed(() -> service.leave(2, DONGHUN, null)), status + " 참여 취소");
        reset.run();
        assertEquals(edit, allowed(() -> service.update(2, JEONGRAN, same, null)), status + " 수정");
        reset.run();
        assertEquals(edit, allowed(() -> service.cancel(2, JEONGRAN)), status + " 모집 취소");
        reset.run();
        assertEquals(close, allowed(() -> service.close(2, JEONGRAN)), status + " 마감");
        reset.run();
        assertEquals(kick, allowed(() -> service.kick(2, JEONGRAN, DONGHUN)), status + " 내보내기");
    }

    @Test void statusTableOpen()     { assertRow("OPEN", true, true, true, true, true); }
    @Test void statusTableFull()     { assertRow("FULL", false, true, true, true, true); }
    @Test void statusTableClosed()   { assertRow("CLOSED", false, true, true, false, true); }
    @Test void statusTableCanceled() { assertRow("CANCELED", false, false, false, false, false); }
    @Test void statusTableDone()     { assertRow("DONE", false, false, false, false, false); }

    // ----- 모집자 권한, 숨긴 글 -----

    @Test void onlyHostCanManage() {
        assertFails(403, "FORBIDDEN", () -> service.update(1, DONGHUN, form(), null));
        assertFails(403, "FORBIDDEN", () -> service.cancel(1, DONGHUN));
        assertFails(403, "FORBIDDEN", () -> service.close(1, DONGHUN));
        assertFails(403, "FORBIDDEN", () -> service.kick(1, DONGHUN, DONGHUN));
        assertFails(403, "FORBIDDEN", () -> service.cancel(1, ADMIN));   // 관리자도 글 쓴 사람은 아님
    }

    @Test void hiddenOrMissingMatchIs404ForEveryAction() {
        jdbc.update("UPDATE matches SET is_hidden = TRUE WHERE match_id = 1");
        for (long id : new long[] {1, 99999}) {
            assertEquals(404, fail(() -> service.detail(id, MINSEO)).getStatus());
            assertEquals(404, fail(() -> service.join(id, JEONGRAN)).getStatus());
            assertEquals(404, fail(() -> service.leave(id, DONGHUN, null)).getStatus());
            assertEquals(404, fail(() -> service.update(id, MINSEO, form(), null)).getStatus());
            assertEquals(404, fail(() -> service.cancel(id, MINSEO)).getStatus());
            assertEquals(404, fail(() -> service.close(id, MINSEO)).getStatus());
            assertEquals(404, fail(() -> service.kick(id, MINSEO, DONGHUN)).getStatus());
        }
    }

    // ----- 수정 -----

    @Test void updateReplacesFieldsAndTags() {
        var r = new MatchRequest("새 제목", 2L, LocalDateTime.now().plusDays(5).withNano(0), 5, "새 장소",
                List.of("SAME_LANGUAGE", "MBTI", "MBTI"), "enfp",
                List.of("30s"), List.of(), List.of("EN", "VI"), "소개", null);
        service.update(1, MINSEO, r, null);
        Map<String, Object> d = service.detail(1, MINSEO);
        assertEquals("새 제목", d.get("title"));
        assertEquals("ENFP", d.get("mbti"));
        assertEquals(List.of("MBTI", "SAME_LANGUAGE"), d.get("targets"));   // 중복은 한 번만, 코드 순
        assertEquals(List.of("30s"), d.get("ageGroups"));
        assertEquals(List.of(), d.get("styles"));
        assertEquals(List.of("EN", "VI"), d.get("languages"));
        assertEquals("소개", d.get("description"));
        assertEquals(2L, ((Map<?, ?>) d.get("course")).get("courseId"));
        assertEquals(2, d.get("current"));                 // 참여자는 그대로
        assertEquals(true, d.get("hasGpx"));               // 첨부는 그대로
    }

    @Test void capacityCannotGoBelowCurrent() {
        assertFails(409, "CAPACITY_TOO_SMALL", () -> service.update(3, DONGEON, form(List.of(), null, 2, 4), null));
        service.update(1, MINSEO, form(List.of(), null, 2, 1), null);   // 지금 2명 → 정원 2 는 가능, 정원 참이 됨
        assertEquals("FULL", statusOf(1));
    }

    // ----- 참여 조건 -----

    @Test void joinReasons() {
        assertFails(409, "ALREADY_JOINED", () -> service.join(1, MINSEO));     // 모집자
        assertFails(409, "ALREADY_JOINED", () -> service.join(1, DONGHUN));    // 참여 중
        assertFails(403, "KICKED", () -> service.join(2, SEUNGMIN));           // 내보내짐 (seed)
        assertFails(409, "FULL", () -> service.join(3, DONGHUN));
        assertFails(409, "CLOSED", () -> service.join(4, DONGHUN));
        assertFails(409, "CANCELED", () -> service.join(5, MINSEO));
        assertFails(409, "DONE", () -> service.join(6, DONGEON));
    }

    @Test void joinConditionsAge() {
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (2, 'AGE_GROUP', '40s')");
        assertFails(403, "AGE_MISMATCH", () -> service.join(2, DONGEON));
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (2, 'AGE_GROUP', '20s')");
        service.join(2, DONGEON);
    }

    @Test void joinConditionsMbti() {
        jdbc.update("UPDATE matches SET status = 'OPEN' WHERE match_id = 4");   // 4번: ISFP 모집
        assertFails(403, "MBTI_MISMATCH", () -> service.join(4, DONGEON));      // INTJ
        assertFails(403, "MBTI_MISMATCH", () -> service.join(4, MINSEO));       // MBTI 없음
        jdbc.update("UPDATE users SET mbti = 'ISFP' WHERE user_id = ?", DONGEON);
        service.join(4, DONGEON);
    }

    @Test void severalTargetsMustAllMatch() {
        // 4번을 MBTI 맞춤 + 같은 언어끼리(VI)로 → MBTI 는 맞아도 언어가 안 맞으면 못 들어감
        jdbc.update("UPDATE matches SET status = 'OPEN' WHERE match_id = 4");
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (4, 'TARGET', 'SAME_LANGUAGE')");
        jdbc.update("DELETE FROM match_tags WHERE match_id = 4 AND tag_type = 'LANGUAGE'");
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (4, 'LANGUAGE', 'VI')");
        jdbc.update("UPDATE users SET mbti = 'ISFP' WHERE user_id = ?", DONGEON);
        assertFails(403, "LANGUAGE_MISMATCH", () -> service.join(4, DONGEON));   // KO 만
        jdbc.update("INSERT INTO user_tags (user_id, tag_type, tag_value) VALUES (?, 'LANGUAGE', 'VI')", DONGEON);
        service.join(4, DONGEON);
    }

    @Test void joinConditionsLanguage() {
        jdbc.update("UPDATE matches SET capacity = 6 WHERE match_id = 3");      // 3번: 같은 언어끼리
        jdbc.update("DELETE FROM match_tags WHERE match_id = 3 AND tag_type IN ('LANGUAGE', 'AGE_GROUP')");
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (3, 'LANGUAGE', 'ZH')");
        assertFails(403, "LANGUAGE_MISMATCH", () -> service.join(3, ADMIN));    // KO 만
        service.join(3, DONGHUN);                                               // KO · ZH
    }

    @Test void exchangeAndAnyoneHaveNoLanguageLimit() {
        jdbc.update("DELETE FROM match_tags WHERE match_id = 1 AND tag_type = 'AGE_GROUP'");
        jdbc.update("DELETE FROM match_tags WHERE match_id = 1 AND tag_type = 'LANGUAGE'");
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (1, 'LANGUAGE', 'VI')");
        service.join(1, ADMIN);
        assertEquals("JOINED", participant(1, ADMIN));
    }

    /** 상세의 joinable 이 true 면 참여가 되고, false 면 그 reason 으로 거절돼야 함 (화면과 서버 판단이 같아야 함) */
    @Test void detailAndJoinAlwaysAgree() {
        int checked = 0;
        for (long matchId = 1; matchId <= 6; matchId++) {
            for (long userId = 1; userId <= 6; userId++) {
                Map<String, Object> d = service.detail(matchId, userId);
                final long m = matchId, u = userId;
                String label = "동행 " + m + ", 회원 " + u;
                if (Boolean.TRUE.equals(d.get("joinable"))) {
                    service.join(m, u);
                    assertEquals("JOINED", participant(m, u), label);
                    jdbc.update("DELETE FROM match_participants WHERE match_id = ? AND user_id = ?", m, u);
                } else {
                    ApiException e = fail(() -> service.join(m, u));
                    String reason = (String) d.get("reason");
                    assertEquals("HOST".equals(reason) ? "ALREADY_JOINED" : reason, e.getCode(), label);
                }
                checked++;
            }
        }
        assertEquals(36, checked);
    }

    @Test void guestSeesStateReasonButNotProfileConditions() {
        jdbc.update("INSERT INTO match_tags (match_id, tag_type, tag_value) VALUES (2, 'AGE_GROUP', '50s+')");
        assertEquals(true, service.detail(2, null).get("joinable"));            // 프로필 조건은 안 봄 → 누르면 로그인 창
        assertEquals("FULL", service.detail(3, null).get("reason"));            // 글 상태는 봄
        assertEquals(false, service.detail(2, null).get("isHost"));
        assertNull(service.detail(2, null).get("myStatus"));
    }

    // ----- 참여 취소 · 재참여 · 내보내기 -----

    @Test void leaveRules() {
        assertFails(409, "HOST_CANNOT_LEAVE", () -> service.leave(1, MINSEO, "사유"));
        assertFails(409, "NOT_JOINED", () -> service.leave(1, JEONGRAN, null));
        assertFails(409, "NOT_JOINED", () -> service.leave(2, SEUNGMIN, null));  // 내보내진 사람

        service.leave(1, DONGHUN, "  일정이 생겼어요  ");
        assertEquals("LEFT", participant(1, DONGHUN));
        assertEquals("일정이 생겼어요", jdbc.queryForObject(
                "SELECT leave_reason FROM match_participants WHERE match_id = 1 AND user_id = ?", String.class, DONGHUN));
        assertEquals(1, service.detail(1, MINSEO).get("current"));
        assertFails(409, "NOT_JOINED", () -> service.leave(1, DONGHUN, null));   // 두 번 취소
    }

    @Test void rejoinAfterLeavingClearsReason() {
        service.leave(1, DONGHUN, "일정이 생겼어요");
        assertEquals("LEFT", service.detail(1, DONGHUN).get("myStatus"));
        assertEquals(true, service.detail(1, DONGHUN).get("joinable"));
        service.join(1, DONGHUN);
        assertEquals("JOINED", participant(1, DONGHUN));
        assertNull(jdbc.queryForObject(
                "SELECT leave_reason FROM match_participants WHERE match_id = 1 AND user_id = ?", String.class, DONGHUN));
    }

    @Test void leavingFullMatchReopensIt() {
        jdbc.update("UPDATE matches SET capacity = 2 WHERE match_id = 1");
        assertEquals("FULL", statusOf(1));
        service.leave(1, DONGHUN, null);
        assertEquals("OPEN", statusOf(1));
    }

    @Test void kickRules() {
        assertEquals(400, fail(() -> service.kick(1, MINSEO, MINSEO)).getStatus());
        assertFails(409, "NOT_JOINED", () -> service.kick(1, MINSEO, JEONGRAN));

        service.kick(1, MINSEO, DONGHUN);
        assertEquals("KICKED", participant(1, DONGHUN));
        assertEquals(false, service.detail(1, DONGHUN).get("joinable"));
        assertEquals("KICKED", service.detail(1, DONGHUN).get("reason"));
        assertFails(403, "KICKED", () -> service.join(1, DONGHUN));
        assertFails(409, "NOT_JOINED", () -> service.kick(1, MINSEO, DONGHUN));
    }

    @Test void cancelKeepsRowAndParticipants() {
        service.cancel(1, MINSEO);
        assertEquals("CANCELED", statusOf(1));
        Map<String, Object> d = service.detail(1, DONGHUN);   // 글은 남음
        assertEquals("CANCELED", d.get("status"));
        assertEquals(2, d.get("current"));
    }

    @Test void closeThenFullStaysClosed() {
        service.close(1, MINSEO);
        assertEquals("CLOSED", statusOf(1));
        assertFails(409, "ALREADY_CLOSED", () -> service.close(1, MINSEO));
    }
}
