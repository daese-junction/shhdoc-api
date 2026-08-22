package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;
import com.shhdoc.policy.service.PolicyRuleService.RuleData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 조건 필드는 전부 선택이며 비우면 "조건 없음"으로 취급된다. */
public record PolicyRuleRequest(
        @Schema(description = "규칙 이름", example = "급여명세서 사외 반출 차단")
        @NotBlank String name,
        @Schema(description = "대상 대분류 id. 비우면 모든 대분류.")
        Long categoryId,
        @Schema(description = "대상 문서 유형 id. 비우면 모든 유형.")
        Long documentTypeId,
        @Schema(description = "대상 민감정보 유형 id. 비우면 민감정보 무관.")
        Long sensitiveTypeId,
        @Schema(description = "보안등급 조건. 비우면 등급 무관.", example = "CONFIDENTIAL")
        Classification classification,
        @Schema(description = "발송 방향. OUTBOUND 는 사외 전체.", example = "OUTBOUND")
        @NotNull SendDirection direction,
        @Schema(description = "수신 범위 조건. direction=OUTBOUND 일 때만 지정 가능.", example = "PERSONAL_EMAIL")
        RecipientScope recipientScope,
        @Schema(description = "매치 시 판정", example = "BLOCK")
        @NotNull PolicyAction action
) {

    public RuleData toData() {
        return new RuleData(name, categoryId, documentTypeId, sensitiveTypeId,
                classification, direction, recipientScope, action);
    }
}
