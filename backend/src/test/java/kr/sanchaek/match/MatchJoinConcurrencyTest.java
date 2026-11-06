package kr.sanchaek.match;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import kr.sanchaek.common.ApiException;

/**
 * 동시 참여: 트랜잭션이 실제로 커밋돼야 잠금을 시험할 수 있어서 @Transactional 을 쓰지 않습니다.
 * 테스트 글을 만들고 finally 에서 지우므로 시드 데이터에는 남지 않습니다.
 */
@SpringBootTest
class MatchJoinConcurrencyTest {
    @Autowired MatchService service;
    @Autowired JdbcTemplate jdbc;

    private int joinAtOnce(long matchId, List<Long> users) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(users.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> results = users.stream().map(u -> pool.submit(() -> {
                start.await();
                try {
                    service.join(matchId, u);
                    return "OK";
                } catch (ApiException e) {
                    return e.getCode();
                }
            })).toList();
            start.countDown();
            int ok = 0;
            for (Future<String> r : results) {
                String code = r.get();
                if ("OK".equals(code)) {
                    ok++;
                } else {
                    assertEquals("FULL", code);
                }
            }
            return ok;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test void onlyRemainingSeatsAreTaken() throws Exception {
        for (int capacity : new int[] {2, 3, 4}) {
            MatchRequest form = new MatchRequest("동시 참여 시험", 1L, LocalDateTime.now().plusDays(3), capacity, "정문", "ANYONE", null,
                    null, null, List.of("KO"), null, null);
            long id = service.create(1, form, null);    // 모집자는 관리자, 남은 자리 = capacity - 1
            try {
                int ok = joinAtOnce(id, List.of(2L, 3L, 4L, 5L, 6L));
                assertEquals(capacity - 1, ok, "정원 " + capacity);
                assertEquals(capacity, jdbc.queryForObject(
                        "SELECT current_count FROM v_match_status WHERE match_id = ?", Integer.class, id));
                assertEquals("FULL", jdbc.queryForObject(
                        "SELECT status FROM v_match_status WHERE match_id = ?", String.class, id));
            } finally {
                jdbc.update("DELETE FROM matches WHERE match_id = ?", id);
            }
        }
    }
}
