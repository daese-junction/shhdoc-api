package com.shhdoc.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;

/** 초안 생성. 수신자는 비워둘 수 있고, 발송할 때 한 명 이상이면 된다. */
public record CreateEmailRequest(
        @Schema(example = "3분기 실적 공유") String subject,
        @Schema(example = "첨부 확인 부탁드립니다.") String body,
        @Valid List<RecipientDto> recipients) {

    public List<RecipientDto> recipientsOrEmpty() {
        return recipients == null ? List.of() : recipients;
    }
}
