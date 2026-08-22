package com.shhdoc.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UploadUrlRequest(
        @Schema(description = "원본 파일명", example = "내부_설계도.pdf")
        @NotBlank String filename) {
}
