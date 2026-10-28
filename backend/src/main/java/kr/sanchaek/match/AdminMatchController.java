package kr.sanchaek.match;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.PageResult;

/**
 * 관리자 AD3 모집 글 관리 (민서). /api/admin/** 라서 관리자 검사는 AdminInterceptor 가 함.
 */
@RestController
public class AdminMatchController {

    private final MatchService service;

    public AdminMatchController(MatchService service) {
        this.service = service;
    }

    /** { "hidden": true } 숨김, { "hidden": false } 복구 */
    public record VisibilityForm(@NotNull(message = "필수예요") Boolean hidden) {
    }

    /** 모집 글 목록 · 검색. q: 제목 · 모집자 · 코스 이름, status: OPEN · CLOSED · HIDDEN (비우면 전체) */
    @GetMapping("/api/admin/matches")
    public ApiResponse<PageResult<AdminMatchRow>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.adminList(q, status, page, size));
    }

    /** 모집 글 숨김 · 복구 */
    @PutMapping("/api/admin/matches/{id}/visibility")
    public ApiResponse<Map<String, Object>> visibility(@PathVariable long id,
            @Valid @RequestBody VisibilityForm form) {
        service.setHidden(id, form.hidden());
        return ApiResponse.ok(Map.of("matchId", id, "hidden", form.hidden()));
    }
}
