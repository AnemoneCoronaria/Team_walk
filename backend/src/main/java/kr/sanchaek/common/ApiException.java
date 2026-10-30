package kr.sanchaek.common;

/**
 * 실패 응답을 보내고 싶을 때 던지는 예외.
 * 던지면 {@link ApiExceptionHandler} 가 { "success": false, "error": { code, message } } 로 바꿔서 보냅니다.
 *
 * <pre>
 * throw ApiException.notFound("모집 글을 찾을 수 없어요.");          // 404 NOT_FOUND
 * throw ApiException.forbidden("모집자만 수정할 수 있어요.");        // 403 FORBIDDEN
 * throw ApiException.badRequest("정원은 2~6명이에요.");              // 400 INVALID_INPUT
 * throw ApiException.conflict("FULL", "정원이 다 찼어요.");          // 409 FULL
 * throw new ApiException(401, "LOGIN_FAILED", "아이디 또는 비밀번호가 맞지 않아요.");  // 그 밖의 코드
 * </pre>
 *
 * 상태 코드 · 에러 코드는 API 명세 "공통 규칙"의 표를 따릅니다.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    /** 400 INVALID_INPUT — 필수 값 누락, 형식 오류 */
    public static ApiException badRequest(String message) {
        return new ApiException(400, "INVALID_INPUT", message);
    }

    /** 401 LOGIN_REQUIRED — 로그인 안 함 (보통은 @LoginUser 가 알아서 보냄) */
    public static ApiException loginRequired() {
        return new ApiException(401, "LOGIN_REQUIRED", "로그인이 필요해요.");
    }

    /** 403 FORBIDDEN — 남의 글 수정, 관리자 아님 */
    public static ApiException forbidden(String message) {
        return new ApiException(403, "FORBIDDEN", message);
    }

    /** 404 NOT_FOUND — 대상 없음 */
    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }

    /** 409 — code 는 DUPLICATE(중복), FULL(정원 초과), CLOSED(마감) 등 */
    public static ApiException conflict(String code, String message) {
        return new ApiException(409, code, message);
    }
}
