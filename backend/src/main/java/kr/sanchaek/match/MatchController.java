package kr.sanchaek.match;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import kr.sanchaek.common.ApiResponse;

@RestController                     // web api endpoint 
public class MatchController { 

    private final MatchRepository repo;     // MatchRepository를 DI함

    public MatchController(MatchRepository repo) {
        this.repo = repo;
    }


    // 동행 목록
    @GetMapping("/api/matches")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(repo.findOpenMatches());
    }


    // 동행 상세
    @GetMapping("/api/matches/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(repo.findMatchById(id));
}

    
}
