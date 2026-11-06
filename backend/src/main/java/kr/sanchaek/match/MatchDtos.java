package kr.sanchaek.match;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 동행 댓글(W4) · 관리자 모집 글 관리(AD3) 의 요청 · 응답 모양 */
public final class MatchDtos {

    private MatchDtos() {
    }

    /** 응답에 넣는 사람 (탈퇴한 회원은 nickname 이 "탈퇴한 회원") */
    public record Author(long userId, String nickname, String avatar) {
    }

    // ----- 댓글 · 공지 -----

    /** POST /api/matches/{id}/comments. isNotice 는 모집자만 true 로 보낼 수 있음 (비우면 일반 댓글) */
    public record CommentRequest(
            @NotBlank(message = "댓글을 입력해 주세요.")
            @Size(max = 500, message = "댓글은 500자까지예요.")
            String content,
            Boolean isNotice) {
    }

    public record CommentItem(long commentId, Author author, String content, boolean isNotice, LocalDateTime createdAt) {
    }

    // ----- AD3 모집 글 관리 -----

    /** 표 한 줄. hidden 이면 화면 상태는 "숨김", 아니면 status (OPEN · FULL · CLOSED · CANCELED · DONE) */
    public record AdminMatchItem(long matchId, String title, Author host, LocalDateTime meetAt, String target,
            int current, int capacity, String status, boolean hidden) {
    }

    /** 탭 옆에 보이는 개수 (Figma AD3 "모집 중 24 · 마감 · 숨김 1"). 검색어가 있으면 그 검색 결과 안에서 셈 */
    public record AdminMatchCounts(long open, long closed, long hidden) {
    }

    /** GET /api/admin/matches 응답: 공통 목록 칸(items · page · size · total) + 탭 개수 */
    public record AdminMatchPage(List<AdminMatchItem> items, int page, int size, long total, AdminMatchCounts counts) {
    }

    /** PUT /api/admin/matches/{id}/visibility */
    public record VisibilityRequest(
            @NotNull(message = "숨길지(true) 복구할지(false) 보내 주세요.") Boolean hidden) {
    }

    public record VisibilityResult(long matchId, boolean hidden) {
    }
}
