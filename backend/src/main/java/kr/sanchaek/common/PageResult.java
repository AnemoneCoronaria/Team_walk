package kr.sanchaek.common;

import java.util.List;

/**
 * 목록 API의 data 모양 (API 명세 "공통 규칙").
 * { "items": [ ... ], "page": 1, "size": 20, "total": 57 }
 *
 * <pre>
 * &#64;GetMapping("/api/matches")
 * public ApiResponse&lt;PageResult&lt;MatchSummary&gt;&gt; list(
 *         &#64;RequestParam(defaultValue = "1") int page,
 *         &#64;RequestParam(defaultValue = "20") int size) {
 *     List&lt;MatchSummary&gt; items = repo.findPage(PageResult.offset(page, size), size);  // SQL: LIMIT ? OFFSET ?
 *     long total = repo.count();
 *     return ApiResponse.ok(new PageResult&lt;&gt;(items, page, size, total));
 * }
 * </pre>
 */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    /** page 는 1 이상, size 는 1~100, OFFSET 이 int 를 넘지 않아야 함. 아니면 400 INVALID_INPUT */
    public static void validate(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw ApiException.badRequest("페이지 값을 확인해 주세요.");
        }
    }

    /** SQL 의 OFFSET 값. page 는 1부터 시작 */
    public static int offset(int page, int size) {
        return (Math.max(page, 1) - 1) * size;
    }
}
