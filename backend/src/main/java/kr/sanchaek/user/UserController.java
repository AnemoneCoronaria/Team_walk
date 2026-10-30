package kr.sanchaek.user;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.SessionUtil;

/** 계정 API (1주차): 아이디 중복 확인, 회원가입, 내 정보. 담당: 정란 */
@RestController
public class UserController {

    private final UserService userService;
    private final UserRepository repo;

    public UserController(UserService userService, UserRepository repo) {
        this.userService = userService;
        this.repo = repo;
    }

    // 아이디 중복 확인 (A3 "중복 확인" 버튼)
    @GetMapping("/api/users/check-id")
    public ApiResponse<Map<String, Boolean>> checkId(@RequestParam("loginId") String loginId) {
        if (!loginId.trim().matches("^[A-Za-z0-9]{4,16}$")) {
            throw ApiException.badRequest("아이디는 영문 · 숫자 4~16자예요.");
        }
        return ApiResponse.ok(Map.of("available", userService.isLoginIdAvailable(loginId)));
    }

    // 회원가입 (A3 + A4 + A5)
    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> signup(@Valid @RequestBody SignupRequest req) {
        long userId = userService.signup(req);
        return ApiResponse.ok(Map.of("userId", userId));
    }

    // 내 정보 (Header 로그인 상태, P1, P2)
    @GetMapping("/api/users/me")
    public ApiResponse<MyInfo> me(@LoginUser Long userId, HttpServletRequest request) {
        MyInfo info = repo.findMyInfo(userId);
        if (info == null) {
            // 세션은 있는데 회원이 없거나 탈퇴한 경우 → 세션을 끝내고 로그인 안 한 것으로
            SessionUtil.logout(request);
            throw ApiException.loginRequired();
        }
        return ApiResponse.ok(info);
    }
}
