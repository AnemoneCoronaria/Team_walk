package kr.sanchaek.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.SessionUtil;

/** 로그인 · 로그아웃 (세션 방식, 응답에 토큰 없음). 담당: 정란 */
@RestController
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 로그인 (A1, 실패하면 A2)
    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        LoginResult user = userService.login(req);
        SessionUtil.login(request, user.userId(), user.role());
        return ApiResponse.ok(user);
    }

    // 로그아웃 (Header). 로그인 안 했으면 @LoginUser 가 401
    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(@LoginUser Long userId, HttpServletRequest request) {
        SessionUtil.logout(request);
        return ApiResponse.ok();
    }
}
