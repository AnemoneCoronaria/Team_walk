package kr.sanchaek.match;

import java.time.LocalDateTime;

/** 관리자 AD3 모집 글 목록 한 줄 */
public record AdminMatchRow(
        long matchId,
        String title,
        Host host,
        LocalDateTime meetAt,
        String target,
        int current,
        int capacity,
        String status,
        boolean hidden) {

    public record Host(long userId, String nickname) {
    }
}
