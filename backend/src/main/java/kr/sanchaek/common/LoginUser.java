package kr.sanchaek.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 로그인한 사용자 번호(users.user_id)가 들어옵니다.
 *
 * <pre>
 * // 로그인 "필요" API — 로그인 안 했으면 메서드에 들어오기 전에 401 LOGIN_REQUIRED
 * &#64;PostMapping("/api/matches/{id}/join")
 * public ApiResponse&lt;Void&gt; join(&#64;PathVariable long id, &#64;LoginUser Long userId) { ... }
 *
 * // 로그인 "선택" API — 안 했으면 userId 가 null (예: 랜드마크 목록의 "내 스탬프 여부")
 * &#64;GetMapping("/api/landmarks")
 * public ApiResponse&lt;List&lt;Landmark&gt;&gt; list(&#64;LoginUser(required = false) Long userId) { ... }
 * </pre>
 *
 * 관리자 API(/api/admin/...)는 {@link AdminInterceptor} 가 따로 막으므로 이걸로 검사하지 않아도 됩니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

    /** false 면 로그인 안 했을 때 401 대신 null */
    boolean required() default true;
}
