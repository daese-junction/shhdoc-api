package com.shhdoc.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 대표자의 최초 가입. 회사와 ADMIN 계정이 함께 만들어진다. */
public record CreateCompanyRequest(
        @Schema(description = "회사 이름", example = "쉿닥")
        @NotBlank String companyName,

        @Schema(description = "회사 이메일 도메인. 이후 모든 직원 계정이 이 도메인을 써야 한다.",
                example = "shhdoc.com")
        @NotBlank String emailDomain,

        @Schema(description = "대표자 이메일. emailDomain 과 같은 도메인이어야 한다.",
                example = "alice@shhdoc.com")
        @Email @NotBlank String email,

        @Schema(description = "대표자 비밀번호 (8~64자)", example = "password123")
        @NotBlank @Size(min = 8, max = 64) String password,

        @Schema(description = "대표자 이름", example = "김대표")
        @NotBlank String name,
        @Schema(description = "부서 (선택)", example = "경영지원")
        @Size(max = 50) String department,
        @Schema(description = "직급 (선택)", example = "대표")
        @Size(max = 50) String position) {
}
