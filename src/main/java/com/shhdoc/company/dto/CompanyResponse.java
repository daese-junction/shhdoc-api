package com.shhdoc.company.dto;

import com.shhdoc.company.Company;
import io.swagger.v3.oas.annotations.media.Schema;

public record CompanyResponse(
        Long id,

        @Schema(example = "쉿닥")
        String name,

        @Schema(description = "이 회사 계정이 쓰는 고정 이메일 도메인", example = "shhdoc.com")
        String emailDomain) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getEmailDomain());
    }
}
