package com.shhdoc.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadUrlResponse(
        @Schema(description = "이 값을 그대로 등록 API 에 넘긴다") String storageKey,
        @Schema(description = "여기로 파일을 PUT 한다. 앱 서버를 거치지 않는다.") String uploadUrl,
        long expiresInSeconds) {
}
