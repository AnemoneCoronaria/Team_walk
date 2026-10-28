package kr.sanchaek.match;

import jakarta.validation.constraints.Size;

/** 참여 취소 { "reason": "일정이 생겼어요" } (선택) */
public record LeaveForm(@Size(max = 200, message = "200자 이내로 써 주세요") String reason) {
}
