package kr.sanchaek.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 로그인 세션 다루기. 세션에는 userId 와 role(USER · ADMIN) 두 가지만 넣습니다.
 *
 * <pre>
 * // 로그인 API (정란) — 아이디 · 비밀번호 확인이 끝난 뒤
 * SessionUtil.login(request, user.userId(), user.role());
 *
 * // 로그아웃 · 탈퇴 API (정란)
 * SessionUtil.logout(request);
 * </pre>
 *
 * 다른 API에서 "로그인한 사람"이 필요하면 이 클래스를 직접 쓰지 말고
 * 컨트롤러 파라미터에 {@code @LoginUser Long userId} 를 붙이세요. ({@link LoginUser})
 */
public final class SessionUtil {

    public static final String USER_ID = "userId";
    public static final String ROLE = "role";

    private SessionUtil() {
    }

    /** 로그인 성공 시. 로그인 전 세션 ID 를 새로 바꿔서(세션 고정 공격 방지) userId · role 저장 */
    public static void login(HttpServletRequest request, long userId, String role) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(USER_ID, userId);
        session.setAttribute(ROLE, role);
    }

    /** 로그아웃 · 탈퇴 시 세션 끝내기 */
    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** 로그인한 사용자 번호. 로그인 안 했으면 null */
    public static Long loginUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Long) session.getAttribute(USER_ID);
    }

    /** 세션의 role 이 ADMIN 인지 */
    public static boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && "ADMIN".equals(session.getAttribute(ROLE));
    }
}
