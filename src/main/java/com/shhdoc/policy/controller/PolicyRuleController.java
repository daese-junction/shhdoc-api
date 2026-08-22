package com.shhdoc.policy.controller;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.policy.dto.PolicyRuleRequest;
import com.shhdoc.policy.dto.PolicyRuleResponse;
import com.shhdoc.policy.dto.RuleEnabledRequest;
import com.shhdoc.policy.service.PolicyRuleService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "정책 - 반출 규칙", description = """
        문서 반출 규칙 관리. 조건(대분류/문서 유형/민감정보/보안등급/발송 방향/수신 범위)을 조합하고
        매치 시 판정(ALLOW/REVIEW/BLOCK)을 정한다. 비운 조건은 "무관"이다. ADMIN 전용.""")
@RestController
@RequestMapping("/admin/policy/rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PolicyRuleController {

    private final PolicyRuleService ruleService;

    @Operation(summary = "규칙 목록")
    @GetMapping
    public List<PolicyRuleResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ruleService.list(principal.companyId()).stream()
                .map(PolicyRuleResponse::from)
                .toList();
    }

    @Operation(summary = "규칙 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "direction 없이 recipientScope 지정 등 잘못된 조합",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "조건으로 지정한 분류·유형 없음", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyRuleResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PolicyRuleRequest request) {
        return PolicyRuleResponse.from(ruleService.create(principal.companyId(), request.toData()));
    }

    @Operation(summary = "규칙 수정", description = "조건 전체를 요청 값으로 덮어쓴다(부분 수정 아님).")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "잘못된 조건 조합", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 없음 또는 조건 대상 없음", content = @Content)
    })
    @PutMapping("/{ruleId}")
    public PolicyRuleResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long ruleId,
            @Valid @RequestBody PolicyRuleRequest request) {
        return PolicyRuleResponse.from(ruleService.update(principal.companyId(), ruleId, request.toData()));
    }

    @Operation(summary = "규칙 사용/중지")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "대상 없음", content = @Content))
    @PatchMapping("/{ruleId}/enabled")
    public PolicyRuleResponse changeEnabled(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long ruleId,
            @Valid @RequestBody RuleEnabledRequest request) {
        return PolicyRuleResponse.from(
                ruleService.changeEnabled(principal.companyId(), ruleId, request.enabled()));
    }

    @Operation(summary = "규칙 삭제")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "대상 없음", content = @Content))
    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long ruleId) {
        ruleService.delete(principal.companyId(), ruleId);
    }
}
