package kr.sanchaek.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PUT /api/users/me/password 비밀번호 변경 요청 (P5). 새 비밀번호 규칙은 회원가입과 같음 */
public record PasswordChangeRequest(

        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(max = 64, message = "비밀번호는 64자까지예요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "비밀번호는 8자 이상, 영문 · 숫자 · 특수문자를 모두 넣어 주세요.")
        String newPassword) {
}
