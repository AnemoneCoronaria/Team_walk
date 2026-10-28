package kr.sanchaek.match;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 동행 목록 한 줄 (GET /api/matches, GET /api/users/me/matches)
 * reviewed 는 내 동행 past 탭에서만 나갑니다. (나머지는 칸 자체가 없음)
 */
public record MatchSummary(
        long matchId,
        String title,
        LocalDateTime meetAt,
        CourseBrief course,
        String target,
        List<String> styles,
        List<String> languages,
        int current,
        int capacity,
        String status,
        UserBrief host,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean reviewed) {
}
