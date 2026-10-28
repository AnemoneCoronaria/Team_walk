package kr.sanchaek.match;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.GpxParser;
import kr.sanchaek.common.PageResult;
import kr.sanchaek.match.MatchRepository.DetailRow;
import kr.sanchaek.match.MatchRepository.MatchState;
import kr.sanchaek.match.MatchRepository.Tags;

/**
 * 동행 규칙 (API 명세 "동행 (담당: 민서)").
 * 여러 SQL을 한 번에 저장해야 하는 곳은 @Transactional 로 묶습니다 (하나라도 실패하면 전부 취소).
 */
@Service
public class MatchService {

    private final MatchRepository repo;
    private final FileStore fileStore;

    public MatchService(MatchRepository repo, FileStore fileStore) {
        this.repo = repo;
        this.fileStore = fileStore;
    }

    // ------------------------------------------------------------------
    // 목록
    // ------------------------------------------------------------------

    /** 목록 검색 조건 (GET /api/matches 파라미터) */
    public record ListQuery(LocalDate from, LocalDate to, String timeSlot, String style, String lang, String target,
            String age, String area, String status, String sort, int page, int size) {
    }

    public PageResult<MatchSummary> list(ListQuery q) {
        int page = Math.max(q.page(), 1);
        int size = clampSize(q.size(), 20);

        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        where.add("m.is_hidden = FALSE");

        String status = q.status() == null || q.status().isBlank() ? "OPEN" : q.status().trim().toUpperCase();
        if (!"ALL".equals(status)) {
            MatchCodes.check("status", status, MatchCodes.STATUSES);
            where.add("s.status = ?");
            params.add(status);
        }
        if (q.from() != null) {
            where.add("m.meet_at >= ?");
            params.add(q.from().atStartOfDay());
        }
        if (q.to() != null) {
            where.add("m.meet_at < ?");
            params.add(q.to().plusDays(1).atStartOfDay());
        }
        if (q.timeSlot() != null && !q.timeSlot().isBlank()) {
            String slot = MatchCodes.check("timeSlot", q.timeSlot().trim(), MatchCodes.TIME_SLOTS);
            where.add(switch (slot) {
                case "MORNING" -> "HOUR(m.meet_at) BETWEEN 5 AND 10";
                case "AFTERNOON" -> "HOUR(m.meet_at) BETWEEN 11 AND 16";
                case "EVENING" -> "HOUR(m.meet_at) BETWEEN 17 AND 20";
                default -> "(HOUR(m.meet_at) >= 21 OR HOUR(m.meet_at) <= 4)";   // NIGHT
            });
        }
        addTagFilter(where, params, "STYLE", MatchCodes.splitAndCheck("style", q.style(), MatchCodes.STYLES));
        addTagFilter(where, params, "LANGUAGE", MatchCodes.splitAndCheck("lang", q.lang(), MatchCodes.LANGUAGES));
        if (q.target() != null && !q.target().isBlank()) {
            where.add("m.target = ?");
            params.add(MatchCodes.check("target", q.target().trim(), MatchCodes.TARGETS));
        }
        if (q.age() != null && !q.age().isBlank()) {
            // 연령대를 하나도 안 고른 글은 "누구나"로 보고 같이 보여 줌
            where.add("""
                    (EXISTS (SELECT 1 FROM match_tags t WHERE t.match_id = m.match_id AND t.tag_type = 'AGE_GROUP' AND t.tag_value = ?)
                     OR NOT EXISTS (SELECT 1 FROM match_tags t WHERE t.match_id = m.match_id AND t.tag_type = 'AGE_GROUP'))""");
            params.add(MatchCodes.check("age", q.age().trim(), MatchCodes.AGE_GROUPS));
        }
        if (q.area() != null && !q.area().isBlank()) {
            where.add("c.area = ?");
            params.add(MatchCodes.check("area", q.area().trim(), MatchCodes.AREAS));
        }

        String orderBy = "recent".equals(q.sort())
                ? "m.created_at DESC, m.match_id DESC"
                : "m.meet_at ASC, m.match_id ASC";   // deadline (기본): 만나는 시간이 가까운 순

        String whereSql = String.join(" AND ", where);
        List<MatchSummary> items = repo.findSummaries(whereSql, params, orderBy, size, PageResult.offset(page, size),
                null);
        return new PageResult<>(items, page, size, repo.count(whereSql, params));
    }

