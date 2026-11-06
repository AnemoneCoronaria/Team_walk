package kr.sanchaek.user;

import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import kr.sanchaek.common.ApiException;

/** 신고 규칙 (API 명세 "신고하기"). 담당: 정란 */
@Service
public class ReportService {

    /** C2-1 코스 신고 사유 */
    private static final Set<String> COURSE_REASONS = Set.of("PRIVATE_AREA", "DANGER", "WRONG_INFO", "HOME_EXPOSED", "ETC");
    /** W6 동행 글 · 사용자 · 후기 신고 사유 */
    private static final Set<String> OTHER_REASONS = Set.of("NO_SHOW", "RUDE", "INAPPROPRIATE", "SPAM", "SAFETY", "ETC");

    private final ReportRepository repo;

    public ReportService(ReportRepository repo) {
        this.repo = repo;
    }

    /** 신고 접수. 만들어진 reportId 를 돌려줌 */
    public long report(long reporterId, ReportRequest req) {
        Set<String> allowed = "COURSE".equals(req.targetType()) ? COURSE_REASONS : OTHER_REASONS;
        if (!allowed.contains(req.reason())) {
            throw ApiException.badRequest("신고 대상에 맞지 않는 사유예요.");
        }

        Long owner = repo.findTargetOwner(req.targetType(), req.targetId());
        if (owner == null) {
            throw ApiException.notFound("신고할 대상을 찾을 수 없어요.");
        }
        if (owner == reporterId) {
            throw ApiException.badRequest("자기 자신이나 자기 글은 신고할 수 없어요.");
        }

        String detail = req.detail() == null || req.detail().isBlank() ? null : req.detail().trim();
        try {
            return repo.insert(reporterId, req.targetType(), req.targetId(), req.reason(), detail);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("DUPLICATE", "이미 신고한 대상이에요.");
        }
    }
}
