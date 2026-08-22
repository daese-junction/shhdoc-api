package com.shhdoc.email;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.email.dto.EmailDetailResponse;
import com.shhdoc.email.dto.EmailResponse;
import com.shhdoc.email.dto.ReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 메일 승인", description = "사외 발송으로 보류된 메일을 승인하거나 거절한다. 자기 회사 메일만 보인다.")
@RestController
@RequestMapping("/admin/emails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailController {

    private final EmailService emailService;

    @Operation(
            summary = "승인 대기열",
            description = "기본은 BLOCKED(승인 대기). status 를 바꾸면 처리된 메일도 볼 수 있다. 오래된 것부터 나온다.")
    @ApiResponses(@ApiResponse(responseCode = "403", description = "ADMIN 아님", content = @Content))
    @GetMapping
    public List<EmailResponse> queue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "BLOCKED") EmailStatus status) {
        return emailService.queue(principal.companyId(), status);
    }

    @Operation(summary = "검토할 메일 상세", description = "본문과 수신자를 보고 판단한다.")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "없거나 다른 회사 메일", content = @Content))
    @GetMapping("/{id}")
    public EmailDetailResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return emailService.getForReview(principal.companyId(), id);
    }

    @Operation(summary = "승인", description = "SENT 로 바뀌고 발송 시각이 기록된다. 사유(note)는 선택이다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "승인 대기 상태가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 다른 회사 메일", content = @Content)
    })
    @PostMapping("/{id}/approve")
    public EmailDetailResponse approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequest request) {
        return emailService.approve(principal.id(), principal.companyId(), id, request);
    }

    @Operation(summary = "거절", description = "REJECTED 로 바뀐다. 사유는 필수이며 발신자에게 그대로 보인다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "사유가 없거나 승인 대기 상태가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 다른 회사 메일", content = @Content)
    })
    @PostMapping("/{id}/reject")
    public EmailDetailResponse reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequest request) {
        return emailService.reject(principal.id(), principal.companyId(), id, request);
    }
}
