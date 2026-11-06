package kr.sanchaek.user;

import jakarta.validation.constraints.NotNull;

/** PUT /api/users/me/consents 위치 정보 이용 동의 (R1-1 모달) */
public record ConsentRequest(
        @NotNull(message = "위치 정보 동의 여부를 보내 주세요.") Boolean agreeLocation) {
}
