package com.shhdoc.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "가입한 이메일", example = "alice@shhdoc.com")
        @NotBlank String email,

        @Schema(description = "비밀번호", example = "password123")
        @NotBlank String password) {
}
