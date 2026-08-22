package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.DocumentCategory;

public record CategoryResponse(Long id, String code, String name) {

    public static CategoryResponse from(DocumentCategory category) {
        return new CategoryResponse(category.getId(), category.getCode(), category.getName());
    }
}
