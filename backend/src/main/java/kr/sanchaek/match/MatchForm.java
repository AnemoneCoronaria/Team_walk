package kr.sanchaek.match;

import java.time.LocalDateTime;
import java.util.List;

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
 * 모집 글 작성(multipart) · 수정(JSON)에 같이 쓰는 입력값. 규칙은 API 명세 "POST /api/matches".
 * 목록 값(ageGroups 등)은 multipart 에서는 "20s,30s", JSON 에서는 ["20s", "30s"].
 * 코드 값(target, styles …)이 맞는지는 MatchService 에서 한 번 더 검사합니다.
 */
public record MatchForm(
        @NotBlank(message = "필수예요") @Size(max = 30, message = "30자 이내로 써 주세요") String title,
        @Size(max = 2000, message = "2000자 이내로 써 주세요") String content,
        @NotNull(message = "필수예요") Long courseId,
        @NotNull(message = "필수예요") @Future(message = "지금보다 뒤의 시간이어야 해요")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime meetAt,
        @NotBlank(message = "필수예요") @Size(max = 100, message = "100자 이내로 써 주세요") String meetPlace,
        @NotNull(message = "필수예요") @Min(value = 2, message = "2~6명이에요") @Max(value = 6, message = "2~6명이에요") Integer capacity,
        @NotBlank(message = "필수예요") String target,
        List<String> ageGroups,
        List<String> styles,
        @NotEmpty(message = "1개 이상 골라 주세요") List<String> languages,
        @Pattern(regexp = "^[EIei][SNsn][TFtf][JPjp]$", message = "ENFP 같은 4글자예요") String mbti) {
}
