package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.SensitiveInfoType;

public record SensitiveTypeResponse(Long id, String code, String name, String description) {

    public static SensitiveTypeResponse from(SensitiveInfoType sensitiveType) {
        return new SensitiveTypeResponse(sensitiveType.getId(), sensitiveType.getCode(),
                sensitiveType.getName(), sensitiveType.getDescription());
    }
}
