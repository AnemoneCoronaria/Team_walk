package kr.sanchaek.match;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.PageResult;
import kr.sanchaek.match.MatchDtos.AdminMatchPage;
import kr.sanchaek.match.MatchDtos.VisibilityRequest;
import kr.sanchaek.match.MatchDtos.VisibilityResult;

/**
 * 관리자 API — AD3 모집 글 관리. 담당: 민서
 * /api/admin/** 는 AdminInterceptor 가 먼저 검사 (로그인 안 함 401, 관리자 아님 403)
 */
@RestController
public class AdminMatchController {

    private final MatchService service;

    public AdminMatchController(MatchService service) {
        this.service = service;
    }

    // 모집 글 목록 · 검색 (탭: 모집 중 OPEN · 마감 CLOSED · 숨김 HIDDEN, q: 제목 · 모집자 · 코스)
    @GetMapping("/api/admin/matches")
    public ApiResponse<AdminMatchPage> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", defaultValue = "OPEN") String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult.validate(page, size);
        return ApiResponse.ok(service.adminMatches(q, status, page, size));
    }

    // 모집 글 숨김 · 복구
    @PutMapping("/api/admin/matches/{id}/visibility")
    public ApiResponse<VisibilityResult> changeVisibility(@PathVariable("id") long matchId,
            @Valid @RequestBody VisibilityRequest request) {
        return ApiResponse.ok(service.changeHidden(matchId, request.hidden()));
    }
}
