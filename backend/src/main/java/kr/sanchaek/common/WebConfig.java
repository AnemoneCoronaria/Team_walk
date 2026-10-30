package kr.sanchaek.common;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 공통 설정 세 가지 (팀원은 고칠 일 없음)
 * 1. @LoginUser 파라미터 → 세션의 userId
 * 2. /api/admin/** → 관리자 검사 (AdminInterceptor)
 * 3. /files/** → 업로드 폴더의 파일 내려 주기 (예: /files/reviews/88-1.jpg)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStore fileStore;

    public WebConfig(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminInterceptor()).addPathPatterns("/api/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStore.baseDir().toUri().toString();   // file:///C:/dev/jspTeam/uploads/
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
