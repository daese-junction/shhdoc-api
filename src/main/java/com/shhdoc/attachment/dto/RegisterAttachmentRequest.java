package com.shhdoc.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RegisterAttachmentRequest(
        @Schema(description = "업로드 URL 발급 때 받은 값") @NotBlank String storageKey,
        @Schema(example = "내부_설계도.pdf") @NotBlank String filename) {
}
