package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.RecipientScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipientDomainRequest(
        @Schema(description = "이메일 도메인", example = "partner-corp.co.kr")
        @NotBlank String domain,
        @Schema(description = "수신 범위. PARTNER, PERSONAL_EMAIL 만 등록할 수 있다.", example = "PARTNER")
        @NotNull RecipientScope scope
) {
}
