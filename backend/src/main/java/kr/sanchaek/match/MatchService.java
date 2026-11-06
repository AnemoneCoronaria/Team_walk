package kr.sanchaek.match;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.PageResult;
import kr.sanchaek.course.GpxService;
import kr.sanchaek.match.MatchDtos.AdminMatchCounts;
import kr.sanchaek.match.MatchDtos.AdminMatchPage;
import kr.sanchaek.match.MatchDtos.CommentItem;
import kr.sanchaek.match.MatchDtos.CommentRequest;
import kr.sanchaek.match.MatchDtos.VisibilityResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * 동행 쓰기 규칙 (2주차: 모집 글 작성 · 수정 · 취소 · 마감, 참여 · 취소, 내보내기). 담당: 민서
 * 권한 · 상태 검사는 전부 여기서 하고, SQL 은 MatchRepository 에 둡니다.
 *
 * 상태 규칙 (상태는 v_match_status 뷰의 계산 값)
 * | 상태     | 참여 | 참여 취소 | 수정 · 모집 취소 | 마감 | 내보내기 |
 * | OPEN     | O    | O         | O                | O    | O        |
 * | FULL     | X    | O         | O                | O    | O        |
 * | CLOSED   | X    | O         | O                | X    | O        |
 * | CANCELED | X    | X         | X                | X    | X        |
 * | DONE     | X    | X         | X                | X    | X        |
 */
@Service
public class MatchService {

    private static final Set<String> ADMIN_TABS = Set.of("OPEN", "CLOSED", "HIDDEN");

    private final MatchRepository repo;
    private final GpxService gpxService;     // GPX 검사 · 경로 읽기는 코스 쪽 코드를 그대로 씀 (5MB, 300m, 점 20개)
    private final FileStore files;
    private final JsonMapper jsonMapper;

    public MatchService(MatchRepository repo, GpxService gpxService, FileStore files, JsonMapper jsonMapper) {
        this.repo = repo;
        this.gpxService = gpxService;
        this.files = files;
        this.jsonMapper = jsonMapper;
    }

    // ----- 상세 (W2 · W2-2 · W4) -----

    /**
     * 상세 + 보는 사람 기준 정보.
     * isHost(내가 모집자인지), myStatus(JOINED · LEFT · KICKED · null),
     * joinable / reason (참여 버튼을 켤지, 끌 때 이유. Figma W2-2) — 참여하기와 같은 검사를 써서 화면과 서버 판단이 같음
     * 로그인 안 했으면 조건(연령대 · MBTI · 언어)은 검사하지 않음 → 참여 누르면 프론트가 로그인 모달(G1)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(long matchId, Long userId) {
        Map<String, Object> match = repo.findMatchById(matchId);
        MatchState state = repo.findState(matchId, false);
        String myStatus = userId == null ? null : repo.findParticipantStatus(matchId, userId);
        String reason = joinBlockReason(state, userId, myStatus);
        match.put("isHost", userId != null && userId == state.hostId());
        match.put("myStatus", myStatus);
        match.put("joinable", reason == null);
        match.put("reason", reason);
        return match;
    }

    // ----- 모집 글 (W3 · W4) -----

    /** 모집 글 쓰기. 모집자도 참여자 1명으로 같이 들어감. GPX 첨부는 선택 */
    @Transactional
    public long create(long userId, MatchRequest request, MultipartFile gpx) {
        MatchRequest r = request.cleaned();
        checkRequest(r);
        checkCourse(r.courseId(), userId);
        GpxService.Result route = parseGpx(gpx);     // 파일이 잘못됐으면 아무것도 저장하기 전에 400

        long matchId = repo.insertMatch(userId, r);
        repo.replaceTags(matchId, r);
        repo.upsertJoined(matchId, userId);
        if (route != null) {
            saveGpx(matchId, gpx, route);
        }
        return matchId;
    }

    /** 모집 글 수정 (모집자만). 정원은 지금 참여 인원보다 적게 줄일 수 없음 */
    @Transactional
    public void update(long matchId, long userId, MatchRequest request, MultipartFile gpx) {
        MatchRequest r = request.cleaned();
        checkRequest(r);
        MatchState state = repo.findState(matchId, true);
        checkHost(state, userId);
        checkNotFinished(state);
        if (r.capacity() < state.current()) {
            throw ApiException.conflict("CAPACITY_TOO_SMALL",
                    "지금 참여 인원(" + state.current() + "명)보다 적게 줄일 수 없어요.");
        }
        checkCourse(r.courseId(), userId);
        GpxService.Result route = parseGpx(gpx);

        repo.updateMatch(matchId, r);
        repo.replaceTags(matchId, r);
        if (route != null) {
            saveGpx(matchId, gpx, route);
        } else if (Boolean.TRUE.equals(r.removeGpx()) && state.gpxPath() != null) {
            repo.updateGpx(matchId, null, null);
            files.delete(state.gpxPath());
        }
    }

