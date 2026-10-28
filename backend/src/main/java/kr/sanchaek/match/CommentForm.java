package kr.sanchaek.match;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 댓글 쓰기 { "content": "...", "isNotice": true } — isNotice 는 모집자만 (다른 사람은 false 로 저장) */
public record CommentForm(
        @NotBlank(message = "필수예요") @Size(max = 500, message = "500자 이내로 써 주세요") String content,
        @JsonProperty("isNotice") Boolean isNotice) {
}
