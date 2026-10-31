package kr.sanchaek.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 오류를 { "success": false, "error": { code, message } } JSON 으로 바꿔 줍니다.
 * 이게 없으면 스프링이 HTML 오류 화면이나 다른 모양의 JSON 을 보내서, React 쪽 처리가 제각각이 됩니다.
 *
 * 각자 컨트롤러에서는 아무것도 안 해도 됩니다. 실패는 {@code throw ApiException.xxx(...)} 만.
 *
 * | 상황                                         | 응답 |
 * | ApiException                                 | 던진 그대로 |
 * | @Valid 검사 실패, JSON 형식 오류, 파라미터 누락 · 타입 틀림, multipart 아님 | 400 INVALID_INPUT |
 * | 업로드 용량 초과 (application.properties 5MB / 30MB)       | 400 TOO_LARGE |
 * | 없는 주소                                    | 404 NOT_FOUND |
 * | GET 만 만든 곳에 POST 등                      | 405 METHOD_NOT_ALLOWED |
 * | 그 밖의 예외 (SQL 오류, NullPointerException …) | 500 SERVER_ERROR (자세한 내용은 서버 로그에만) |
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException e) {
        return fail(e.getStatus(), e.getCode(), e.getMessage());
    }

    /** @Valid 를 붙인 요청 본문(record)의 검사 실패 → 첫 번째 메시지를 보여 줌 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
        FieldError field = e.getBindingResult().getFieldError();
        String message = field != null
                ? field.getField() + ": " + field.getDefaultMessage()
                : "요청 값을 확인해 주세요.";
        return fail(400, "INVALID_INPUT", message);
    }

    @ExceptionHandler({
            HandlerMethodValidationException.class,     // @RequestParam @Size 등 검사 실패
            HttpMessageNotReadableException.class,      // JSON 형식 오류, 날짜 형식 틀림
            HttpMediaTypeNotSupportedException.class,   // JSON 이어야 하는데 다른 형식
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,   // multipart 파일 칸 누락
            MultipartException.class,                   // 파일 칸이 필요한데 multipart 요청이 아님 (용량 초과는 아래 TOO_LARGE)
            MethodArgumentTypeMismatchException.class   // /api/matches/abc 처럼 숫자 자리에 글자
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        return fail(400, "INVALID_INPUT", "요청 값을 확인해 주세요.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException e) {
        return fail(400, "TOO_LARGE", "파일 용량이 너무 커요. (한 장 5MB, 합계 30MB까지)");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return fail(404, "NOT_FOUND", "요청한 주소가 없어요.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return fail(405, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식이에요.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        // 스프링이 상태 코드를 정해 둔 오류 (406 등) 는 그 코드를 그대로
        if (e instanceof ErrorResponse er && er.getStatusCode().value() < 500) {
            return fail(er.getStatusCode().value(), "INVALID_INPUT", "요청을 처리하지 못했어요.");
        }
        log.error("API 오류", e);   // VS Code 터미널(서버 로그)에 자세한 원인이 찍힘
        return fail(500, "SERVER_ERROR", "서버에 오류가 생겼어요. 잠시 후 다시 시도해 주세요.");
    }

    private static ResponseEntity<ApiResponse<Void>> fail(int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(code, message));
    }
}
