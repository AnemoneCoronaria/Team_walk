package kr.sanchaek.match;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.match.MatchDtos.CommentRequest;
import kr.sanchaek.user.UserRepository;

/** 3주차 댓글 · AD3 규칙. seed 기준이지만 날짜가 지나도 결과가 같은 것만 검사 (끝나면 되돌림) */
@SpringBootTest
@Transactional
class MatchCommentAdminTest {
    private static final long MINSEO = 2, JEONGRAN = 3, DONGEON = 4, DONGHUN = 5;

    @Autowired MatchService service;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    private static int statusOf(Runnable action) {
        return assertThrows(ApiException.class, action::run).getStatus();
    }

    @Test void participantsReadCommentsInOrder() {
        var page = service.comments(1, DONGHUN, 1, 50);   // 동훈은 1번 참여자
        assertEquals(3, page.total());
        assertTrue(page.items().get(0).isNotice());
        assertEquals("민서", page.items().get(0).author().nickname());
        assertFalse(page.items().get(1).isNotice());
        assertEquals(2, service.comments(1, MINSEO, 1, 2).items().size());
    }

    @Test void onlyHostAndJoinedCanUseComments() {
        assertEquals(403, statusOf(() -> service.comments(1, JEONGRAN, 1, 20)));                      // 참여 안 함
        assertEquals(403, statusOf(() -> service.comments(6, DONGHUN, 1, 20)));                       // 6번 참여 취소(LEFT)
        assertEquals(403, statusOf(() -> service.addComment(1, JEONGRAN, new CommentRequest("안녕하세요", false))));
        assertEquals(404, statusOf(() -> service.comments(99999, MINSEO, 1, 20)));
    }

    @Test void noticeIsHostOnlyAndContentIsTrimmed() {
        assertEquals(403, statusOf(() -> service.addComment(1, DONGHUN, new CommentRequest("공지예요", true))));

        long notice = service.addComment(1, MINSEO, new CommentRequest("  모임 장소 변경  ", true));
        long normal = service.addComment(1, DONGHUN, new CommentRequest("네 알겠어요", null));

        var items = service.comments(1, MINSEO, 1, 50).items();
        var saved = items.stream().filter(c -> c.commentId() == notice).findFirst().orElseThrow();
        assertEquals("모임 장소 변경", saved.content());
        assertTrue(saved.isNotice());
        assertFalse(items.stream().filter(c -> c.commentId() == normal).findFirst().orElseThrow().isNotice());
    }

    @Test void hidingMovesToHiddenTabAndDetailIs404() {
        long hiddenBefore = service.adminMatches(null, "OPEN", 1, 20).counts().hidden();

        assertTrue(service.changeHidden(1, true).hidden());
        var hidden = service.adminMatches(null, "HIDDEN", 1, 20);
        assertEquals(hiddenBefore + 1, hidden.counts().hidden());
        assertEquals(hidden.counts().hidden(), hidden.total());
        var row = hidden.items().stream().filter(m -> m.matchId() == 1).findFirst().orElseThrow();
        assertTrue(row.hidden());
        assertEquals("민서", row.host().nickname());
        assertEquals(404, statusOf(() -> service.detail(1, null)));
        assertEquals(404, statusOf(() -> service.comments(1, MINSEO, 1, 20)));

        assertFalse(service.changeHidden(1, false).hidden());
        assertEquals(1L, service.detail(1, null).get("matchId"));   // 복구하면 다시 보임
    }

    @Test void adminSearchLooksAtTitleHostAndCourse() {
        for (long id : new long[] {1, 2, 3}) {
            service.changeHidden(id, true);
        }
        assertEquals(1, service.adminMatches("퇴근", "HIDDEN", 1, 20).total());      // 제목
        assertEquals(1, service.adminMatches("정란", "HIDDEN", 1, 20).total());      // 모집자
        assertEquals(1, service.adminMatches("오이도 빨강", "HIDDEN", 1, 20).total()); // 코스
        assertEquals(0, service.adminMatches("없는말", "HIDDEN", 1, 20).total());
        assertEquals(0, service.adminMatches("없는말", "HIDDEN", 1, 20).counts().open());
    }

