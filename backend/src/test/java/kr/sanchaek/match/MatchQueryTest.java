package kr.sanchaek.match;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import kr.sanchaek.common.ApiException;

/** 동행 목록 필터 · 정렬 · 페이지, 내 동행 탭. 시드 날짜와 상관없게 날짜를 고정 (끝나면 되돌림) */
@SpringBootTest
@Transactional
class MatchQueryTest {
    @Autowired MatchRepository repo;
    @Autowired JdbcTemplate jdbc;

    private LocalDate base;

    @BeforeEach void fixDates() {
        base = LocalDate.now().plusDays(10);
        for (long id = 1; id <= 5; id++) {
            at(id, base.plusDays(id), "10:00:00");
        }
        jdbc.update("UPDATE matches SET meet_at = NOW() - INTERVAL 7 DAY WHERE match_id = 6");
    }

    private void at(long id, LocalDate day, String time) {
        jdbc.update("UPDATE matches SET meet_at = ? WHERE match_id = ?", day + " " + time, id);
    }

    private record S(String status, LocalDate from, LocalDate to, String timeSlot, List<String> styles, List<String> languages,
            String target, String age, String area, Long courseId, String sort, int page, int size) {
        static S open() {
            return new S("OPEN", null, null, null, null, null, null, null, null, null, "deadline", 1, 50);
        }
        MatchSearch q() {
            return new MatchSearch(status, from, to, timeSlot, styles, languages, target, age, area, courseId, sort, page, size);
        }
    }

    private List<Long> ids(MatchSearch q) {
        return repo.findMatches(q).items().stream().map(m -> (Long) m.get("matchId")).toList();
    }

    private List<Long> tab(long userId, String tab) {
        return repo.findMyMatches(userId, tab, 1, 50).items().stream().map(m -> (Long) m.get("matchId")).toList();
    }

    @Test void statusFilterUsesCalculatedStatus() {
        assertEquals(List.of(1L, 2L), ids(S.open().q()));
        for (var e : Map.of("FULL", 3L, "CLOSED", 4L, "CANCELED", 5L, "DONE", 6L).entrySet()) {
            assertEquals(List.of(e.getValue()), ids(new S(e.getKey(), null, null, null, null, null, null, null, null, null, "deadline", 1, 50).q()), e.getKey());
        }
    }

    @Test void hiddenMatchesNeverShow() {
        jdbc.update("UPDATE matches SET is_hidden = TRUE WHERE match_id = 1");
        assertEquals(List.of(2L), ids(S.open().q()));
        assertEquals(1, repo.findMatches(S.open().q()).total());
    }

    @Test void timeSlotBoundaries() {
        // 아침 5~10시, 점심 11~16시, 저녁 17~20시, 밤 21~4시
        record C(String time, String slot) { }
        for (C c : List.of(new C("05:00:00", "MORNING"), new C("10:59:59", "MORNING"), new C("11:00:00", "AFTERNOON"),
                new C("16:59:59", "AFTERNOON"), new C("17:00:00", "EVENING"), new C("20:59:59", "EVENING"),
                new C("21:00:00", "NIGHT"), new C("23:59:59", "NIGHT"), new C("00:00:00", "NIGHT"), new C("04:59:59", "NIGHT"))) {
            at(1, base, c.time());
            at(2, base.plusDays(1), "12:00:00");      // 2번은 항상 점심
            for (String slot : List.of("MORNING", "AFTERNOON", "EVENING", "NIGHT")) {
                var s = new S("OPEN", null, null, slot, null, null, null, null, null, null, "deadline", 1, 50);
                boolean expected = slot.equals(c.slot());
                assertEquals(expected, ids(s.q()).contains(1L), c.time() + " " + slot);
            }
        }
        var bad = new S("OPEN", null, null, "DAWN", null, null, null, null, null, null, "deadline", 1, 50);
        assertEquals(400, assertThrows(ApiException.class, () -> repo.findMatches(bad.q())).getStatus());
    }

    @Test void dateRangeIncludesBothEnds() {
        at(1, base, "00:00:00");
        at(2, base.plusDays(2), "23:59:59");
        var both = new S("OPEN", base, base.plusDays(2), null, null, null, null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(1L, 2L), ids(both.q()));
        var first = new S("OPEN", base, base, null, null, null, null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(1L), ids(first.q()));
        var after = new S("OPEN", base.plusDays(3), null, null, null, null, null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(), ids(after.q()));
    }

    @Test void tagFiltersMatchAnyValue() {
        var slow = new S("OPEN", null, null, null, List.of("SLOW"), null, null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(1L), ids(slow.q()));
        var either = new S("OPEN", null, null, null, List.of("SLOW", "CHAT"), null, null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(1L, 2L), ids(either.q()));
        var zh = new S("OPEN", null, null, null, null, List.of("ZH"), null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(1L), ids(zh.q()));
        var styleAndLang = new S("OPEN", null, null, null, List.of("CHAT"), List.of("ZH"), null, null, null, null, "deadline", 1, 50);
        assertEquals(List.of(), ids(styleAndLang.q()));
    }