    /** 모집 취소 (모집자만). 글은 지우지 않고 CANCELED 로 (db/README "지우지 않습니다") */
    @Transactional
    public void cancel(long matchId, long userId) {
        MatchState state = repo.findState(matchId, true);
        checkHost(state, userId);
        checkNotFinished(state);
        repo.updateStatus(matchId, "CANCELED");
    }

    /** 모집 마감 (모집자만). 모집 중(OPEN) 이거나 정원이 찬(FULL) 글만 */
    @Transactional
    public void close(long matchId, long userId) {
        MatchState state = repo.findState(matchId, true);
        checkHost(state, userId);
        checkNotFinished(state);
        if ("CLOSED".equals(state.status())) {
            throw ApiException.conflict("ALREADY_CLOSED", "이미 마감한 모집 글이에요.");
        }
        repo.updateStatus(matchId, "CLOSED");
    }

    // ----- 참여 (W2-1 · W5 · W4) -----

    /** 참여하기. 글을 잠근 다음 검사하고 넣어서, 마지막 자리에 두 명이 동시에 눌러도 한 명만 들어감 */
    @Transactional
    public void join(long matchId, long userId) {
        MatchState state = repo.findState(matchId, true);
        String reason = joinBlockReason(state, userId, repo.findParticipantStatus(matchId, userId));
        if (reason != null) {
            throw joinError(reason);
        }
        repo.upsertJoined(matchId, userId);
    }

    /** 참여 취소. 사유는 모집자에게 전달됨 (W5). 모집자는 참여 취소 대신 모집 취소를 써야 함 */
    @Transactional
    public void leave(long matchId, long userId, String reason) {
        MatchState state = repo.findState(matchId, true);
        if (state.hostId() == userId) {
            throw ApiException.conflict("HOST_CANNOT_LEAVE", "모집자는 참여를 취소할 수 없어요. 모집 취소를 이용해 주세요.");
        }
        if (!"JOINED".equals(repo.findParticipantStatus(matchId, userId))) {
            throw ApiException.conflict("NOT_JOINED", "참여 중인 동행이 아니에요.");
        }
        checkNotFinished(state);
        String text = reason == null || reason.isBlank() ? null : reason.trim();
        repo.updateParticipantStatus(matchId, userId, "LEFT", text);
    }

    /** 내보내기 (모집자만). 내보내진 사람은 이 글에 다시 참여할 수 없음 */
    @Transactional
    public void kick(long matchId, long userId, long targetUserId) {
        MatchState state = repo.findState(matchId, true);
        checkHost(state, userId);
        if (targetUserId == userId) {
            throw ApiException.badRequest("모집자 자신은 내보낼 수 없어요.");
        }
        if (!"JOINED".equals(repo.findParticipantStatus(matchId, targetUserId))) {
            throw ApiException.conflict("NOT_JOINED", "참여 중인 사람이 아니에요.");
        }
        checkNotFinished(state);
        repo.updateParticipantStatus(matchId, targetUserId, "KICKED", null);
    }

    // ----- 댓글 · 공지 (W4) -----

    /** 댓글 목록. 모집자와 참여 중(JOINED)인 사람만 볼 수 있음 (db/README "동행 댓글 목록") */
    @Transactional(readOnly = true)
    public PageResult<CommentItem> comments(long matchId, long userId, int page, int size) {
        checkCanComment(repo.findState(matchId, false), userId);
        return repo.findComments(matchId, page, size);
    }

    /** 댓글 쓰기. 공지(isNotice)는 모집자만 */
    @Transactional
    public long addComment(long matchId, long userId, CommentRequest request) {
        MatchState state = repo.findState(matchId, false);
        checkCanComment(state, userId);
        boolean notice = Boolean.TRUE.equals(request.isNotice());
        if (notice && state.hostId() != userId) {
            throw ApiException.forbidden("공지는 모집자만 쓸 수 있어요.");
        }
        return repo.insertComment(matchId, userId, request.content().trim(), notice);
    }

    private void checkCanComment(MatchState state, long userId) {
        if (state.hostId() != userId && !"JOINED".equals(repo.findParticipantStatus(state.matchId(), userId))) {
            throw ApiException.forbidden("참여자만 볼 수 있어요.");
        }
    }

    // ----- 관리자 AD3 모집 글 관리 -----

    /** 모집 글 목록 · 검색. status(탭): OPEN 모집 중 · CLOSED 마감 · HIDDEN 숨김 */
    @Transactional(readOnly = true)
    public AdminMatchPage adminMatches(String q, String status, int page, int size) {
        if (!ADMIN_TABS.contains(status)) {
            throw ApiException.badRequest("status 는 OPEN · CLOSED · HIDDEN 중 하나예요.");
        }
        String keyword = q == null || q.isBlank() ? null : q.trim();
        AdminMatchCounts counts = repo.countAdminMatches(keyword);
        long total = switch (status) {
            case "CLOSED" -> counts.closed();
            case "HIDDEN" -> counts.hidden();
            default -> counts.open();
        };
        return new AdminMatchPage(repo.findAdminMatches(keyword, status, size, PageResult.offset(page, size)),
                page, size, total, counts);
    }

