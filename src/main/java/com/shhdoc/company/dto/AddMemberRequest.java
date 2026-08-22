package com.shhdoc.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자가 직원을 추가할 때. 초기 비밀번호는 관리자가 정한다. */
public record AddMemberRequest(
        @Schema(description = "직원 이메일. 반드시 회사 도메인이어야 한다.", example = "bob@shhdoc.com")
        @Email @NotBlank String email,

        @Schema(description = "관리자가 정하는 초기 비밀번호 (8~64자)", example = "password123")
        @NotBlank @Size(min = 8, max = 64) String password,

        @Schema(description = "직원 이름", example = "박직원")
        @NotBlank String name) {
}
