package kr.sanchaek.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /api/admin/** 는 관리자만. (WebConfig 에서 등록)
 * 로그인 안 했으면 401 LOGIN_REQUIRED, 로그인했지만 ADMIN 이 아니면 403 FORBIDDEN.
 * 관리자 API 컨트롤러(민서 AD3, 정란 AD1 · AD2 · AD4)는 따로 검사하지 않아도 됩니다.
 */
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (SessionUtil.loginUserId(request) == null) {
            throw ApiException.loginRequired();
        }
        if (!SessionUtil.isAdmin(request)) {
            throw ApiException.forbidden("관리자만 쓸 수 있어요.");
        }
        return true;
    }
}
