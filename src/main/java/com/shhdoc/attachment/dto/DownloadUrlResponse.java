package com.shhdoc.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DownloadUrlResponse(
        @Schema(description = "브라우저에서 바로 열 수 있는 URL") String downloadUrl,
        long expiresInSeconds) {
}
