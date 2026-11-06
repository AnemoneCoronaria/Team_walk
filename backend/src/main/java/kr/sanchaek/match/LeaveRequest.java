package kr.sanchaek.match;

import jakarta.validation.constraints.Size;

/**
 * DELETE /api/matches/{id}/join (참여 취소) 요청 (Figma W5). 본문은 보내지 않아도 됨.
 * reason 은 모집자에게 전달되는 취소 사유. 예) "일정이 생겼어요"
 */
public record LeaveRequest(
        @Size(max = 200, message = "취소 사유는 200자까지예요.")
        String reason) {
}
