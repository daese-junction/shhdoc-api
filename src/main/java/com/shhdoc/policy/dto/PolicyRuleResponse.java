package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;

public record PolicyRuleResponse(
        Long id,
        String name,
        boolean enabled,
        Long categoryId,
        Long documentTypeId,
        Long sensitiveTypeId,
        Classification classification,
        SendDirection direction,
        RecipientScope recipientScope,
        PolicyAction action
) {

    public static PolicyRuleResponse from(PolicyRule rule) {
        return new PolicyRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.isEnabled(),
                rule.getCategory() == null ? null : rule.getCategory().getId(),
                rule.getDocumentType() == null ? null : rule.getDocumentType().getId(),
                rule.getSensitiveType() == null ? null : rule.getSensitiveType().getId(),
                rule.getClassification(),
                rule.getDirection(),
                rule.getRecipientScope(),
                rule.getAction());
    }
}