    /** 숨김(true) · 복구(false). 숨기면 목록에서 빠지고 상세는 404 */
    public VisibilityResult changeHidden(long matchId, boolean hidden) {
        if (!repo.updateHidden(matchId, hidden)) {
            throw ApiException.notFound("동행 찾을 수 없음");
        }
        return new VisibilityResult(matchId, hidden);
    }

    // ----- 검사 도우미 -----

    /**
     * 참여할 수 없는 이유. 참여할 수 있으면 null.
     * HOST · ALREADY_JOINED · KICKED (나와의 관계) → FULL · CLOSED · CANCELED · DONE (글 상태)
     * → AGE_MISMATCH · MBTI_MISMATCH · LANGUAGE_MISMATCH (조건, Figma W2-2) 순서로 봄
     */
    private String joinBlockReason(MatchState state, Long userId, String myStatus) {
        if (userId != null && userId == state.hostId()) {
            return "HOST";
        }
        if ("JOINED".equals(myStatus)) {
            return "ALREADY_JOINED";
        }
        if ("KICKED".equals(myStatus)) {
            return "KICKED";
        }
        if (!"OPEN".equals(state.status())) {
            return state.status();
        }
        if (userId == null) {
            return null;
        }
        MatchRepository.Viewer me = repo.findViewer(userId);
        if (!state.ageGroups().isEmpty() && !state.ageGroups().contains(me.ageGroup())) {
            return "AGE_MISMATCH";
        }
        if ("MBTI".equals(state.target()) && !state.mbti().equals(me.mbti())) {
            return "MBTI_MISMATCH";
        }
        if ("SAME_LANGUAGE".equals(state.target()) && Collections.disjoint(state.languages(), me.languages())) {
            return "LANGUAGE_MISMATCH";
        }
        return null;
    }

    /** 참여할 수 없는 이유 → 응답 (상태 코드 · 에러 코드 · 메시지) */
    private static ApiException joinError(String reason) {
        return switch (reason) {
            case "HOST", "ALREADY_JOINED" -> ApiException.conflict("ALREADY_JOINED", "이미 참여 중인 동행이에요.");
            case "KICKED" -> new ApiException(403, "KICKED", "모집자가 내보낸 동행에는 다시 참여할 수 없어요.");
            case "FULL" -> ApiException.conflict("FULL", "정원이 다 찼어요.");
            case "CLOSED" -> ApiException.conflict("CLOSED", "모집이 마감된 동행이에요.");
            case "CANCELED" -> ApiException.conflict("CANCELED", "모집이 취소된 동행이에요.");
            case "DONE" -> ApiException.conflict("DONE", "이미 끝난 동행이에요.");
            case "AGE_MISMATCH" -> new ApiException(403, reason, "모집 연령대가 내 연령대와 맞지 않아요.");
            case "MBTI_MISMATCH" -> new ApiException(403, reason, "모집하는 MBTI 와 내 MBTI 가 맞지 않아요.");
            case "LANGUAGE_MISMATCH" -> new ApiException(403, reason, "모집 언어 중에 내가 쓰는 언어가 없어요.");
            default -> throw new IllegalStateException("알 수 없는 이유: " + reason);
        };
    }

    private static void checkHost(MatchState state, long userId) {
        if (state.hostId() != userId) {
            throw ApiException.forbidden("모집자만 할 수 있어요.");
        }
    }

    /** 취소됐거나 끝난 글은 더 바꿀 수 없음 */
    private static void checkNotFinished(MatchState state) {
        if ("CANCELED".equals(state.status())) {
            throw ApiException.conflict("CANCELED", "모집이 취소된 동행이에요.");
        }
        if ("DONE".equals(state.status())) {
            throw ApiException.conflict("DONE", "이미 끝난 동행이에요.");
        }
    }

    /** 칸끼리 맞춰 봐야 하는 규칙 (record 의 @Valid 로는 칸 하나씩만 검사됨) */
    private static void checkRequest(MatchRequest r) {
        if ("MBTI".equals(r.target()) && r.mbti() == null) {
            throw ApiException.badRequest("MBTI 맞춤 모집은 원하는 MBTI 를 골라 주세요.");
        }
    }

    private void checkCourse(long courseId, long userId) {
        if (!repo.isCourseSelectable(courseId, userId)) {
            throw ApiException.badRequest("고를 수 없는 코스예요.");
        }
    }

    /** 첨부 파일이 없으면 null */
    private GpxService.Result parseGpx(MultipartFile gpx) {
        return gpx == null || gpx.isEmpty() ? null : gpxService.parse(gpx);
    }

    /** 업로드 폴더의 gpx/match-{번호}.gpx 에 그대로 저장하고, DB 에는 경로와 좌표 (db/README "GPX") */
    private void saveGpx(long matchId, MultipartFile gpx, GpxService.Result route) {
        String path = "gpx/match-" + matchId + ".gpx";
        files.save(gpx, path);
        repo.updateGpx(matchId, path, jsonMapper.writeValueAsString(route.path()));
    }
}
