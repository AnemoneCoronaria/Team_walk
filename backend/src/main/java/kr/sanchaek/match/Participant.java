package kr.sanchaek.match;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 동행 상세의 참여자 한 명 */
public record Participant(
        long userId,
        String nickname,
        String avatar,
        List<String> languages,
        @JsonProperty("isHost") boolean isHost) {
}
