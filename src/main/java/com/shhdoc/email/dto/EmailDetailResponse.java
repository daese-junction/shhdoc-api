package com.shhdoc.email.dto;

import com.shhdoc.email.Email;
import com.shhdoc.email.EmailStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record EmailDetailResponse(
        Long id,
        String senderAddress,
        String subject,
        String body,
        @Schema(description = "DRAFT | BLOCKED | REJECTED | SENT") EmailStatus status,
        List<RecipientDto> recipients,
        @Schema(description = "관리자 사유. 거절된 메일이면 여기에 이유가 있다.") String reviewNote,
        Instant reviewedAt,
        Instant sentAt,
        Instant createdAt) {

    public static EmailDetailResponse from(Email email) {
        return new EmailDetailResponse(
                email.getId(),
                email.getSenderAddress(),
                email.getSubject(),
                email.getBody(),
                email.getStatus(),
                email.getRecipients().stream().map(RecipientDto::from).toList(),
                email.getReviewNote(),
                email.getReviewedAt(),
                email.getSentAt(),
                email.getCreatedAt());
    }
}
