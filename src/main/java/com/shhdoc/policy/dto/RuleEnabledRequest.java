package com.shhdoc.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RuleEnabledRequest(
        @Schema(description = "규칙 사용 여부", example = "false")
        @NotNull Boolean enabled
) {
}
