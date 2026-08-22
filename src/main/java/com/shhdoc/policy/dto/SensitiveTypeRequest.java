package com.shhdoc.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SensitiveTypeRequest(
        @Schema(description = "민감정보 유형 코드", example = "PERSONAL")
        @NotBlank String code,
        @Schema(description = "민감정보 유형 이름", example = "개인정보")
        @NotBlank String name,
        @Schema(description = "설명. AI 탐지 프롬프트에 힌트로 쓰인다.",
                example = "주민등록번호, 연락처, 주소, 계좌번호 등 개인 식별 정보")
        String description
) {
}
