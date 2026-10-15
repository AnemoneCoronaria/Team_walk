package kr.sanchaek.common;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 비밀번호 저장 · 확인 (BCrypt).
 *
 * <pre>
 * String hash = PasswordUtil.hash("test1234!");      // 회원가입 · 비밀번호 변경 → users.password_hash
 * boolean ok = PasswordUtil.matches(입력값, hash);     // 로그인
 * </pre>
 *
 * 같은 비밀번호라도 매번 다른 해시가 나오는 게 정상입니다. 그래서 SQL 로 비교하지 말고 matches 로 확인하세요.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
    }

    public static boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
