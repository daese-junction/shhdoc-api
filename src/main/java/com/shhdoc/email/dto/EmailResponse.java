package com.shhdoc.email.dto;

import com.shhdoc.email.Email;
import com.shhdoc.email.EmailStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 목록용 요약. 본문·수신자는 상세에서 준다. */
public record EmailResponse(
        Long id,
        String subject,
        @Schema(description = "DRAFT | BLOCKED | REJECTED | SENT") EmailStatus status,
        int recipientCount,
        Instant createdAt,
        Instant sentAt) {

    public static EmailResponse from(Email email) {
        return new EmailResponse(
                email.getId(),
                email.getSubject(),
                email.getStatus(),
                email.getRecipients().size(),
                email.getCreatedAt(),
                email.getSentAt());
    }
}
