package kr.sanchaek.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * /api/** 요청마다 세션의 회원이 아직 쓸 수 있는지 확인합니다. (WebConfig 에서 AdminInterceptor 보다 먼저 등록)
 * 로그인한 뒤에 탈퇴했거나 관리자에게 정지된 회원은 세션을 끝내서, 그 뒤로는 로그인 안 한 것으로 처리됩니다.
 * 다른 기기에 남은 세션으로 글 · 참여 · 댓글을 쓰지 못하게 하려는 것. 정지 기간이 지났으면 그대로 통과합니다.
 */
public class SessionCheckInterceptor implements HandlerInterceptor {

    private final JdbcTemplate jdbc;

    public SessionCheckInterceptor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = SessionUtil.loginUserId(request);
        if (userId != null && !isUsable(userId)) {
            SessionUtil.logout(request);
        }
        return true;
    }

    private boolean isUsable(long userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM users
            WHERE user_id = ? AND status <> 'WITHDRAWN'
              AND NOT (status = 'SUSPENDED' AND (suspended_until IS NULL OR suspended_until > NOW()))
            """, Integer.class, userId);
        return count != null && count > 0;
    }
}