    /** 내 동행 (P1). tab: upcoming 예정(기본), hosted 내가 모집한, past 끝난 */
    public PageResult<MatchSummary> myMatches(long userId, String tab, int page, int size) {
        page = Math.max(page, 1);
        size = clampSize(size, 20);
        List<Object> params = new ArrayList<>();
        String where;
        String orderBy;
        Long reviewedBy = null;

        switch (tab == null || tab.isBlank() ? "upcoming" : tab) {
            case "upcoming" -> {
                // 내가 참여 중인 앞으로의 동행. 모집 취소된 글도 "취소됨"으로 남김
                where = "m.is_hidden = FALSE AND m.meet_at > NOW() AND " + joinedBy();
                params.add(userId);
                orderBy = "m.meet_at ASC, m.match_id ASC";
            }
            case "hosted" -> {
                where = "m.is_hidden = FALSE AND m.host_id = ?";
                params.add(userId);
                orderBy = "m.meet_at DESC, m.match_id DESC";
            }
            case "past" -> {
                where = "m.is_hidden = FALSE AND s.status = 'DONE' AND " + joinedBy();
                params.add(userId);
                orderBy = "m.meet_at DESC, m.match_id DESC";
                reviewedBy = userId;
            }
            default -> throw ApiException.badRequest("tab: upcoming, hosted, past 중 하나여야 해요");
        }

        List<MatchSummary> items = repo.findSummaries(where, params, orderBy, size, PageResult.offset(page, size),
                reviewedBy);
        return new PageResult<>(items, page, size, repo.count(where, params));
    }

    // ------------------------------------------------------------------
    // 상세 · GPX
    // ------------------------------------------------------------------

    /** loginUserId 는 로그인 안 했으면 null (isHost, joined 가 false) */
    public MatchDetail detail(long matchId, Long loginUserId) {
        DetailRow d = repo.findDetail(matchId);
        if (d == null) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
        Tags tags = repo.findTags(matchId);
        List<Participant> participants = repo.findParticipants(matchId, d.hostId);
        boolean isHost = loginUserId != null && loginUserId == d.hostId;
        boolean joined = loginUserId != null && participants.stream().anyMatch(p -> p.userId() == loginUserId);

        return new MatchDetail(d.matchId, d.title, d.content, d.meetAt, d.durationMin, d.meetPlace, d.meetPoint,
                d.course, d.path, d.hasGpx, d.target, tags.ageGroups, tags.styles, tags.languages, d.mbti,
                d.capacity, d.status,
                new MatchDetail.Host(d.hostId, d.hostNickname, d.hostAvatar, repo.countDoneMatches(d.hostId)),
                participants, isHost, joined);
    }

    /** 첨부한 GPX, 없으면 코스 GPX */
    public ResponseEntity<Resource> gpx(long matchId) {
        String[] paths = repo.findGpxPaths(matchId);
        if (paths == null) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
        String path = paths[1] != null ? paths[1] : paths[2];
        return fileStore.download(path, paths[0] + ".gpx");
    }

    // ------------------------------------------------------------------
    // 작성 · 수정 · 취소 · 마감
    // ------------------------------------------------------------------

    @Transactional
    public long create(long userId, MatchForm form, MultipartFile gpx) {
        Checked c = check(form, userId);
        boolean hasGpx = gpx != null && !gpx.isEmpty();
        String pathJson = hasGpx ? GpxParser.toJson(GpxParser.parse(gpx)) : null;

        long matchId = repo.insert(userId, form, c.mbti, pathJson);
        repo.replaceTags(matchId, c.ageGroups, c.styles, c.languages);
        repo.join(matchId, userId);   // 모집자도 참여자 목록에 자동으로

        if (hasGpx) {
            String gpxPath = "gpx/match-" + matchId + ".gpx";
            fileStore.save(gpx, gpxPath);
            repo.updateGpxPath(matchId, gpxPath);
        }
        return matchId;
    }

    @Transactional
    public void update(long userId, long matchId, MatchForm form) {
        MatchState m = hostOnly(matchId, userId);
        String status = m.displayStatus();
        if ("CANCELED".equals(status) || "DONE".equals(status)) {
            throw ApiException.conflict("CLOSED", "취소됐거나 끝난 동행은 수정할 수 없어요.");
        }
        Checked c = check(form, userId);
        if (form.capacity() < m.current()) {
            throw ApiException.badRequest("capacity: 지금 참여 인원(" + m.current() + "명)보다 작게 바꿀 수 없어요");
        }
        repo.update(matchId, form, c.mbti);
        repo.replaceTags(matchId, c.ageGroups, c.styles, c.languages);
    }

