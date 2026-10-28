package kr.sanchaek.match;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;

/**
 * 동행 API 13개 (API 명세 "동행 (담당: 민서)").
 * 로그인 "필요" API는 @LoginUser Long userId 를 받습니다 (안 했으면 자동 401).
 * @LoginUser 를 맨 앞에 두는 이유: 파라미터는 앞에서부터 읽어서, 로그인 확인(401)이 입력값 검사(400)보다 먼저 되게.
 */
@RestController
public class MatchController {

    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    /** 동행 목록 · 필터 (W1, E1, M0, M1) */
    @GetMapping("/api/matches")
    public ApiResponse<PageResult<MatchSummary>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timeSlot,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "deadline") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(new MatchService.ListQuery(
                from, to, timeSlot, style, lang, target, age, area, status, sort, page, size)));
    }

    /** 동행 상세 (W2, W4). 로그인 안 해도 됨 */
    @GetMapping("/api/matches/{id}")
    public ApiResponse<MatchDetail> detail(@PathVariable long id, @LoginUser(required = false) Long userId) {
        return ApiResponse.ok(service.detail(id, userId));
    }

    /** 동행 경로 GPX 받기 (W2) — JSON 이 아니라 파일 */
    @GetMapping("/api/matches/{id}/gpx")
    public ResponseEntity<Resource> gpx(@PathVariable long id) {
        return service.gpx(id);
    }

    /** 모집 글 작성 (W3) — multipart/form-data, gpx 는 선택 */
    @PostMapping("/api/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> create(@LoginUser Long userId,
            @Valid @ModelAttribute MatchForm form,
            @RequestParam(required = false) MultipartFile gpx) {
        return ApiResponse.ok(Map.of("matchId", service.create(userId, form, gpx)));
    }

    /** 모집 글 수정 (W4) — JSON, 모집자만 */
    @PutMapping("/api/matches/{id}")
    public ApiResponse<Void> update(@LoginUser Long userId, @PathVariable long id,
            @Valid @RequestBody MatchForm form) {
        service.update(userId, id, form);
        return ApiResponse.ok();
    }

    /** 모집 취소 (W4) — 모집자만, 지우지 않고 CANCELED */
    @DeleteMapping("/api/matches/{id}")
    public ApiResponse<Void> cancel(@PathVariable long id, @LoginUser Long userId) {
        service.cancel(userId, id);
        return ApiResponse.ok();
    }

    /** 모집 마감 (W4) — 모집자만 */
    @PostMapping("/api/matches/{id}/close")
    public ApiResponse<Void> close(@PathVariable long id, @LoginUser Long userId) {
        service.close(userId, id);
        return ApiResponse.ok();
    }

    /** 참여하기 (W2-1) */
    @PostMapping("/api/matches/{id}/join")
    public ApiResponse<JoinResult> join(@PathVariable long id, @LoginUser Long userId) {
        return ApiResponse.ok(service.join(userId, id));
    }

    /** 참여 취소 (W5). 본문 { "reason": "..." } 은 선택 */
    @DeleteMapping("/api/matches/{id}/join")
    public ApiResponse<Void> leave(@LoginUser Long userId, @PathVariable long id,
            @Valid @RequestBody(required = false) LeaveForm form) {
        service.leave(userId, id, form == null ? null : form.reason());
        return ApiResponse.ok();
    }

    /** 참여자 내보내기 (W4) — 모집자만 */
    @DeleteMapping("/api/matches/{id}/participants/{targetUserId}")
    public ApiResponse<Void> kick(@PathVariable long id, @PathVariable long targetUserId, @LoginUser Long userId) {
        service.kick(userId, id, targetUserId);
        return ApiResponse.ok();
    }

    /** 댓글 목록 (W2, W4) — 참여자 · 모집자만 */
    @GetMapping("/api/matches/{id}/comments")
    public ApiResponse<PageResult<MatchComment>> comments(@PathVariable long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @LoginUser Long userId) {
        return ApiResponse.ok(service.comments(userId, id, page, size));
    }

    /** 댓글 쓰기 (W2, W4) — 참여자 · 모집자만. 공지(isNotice)는 모집자만 */
    @PostMapping("/api/matches/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MatchComment> addComment(@LoginUser Long userId, @PathVariable long id,
            @Valid @RequestBody CommentForm form) {
        return ApiResponse.ok(service.addComment(userId, id, form));
    }

    /** 내 동행 (P1). tab: upcoming(기본) · hosted · past */
    @GetMapping("/api/users/me/matches")
    public ApiResponse<PageResult<MatchSummary>> myMatches(
            @RequestParam(defaultValue = "upcoming") String tab,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @LoginUser Long userId) {
        return ApiResponse.ok(service.myMatches(userId, tab, page, size));
    }
}
