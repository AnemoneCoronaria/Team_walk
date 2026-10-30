package kr.sanchaek.user;

/** POST /api/auth/login 성공 응답의 data */
public record LoginResult(long userId, String nickname, String avatar, String role) {
}
