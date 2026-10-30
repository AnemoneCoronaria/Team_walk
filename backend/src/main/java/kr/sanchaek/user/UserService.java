package kr.sanchaek.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import kr.sanchaek.common.ApiException;

/**
 * 회원가입 · 로그인 규칙 (API 명세 "계정"). 담당: 정란
 * 비밀번호는 BCrypt 로 암호화해서 저장하고, 비교도 BCrypt 로 합니다 (SQL 로 비교하지 않음).
 */
@Service
public class UserService {

    private final UserRepository repo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    /** 아이디는 대소문자 구분 없이 소문자로 저장 · 비교 */
    static String normalizeLoginId(String loginId) {
        return loginId == null ? null : loginId.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isLoginIdAvailable(String loginId) {
        return !repo.existsLoginId(normalizeLoginId(loginId));
    }

    /** 회원가입. 만들어진 userId 를 돌려줌 */
    public long signup(SignupRequest req) {
        String loginId = normalizeLoginId(req.loginId());
        if (repo.existsLoginId(loginId)) {
            throw ApiException.conflict("DUPLICATE", "이미 사용 중인 아이디예요.");
        }
        String nickname = req.nickname().trim();
        if (nickname.length() < 2) {
            throw ApiException.badRequest("닉네임은 2~10자예요.");
        }
        String avatar = req.avatar() == null ? "seagull" : req.avatar();
        String mbti = req.mbti() == null ? null : req.mbti().toUpperCase(Locale.ROOT);
        // 같은 값이 두 번 와도 한 번만 저장
        List<String> styles = req.styles() == null ? List.of() : req.styles().stream().distinct().toList();
        List<String> languages = req.languages().stream().distinct().toList();

        try {
            return repo.insertUser(loginId, passwordEncoder.encode(req.password()), nickname, avatar,
                    req.ageGroup(), mbti, Boolean.TRUE.equals(req.agreeLocation()), styles, languages);
        } catch (DuplicateKeyException e) {
            // 두 사람이 동시에 같은 아이디로 가입한 경우
            throw ApiException.conflict("DUPLICATE", "이미 사용 중인 아이디예요.");
        }
    }

    /** 로그인 확인. 성공하면 세션에 넣을 회원 정보를 돌려줌 */
    public LoginResult login(LoginRequest req) {
        UserAccount user = repo.findAccountByLoginId(normalizeLoginId(req.loginId()));
        if (user == null || "WITHDRAWN".equals(user.status())
                || !passwordEncoder.matches(req.password(), user.passwordHash())) {
            throw new ApiException(401, "LOGIN_FAILED", "아이디 또는 비밀번호가 맞지 않아요.");
        }

        if ("SUSPENDED".equals(user.status())) {
            LocalDateTime until = user.suspendedUntil();
            if (until != null && !until.isAfter(LocalDateTime.now())) {
                repo.releaseSuspension(user.userId());   // 정지 기간이 지났으면 정상으로 되돌리고 로그인
            } else {
                String message = until == null
                        ? "이용이 정지된 계정입니다."
                        : until.getMonthValue() + "월 " + until.getDayOfMonth() + "일까지 이용이 정지된 계정입니다.";
                throw new ApiException(403, "SUSPENDED", message);
            }
        }
        return new LoginResult(user.userId(), user.nickname(), user.avatar(), user.role());
    }
}
