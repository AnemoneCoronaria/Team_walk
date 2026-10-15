package kr.sanchaek.common;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * /api/* 의 오류를 항상 JSON 으로 바꿔 줍니다.
 *
 * Tomcat 은 원래 오류가 나면 HTML 오류 페이지(자바 오류 내용 포함)를 보냅니다.
 * 그러면 React 의 res.json() 이 "Unexpected token '<'" 로 깨지기 때문에, /api 아래는 전부 JSON 으로 통일합니다.
 * - 없는 주소 → 404 NOT_FOUND
 * - 안 만든 메서드 (GET만 만든 곳에 POST) → 405 METHOD_NOT_ALLOWED
 * - 서블릿에서 예외 → 500 SERVER_ERROR (자세한 오류는 이클립스 Console 에만 출력)
 * - 업로드 용량 초과 → 400 TOO_LARGE
 *
 * 각 서블릿은 아무것도 안 해도 됩니다. /api 가 아닌 주소(관리자 JSP 등)는 건드리지 않습니다.
 */
@WebFilter("/api/*")
public class ApiFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        try {
            chain.doFilter(req, new JsonErrorResponse(resp));
        } catch (Exception e) {
            boolean tooLarge = isSizeLimit(e);
            if (!tooLarge) {
                req.getServletContext().log("API 오류: " + req.getMethod() + " " + req.getRequestURI(), e);
            }
            if (resp.isCommitted()) {
                return;
            }
            resp.reset();
            if (tooLarge) {
                ApiResponse.fail(resp, 400, "TOO_LARGE", "파일 용량이 너무 커요.");
            } else {
                ApiResponse.fail(resp, 500, "SERVER_ERROR", "서버에 오류가 생겼어요. 잠시 후 다시 시도해 주세요.");
            }
        }
    }

    /** @MultipartConfig 의 용량 제한을 넘으면 Tomcat 이 IllegalStateException 으로 알려 줌 */
    private static boolean isSizeLimit(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getClass().getName().contains("SizeLimitExceeded")) {
                return true;
            }
        }
        return false;
    }

    /** resp.sendError(…) 를 HTML 대신 JSON 으로 */
    private static class JsonErrorResponse extends HttpServletResponseWrapper {

        JsonErrorResponse(HttpServletResponse resp) {
            super(resp);
        }

        @Override
        public void sendError(int sc) throws IOException {
            sendError(sc, null);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            if (isCommitted()) {
                super.sendError(sc, msg);
                return;
            }
            resetBuffer();
            HttpServletResponse resp = (HttpServletResponse) getResponse();
            switch (sc) {
                case 400 -> ApiResponse.fail(resp, sc, "INVALID_INPUT", "요청 값을 확인해 주세요.");
                case 401 -> ApiResponse.fail(resp, sc, "LOGIN_REQUIRED", "로그인이 필요해요.");
                case 403 -> ApiResponse.fail(resp, sc, "FORBIDDEN", "권한이 없어요.");
                case 404 -> ApiResponse.fail(resp, sc, "NOT_FOUND", "요청한 API가 없어요.");
                case 405 -> ApiResponse.fail(resp, sc, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식이에요.");
                default -> ApiResponse.fail(resp, sc, sc >= 500 ? "SERVER_ERROR" : "INVALID_INPUT",
                        "요청을 처리하지 못했어요.");
            }
            flushBuffer();
        }
    }
}
