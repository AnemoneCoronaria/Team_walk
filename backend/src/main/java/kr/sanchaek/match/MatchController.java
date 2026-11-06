package kr.sanchaek.match;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;

@RestController                     // web api endpoint 
public class MatchController { 

    private final MatchRepository repo;     // MatchRepository를 DI함
    private final MatchService service;     // 쓰기 규칙 (권한 · 상태 · 참여 조건)
    private final FileStore files;          // GPX 파일 내려주기 (공통 코드)

    public MatchController(MatchRepository repo, MatchService service, FileStore files) {
        this.repo = repo;
        this.service = service;
        this.files = files;
    }


    // 동행 목록 (필터 · 정렬 · 페이지)
    // 쿼리 파라미터는 Figma W1 "API (안)" 과 같음. @RequestParam 도 이름을 직접 적음 (@PathVariable 과 같은 이유)
    @GetMapping("/api/matches")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "status", defaultValue = "OPEN") String status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "timeSlot", required = false) String timeSlot,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestParam(value = "target", required = false) String target,
            @RequestParam(value = "age", required = false) String age,
            @RequestParam(value = "area", required = false) String area,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "sort", defaultValue = "deadline") String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (!STATUSES.contains(status)) {
            throw ApiException.badRequest("status 는 OPEN · FULL · CLOSED · CANCELED · DONE 중 하나예요.");
        }
        if (!sort.equals("deadline") && !sort.equals("recent")) {
            throw ApiException.badRequest("sort 는 deadline · recent 중 하나예요.");
        }
        checkPage(page, size);

        MatchSearch q = new MatchSearch(status, from, to, timeSlot, split(style), split(lang),
                target, age, area, courseId, sort, page, size);
        return ApiResponse.ok(repo.findMatches(q));
    }

    private static final Set<String> STATUSES = Set.of("OPEN", "FULL", "CLOSED", "CANCELED", "DONE");
    private static final Set<String> MY_TABS = Set.of("upcoming", "hosting", "past");

    private static void checkPage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw ApiException.badRequest("page 는 1 이상, size 는 1~100 이어야 해요.");
        }
    }

    /** "SLOW,CHAT" → ["SLOW", "CHAT"]. 비어 있으면 null */
    private static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }


    // 내 동행 (P1 탭, 로그인 필요). tab: upcoming 참여 예정 · hosting 내가 모집한 글 · past 지난 동행
    @GetMapping("/api/users/me/matches")
    public ApiResponse<PageResult<Map<String, Object>>> myMatches(@LoginUser Long userId,
            @RequestParam(value = "tab", defaultValue = "upcoming") String tab,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (!MY_TABS.contains(tab)) {
            throw ApiException.badRequest("tab 은 upcoming · hosting · past 중 하나예요.");
        }
        checkPage(page, size);
        return ApiResponse.ok(repo.findMyMatches(userId, tab, page, size));
    }


    // 동행 상세 (로그인 선택: 로그인했으면 isHost · myStatus · joinable · reason 이 나에 맞게 나옴)
    @GetMapping("/api/matches/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Long id,
            @LoginUser(required = false) Long userId) {
                // @PathVariable에 "id"를 추가함
                // ==> 스프링이 이름을 알 수 없어서 Name for argument of type [java.lang.Long] not specified 같은 오류가 발생했음
        return ApiResponse.ok(service.detail(id, userId));
    }



    // 동행 경로 GPX 받기 (모집자가 첨부한 경우만. 없으면 404)
    @GetMapping("/api/matches/{id}/gpx")
    public ResponseEntity<Resource> gpx(@PathVariable("id") Long id) {
        return files.download(repo.findGpxPath(id), "match-" + id + ".gpx");
    }


    // ----- 2주차: 모집 글 (W3 · W4) — 전부 로그인 필요 -----

    // 모집 글 쓰기. multipart/form-data: 글 칸들 + gpx 파일(선택) → 201 { matchId }
    @PostMapping(value = "/api/matches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> create(@LoginUser Long userId,
            @Valid @ModelAttribute MatchRequest request,
            @RequestPart(value = "gpx", required = false) MultipartFile gpx) {
        return ApiResponse.ok(Map.of("matchId", service.create(userId, request, gpx)));
    }

    // 모집 글 수정 (모집자만). 쓰기와 같은 칸 + removeGpx
    @PutMapping(value = "/api/matches/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> update(@PathVariable("id") Long id, @LoginUser Long userId,
            @Valid @ModelAttribute MatchRequest request,
            @RequestPart(value = "gpx", required = false) MultipartFile gpx) {
        service.update(id, userId, request, gpx);
        return ApiResponse.ok();
    }

    // 모집 취소 (모집자만, 글은 남고 상태만 CANCELED)
    @DeleteMapping("/api/matches/{id}")
    public ApiResponse<Void> cancel(@PathVariable("id") Long id, @LoginUser Long userId) {
        service.cancel(id, userId);
        return ApiResponse.ok();
    }

    // 모집 마감 (모집자만)
    @PostMapping("/api/matches/{id}/close")
    public ApiResponse<Void> close(@PathVariable("id") Long id, @LoginUser Long userId) {
        service.close(id, userId);
        return ApiResponse.ok();
    }


    // ----- 2주차: 참여 (W2-1 · W5 · W4) — 전부 로그인 필요 -----

    // 참여하기
    @PostMapping("/api/matches/{id}/join")
    public ApiResponse<Void> join(@PathVariable("id") Long id, @LoginUser Long userId) {
        service.join(id, userId);
        return ApiResponse.ok();
    }

    // 참여 취소. 본문 { "reason": "일정이 생겼어요" } 은 선택
    @DeleteMapping("/api/matches/{id}/join")
    public ApiResponse<Void> leave(@PathVariable("id") Long id, @LoginUser Long userId,
            @Valid @RequestBody(required = false) LeaveRequest request) {
        service.leave(id, userId, request == null ? null : request.reason());
        return ApiResponse.ok();
    }

    // 내보내기 (모집자만)
    @DeleteMapping("/api/matches/{id}/participants/{targetUserId}")
    public ApiResponse<Void> kick(@PathVariable("id") Long id, @PathVariable("targetUserId") Long targetUserId,
            @LoginUser Long userId) {
        service.kick(id, userId, targetUserId);
        return ApiResponse.ok();
    }
}
