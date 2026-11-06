package kr.sanchaek.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import kr.sanchaek.user.AdminDtos.CourseItem;
import kr.sanchaek.user.AdminDtos.ReportItem;
import kr.sanchaek.user.AdminDtos.UserItem;
import kr.sanchaek.user.AdminDtos.UserRef;

/** 관리자 화면 DB 접근 (AD1 신고 · AD2 회원 · AD4 코스). 담당: 정란 */
@Repository
public class AdminRepository {

    /** 정지 기간이 지난 회원은 로그인 전이라도 정상(ACTIVE)으로 보여 줌 (로그인할 때 실제로 되돌림) */
    private static final String USER_STATUS =
            "(CASE WHEN u.status = 'SUSPENDED' AND u.suspended_until <= NOW() THEN 'ACTIVE' ELSE u.status END)";

    private final JdbcTemplate jdbc;

    public AdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ===================== AD1 신고 =====================

    /** 신고 목록 (PENDING 대기 · DONE 처리 완료), 최신순 */
    public List<ReportItem> findReports(String status, int size, int offset) {
        String sql = "SELECT r.report_id, r.target_type, r.target_id, r.reason, r.detail, r.status, r.action, "
                   + "       r.admin_memo, r.created_at, u.user_id AS reporter_id, u.nickname AS reporter_nickname, "
                   + "       (CASE r.target_type "
                   + "          WHEN 'MATCH'  THEN (SELECT m.title FROM matches m WHERE m.match_id = r.target_id) "
                   + "          WHEN 'COURSE' THEN (SELECT c.name FROM courses c WHERE c.course_id = r.target_id) "
                   + "          WHEN 'USER'   THEN (SELECT CONCAT(t.nickname, ' (', t.login_id, ')') FROM users t WHERE t.user_id = r.target_id) "
                   + "          WHEN 'REVIEW' THEN (SELECT c.name FROM reviews v JOIN courses c ON c.course_id = v.course_id WHERE v.review_id = r.target_id) "
                   + "        END) AS target_label, "
                   + "       (SELECT COUNT(*) FROM reports x WHERE x.target_type = r.target_type AND x.target_id = r.target_id) AS target_count "
                   + "FROM reports r JOIN users u ON u.user_id = r.reporter_id "
                   + "WHERE r.status = ? "
                   + "ORDER BY r.created_at DESC, r.report_id DESC LIMIT ? OFFSET ?";
        return jdbc.query(sql, (rs, i) -> new ReportItem(
                rs.getLong("report_id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("target_label"),
                rs.getString("reason"),
                rs.getString("detail"),
                new UserRef(rs.getLong("reporter_id"), rs.getString("reporter_nickname")),
                rs.getInt("target_count"),
                rs.getString("status"),
                rs.getString("action"),
                rs.getString("admin_memo"),
                rs.getObject("created_at", LocalDateTime.class)), status, size, offset);
    }

    public long countReports(String status) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM reports WHERE status = ?", Long.class, status);
        return count == null ? 0 : count;
    }

