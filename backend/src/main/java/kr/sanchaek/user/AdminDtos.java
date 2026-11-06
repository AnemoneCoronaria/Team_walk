package kr.sanchaek.user;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 화면(AD1 · AD2 · AD4)이 주고받는 데이터 모양. 담당: 정란
 * API 명세 "관리자" 의 응답 예시와 필드 이름이 같습니다.
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    /** 목록에 같이 나오는 회원 이름표 { userId, nickname } */
    public record UserRef(long userId, String nickname) {
    }

    // ---------- AD1 신고 처리 ----------

    /** GET /api/admin/reports 한 줄 */
    public record ReportItem(
            long reportId,
            String targetType,
            long targetId,
            String targetLabel,       // 동행 글 제목 · 코스 이름 · "닉네임 (아이디)" · 후기의 코스 이름
            String reason,
            String detail,
            UserRef reporter,
            int targetReportCount,    // 같은 대상에 들어온 신고 수 (AD1 "누적 신고")
            String status,            // PENDING · DONE
            String action,            // WARN · SUSPEND · HIDE · REJECT (처리 완료일 때만)
            String adminMemo,
            LocalDateTime createdAt) {
    }

    /** PUT /api/admin/reports/{id} 요청 (AD1 버튼). 보류는 API 를 부르지 않음 */
    public record ReportProcessRequest(
            @NotNull(message = "처리 방법을 골라 주세요.")
            @Pattern(regexp = "^(WARN|SUSPEND|HIDE|REJECT)$", message = "처리 방법 값이 올바르지 않아요.")
            String action,

            @Size(max = 500, message = "메모는 500자까지예요.")
            String memo) {
    }

    /** PUT /api/admin/reports/{id} 응답 */
    public record ReportProcessResult(long reportId, String status, String action) {
    }

    // ---------- AD2 회원 관리 ----------

    /** GET /api/admin/users 한 줄 */
    public record UserItem(
            long userId,
            String nickname,
            String loginId,
            String ageGroup,
            List<String> languages,
            int matchCount,           // 함께 걸은 동행 (끝난 동행 참여 수)
            int reportCount,          // 이 회원(USER)이 대상인 신고 수
            String status,            // ACTIVE · SUSPENDED · WITHDRAWN (정지 기간이 지났으면 ACTIVE)
            String role,              // USER · ADMIN (관리자 줄은 정지 버튼을 숨기려고)
            int warningCount,
            LocalDateTime suspendedUntil,
            LocalDateTime createdAt) {
    }

    /** PUT /api/admin/users/{id}/status 요청 — SUSPENDED 7일 정지, ACTIVE 정지 풀기 */
    public record UserStatusRequest(
            @NotNull(message = "바꿀 상태를 보내 주세요.")
            @Pattern(regexp = "^(SUSPENDED|ACTIVE)$", message = "상태 값은 SUSPENDED 또는 ACTIVE 예요.")
            String status) {
    }

    /** PUT /api/admin/users/{id}/status 응답 */
    public record UserStatusResult(long userId, String status, LocalDateTime suspendedUntil) {
    }

    /** POST /api/admin/users/{id}/temp-password 응답. 임시 비밀번호는 이 응답에서 한 번만 볼 수 있음 */
    public record TempPasswordResult(long userId, String temporaryPassword) {
    }

    // ---------- AD4 코스 관리 ----------

    /** GET /api/admin/courses 한 줄 */
    public record CourseItem(
            long courseId,
            String name,
            UserRef author,
            boolean official,         // 관리자 계정이 올린 코스
            double distanceKm,
            String level,
            int downloadCount,
            int reviewCount,
            String visibility,
            boolean hidden,
            int pendingReports) {     // 대기 중 신고 수 (1 이상이면 "검토 필요")
    }

    /** PUT /api/admin/courses/{id}/visibility 요청 · 응답의 hidden */
    public record VisibilityRequest(
            @NotNull(message = "숨길지(true) 복구할지(false) 보내 주세요.") Boolean hidden) {
    }

    public record CourseVisibilityResult(long courseId, boolean hidden) {
    }
}
