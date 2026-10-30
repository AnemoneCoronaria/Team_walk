package kr.sanchaek.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API의 응답 모양 (API 명세 "공통 규칙").
 *
 * <pre>
 * return ApiResponse.ok(match);     // { "success": true, "data": { ... } }
 * return ApiResponse.ok();          // { "success": true }   (로그아웃, 삭제 등 돌려줄 값이 없을 때)
 * </pre>
 *
 * 실패는 이 클래스를 직접 만들지 말고 {@code throw new ApiException(...)} 을 던지세요.
 * 그러면 {@link ApiExceptionHandler} 가 { "success": false, "error": { code, message } } 로 바꿔 줍니다.
 *
 * 201(새로 만들기 성공)은 컨트롤러 메서드에 {@code @ResponseStatus(HttpStatus.CREATED)} 를 붙입니다.
 * data 안의 null 값은 "mbti": null 처럼 그대로 나갑니다. (빠지는 건 비어 있는 data · error 칸뿐)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {

    /** 실패 내용. code 는 명세의 에러 코드 (INVALID_INPUT, NOT_FOUND …) */
    public record ErrorDetail(String code, String message) {
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /** 공통 처리기용. 컨트롤러에서는 ApiException 을 던지세요 */
    static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }
}
