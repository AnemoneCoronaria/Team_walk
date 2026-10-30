package kr.sanchaek.user;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/users/me 응답의 data (Header, P1, P2).
 * matchCount · courseCount · totalDistanceKm 은 저장하지 않고 조회할 때 계산합니다.
 */
public record MyInfo(
        long userId,
        String loginId,
        String nickname,
        String avatar,
        String ageGroup,
        List<String> styles,
        List<String> languages,
        String mbti,
        boolean agreeLocation,
        String role,
        int matchCount,          // 함께 걸은 동행 (끝난 동행에 참여한 수)
        int courseCount,         // 올린 코스 수
        double totalDistanceKm,  // 끝난 동행 코스 거리의 합
        LocalDateTime createdAt) {
}
