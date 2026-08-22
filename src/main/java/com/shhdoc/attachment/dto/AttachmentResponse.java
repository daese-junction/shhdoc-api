package com.shhdoc.attachment.dto;

import com.shhdoc.attachment.Attachment;
import com.shhdoc.attachment.ScanStatus;
import com.shhdoc.attachment.Verdict;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String filename,
        Long sizeBytes,
        @Schema(description = "PENDING | DONE | FAILED") ScanStatus scanStatus,
        @Schema(description = "ALLOWED | BLOCKED. 검사 전에는 null") Verdict verdict,
        @Schema(description = "판정 근거") String reason,
        Instant createdAt) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getSizeBytes(),
                attachment.getScanStatus(),
                attachment.getVerdict(),
                attachment.getReason(),
                attachment.getCreatedAt());
    }
}
