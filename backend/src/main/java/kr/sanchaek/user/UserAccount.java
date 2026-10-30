package kr.sanchaek.user;

import java.time.LocalDateTime;

/** 로그인 확인용 users 한 줄 (응답으로 내보내지 않음. password_hash 가 들어 있음) */
public record UserAccount(
        long userId,
        String passwordHash,
        String nickname,
        String avatar,
        String role,
        String status,
        LocalDateTime suspendedUntil) {
}