    @Test void ageFilterPassesNoAgeTag() {
        for (var e : Map.of("20s", List.of(1L, 2L), "30s", List.of(1L, 2L), "40s", List.of(2L), "50s+", List.of(2L)).entrySet()) {
            var s = new S("OPEN", null, null, null, null, null, null, e.getKey(), null, null, "deadline", 1, 50);
            assertEquals(e.getValue(), ids(s.q()), e.getKey());
        }
    }

    @Test void targetAreaCourseFilters() {
        assertEquals(List.of(1L), ids(new S("OPEN", null, null, null, null, null, "EXCHANGE", null, null, null, "deadline", 1, 50).q()));
        assertEquals(List.of(2L), ids(new S("OPEN", null, null, null, null, null, null, null, "BAEGOT", null, "deadline", 1, 50).q()));
        assertEquals(List.of(1L), ids(new S("OPEN", null, null, null, null, null, null, null, null, 1L, "deadline", 1, 50).q()));
        assertEquals(List.of(), ids(new S("OPEN", null, null, null, null, null, "없는값", null, null, null, "deadline", 1, 50).q()));
    }

    @Test void sortAndPaging() {
        assertEquals(List.of(1L, 2L), ids(S.open().q()));
        at(1, base.plusDays(9), "10:00:00");   // 1번을 뒤로
        assertEquals(List.of(2L, 1L), ids(S.open().q()));
        var recent = new S("OPEN", null, null, null, null, null, null, null, null, null, "recent", 1, 50);
        assertEquals(List.of(2L, 1L), ids(recent.q()));
        at(1, base, "10:00:00");
        var page2 = new S("OPEN", null, null, null, null, null, null, null, null, null, "deadline", 2, 1);
        var result = repo.findMatches(page2.q());
        assertEquals(List.of(2L), result.items().stream().map(m -> m.get("matchId")).toList());
        assertEquals(2, result.total());
        assertEquals(2, result.page());
        var page3 = new S("OPEN", null, null, null, null, null, null, null, null, null, "deadline", 3, 1);
        assertEquals(List.of(), ids(page3.q()));
    }

    @Test void listItemShape() {
        Map<String, Object> m = repo.findMatches(S.open().q()).items().get(0);
        assertEquals(List.of("matchId", "title", "meetAt", "course", "target", "styles", "languages", "current", "capacity", "status", "host"),
                List.copyOf(m.keySet()));
        assertEquals(List.of("KO", "ZH"), m.get("languages"));
        assertEquals(2, m.get("current"));
        assertEquals("민서", ((Map<?, ?>) m.get("host")).get("nickname"));
    }

    @Test void withdrawnPeopleShowAsWithdrawnEverywhere() {
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE user_id IN (2, 5)");
        assertEquals("탈퇴한 회원", ((Map<?, ?>) repo.findMatches(S.open().q()).items().get(0).get("host")).get("nickname"));
        Map<String, Object> d = repo.findMatchById(1L, null);
        assertEquals("탈퇴한 회원", ((Map<?, ?>) d.get("host")).get("nickname"));
        List<?> people = (List<?>) d.get("participants");
        assertTrue(people.stream().allMatch(p -> "탈퇴한 회원".equals(((Map<?, ?>) p).get("nickname"))));
        assertTrue(repo.findComments(1, 1, 50).items().stream().allMatch(c -> "탈퇴한 회원".equals(c.author().nickname())));
    }

    // ----- 내 동행 (P1) -----

    @Test void myMatchTabs() {
        assertEquals(List.of(1L, 3L), tab(2, "upcoming"));   // 민서: 1번 모집자, 3번 참여
        assertEquals(List.of(1L, 6L), tab(2, "hosting"));    // 진행 중인 글이 먼저, 끝난 글은 뒤로
        assertEquals(List.of(6L), tab(2, "past"));
    }

    @Test void myMatchesExcludeLeftKickedHiddenCanceled() {
        assertEquals(List.of(), tab(5, "past"));             // 동훈: 6번 참여 취소
        assertFalse(tab(6, "upcoming").contains(2L));        // 승민: 2번에서 내보내짐
        assertFalse(tab(5, "hosting").contains(5L));         // 동훈: 5번 모집 취소
        assertFalse(tab(5, "upcoming").contains(5L));
        jdbc.update("UPDATE matches SET is_hidden = TRUE WHERE match_id = 1");
        assertEquals(List.of(3L), tab(2, "upcoming"));
    }

    @Test void myMatchesRoleAndReviewed() {
        var rows = repo.findMyMatches(2, "upcoming", 1, 50).items();
        assertEquals("HOST", rows.get(0).get("role"));
        assertEquals("PARTICIPANT", rows.get(1).get("role"));
        assertFalse(rows.get(0).containsKey("reviewed"));

        assertEquals(true, repo.findMyMatches(2, "past", 1, 50).items().get(0).get("reviewed"));    // 민서는 썼음
        assertEquals(false, repo.findMyMatches(6, "past", 1, 50).items().get(0).get("reviewed"));   // 승민은 안 썼음
    }

    @Test void myMatchesCountRespectsPaging() {
        var page = repo.findMyMatches(2, "upcoming", 2, 1);
        assertEquals(2, page.total());
        assertEquals(1, page.items().size());
        assertEquals(3L, page.items().get(0).get("matchId"));
    }
}
