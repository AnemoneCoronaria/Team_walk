package kr.sanchaek.user;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/users 회원가입 요청 (A3 계정 정보 + A4 프로필 · 태그를 프론트가 모아서 한 번에 보냄).
 * 규칙은 API 명세 "계정 > 입력 규칙". 어기면 공통 처리기가 400 INVALID_INPUT 으로 돌려줌.
 */
public record SignupRequest(

        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z0-9]{4,16}$", message = "아이디는 영문 · 숫자 4~16자예요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(max = 64, message = "비밀번호는 64자까지예요.")   // BCrypt 는 72바이트까지만 읽음
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "비밀번호는 8자 이상, 영문 · 숫자 · 특수문자를 모두 넣어 주세요.")
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자예요.")
        String nickname,

        // Boolean(대문자): 칸이 빠져도 JSON 읽기 오류 대신 아래 메시지가 나가게
        @NotNull(message = "이용약관에 동의해 주세요.")
        @AssertTrue(message = "이용약관에 동의해 주세요.")
        Boolean agreeTerms,

        @NotNull(message = "개인정보 수집에 동의해 주세요.")
        @AssertTrue(message = "개인정보 수집에 동의해 주세요.")
        Boolean agreePrivacy,

        // 선택. 비우면 동의 안 함
        Boolean agreeLocation,

        @NotNull(message = "연령대를 골라 주세요.")
        @Pattern(regexp = "^(20s|30s|40s|50s\\+)$", message = "연령대 값이 올바르지 않아요.")
        String ageGroup,

        // 비우면 기본 아바타(seagull)
        @Pattern(regexp = "^(seagull|lighthouse|lotus|reed|sogeumi|wave|dog|cat)$", message = "아바타 값이 올바르지 않아요.")
        String avatar,

        @Size(max = 3, message = "산책 스타일은 3개까지 고를 수 있어요.")
        List<@Pattern(regexp = "^(SLOW|FAST|QUIET|CHAT|PHOTO|SUNRISE_SUNSET|DOG)$", message = "산책 스타일 값이 올바르지 않아요.") String> styles,

        @NotEmpty(message = "언어를 1개 이상 골라 주세요.")
        List<@Pattern(regexp = "^(KO|ZH|EN|VI|RU)$", message = "언어 값이 올바르지 않아요.") String> languages,

        // 선택. ENFP 같은 4글자 (소문자로 와도 됨)
        @Pattern(regexp = "^[EIei][NSns][TFtf][JPjp]$", message = "MBTI 는 ENFP 같은 4글자예요.")
        String mbti) {
}
