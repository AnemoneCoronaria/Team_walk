package kr.sanchaek.user;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;

/** 신고하기 (API 정란 · 화면 승민). 접수된 신고는 관리자 AD1 에서 처리 */
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // 신고하기 (W6 동행 글 · 사용자 · 후기, C2-1 코스)
    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> report(@LoginUser Long userId, @Valid @RequestBody ReportRequest req) {
        long reportId = reportService.report(userId, req);
        return ApiResponse.ok(Map.of("reportId", reportId));
    }
}
