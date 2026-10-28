package kr.sanchaek.common;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발용 로그인 — 정란의 로그인 API가 나오기 전에 "로그인한 상태"로 API를 시험해 보려고 만든 것.
 * application-local.properties 에 app.dev-login=true 를 넣었을 때만 켜집니다. (기본은 꺼짐)
 *
 * 브라우저 주소창에서:
 *   localhost:8080/api/dev/login?userId=2   → seed 의 minseo 로 로그인 (1 admin, 2 minseo, 3 jeongran, 4 dongeon, 5 donghun, 6 seungmin)
 *   localhost:8080/api/dev/logout
 * 같은 브라우저 탭에서 다른 API 주소를 열면 로그인한 상태로 불립니다.
 */
@RestController
@ConditionalOnProperty(prefix = "app", name = "dev-login", havingValue = "true")
public class DevLoginController {

    private final JdbcTemplate jdbc;

    public DevLoginController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/dev/login")
    public ApiResponse<Map<String, Object>> login(@RequestParam long userId, HttpServletRequest request) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT user_id, nickname, role FROM users WHERE user_id = ?", userId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("그 번호의 회원이 없어요.");
        }
        Map<String, Object> user = rows.get(0);
        SessionUtil.login(request, userId, (String) user.get("role"));
        return ApiResponse.ok(Map.of("userId", userId, "nickname", user.get("nickname"), "role", user.get("role")));
    }

    @GetMapping("/api/dev/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        SessionUtil.logout(request);
        return ApiResponse.ok();
    }
}
