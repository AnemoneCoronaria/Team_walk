package kr.sanchaek.user;

import java.util.List;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/users/me 프로필 · 태그 수정 요청 (P2). 바꿀 값만 보내면 되고, 안 보낸 칸(null)은 그대로 둡니다.
 * 아이디 · 비밀번호는 여기서 못 바꿈. 규칙은 회원가입과 같음 ({@link SignupRequest}).
 */
public record ProfileUpdateRequest(

        @Size(min = 2, max = 10, message = "닉네임은 2~10자예요.")
        String nickname,

        @Pattern(regexp = "^(seagull|lighthouse|lotus|reed|sogeumi|wave|dog|cat)$", message = "아바타 값이 올바르지 않아요.")
        String avatar,

        @Pattern(regexp = "^(20s|30s|40s|50s\\+)$", message = "연령대 값이 올바르지 않아요.")
        String ageGroup,

        // 빈 배열 [] 이면 스타일을 모두 지움
        @Size(max = 3, message = "산책 스타일은 3개까지 고를 수 있어요.")
        List<@Pattern(regexp = "^(SLOW|FAST|QUIET|CHAT|PHOTO|SUNRISE_SUNSET|DOG)$", message = "산책 스타일 값이 올바르지 않아요.") String> styles,

        // 보낼 때는 1개 이상
        @Size(min = 1, message = "언어를 1개 이상 골라 주세요.")
        List<@Pattern(regexp = "^(KO|ZH|EN|VI|RU)$", message = "언어 값이 올바르지 않아요.") String> languages,

        // 빈 문자열 "" 이면 MBTI 를 지움 (선택 안 함)
        @Pattern(regexp = "^([EIei][NSns][TFtf][JPjp])?$", message = "MBTI 는 ENFP 같은 4글자예요.")
        String mbti) {
}
