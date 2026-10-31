package kr.sanchaek.common;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * 공통 설정 세 가지 (팀원은 고칠 일 없음)
 * 1. @LoginUser 파라미터 → 세션의 userId
 * 2. /api/admin/** → 관리자 검사 (AdminInterceptor)
 * 3. /files/** → 업로드 폴더의 파일 내려 주기 (예: /files/reviews/88-1.jpg).
 *    단 gpx/ 폴더는 비공개라 404 (비공개 코스 GPX 유출 방지). GPX 는 권한을 확인하는 /api/.../gpx 로만 받음
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
        registry.addResourceHandler("/files/**").addResourceLocations(location)
                .resourceChain(false)
                .addResolver(new PublicFileResolver());
    }

    /** 실제 파일 위치가 FileStore 의 비공개 폴더(gpx/) 안이면 없는 파일처럼 처리 → 404 */
    private final class PublicFileResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource == null || !resource.isFile()) {
                return resource;
            }
            return fileStore.isPrivate(resource.getFile().toPath()) ? null : resource;
        }
    }
}
