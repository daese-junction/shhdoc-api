package com.shhdoc.email.dto;

import jakarta.validation.Valid;
import java.util.List;

/** 초안 수정. 수신자 목록은 통째로 교체된다. */
public record UpdateEmailRequest(
        String subject,
        String body,
        List<@Valid RecipientDto> recipients) {

    public List<RecipientDto> recipientsOrEmpty() {
        return recipients == null ? List.of() : recipients;
    }
}
