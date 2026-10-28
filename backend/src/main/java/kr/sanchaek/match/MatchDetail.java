package kr.sanchaek.match;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 동행 상세 (GET /api/matches/{id}). W2 · W4 가 같이 씀.
 * meetPoint · path 는 DB의 JSON 을 그대로 내보냅니다 (@JsonRawValue).
 */
public record MatchDetail(
        long matchId,
        String title,
        String content,
        LocalDateTime meetAt,
        int durationMin,
        String meetPlace,
        @JsonRawValue String meetPoint,
        CourseBrief course,
        @JsonRawValue String path,
        boolean hasGpx,
        String target,
        List<String> ageGroups,
        List<String> styles,
        List<String> languages,
        String mbti,
        int capacity,
        String status,
        Host host,
        List<Participant> participants,
        @JsonProperty("isHost") boolean isHost,
        boolean joined) {

    /** 모집자. matchCount = 끝난(DONE) 동행에 참여한 수 */
    public record Host(long userId, String nickname, String avatar, int matchCount) {
    }
}
