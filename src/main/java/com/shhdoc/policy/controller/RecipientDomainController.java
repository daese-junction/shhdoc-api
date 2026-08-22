package com.shhdoc.policy.controller;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.policy.dto.RecipientDomainRequest;
import com.shhdoc.policy.dto.RecipientDomainResponse;
import com.shhdoc.policy.service.RecipientDomainService;
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

@Tag(name = "정책 - 수신자 도메인", description = """
        수신자 이메일 도메인을 수신 범위(PARTNER/PERSONAL_EMAIL)로 매핑한다.
        회사 도메인(INTERNAL)과 미등록(EXTERNAL)은 자동 판정이라 등록 대상이 아니다. ADMIN 전용.""")
@RestController
@RequestMapping("/admin/policy/domains")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RecipientDomainController {

    private final RecipientDomainService domainService;

    @Operation(summary = "도메인 매핑 목록")
    @GetMapping
    public List<RecipientDomainResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return domainService.list(principal.companyId()).stream()
                .map(RecipientDomainResponse::from)
                .toList();
    }

    @Operation(summary = "도메인 매핑 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "PARTNER/PERSONAL_EMAIL 외 scope", content = @Content),
            @ApiResponse(responseCode = "409", description = "도메인 중복", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipientDomainResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecipientDomainRequest request) {
        return RecipientDomainResponse.from(
                domainService.create(principal.companyId(), request.domain(), request.scope()));
    }

    @Operation(summary = "도메인 매핑 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "PARTNER/PERSONAL_EMAIL 외 scope", content = @Content),
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "도메인 중복", content = @Content)
    })
    @PutMapping("/{domainId}")
    public RecipientDomainResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long domainId,
            @Valid @RequestBody RecipientDomainRequest request) {
        return RecipientDomainResponse.from(
                domainService.update(principal.companyId(), domainId, request.domain(), request.scope()));
    }

    @Operation(summary = "도메인 매핑 삭제")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "대상 없음", content = @Content))
    @DeleteMapping("/{domainId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long domainId) {
        domainService.delete(principal.companyId(), domainId);
    }
}
