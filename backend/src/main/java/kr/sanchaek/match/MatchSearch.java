package kr.sanchaek.match;

import java.time.LocalDate;
import java.util.List;

/**
 * 동행 목록 검색 조건 (GET /api/matches 의 쿼리 파라미터).
 * 값이 null 이거나 비어 있으면 그 조건은 쓰지 않음.
 */
public record MatchSearch(
        String status,          // OPEN(기본) · FULL · CLOSED · CANCELED · DONE
        LocalDate from,         // 만나는 날짜 범위
        LocalDate to,
        String timeSlot,        // MORNING · AFTERNOON · EVENING · NIGHT
        List<String> styles,    // 하나라도 겹치면 통과
        List<String> languages,
        String target,
        String age,
        String area,
        Long courseId,
        String sort,            // deadline(기본) · recent
        int page,
        int size) {
}