    /** 모집 취소 — 지우지 않고 CANCELED 로 */
    @Transactional
    public void cancel(long userId, long matchId) {
        MatchState m = hostOnly(matchId, userId);
        String status = m.displayStatus();
        if ("CANCELED".equals(status) || "DONE".equals(status)) {
            throw ApiException.conflict("CLOSED", "이미 취소됐거나 끝난 동행이에요.");
        }
        repo.updateStatus(matchId, "CANCELED");
    }

    /** 모집 마감 — CLOSED 로 */
    @Transactional
    public void close(long userId, long matchId) {
        MatchState m = hostOnly(matchId, userId);
        String status = m.displayStatus();
        if (!"OPEN".equals(status) && !"FULL".equals(status)) {
            throw ApiException.conflict("CLOSED", "모집 중인 동행만 마감할 수 있어요.");
        }
        repo.updateStatus(matchId, "CLOSED");
    }

    // ------------------------------------------------------------------
    // 참여 · 참여 취소 · 내보내기
    // ------------------------------------------------------------------

    /**
     * 참여하기. 마지막 자리를 두 명이 동시에 누르는 경우를 막으려고
     * 글 행을 잠근 뒤(FOR UPDATE) 인원 확인과 저장을 한 트랜잭션에서 합니다.
     */
    @Transactional
    public JoinResult join(long userId, long matchId) {
        MatchState m = repo.findState(matchId, true);
        if (m == null) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
        String mine = repo.findParticipantStatus(matchId, userId);
        if ("JOINED".equals(mine)) {
            throw ApiException.conflict("DUPLICATE", "이미 참여한 동행이에요.");
        }
        if ("KICKED".equals(mine)) {
            throw ApiException.forbidden("이 동행에는 다시 참여할 수 없어요.");
        }
        switch (m.displayStatus()) {
            case "FULL" -> throw ApiException.conflict("FULL", "정원이 다 찼어요.");
            case "CLOSED", "CANCELED", "DONE" -> throw ApiException.conflict("CLOSED", "모집이 끝난 동행이에요.");
            default -> { }
        }
        repo.join(matchId, userId);

        MatchState after = new MatchState(m.matchId(), m.hostId(), m.capacity(), m.meetAt(), m.savedStatus(),
                m.current() + 1);
        return new JoinResult(after.current(), after.capacity(), after.displayStatus());
    }

    /** 참여 취소. 모집자는 모집 취소를 써야 함 → 400 */
    @Transactional
    public void leave(long userId, long matchId, String reason) {
        MatchState m = repo.findState(matchId, true);
        if (m == null) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
        if (m.hostId() == userId) {
            throw ApiException.badRequest("모집자는 참여 취소 대신 모집 취소를 해 주세요.");
        }
        if (!"JOINED".equals(repo.findParticipantStatus(matchId, userId))) {
            throw ApiException.badRequest("참여 중인 동행이 아니에요.");
        }
        if ("DONE".equals(m.displayStatus())) {
            throw ApiException.conflict("CLOSED", "이미 끝난 동행이에요.");
        }
        repo.leave(matchId, userId, reason);
    }

    /** 참여자 내보내기 (모집자만). 내보낸 사람은 다시 참여 못 함 */
    @Transactional
    public void kick(long userId, long matchId, long targetUserId) {
        MatchState m = hostOnly(matchId, userId);
        if (targetUserId == m.hostId()) {
            throw ApiException.badRequest("모집자는 내보낼 수 없어요.");
        }
        if (!"JOINED".equals(repo.findParticipantStatus(matchId, targetUserId))) {
            throw ApiException.notFound("참여 중인 사람이 아니에요.");
        }
        repo.kick(matchId, targetUserId);
    }

    // ------------------------------------------------------------------
    // 댓글 (참여자 · 모집자만)
    // ------------------------------------------------------------------

    public PageResult<MatchComment> comments(long userId, long matchId, int page, int size) {
        participantOnly(matchId, userId);
        page = Math.max(page, 1);
        size = clampSize(size, 50);
        return new PageResult<>(repo.findComments(matchId, size, PageResult.offset(page, size)), page, size,
                repo.countComments(matchId));
    }

    @Transactional
    public MatchComment addComment(long userId, long matchId, CommentForm form) {
        MatchState m = participantOnly(matchId, userId);
        boolean notice = Boolean.TRUE.equals(form.isNotice()) && m.hostId() == userId;   // 공지는 모집자만
        long commentId = repo.insertComment(matchId, userId, form.content().trim(), notice);
        return repo.findComment(commentId);
    }

    // ------------------------------------------------------------------
    // 관리자 AD3
    // ------------------------------------------------------------------

