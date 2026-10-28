package kr.sanchaek.match;

/** 모집자 · 댓글 작성자 칸. 탈퇴한 회원은 nickname 이 "탈퇴한 회원" */
public record UserBrief(long userId, String nickname, String avatar) {
}
