package kr.sanchaek.match;

import java.util.List;

/**
 * 쓰기 규칙(권한 · 상태 · 참여 조건)을 검사할 때 쓰는 모집 글 정보. 화면에는 보내지 않음.
 * status 와 current 는 v_match_status 뷰의 계산 값 (OPEN · FULL · CLOSED · CANCELED · DONE)
 */
record MatchState(
        long matchId,
        long hostId,
        long courseId,
        int capacity,
        List<String> targets,   // 모집 대상 (EXCHANGE · SAME_LANGUAGE · MBTI), 비어 있으면 누구나
        String mbti,
        String gpxPath,
        int current,
        String status,
        List<String> ageGroups,
        List<String> languages) {
}
