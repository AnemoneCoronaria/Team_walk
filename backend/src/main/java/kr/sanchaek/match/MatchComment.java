package kr.sanchaek.match;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 동행 댓글 한 줄 */
public record MatchComment(
        long commentId,
        UserBrief author,
        String content,
        @JsonProperty("isNotice") boolean isNotice,
        LocalDateTime createdAt) {
}
