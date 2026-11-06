package kr.sanchaek.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * POST /api/reports 신고 요청 (W6 동행 글 · 사용자 · 후기, C2-1 코스).
 * 사유가 대상에 맞는지(코스 사유 ↔ 그 외 사유)는 {@link ReportService} 에서 검사.
 */
public record ReportRequest(

        @NotNull(message = "신고 대상을 골라 주세요.")
        @Pattern(regexp = "^(MATCH|USER|COURSE|REVIEW)$", message = "신고 대상 값이 올바르지 않아요.")
        String targetType,

        @NotNull(message = "신고 대상 번호를 보내 주세요.")
        @Positive(message = "신고 대상 번호가 올바르지 않아요.")
        Long targetId,

        @NotBlank(message = "신고 사유를 골라 주세요.")
        String reason,

        @Size(max = 300, message = "자세한 내용은 300자까지예요.")
        String detail) {
}
