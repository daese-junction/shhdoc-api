package com.shhdoc.auth.dto;

import com.shhdoc.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "30분 유효. Authorization: Bearer {accessToken} 으로 보낸다.")
        String accessToken,

        @Schema(description = "7일 유효. accessToken 이 만료되면 /auth/refresh 에 보낸다.")
        String refreshToken,

        UserResponse user) {
}
