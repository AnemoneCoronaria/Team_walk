package kr.sanchaek.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ★ 환경 확인용 테스트 API — 지우지 마세요. (frontend/src/App.jsx 가 부릅니다)
 * GET /api/hello → 서버가 켜졌는지, DB(sanchaek)에 접속되는지 한 번에 확인합니다.
 * 응답 형식은 API 명세 공통 규칙 { "success": true, "data": { ... } } 을 따릅니다. (ApiResponse.ok 사용)
 */
@RestController
public class HelloController {

    private final JdbcTemplate jdbc;

    public HelloController(JdbcTemplate jdbc) {   // 스프링이 알아서 넣어 줍니다
        this.jdbc = jdbc;
    }

    @GetMapping("/api/hello")
    public ApiResponse<Map<String, Object>> hello() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "안녕하세요, 산책시루 서버입니다");
        data.put("serverTime", LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        data.put("javaVersion", System.getProperty("java.version"));
        // seed.sql 을 넣었으면 8 이 나옵니다. 테이블이 없으면 500 → DB 세팅부터 확인
        data.put("landmarks", jdbc.queryForObject("SELECT COUNT(*) FROM landmarks", Integer.class));

        return ApiResponse.ok(data);
    }
}
