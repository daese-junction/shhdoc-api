package com.shhdoc.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @Schema(description = "대분류 코드", example = "HR")
        @NotBlank String code,
        @Schema(description = "대분류 이름", example = "인사/노무")
        @NotBlank String name
) {
}
