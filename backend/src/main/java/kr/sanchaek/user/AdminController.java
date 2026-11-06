package kr.sanchaek.user;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;
import kr.sanchaek.user.AdminDtos.CourseItem;
import kr.sanchaek.user.AdminDtos.CourseVisibilityResult;
import kr.sanchaek.user.AdminDtos.ReportItem;
import kr.sanchaek.user.AdminDtos.ReportProcessRequest;
import kr.sanchaek.user.AdminDtos.ReportProcessResult;
import kr.sanchaek.user.AdminDtos.TempPasswordResult;
import kr.sanchaek.user.AdminDtos.UserItem;
import kr.sanchaek.user.AdminDtos.UserStatusRequest;
import kr.sanchaek.user.AdminDtos.UserStatusResult;
import kr.sanchaek.user.AdminDtos.VisibilityRequest;

/**
 * 관리자 API — AD1 신고 처리 · AD2 회원 관리 · AD4 코스 관리. 담당: 정란 (AD3 모집 글 관리는 민서)
 * /api/admin/** 는 AdminInterceptor 가 먼저 검사 (로그인 안 함 401, 관리자 아님 403)
 */
@RestController
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // AD1 신고 목록 (대기 PENDING 기본 · 처리 완료 DONE)
    @GetMapping("/api/admin/reports")
    public ApiResponse<PageResult<ReportItem>> reports(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult.validate(page, size);
        return ApiResponse.ok(adminService.reports(status, page, size));
    }

    // AD1 신고 처리 (경고 · 7일 정지 · 숨김 · 문제 없음)
    @PutMapping("/api/admin/reports/{id}")
    public ApiResponse<ReportProcessResult> processReport(@PathVariable("id") long reportId,
            @Valid @RequestBody ReportProcessRequest req, @LoginUser Long adminId) {
        return ApiResponse.ok(adminService.process(reportId, req, adminId));
    }

    // AD2 회원 목록 · 검색 (탭: 전체 · 이용 정지 · 탈퇴)
    @GetMapping("/api/admin/users")
    public ApiResponse<PageResult<UserItem>> users(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult.validate(page, size);
        return ApiResponse.ok(adminService.users(q, status, page, size));
    }

    // AD2 회원 정지 (7일) · 정지 풀기
    @PutMapping("/api/admin/users/{id}/status")
    public ApiResponse<UserStatusResult> changeUserStatus(@PathVariable("id") long userId,
            @Valid @RequestBody UserStatusRequest req, @LoginUser Long adminId) {
        return ApiResponse.ok(adminService.changeUserStatus(userId, req.status(), adminId));
    }

    // AD2 임시 비밀번호 발급 (회원이 비밀번호를 잊었을 때). 응답에서 한 번만 볼 수 있음
    @PostMapping("/api/admin/users/{id}/temp-password")
    public ApiResponse<TempPasswordResult> issueTempPassword(@PathVariable("id") long userId) {
        return ApiResponse.ok(adminService.issueTempPassword(userId));
    }

    // AD4 코스 목록 · 검색 (탭: 전체 ALL · 검토 필요 REVIEW)
    @GetMapping("/api/admin/courses")
    public ApiResponse<PageResult<CourseItem>> courses(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tab", required = false) String tab,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult.validate(page, size);
        return ApiResponse.ok(adminService.courses(q, tab, page, size));
    }

    // AD4 코스 숨김 · 복구
    @PutMapping("/api/admin/courses/{id}/visibility")
    public ApiResponse<CourseVisibilityResult> changeCourseVisibility(@PathVariable("id") long courseId,
            @Valid @RequestBody VisibilityRequest req) {
        return ApiResponse.ok(adminService.changeCourseHidden(courseId, req.hidden()));
    }
}
