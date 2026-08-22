package com.shhdoc.policy.controller;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.policy.dto.SensitiveTypeRequest;
import com.shhdoc.policy.dto.SensitiveTypeResponse;
import com.shhdoc.policy.service.SensitiveInfoTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "정책 - 민감정보 유형", description = "회사별 민감정보 유형 관리. description 은 AI 탐지 힌트로 쓰인다. ADMIN 전용.")
@RestController
@RequestMapping("/admin/policy/sensitive-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SensitiveInfoTypeController {

    private final SensitiveInfoTypeService sensitiveTypeService;

    @Operation(summary = "민감정보 유형 목록")
    @GetMapping
    public List<SensitiveTypeResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return sensitiveTypeService.list(principal.companyId()).stream()
                .map(SensitiveTypeResponse::from)
                .toList();
    }

    @Operation(summary = "민감정보 유형 추가")
    @ApiResponses(@ApiResponse(responseCode = "409", description = "코드 중복", content = @Content))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensitiveTypeResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SensitiveTypeRequest request) {
        return SensitiveTypeResponse.from(sensitiveTypeService.create(principal.companyId(),
                request.code(), request.name(), request.description()));
    }

    @Operation(summary = "민감정보 유형 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "코드 중복", content = @Content)
    })
    @PutMapping("/{sensitiveTypeId}")
    public SensitiveTypeResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sensitiveTypeId,
            @Valid @RequestBody SensitiveTypeRequest request) {
        return SensitiveTypeResponse.from(sensitiveTypeService.update(principal.companyId(), sensitiveTypeId,
                request.code(), request.name(), request.description()));
    }

    @Operation(summary = "민감정보 유형 삭제", description = "이 유형을 조건으로 쓰는 규칙이 있으면 거부된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "참조 규칙 존재", content = @Content)
    })
    @DeleteMapping("/{sensitiveTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sensitiveTypeId) {
        sensitiveTypeService.delete(principal.companyId(), sensitiveTypeId);
    }
}
