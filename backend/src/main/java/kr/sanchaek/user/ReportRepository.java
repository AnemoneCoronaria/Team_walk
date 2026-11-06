package kr.sanchaek.user;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 신고 DB 접근 (reports). 담당: 정란 */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 신고 대상의 주인(회원 번호). 대상이 없거나 이미 지워 · 숨겨졌으면 null.
     * 대상이 4종류라 외래키를 못 걸어서, 신고 받을 때 여기서 있는지 확인합니다 (db/README 설계 약속 10).
     * 사용자 → 그 회원, 동행 글 → 모집자, 코스 → 올린 사람, 후기 → 작성자
     */
    public Long findTargetOwner(String targetType, long targetId) {
        String sql = switch (targetType) {
            case "USER"   -> "SELECT user_id FROM users WHERE user_id = ? AND status <> 'WITHDRAWN'";
            case "MATCH"  -> "SELECT host_id FROM matches WHERE match_id = ? AND is_hidden = FALSE";
            case "COURSE" -> "SELECT author_id FROM courses WHERE course_id = ? AND is_deleted = FALSE AND is_hidden = FALSE";
            case "REVIEW" -> "SELECT user_id FROM reviews WHERE review_id = ? AND is_hidden = FALSE";
            default -> throw new IllegalArgumentException(targetType);
        };
        List<Long> rows = jdbc.queryForList(sql, Long.class, targetId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 신고 저장 (상태는 PENDING). 같은 사람이 같은 대상을 또 신고하면 DB 가 DuplicateKeyException */
    public long insert(long reporterId, String targetType, long targetId, String reason, String detail) {
        String sql = "INSERT INTO reports (reporter_id, target_type, target_id, reason, detail) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, reporterId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.setString(4, reason);
            ps.setString(5, detail);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
