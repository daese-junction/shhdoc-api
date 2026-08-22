package com.shhdoc.attachment;

import com.shhdoc.attachment.dto.AttachmentResponse;
import com.shhdoc.attachment.dto.DownloadUrlResponse;
import com.shhdoc.attachment.dto.RegisterAttachmentRequest;
import com.shhdoc.attachment.dto.UploadUrlRequest;
import com.shhdoc.attachment.dto.UploadUrlResponse;
import com.shhdoc.auth.UserPrincipal;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "첨부파일", description = """
        파일은 앱 서버를 거치지 않는다. 서버는 서명 URL 만 발급하고 프론트가 스토리지와 직접 주고받는다.

        업로드 순서: ① 업로드 URL 발급 → ② 그 URL 로 PUT → ③ 등록.
        ②를 건너뛰고 ③을 부르면 400 이다.
        """)
@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(
            summary = "① 업로드 URL 발급",
            description = """
                    받은 uploadUrl 로 파일을 그대로 PUT 한다 (Content-Type 외 헤더 불필요).
                    DRAFT 상태의 내 메일에만 첨부할 수 있다. URL 은 expiresInSeconds 뒤 만료된다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "DRAFT 가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 내 메일이 아님", content = @Content)
    })
    @PostMapping("/emails/{emailId}/attachments/upload-url")
    public UploadUrlResponse createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emailId,
            @Valid @RequestBody UploadUrlRequest request) {
        return attachmentService.createUploadUrl(principal.id(), emailId, request);
    }

    @Operation(
            summary = "③ 첨부 등록",
            description = """
                    업로드가 끝난 뒤 호출한다. 서버가 스토리지에 실제로 올라왔는지 확인하고
                    크기와 SHA-256 을 기록한다. 같은 해시의 파일이 이미 검사됐다면 판정을 재사용한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "업로드되지 않았거나 DRAFT 가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 내 메일이 아님", content = @Content)
    })
    @PostMapping("/emails/{emailId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse register(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emailId,
            @Valid @RequestBody RegisterAttachmentRequest request) {
        return attachmentService.register(principal.id(), emailId, request);
    }

    @Operation(summary = "첨부 목록", description = "발신자 본인과 같은 회사 관리자가 볼 수 있다.")
    @GetMapping("/emails/{emailId}/attachments")
    public List<AttachmentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emailId) {
        return attachmentService.list(principal.id(), principal.companyId(), principal.role(), emailId);
    }

    @Operation(
            summary = "다운로드 URL 발급",
            description = """
                    브라우저에서 바로 열 수 있는 URL 을 준다. 원본 파일명으로 내려받아진다.

                    발신자 본인과 **같은 회사 관리자**만 받을 수 있다. 관리자는 승인 판단을 위해 열어봐야 한다.
                    """)
    @ApiResponses(@ApiResponse(responseCode = "404", description = "없거나 볼 권한이 없음", content = @Content))
    @GetMapping("/attachments/{id}/download-url")
    public DownloadUrlResponse createDownloadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return attachmentService.createDownloadUrl(principal.id(), principal.companyId(), principal.role(), id);
    }

    @Operation(summary = "첨부 삭제", description = "DRAFT 상태의 내 메일에서만. 스토리지 객체도 함께 지운다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "DRAFT 가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "없거나 내 메일이 아님", content = @Content)
    })
    @DeleteMapping("/attachments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        attachmentService.delete(principal.id(), id);
    }
}
