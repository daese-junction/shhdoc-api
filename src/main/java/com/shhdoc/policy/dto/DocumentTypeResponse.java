package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.DocumentType;

public record DocumentTypeResponse(Long id, Long categoryId, String code, String name, String description) {

    public static DocumentTypeResponse from(DocumentType documentType) {
        return new DocumentTypeResponse(documentType.getId(), documentType.getCategory().getId(),
                documentType.getCode(), documentType.getName(), documentType.getDescription());
    }
}