    /** status: OPEN 모집 중(OPEN · FULL), CLOSED 마감(CLOSED · DONE · CANCELED), HIDDEN 숨김, 비우면 전체 */
    public PageResult<AdminMatchRow> adminList(String q, String status, int page, int size) {
        page = Math.max(page, 1);
        size = clampSize(size, 20);
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        where.add("TRUE");

        if (q != null && !q.isBlank()) {
            where.add("(m.title LIKE ? OR u.nickname LIKE ? OR c.name LIKE ?)");
            String like = "%" + q.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        String s = status == null ? "" : status.trim().toUpperCase();
        switch (s) {
            case "" -> { }
            case "OPEN" -> where.add("m.is_hidden = FALSE AND s.status IN ('OPEN', 'FULL')");
            case "CLOSED" -> where.add("m.is_hidden = FALSE AND s.status IN ('CLOSED', 'DONE', 'CANCELED')");
            case "HIDDEN" -> where.add("m.is_hidden = TRUE");
            default -> throw ApiException.badRequest("status: OPEN, CLOSED, HIDDEN 중 하나여야 해요");
        }
        String whereSql = String.join(" AND ", where);
        return new PageResult<>(repo.findAdminRows(whereSql, params, size, PageResult.offset(page, size)), page,
                size, repo.count(whereSql, params));
    }

    public void setHidden(long matchId, boolean hidden) {
        if (!repo.updateHidden(matchId, hidden)) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
    }

    // ------------------------------------------------------------------
    // 내부 도우미
    // ------------------------------------------------------------------

    /** 확인을 마친 입력값 (코드 정리 · MBTI 대문자) */
    private record Checked(String mbti, List<String> ageGroups, List<String> styles, List<String> languages) {
    }

    private Checked check(MatchForm f, long userId) {
        MatchCodes.check("target", f.target(), MatchCodes.TARGETS);
        String mbti = MatchCodes.upper(f.mbti());
        if ("MBTI".equals(f.target())) {
            if (mbti == null) {
                throw ApiException.badRequest("mbti: 모집 대상이 MBTI 맞춤이면 MBTI를 골라 주세요");
            }
        } else {
            mbti = null;   // MBTI 맞춤이 아니면 저장하지 않음 (DB 규칙)
        }
        List<String> ageGroups = MatchCodes.checkAll("ageGroups", f.ageGroups(), MatchCodes.AGE_GROUPS);
        List<String> styles = MatchCodes.checkAll("styles", f.styles(), MatchCodes.STYLES);
        List<String> languages = MatchCodes.checkAll("languages", f.languages(), MatchCodes.LANGUAGES);
        if (languages.isEmpty()) {
            throw ApiException.badRequest("languages: 1개 이상 골라 주세요");
        }
        if (!repo.isUsableCourse(f.courseId(), userId)) {
            throw ApiException.badRequest("courseId: 전체 공개 코스나 내가 올린 코스만 고를 수 있어요");
        }
        return new Checked(mbti, ageGroups, styles, languages);
    }

    /** 글이 있고(숨김 아님) 내가 모집자인지. 아니면 404 / 403 */
    private MatchState hostOnly(long matchId, long userId) {
        MatchState m = repo.findState(matchId, true);
        if (m == null) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
        if (m.hostId() != userId) {
            throw ApiException.forbidden("모집자만 할 수 있어요.");
        }
        return m;
    }

    /** 글이 있고 내가 참여 중(모집자 포함)인지. 아니면 404 / 403 */
    private MatchState participantOnly(long matchId, long userId) {
        MatchState m = repo.findState(matchId, false);
        if (m == null) {
            throw ApiException.notFound("모집 글을 찾을 수 없어요.");
        }
        if (!"JOINED".equals(repo.findParticipantStatus(matchId, userId))) {
            throw ApiException.forbidden("참여자만 댓글을 보고 쓸 수 있어요.");
        }
        return m;
    }

    private static void addTagFilter(List<String> where, List<Object> params, String type, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        where.add("EXISTS (SELECT 1 FROM match_tags t WHERE t.match_id = m.match_id AND t.tag_type = ? AND t.tag_value IN ("
                + MatchRepository.marks(values.size()) + "))");
        params.add(type);
        params.addAll(values);
    }

    private static String joinedBy() {
        return "EXISTS (SELECT 1 FROM match_participants p WHERE p.match_id = m.match_id AND p.user_id = ? AND p.status = 'JOINED')";
    }

    private static int clampSize(int size, int defaultSize) {
        if (size <= 0) {
            return defaultSize;
        }
        return Math.min(size, 100);
    }
}
