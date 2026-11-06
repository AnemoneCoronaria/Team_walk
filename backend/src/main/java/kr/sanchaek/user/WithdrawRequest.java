package kr.sanchaek.user;

import jakarta.validation.constraints.NotBlank;

/** DELETE /api/users/me 회원 탈퇴 요청 (P5-1). 본인 확인용 비밀번호 */
public record WithdrawRequest(
        @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {
}
