package kr.sanchaek.match;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.PageResult;

@RestController                     // web api endpoint 
public class MatchController { 

    private final MatchRepository repo;     // MatchRepository를 DI함
    private final FileStore files;          // GPX 파일 내려주기 (공통 코드)

    public MatchController(MatchRepository repo, FileStore files) {
        this.repo = repo;
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
        if (page < 1 || size < 1 || size > 100) {
            throw ApiException.badRequest("page 는 1 이상, size 는 1~100 이어야 해요.");
        }

        MatchSearch q = new MatchSearch(status, from, to, timeSlot, split(style), split(lang),
                target, age, area, courseId, sort, page, size);
        return ApiResponse.ok(repo.findMatches(q));
    }

    private static final Set<String> STATUSES = Set.of("OPEN", "FULL", "CLOSED", "CANCELED", "DONE");

    /** "SLOW,CHAT" → ["SLOW", "CHAT"]. 비어 있으면 null */
    private static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }


    // 동행 상세
    @GetMapping("/api/matches/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Long id) {
                // @PathVariable에 "id"를 추가함  
                // ==> 스프링이 이름을 알 수 없어서 Name for argument of type [java.lang.Long] not specified 같은 오류가 발생했음
        return ApiResponse.ok(repo.findMatchById(id));
}

    

    // 동행 경로 GPX 받기 (모집자가 첨부한 경우만. 없으면 404)
    @GetMapping("/api/matches/{id}/gpx")
    public ResponseEntity<Resource> gpx(@PathVariable("id") Long id) {
        return files.download(repo.findGpxPath(id), "match-" + id + ".gpx");
    }

}
