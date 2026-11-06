package kr.sanchaek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @RequestParam · @PathVariable 은 이름을 직접 적어야 합니다. 예) @PathVariable("id"), @RequestParam(name = "q", required = false)
 * 이름을 안 적으면 VS Code 가 컴파일한 클래스로 돌 때 500 이 납니다. 빠뜨리면 이 테스트가 어느 메서드인지 알려 줍니다.
 */
class ControllerParamNameTest {

    @Test void everyRequestParamAndPathVariableHasExplicitName() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (var bean : scanner.findCandidateComponents("kr.sanchaek")) {
            for (Method method : Class.forName(bean.getBeanClassName()).getDeclaredMethods()) {
                for (Parameter p : method.getParameters()) {
                    RequestParam rp = p.getAnnotation(RequestParam.class);
                    PathVariable pv = p.getAnnotation(PathVariable.class);
                    if (rp == null && pv == null) {
                        continue;
                    }
                    checked++;
                    boolean named = rp != null ? !(rp.name().isEmpty() && rp.value().isEmpty()) : !(pv.name().isEmpty() && pv.value().isEmpty());
                    if (!named) {
                        missing.add(method.getDeclaringClass().getSimpleName() + "." + method.getName());
                    }
                }
            }
        }
        assertEquals(List.of(), missing, "이름을 안 적은 파라미터가 있어요");
        org.junit.jupiter.api.Assertions.assertTrue(checked > 30, "컨트롤러를 못 찾았어요");
    }
}