    /** 처리할 신고 한 줄을 잠그고 가져옴 (두 관리자가 동시에 처리하는 경우 대비). 없으면 null */
    public PendingReport lockReport(long reportId) {
        List<PendingReport> rows = jdbc.query(
                "SELECT report_id, target_type, target_id, status FROM reports WHERE report_id = ? FOR UPDATE",
                (rs, i) -> new PendingReport(rs.getLong("report_id"), rs.getString("target_type"),
                        rs.getLong("target_id"), rs.getString("status")), reportId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public record PendingReport(long reportId, String targetType, long targetId, String status) {
    }

    /**
     * 신고 대상의 주인(회원 번호). 대상이 없어졌으면 null. (숨긴 글이어도 주인은 찾음)
     * 사용자 → 그 회원, 동행 글 → 모집자, 코스 → 올린 사람, 후기 → 작성자 (db/README 설계 약속 10)
     */
    public Long findTargetOwner(String targetType, long targetId) {
        String sql = switch (targetType) {
            case "USER"   -> "SELECT user_id FROM users WHERE user_id = ?";
            case "MATCH"  -> "SELECT host_id FROM matches WHERE match_id = ?";
            case "COURSE" -> "SELECT author_id FROM courses WHERE course_id = ?";
            case "REVIEW" -> "SELECT user_id FROM reviews WHERE review_id = ?";
            default -> throw new IllegalArgumentException(targetType);
        };
        List<Long> rows = jdbc.queryForList(sql, Long.class, targetId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 경고: warning_count + 1. 탈퇴 회원이면 0줄 */
    public int warnUser(long userId) {
        return jdbc.update("UPDATE users SET warning_count = warning_count + 1 WHERE user_id = ? AND status <> 'WITHDRAWN'", userId);
    }

    /** 7일 이용 정지. 탈퇴 회원이면 0줄 */
    public int suspendUser(long userId) {
        return jdbc.update("UPDATE users SET status = 'SUSPENDED', suspended_until = NOW() + INTERVAL 7 DAY "
                         + "WHERE user_id = ? AND status <> 'WITHDRAWN'", userId);
    }

    /** 게시물 숨김: 동행 글 · 코스 · 후기의 is_hidden = TRUE */
    public void hideTarget(String targetType, long targetId) {
        String sql = switch (targetType) {
            case "MATCH"  -> "UPDATE matches SET is_hidden = TRUE WHERE match_id = ?";
            case "COURSE" -> "UPDATE courses SET is_hidden = TRUE WHERE course_id = ?";
            case "REVIEW" -> "UPDATE reviews SET is_hidden = TRUE WHERE review_id = ?";
            default -> throw new IllegalArgumentException(targetType);
        };
        jdbc.update(sql, targetId);
    }

    /** 신고 처리 완료로 바꿈. 이미 처리된 신고면 0줄 */
    public int markReportDone(long reportId, String action, String memo, long adminId) {
        return jdbc.update("UPDATE reports SET status = 'DONE', action = ?, admin_memo = ?, processed_by = ?, processed_at = NOW() "
                         + "WHERE report_id = ? AND status = 'PENDING'", action, memo, adminId, reportId);
    }

    // ===================== AD2 회원 =====================

    /** 회원 목록 · 검색. q 는 닉네임 · 아이디, status 는 ACTIVE · SUSPENDED · WITHDRAWN (null 이면 전체) */
    public List<UserItem> findUsers(String q, String status, int size, int offset) {
        List<Object> args = new ArrayList<>();
        String where = userWhere(q, status, args);
        String sql = "SELECT u.user_id, u.nickname, u.login_id, u.age_group, u.role, u.warning_count, u.created_at, "
                   + "       " + USER_STATUS + " AS shown_status, "
                   + "       IF(" + USER_STATUS + " = 'SUSPENDED', u.suspended_until, NULL) AS shown_until, "
                   + "       (SELECT GROUP_CONCAT(t.tag_value ORDER BY t.tag_value) FROM user_tags t "
                   + "         WHERE t.user_id = u.user_id AND t.tag_type = 'LANGUAGE') AS languages, "
                   + "       (SELECT COUNT(*) FROM match_participants p JOIN v_match_status s ON s.match_id = p.match_id "
                   + "         WHERE p.user_id = u.user_id AND p.status = 'JOINED' AND s.status = 'DONE') AS match_count, "
                   + "       (SELECT COUNT(*) FROM reports r WHERE r.target_type = 'USER' AND r.target_id = u.user_id) AS report_count "
                   + "FROM users u " + where
                   + " ORDER BY u.user_id LIMIT ? OFFSET ?";
        args.add(size);
        args.add(offset);
        return jdbc.query(sql, (rs, i) -> {
            String langs = rs.getString("languages");
            return new UserItem(
                    rs.getLong("user_id"),
                    rs.getString("nickname"),
                    rs.getString("login_id"),
                    rs.getString("age_group"),
                    langs == null ? List.of() : Arrays.asList(langs.split(",")),
                    rs.getInt("match_count"),
                    rs.getInt("report_count"),
                    rs.getString("shown_status"),
                    rs.getString("role"),
                    rs.getInt("warning_count"),
                    rs.getObject("shown_until", LocalDateTime.class),
                    rs.getObject("created_at", LocalDateTime.class));
        }, args.toArray());
    }

    public long countUsers(String q, String status) {
        List<Object> args = new ArrayList<>();
        String where = userWhere(q, status, args);
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM users u " + where, Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private static String userWhere(String q, String status, List<Object> args) {
        List<String> conds = new ArrayList<>();
        if (q != null) {
            conds.add("(u.nickname LIKE ? OR u.login_id LIKE ?)");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (status != null) {
            conds.add(USER_STATUS + " = ?");
            args.add(status);
        }
        return conds.isEmpty() ? "" : "WHERE " + String.join(" AND ", conds);
    }

    /** 상태 바꾸기 대상 확인용 (role, status). 없으면 null */
    public String[] findRoleAndStatus(long userId) {
        List<String[]> rows = jdbc.query("SELECT role, status FROM users WHERE user_id = ?",
                (rs, i) -> new String[] { rs.getString("role"), rs.getString("status") }, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean isAdmin(long userId) {
        String[] roleAndStatus = findRoleAndStatus(userId);
        return roleAndStatus != null && "ADMIN".equals(roleAndStatus[0]);
    }

    /** 정지 풀기 */
    public void activateUser(long userId) {
        jdbc.update("UPDATE users SET status = 'ACTIVE', suspended_until = NULL WHERE user_id = ? AND status <> 'WITHDRAWN'", userId);
    }

    public LocalDateTime findSuspendedUntil(long userId) {
        return jdbc.queryForObject("SELECT suspended_until FROM users WHERE user_id = ?",
                (rs, i) -> rs.getObject("suspended_until", LocalDateTime.class), userId);
    }

    // ===================== AD4 코스 =====================

    /** 코스 목록 · 검색. 작성자가 삭제한 코스는 뺌. reviewOnly 면 대기 중 신고가 있는 코스만 (검토 필요 탭) */
    public List<CourseItem> findCourses(String q, boolean reviewOnly, int size, int offset) {
        List<Object> args = new ArrayList<>();
        String where = courseWhere(q, reviewOnly, args);
        String sql = "SELECT c.course_id, c.name, c.distance_km, c.level, c.download_count, c.visibility, c.is_hidden, "
                   + "       u.user_id, u.nickname, u.role, IFNULL(v.review_count, 0) AS review_count, "
                   + "       " + PENDING_COURSE_REPORTS + " AS pending_reports "
                   + "FROM courses c JOIN users u ON u.user_id = c.author_id "
                   + "LEFT JOIN v_course_rating v ON v.course_id = c.course_id " + where
                   + " ORDER BY pending_reports DESC, c.course_id DESC LIMIT ? OFFSET ?";
        args.add(size);
        args.add(offset);
        return jdbc.query(sql, (rs, i) -> new CourseItem(
                rs.getLong("course_id"),
                rs.getString("name"),
                new UserRef(rs.getLong("user_id"), rs.getString("nickname")),
                "ADMIN".equals(rs.getString("role")),
                rs.getDouble("distance_km"),
                rs.getString("level"),
                rs.getInt("download_count"),
                rs.getInt("review_count"),
                rs.getString("visibility"),
                rs.getBoolean("is_hidden"),
                rs.getInt("pending_reports")), args.toArray());
    }

    public long countCourses(String q, boolean reviewOnly) {
        List<Object> args = new ArrayList<>();
        String where = courseWhere(q, reviewOnly, args);
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM courses c JOIN users u ON u.user_id = c.author_id " + where,
                Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private static final String PENDING_COURSE_REPORTS =
            "(SELECT COUNT(*) FROM reports r WHERE r.target_type = 'COURSE' AND r.target_id = c.course_id AND r.status = 'PENDING')";

    private static String courseWhere(String q, boolean reviewOnly, List<Object> args) {
        List<String> conds = new ArrayList<>();
        conds.add("c.is_deleted = FALSE");
        if (q != null) {
            conds.add("(c.name LIKE ? OR u.nickname LIKE ?)");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (reviewOnly) {
            conds.add(PENDING_COURSE_REPORTS + " > 0");
        }
        return "WHERE " + String.join(" AND ", conds);
    }

    /** 코스 숨김 · 복구. 없거나 작성자가 삭제한 코스면 0줄 */
    public int updateCourseHidden(long courseId, boolean hidden) {
        return jdbc.update("UPDATE courses SET is_hidden = ? WHERE course_id = ? AND is_deleted = FALSE", hidden, courseId);
    }

    public boolean courseExists(long courseId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE course_id = ? AND is_deleted = FALSE",
                Integer.class, courseId);
        return count != null && count > 0;
    }
}
