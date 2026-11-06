package kr.sanchaek.match;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/matches (모집 글 쓰기) · PUT /api/matches/{id} (수정) 요청 (Figma W3).
 * GPX 파일을 같이 받으므로 JSON 이 아니라 multipart/form-data 의 칸으로 옵니다. (파일 칸 이름은 gpx)
 * 여러 개 고르는 값은 같은 이름을 여러 번 보내면 됩니다. 예) styles=SLOW, styles=CHAT
 * 규칙을 어기면 공통 처리기가 400 INVALID_INPUT 으로 돌려줌.
 */
public record MatchRequest(

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 30, message = "제목은 30자까지예요.")
        String title,

        @NotNull(message = "코스를 골라 주세요.")
        Long courseId,

        // 프론트가 날짜 · 시간 칸을 합쳐서 "2026-11-02T19:00:00" 으로 보냄
        @NotNull(message = "만나는 날짜와 시간을 입력해 주세요.")
        @Future(message = "만나는 시간은 지금 이후로 정해 주세요.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime meetAt,

        @NotNull(message = "모집 인원을 입력해 주세요.")
        @Min(value = 2, message = "모집 인원은 2~6명이에요.")
        @Max(value = 6, message = "모집 인원은 2~6명이에요.")
        Integer capacity,

        @NotBlank(message = "만나는 곳을 입력해 주세요.")
        @Size(max = 100, message = "만나는 곳은 100자까지예요.")
        String meetPlace,

        // 모집 대상 (여러 개). 비우면 "누구나". 참여할 때는 고른 조건을 모두 만족해야 함
        List<@Pattern(regexp = "^(EXCHANGE|SAME_LANGUAGE|MBTI)$", message = "모집 대상 값이 올바르지 않아요.") String> targets,

        // 모집 대상에 MBTI 가 있을 때만 씀 (그 외에는 보내도 저장하지 않음)
        @Pattern(regexp = "^[EIei][NSns][TFtf][JPjp]$", message = "MBTI 는 ENFP 같은 4글자예요.")
        String mbti,

        // 비우면 "무관"
        List<@Pattern(regexp = "^(20s|30s|40s|50s\\+)$", message = "연령대 값이 올바르지 않아요.") String> ageGroups,

        List<@Pattern(regexp = "^(SLOW|FAST|QUIET|CHAT|PHOTO|SUNRISE_SUNSET|DOG)$", message = "산책 스타일 값이 올바르지 않아요.") String> styles,

        @NotEmpty(message = "언어를 1개 이상 골라 주세요.")
        List<@Pattern(regexp = "^(KO|ZH|EN|VI|RU)$", message = "언어 값이 올바르지 않아요.") String> languages,

        // 소개글 (선택)
        @Size(max = 1000, message = "소개글은 1000자까지예요.")
        String description,

        // 수정할 때만: true 면 첨부했던 GPX 를 뗌 (새 파일을 같이 보내면 새 파일로 바뀜)
        Boolean removeGpx) {

    /** 앞뒤 공백 정리, 빈 값은 null, 중복 태그 제거, MBTI 는 대문자 (MBTI 대상이 아니면 null) */
    MatchRequest cleaned() {
        String text = description == null || description.isBlank() ? null : description.trim();
        List<String> cleanTargets = distinct(targets);
        String upperMbti = cleanTargets.contains("MBTI") && mbti != null ? mbti.toUpperCase(Locale.ROOT) : null;
        return new MatchRequest(title.trim(), courseId, meetAt.withNano(0), capacity, meetPlace.trim(), cleanTargets,
                upperMbti, distinct(ageGroups), distinct(styles), distinct(languages), text, removeGpx);
    }

    private static List<String> distinct(List<String> values) {
        return values == null ? List.of() : values.stream().distinct().toList();
    }
}
