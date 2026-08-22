package com.shhdoc.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 승인·거절 공통. 거절은 사유가 발신자에게 보이므로 note 가 필수다. */
public record ReviewRequest(
        @Schema(description = "관리자 사유. 거절이면 필수이고 발신자에게 보인다.", example = "고객사 계약서라 발송 허용")
        String note) {
}
