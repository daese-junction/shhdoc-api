package com.shhdoc.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @Schema(description = "로그인 때 받은 refreshToken")
        @NotBlank String refreshToken) {
}