    @Test void adminRejectsBadInput() {
        assertEquals(400, statusOf(() -> service.adminMatches(null, "XX", 1, 20)));
        assertEquals(404, statusOf(() -> service.changeHidden(99999, true)));
    }

    @Test void hiddenMatchDoesNotBlockWithdrawal() {
        // 정란(3)은 2번 글의 모집자. 날짜가 지나지 않게 맞춘 뒤 숨기면, 취소할 수 없는 글이라 탈퇴를 막지 않음
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE host_id = ?", JEONGRAN);
        assertTrue(users.hasOpenMatch(JEONGRAN));
        service.changeHidden(2, true);
        assertFalse(users.hasOpenMatch(JEONGRAN));
    }

    private int pathSize(long matchId, Long viewer) {
        return ((java.util.List<?>) service.detail(matchId, viewer).get("path")).size();
    }

    @Test void coursePathFollowsCourseVisibility() {
        // 2번 글: 코스 3, 첨부 GPX 없음, 모집자 정란(3)이 코스 작성자
        assertTrue(pathSize(2, null) > 0);

        jdbc.update("UPDATE courses SET visibility = 'PRIVATE' WHERE course_id = 3");
        assertEquals(0, pathSize(2, null));
        assertEquals(0, pathSize(2, MINSEO));
        assertNull(service.detail(2, null).get("meetPoint"));
        assertTrue(pathSize(2, JEONGRAN) > 0);       // 코스 작성자

        // 동언(4)은 코스 3 으로 잡힌 동행에 참여한 적이 없음
        jdbc.update("UPDATE courses SET visibility = 'MATCH_ONLY' WHERE course_id = 3");
        assertEquals(0, pathSize(2, DONGEON));
        jdbc.update("INSERT INTO match_participants (match_id, user_id) VALUES (2, ?)", DONGEON);
        assertTrue(pathSize(2, DONGEON) > 0);        // 이 코스로 잡힌 동행의 참여자

        jdbc.update("UPDATE courses SET visibility = 'PUBLIC', is_hidden = TRUE WHERE course_id = 3");
        assertEquals(0, pathSize(2, null));
        jdbc.update("UPDATE courses SET is_hidden = FALSE, is_deleted = TRUE WHERE course_id = 3");
        assertEquals(0, pathSize(2, null));
    }

    @Test void attachedRouteIsShownEvenIfCourseIsPrivate() {
        // 1번 글은 모집자가 GPX 를 첨부함 → 코스가 비공개여도 이 동행의 경로는 보임
        jdbc.update("UPDATE courses SET visibility = 'PRIVATE' WHERE course_id = 1");
        assertTrue(pathSize(1, null) > 0);
        assertNotNull(service.detail(1, null).get("meetPoint"));
    }

    private static MatchRequest request(long courseId) {
        return new MatchRequest("제목만 고침", courseId, java.time.LocalDateTime.now().plusDays(7), 4, "정문", "ANYONE", null,
                null, null, java.util.List.of("KO"), null, null);
    }

    @Test void hostCanEditWhenUnchangedCourseBecamePrivate() {
        // 1번 글(모집자 민서)의 코스 1 은 동언 코스. 나중에 비공개가 돼도 코스를 안 바꾸면 수정 가능
        jdbc.update("UPDATE matches SET meet_at = NOW() + INTERVAL 7 DAY WHERE match_id = 1");
        jdbc.update("UPDATE courses SET visibility = 'PRIVATE' WHERE course_id = 1");
        service.update(1, MINSEO, request(1), null);
        assertEquals("제목만 고침", service.detail(1, MINSEO).get("title"));

        // 고를 수 없는 다른 코스로 바꾸는 것은 막음
        jdbc.update("UPDATE courses SET visibility = 'PRIVATE' WHERE course_id = 4");
        assertEquals(400, statusOf(() -> service.update(1, MINSEO, request(4), null)));
    }
}
