package kr.sanchaek.match;

/** 참여하기 성공 응답 { current, capacity, status } */
public record JoinResult(int current, int capacity, String status) {
}
