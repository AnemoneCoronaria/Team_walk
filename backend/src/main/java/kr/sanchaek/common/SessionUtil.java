package kr.sanchaek.common;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 로그인 세션. 명세의 "로그인 필요" API는 서블릿 맨 앞에서 이렇게 확인합니다.
 *
 * <pre>
 * Long userId = SessionUtil.requireLogin(req, resp);
 * if (userId == null) return;   // 401 LOGIN_REQUIRED 는 이미 보냄
 * </pre>
 */
public final class SessionUtil {

    public static final String USER_ID = "userId";
    public static final String ROLE = "role";

    private SessionUtil() {
    }

    /** 로그인 성공 시 (정란: 로그인 API) */
    public static void login(HttpServletRequest req, long userId, String role) {
        HttpSession session = req.getSession(true);
        req.changeSessionId(); // 로그인 전 세션 ID 재사용 방지
        session.setAttribute(USER_ID, userId);
        session.setAttribute(ROLE, role);
    }

    /** 로그아웃 · 탈퇴 시 */
    public static void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** 로그인한 사용자 번호. 로그인 안 했으면 null (로그인 선택 API에서) */
    public static Long loginUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (Long) session.getAttribute(USER_ID);
    }

    public static boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && "ADMIN".equals(session.getAttribute(ROLE));
    }

    /** 로그인 필수 API. 로그인 안 했으면 401 을 보내고 null */
    public static Long requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = loginUserId(req);
        if (userId == null) {
            ApiResponse.fail(resp, 401, "LOGIN_REQUIRED", "로그인이 필요해요.");
        }
        return userId;
    }
}
