package kr.sanchaek.common;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** {@link LoginUser} 가 붙은 파라미터에 세션의 userId 를 넣어 줍니다. (WebConfig 에서 등록) */
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Long userId = request == null ? null : SessionUtil.loginUserId(request);
        LoginUser annotation = parameter.getParameterAnnotation(LoginUser.class);
        if (userId == null && annotation != null && annotation.required()) {
            throw ApiException.loginRequired();
        }
        return userId;
    }
}
