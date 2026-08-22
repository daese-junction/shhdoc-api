package com.shhdoc.attachment;

import com.shhdoc.attachment.dto.AttachmentResponse;
import com.shhdoc.attachment.dto.DownloadUrlResponse;
import com.shhdoc.attachment.dto.RegisterAttachmentRequest;
import com.shhdoc.attachment.dto.UploadUrlRequest;
import com.shhdoc.attachment.dto.UploadUrlResponse;
import com.shhdoc.common.ApiException;
import com.shhdoc.email.Email;
import com.shhdoc.email.EmailRepository;
import com.shhdoc.storage.AttachmentStorage;
import com.shhdoc.user.Role;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {

    private static final String EMAIL_NOT_FOUND = "메일을 찾을 수 없습니다.";
    private static final String NOT_FOUND = "첨부파일을 찾을 수 없습니다.";

    private final AttachmentRepository attachmentRepository;
    private final EmailRepository emailRepository;
    private final AttachmentStorage storage;
    private final ApplicationEventPublisher eventPublisher;

    /** 1단계: 프론트가 파일을 직접 올릴 URL 을 발급한다. 아직 DB 에 남기지 않는다. */
    public UploadUrlResponse createUploadUrl(Long userId, Long emailId, UploadUrlRequest request) {
        requireEditableEmail(userId, emailId);
        AttachmentStorage.PresignedUpload upload = storage.presignUpload(request.filename());
        return new UploadUrlResponse(upload.storageKey(), upload.uploadUrl(), upload.expiresInSeconds());
    }

    /** 2단계: 업로드가 끝난 뒤 등록한다. 실제로 올라왔는지 확인하고 해시를 계산한다. */
    @Transactional
    public AttachmentResponse register(Long userId, Long emailId, RegisterAttachmentRequest request) {
        Email email = requireEditableEmail(userId, emailId);

        long size = storage.requireUploaded(request.storageKey());
        String hash = storage.sha256(request.storageKey());

        Attachment attachment = new Attachment(email, request.filename(), size, request.storageKey(), hash);
        // 같은 파일을 이미 검사했다면 판정을 그대로 가져온다 (ERD의 content_hash 재사용).
        // DONE 만 본다 — 실패(FAILED)를 물려받으면 그 파일이 영원히 재검사되지 않는다.
        attachmentRepository.findFirstByContentHashAndScanStatusAndEmailSenderCompanyIdOrderByIdAsc(
                        hash, ScanStatus.DONE, email.getSender().getCompany().getId())
                .ifPresent(attachment::reuseVerdictOf);

        Attachment saved = attachmentRepository.save(attachment);
        // 판정을 물려받았으면 다시 분석할 이유가 없다.
        if (saved.getScanStatus() == ScanStatus.PENDING) {
            eventPublisher.publishEvent(new AttachmentRegisteredEvent(saved.getId()));
        }
        return AttachmentResponse.from(saved);
    }

    /**
     * 판정을 지우고 다시 검사한다. 검사가 실패하면 그 첨부는 계속 보류라 발송이 막히는데,
     * 다시 시도할 길이 없으면 관리자 승인 말고는 풀 방법이 없다.
     */
    @Transactional
    public AttachmentResponse rescan(Long userId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND));
        requireEditableEmail(userId, attachment.getEmail().getId());

        if (attachment.getScanStatus() == ScanStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 검사 중입니다.");
        }
        attachment.resetScan();
        eventPublisher.publishEvent(new AttachmentRegisteredEvent(attachment.getId()));
        return AttachmentResponse.from(attachment);
    }

    public List<AttachmentResponse> list(Long userId, Long companyId, Role role, Long emailId) {
        requireViewableEmail(userId, companyId, role, emailId);
        return attachmentRepository.findByEmailIdOrderByIdAsc(emailId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    /** 발신자 본인과 같은 회사 관리자만 연다. 관리자는 승인 판단을 위해 열어봐야 한다. */
    public DownloadUrlResponse createDownloadUrl(Long userId, Long companyId, Role role, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND));
        requireViewableEmail(userId, companyId, role, attachment.getEmail().getId());

        AttachmentStorage.PresignedDownload download =
                storage.presignDownload(attachment.getStorageKey(), attachment.getFilename());
        return new DownloadUrlResponse(download.downloadUrl(), download.expiresInSeconds());
    }

    @Transactional
    public void delete(Long userId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND));
        requireEditableEmail(userId, attachment.getEmail().getId());

        attachmentRepository.delete(attachment);
        storage.delete(attachment.getStorageKey());
    }

    /** 첨부를 붙이거나 떼는 것은 내 메일이면서 아직 DRAFT 일 때만. */
    private Email requireEditableEmail(Long userId, Long emailId) {
        Email email = emailRepository.findByIdAndSenderId(emailId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, EMAIL_NOT_FOUND));
        email.requireDraft();
        return email;
    }

    private Email requireViewableEmail(Long userId, Long companyId, Role role, Long emailId) {
        return emailRepository.findByIdAndSenderId(emailId, userId)
                .or(() -> role == Role.ADMIN
                        ? emailRepository.findByIdAndSenderCompanyId(emailId, companyId)
                        : java.util.Optional.empty())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, EMAIL_NOT_FOUND));
    }
}
