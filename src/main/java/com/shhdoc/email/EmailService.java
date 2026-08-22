package com.shhdoc.email;

import com.shhdoc.common.ApiException;
import com.shhdoc.email.dto.CreateEmailRequest;
import com.shhdoc.email.dto.EmailDetailResponse;
import com.shhdoc.email.dto.EmailResponse;
import com.shhdoc.email.dto.RecipientDto;
import com.shhdoc.email.dto.ReviewRequest;
import com.shhdoc.email.dto.UpdateEmailRequest;
import com.shhdoc.user.User;
import com.shhdoc.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {

    private static final String NOT_FOUND = "메일을 찾을 수 없습니다.";

    private final EmailRepository emailRepository;
    private final UserRepository userRepository;

    @Transactional
    public EmailDetailResponse createDraft(Long userId, CreateEmailRequest request) {
        Email email = new Email(findUser(userId), request.subject(), request.body());
        email.replaceRecipients(toRecipients(email, request.recipientsOrEmpty()));
        return EmailDetailResponse.from(emailRepository.save(email));
    }

    // ponytail: 목록에서 수신자 수를 세느라 메일당 쿼리가 한 번씩 더 나간다.
    // 내 메일함이라 건수가 적어 그냥 둔다. 느려지면 @EntityGraph 로 묶는다.
    public List<EmailResponse> listMine(Long userId, EmailStatus status) {
        List<Email> emails = status == null
                ? emailRepository.findBySenderIdOrderByIdDesc(userId)
                : emailRepository.findBySenderIdAndStatusOrderByIdDesc(userId, status);
        return emails.stream().map(EmailResponse::from).toList();
    }

    public EmailDetailResponse getMine(Long userId, Long emailId) {
        return EmailDetailResponse.from(findMine(userId, emailId));
    }

    @Transactional
    public EmailDetailResponse update(Long userId, Long emailId, UpdateEmailRequest request) {
        Email email = findMine(userId, emailId);
        email.edit(request.subject(), request.body());
        email.replaceRecipients(toRecipients(email, request.recipientsOrEmpty()));
        return EmailDetailResponse.from(email);
    }

    @Transactional
    public void delete(Long userId, Long emailId) {
        Email email = findMine(userId, emailId);
        email.requireDraft();
        emailRepository.delete(email);
    }

    /** 발송 시도. 사외 수신자가 있으면 관리자 승인 대기로 넘어간다. */
    @Transactional
    public EmailDetailResponse send(Long userId, Long emailId) {
        Email email = findMine(userId, emailId);
        email.requireDraft();
        if (email.getRecipients().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "수신자를 한 명 이상 지정해주세요.");
        }

        // ponytail: 1단계는 사외 발송이면 무조건 보류. 첨부 판정이 붙으면
        // "이 메일에 REVIEW 판정 첨부가 있는가"로 조건만 갈아끼운다.
        if (email.hasExternalRecipient()) {
            email.markBlocked();
        } else {
            email.markSent();
        }
        return EmailDetailResponse.from(email);
    }

    public List<EmailResponse> queue(Long companyId, EmailStatus status) {
        return emailRepository.findBySenderCompanyIdAndStatusOrderByIdAsc(companyId, status).stream()
                .map(EmailResponse::from)
                .toList();
    }

    public EmailDetailResponse getForReview(Long companyId, Long emailId) {
        return EmailDetailResponse.from(findInCompany(companyId, emailId));
    }

    @Transactional
    public EmailDetailResponse approve(Long adminId, Long companyId, Long emailId, ReviewRequest request) {
        Email email = findInCompany(companyId, emailId);
        email.approve(findUser(adminId), noteOf(request));
        return EmailDetailResponse.from(email);
    }

    @Transactional
    public EmailDetailResponse reject(Long adminId, Long companyId, Long emailId, ReviewRequest request) {
        String note = noteOf(request);
        if (note == null || note.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "거절 사유를 입력해주세요. 발신자에게 그대로 보입니다.");
        }
        Email email = findInCompany(companyId, emailId);
        email.reject(findUser(adminId), note);
        return EmailDetailResponse.from(email);
    }

    /** 남의 메일은 404로 답한다. 존재 여부까지 알려줄 이유가 없다. */
    private Email findMine(Long userId, Long emailId) {
        return emailRepository.findByIdAndSenderId(emailId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND));
    }

    /** 관리자라도 자기 회사 메일만 볼 수 있다. */
    private Email findInCompany(Long companyId, Long emailId) {
        return emailRepository.findByIdAndSenderCompanyId(emailId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 계정입니다."));
    }

    private static String noteOf(ReviewRequest request) {
        return request == null ? null : request.note();
    }

    private static List<EmailRecipient> toRecipients(Email email, List<RecipientDto> recipients) {
        return recipients.stream()
                .map(dto -> new EmailRecipient(email, dto.address().trim().toLowerCase(), dto.type()))
                .toList();
    }
}
