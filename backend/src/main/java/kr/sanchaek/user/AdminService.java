package kr.sanchaek.user;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.PageResult;
import kr.sanchaek.user.AdminDtos.CourseItem;
import kr.sanchaek.user.AdminDtos.CourseVisibilityResult;
import kr.sanchaek.user.AdminDtos.ReportItem;
import kr.sanchaek.user.AdminDtos.ReportProcessRequest;
import kr.sanchaek.user.AdminDtos.ReportProcessResult;
import kr.sanchaek.user.AdminDtos.UserItem;
import kr.sanchaek.user.AdminDtos.UserStatusResult;
import kr.sanchaek.user.AdminRepository.PendingReport;

/**
 * 관리자 규칙 (API 명세 "관리자" AD1 · AD2 · AD4). 담당: 정란
 * 관리자인지는 AdminInterceptor 가 /api/admin/** 에서 먼저 검사합니다.
 */
@Service
public class AdminService {

    private final AdminRepository repo;

    public AdminService(AdminRepository repo) {
        this.repo = repo;
    }

    /** 빈 검색어는 없는 것으로 */
    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ===================== AD1 신고 처리 =====================

    public PageResult<ReportItem> reports(String status, int page, int size) {
        String s = blankToNull(status) == null ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        if (!s.equals("PENDING") && !s.equals("DONE")) {
            throw ApiException.badRequest("status 는 PENDING 또는 DONE 이에요.");
        }
        return new PageResult<>(repo.findReports(s, size, PageResult.offset(page, size)), page, size, repo.countReports(s));
    }

    /**
     * 신고 처리. 신고 행과 대상 변경을 한 트랜잭션으로 (db/README 설계 약속 10)
     * WARN 경고 · SUSPEND 7일 정지 → 대상 회원(글이면 작성자), HIDE → 글 숨김, REJECT → 바꾸는 것 없음
     */
    @Transactional
    public ReportProcessResult process(long reportId, ReportProcessRequest req, long adminId) {
        PendingReport report = repo.lockReport(reportId);
        if (report == null) {
            throw ApiException.notFound("신고를 찾을 수 없어요.");
        }
        if ("DONE".equals(report.status())) {
            throw ApiException.conflict("DUPLICATE", "이미 처리한 신고예요.");
        }

        String action = req.action();
        if (!action.equals("REJECT")) {
            Long owner = repo.findTargetOwner(report.targetType(), report.targetId());
            if (owner == null) {
                throw ApiException.notFound("신고 대상이 이미 없어졌어요. '문제 없음'으로 처리해 주세요.");
            }
            switch (action) {
                case "WARN" -> {
                    if (repo.warnUser(owner) == 0) {
                        throw ApiException.badRequest("탈퇴한 회원이라 경고할 수 없어요.");
                    }
                }
                case "SUSPEND" -> {
                    if (repo.isAdmin(owner)) {
                        throw ApiException.badRequest("관리자 계정은 정지할 수 없어요.");
                    }
                    if (repo.suspendUser(owner) == 0) {
                        throw ApiException.badRequest("탈퇴한 회원이라 정지할 수 없어요.");
                    }
                }
                case "HIDE" -> {
                    if ("USER".equals(report.targetType())) {
                        throw ApiException.badRequest("사용자 신고는 숨길 게시물이 없어요. 경고나 정지를 골라 주세요.");
                    }
                    repo.hideTarget(report.targetType(), report.targetId());
                }
                default -> throw ApiException.badRequest("처리 방법 값이 올바르지 않아요.");
            }
        }

        String memo = blankToNull(req.memo());
        if (repo.markReportDone(reportId, action, memo, adminId) == 0) {
            throw ApiException.conflict("DUPLICATE", "이미 처리한 신고예요.");
        }
        return new ReportProcessResult(reportId, "DONE", action);
    }

    // ===================== AD2 회원 관리 =====================

    public PageResult<UserItem> users(String q, String status, int page, int size) {
        String s = blankToNull(status) == null ? null : status.trim().toUpperCase(Locale.ROOT);
        if (s != null && !s.equals("ACTIVE") && !s.equals("SUSPENDED") && !s.equals("WITHDRAWN")) {
            throw ApiException.badRequest("status 는 ACTIVE · SUSPENDED · WITHDRAWN 중 하나예요.");
        }
        String keyword = blankToNull(q);
        return new PageResult<>(repo.findUsers(keyword, s, size, PageResult.offset(page, size)), page, size,
                repo.countUsers(keyword, s));
    }

    /** SUSPENDED → 7일 정지, ACTIVE → 정지 풀기. 탈퇴 회원 · 관리자 계정은 400 */
    public UserStatusResult changeUserStatus(long userId, String status, long adminId) {
        String[] roleAndStatus = repo.findRoleAndStatus(userId);
        if (roleAndStatus == null) {
            throw ApiException.notFound("회원을 찾을 수 없어요.");
        }
        if ("ADMIN".equals(roleAndStatus[0])) {
            throw ApiException.badRequest("관리자 계정의 상태는 바꿀 수 없어요.");
        }
        if ("WITHDRAWN".equals(roleAndStatus[1])) {
            throw ApiException.badRequest("탈퇴한 회원의 상태는 바꿀 수 없어요.");
        }
        if ("SUSPENDED".equals(status)) {
            repo.suspendUser(userId);
        } else {
            repo.activateUser(userId);
        }
        return new UserStatusResult(userId, status, repo.findSuspendedUntil(userId));
    }

    // ===================== AD4 코스 관리 =====================

    public PageResult<CourseItem> courses(String q, String tab, int page, int size) {
        String t = blankToNull(tab) == null ? "ALL" : tab.trim().toUpperCase(Locale.ROOT);
        if (!t.equals("ALL") && !t.equals("REVIEW")) {
            throw ApiException.badRequest("tab 은 ALL 또는 REVIEW 예요.");
        }
        String keyword = blankToNull(q);
        boolean reviewOnly = t.equals("REVIEW");
        return new PageResult<>(repo.findCourses(keyword, reviewOnly, size, PageResult.offset(page, size)), page, size,
                repo.countCourses(keyword, reviewOnly));
    }

    public CourseVisibilityResult changeCourseHidden(long courseId, boolean hidden) {
        if (!repo.courseExists(courseId)) {
            throw ApiException.notFound("코스를 찾을 수 없어요.");
        }
        repo.updateCourseHidden(courseId, hidden);
        return new CourseVisibilityResult(courseId, hidden);
    }
}
