package com.shhdoc.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentTypeRequest(
        @Schema(description = "소속 대분류 id")
        @NotNull Long categoryId,
        @Schema(description = "문서 유형 코드", example = "PAYROLL")
        @NotBlank String code,
        @Schema(description = "문서 유형 이름", example = "급여명세서")
        @NotBlank String name,
        @Schema(description = "설명. AI 분류 프롬프트에 힌트로 쓰이므로 구체적일수록 좋다.",
                example = "급여·공제 내역이 담긴 급여 명세 문서")
        String description
) {
}
