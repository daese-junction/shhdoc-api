package com.shhdoc.email;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.email.dto.CreateEmailRequest;
import com.shhdoc.email.dto.EmailDetailResponse;
import com.shhdoc.email.dto.EmailResponse;
import com.shhdoc.email.dto.UpdateEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메일", description = "내 메일 작성과 발송. 사외 수신자가 있으면 발송이 관리자 승인 대기로 넘어간다.")
@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "초안 생성", description = "수신자 없이도 만들 수 있다. 발송할 때 한 명 이상이면 된다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmailDetailResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEmailRequest request) {
        return emailService.createDraft(principal.id(), request);
    }

    @Operation(summary = "내 메일 목록", description = "status 로 거를 수 있다. 남이 쓴 메일은 보이지 않는다.")
    @GetMapping
    public List<EmailResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) EmailStatus status) {
        return emailService.listMine(principal.id(), status);
    }

    @Operation(summary = "메일 상세", description = "수신자 목록과, 거절된 메일이면 관리자 사유가 함께 온다.")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "없거나 내 메일이 아님", content = @Content))
    @GetMapping("/{id}")
    public EmailDetailResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return emailService.getMine(principal.id(), id);
    }

    @Operation(summary = "초안 수정", description = "DRAFT 일 때만 가능하다. 수신자 목록은 통째로 교체된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "DRAFT 가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 내 메일이 아님", content = @Content)
    })
    @PatchMapping("/{id}")
    public EmailDetailResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailRequest request) {
        return emailService.update(principal.id(), id, request);
    }

    @Operation(summary = "초안 삭제", description = "DRAFT 일 때만 가능하다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        emailService.delete(principal.id(), id);
    }

    @Operation(
            summary = "발송",
            description = """
                    수신자가 모두 사내면 바로 SENT 가 된다.

                    사외 수신자가 하나라도 있으면 BLOCKED 로 바뀌어 관리자 승인 대기열에 올라간다.
                    응답의 status 로 어느 쪽인지 확인한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SENT 또는 BLOCKED"),
            @ApiResponse(responseCode = "400", description = "DRAFT 가 아니거나 수신자가 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 내 메일이 아님", content = @Content)
    })
    @PostMapping("/{id}/send")
    public EmailDetailResponse send(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return emailService.send(principal.id(), id);
    }
}
