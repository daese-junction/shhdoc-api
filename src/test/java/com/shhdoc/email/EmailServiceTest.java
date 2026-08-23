package com.shhdoc.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.Company;
import com.shhdoc.email.dto.CreateEmailRequest;
import com.shhdoc.email.dto.RecipientDto;
import com.shhdoc.email.dto.ReviewRequest;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import com.shhdoc.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** 사외 판정과 소유권·회사 경계가 실제로 막히는지 확인한다. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final Company COMPANY = new Company("쉿닥", "shhdoc.com");

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.shhdoc.attachment.AttachmentRepository attachmentRepository;

    @InjectMocks
    private EmailService emailService;

    private static User sender() {
        return new User(COMPANY, "bob@shhdoc.com", "hashed", "bob", Role.USER);
    }

    private static Email draftTo(String... addresses) {
        Email email = new Email(sender(), "제목", "본문");
        email.replaceRecipients(List.of(addresses).stream()
                .map(address -> new EmailRecipient(email, address, RecipientType.TO))
                .toList());
        return email;
    }

    @Test
    void 사내_수신자만_있으면_바로_발송된다() {
        given(emailRepository.findByIdAndSenderId(1L, 1L))
                .willReturn(Optional.of(draftTo("carol@shhdoc.com")));

        var response = emailService.send(1L, 1L);

        assertThat(response.status()).isEqualTo(EmailStatus.SENT);
        assertThat(response.sentAt()).isNotNull();
    }

    /**
     * 승인 트리거는 수신자가 아니라 첨부 판정이다. 사외 수신자는 첨부를 검사할 때
     * {@code ContextBuilder} 가 recipientType 으로 반영하고, 위험하면 그때 BLOCKED 가 찍힌다.
     */
    @Test
    void 첨부가_깨끗하면_사외_수신자가_있어도_바로_발송된다() {
        given(emailRepository.findByIdAndSenderId(1L, 1L))
                .willReturn(Optional.of(draftTo("carol@shhdoc.com", "partner@example.com")));

        var response = emailService.send(1L, 1L);

        assertThat(response.status()).isEqualTo(EmailStatus.SENT);
        assertThat(response.sentAt()).isNotNull();
    }

    @Test
    void 차단_판정_첨부가_있으면_승인_대기로_간다() {
        given(emailRepository.findByIdAndSenderId(1L, 1L))
                .willReturn(Optional.of(draftTo("partner@example.com")));
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.PENDING)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.FAILED)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndVerdict(
                1L, com.shhdoc.attachment.Verdict.BLOCKED)).willReturn(true);

        var response = emailService.send(1L, 1L);

        assertThat(response.status()).isEqualTo(EmailStatus.BLOCKED);
        assertThat(response.sentAt()).isNull();
    }

    /**
     * 검사에 실패한 첨부는 판정이 비어 있어 BLOCKED 조건에 안 걸린다. 따로 막지 않으면
     * 아무도 열어보지 못한 파일이 사내 발송으로 그냥 나간다.
     */
    @Test
    void 검사에_실패한_첨부가_있으면_사내라도_승인_대기로_간다() {
        given(emailRepository.findByIdAndSenderId(1L, 1L))
                .willReturn(Optional.of(draftTo("carol@shhdoc.com")));
        // 검사 중(PENDING) 여부를 먼저 묻는다. 안 채워두면 strict stub 이 인자 불일치로 막는다.
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.PENDING)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.FAILED)).willReturn(true);

        var response = emailService.send(1L, 1L);

        assertThat(response.status()).isEqualTo(EmailStatus.BLOCKED);
        assertThat(response.sentAt()).isNull();
    }

    private Email blockedTo(String... addresses) {
        Email email = draftTo(addresses);
        email.markBlocked();
        return email;
    }

    @Test
    void 판정이_늦게_깨끗해지면_승인_대기_메일이_발송함으로_풀린다() {
        Email email = blockedTo("partner@example.com");
        given(emailRepository.findById(1L)).willReturn(Optional.of(email));
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.PENDING)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.FAILED)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndVerdict(
                1L, com.shhdoc.attachment.Verdict.BLOCKED)).willReturn(false);

        emailService.releaseIfCleared(1L);

        assertThat(email.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(email.getSentAt()).isNotNull();
    }

    @Test
    void 차단_첨부가_남아_있으면_승인_대기_그대로_둔다() {
        Email email = blockedTo("partner@example.com");
        given(emailRepository.findById(1L)).willReturn(Optional.of(email));
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.PENDING)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndScanStatus(
                1L, com.shhdoc.attachment.ScanStatus.FAILED)).willReturn(false);
        given(attachmentRepository.existsByEmailIdAndVerdict(
                1L, com.shhdoc.attachment.Verdict.BLOCKED)).willReturn(true);

        emailService.releaseIfCleared(1L);

        assertThat(email.getStatus()).isEqualTo(EmailStatus.BLOCKED);
    }

    /** 아직 보내기를 누르지 않은 메일이다. 검사가 끝났다고 대신 발송하면 안 된다. */
    @Test
    void 초안은_검사가_끝나도_자동_발송하지_않는다() {
        Email email = draftTo("partner@example.com");
        given(emailRepository.findById(1L)).willReturn(Optional.of(email));

        emailService.releaseIfCleared(1L);

        assertThat(email.getStatus()).isEqualTo(EmailStatus.DRAFT);
    }

    @Test
    void 수신자가_없으면_발송할_수_없다() {
        given(emailRepository.findByIdAndSenderId(1L, 1L)).willReturn(Optional.of(draftTo()));

        assertThatThrownBy(() -> emailService.send(1L, 1L))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 이미_보낸_메일은_다시_발송할_수_없다() {
        Email email = draftTo("carol@shhdoc.com");
        email.markSent();
        given(emailRepository.findByIdAndSenderId(1L, 1L)).willReturn(Optional.of(email));

        assertThatThrownBy(() -> emailService.send(1L, 1L))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 남의_메일은_존재_여부도_알려주지_않는다() {
        given(emailRepository.findByIdAndSenderId(1L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> emailService.send(99L, 1L))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void 다른_회사_관리자는_승인할_수_없다() {
        given(emailRepository.findByIdAndSenderCompanyId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> emailService.approve(9L, 2L, 1L, new ReviewRequest("확인")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void 거절_사유가_없으면_거절할_수_없다() {
        assertThatThrownBy(() -> emailService.reject(9L, 1L, 1L, new ReviewRequest("  ")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(emailRepository, never()).findByIdAndSenderCompanyId(any(), any());
    }

    @Test
    void 승인_대기가_아닌_메일은_승인할_수_없다() {
        Email email = draftTo("partner@example.com");
        given(emailRepository.findByIdAndSenderCompanyId(1L, 1L)).willReturn(Optional.of(email));
        given(userRepository.findById(9L)).willReturn(Optional.of(sender()));

        assertThatThrownBy(() -> emailService.approve(9L, 1L, 1L, new ReviewRequest("확인")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 초안은_수신자_없이도_만들_수_있다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(sender()));
        given(emailRepository.save(any(Email.class))).willAnswer(call -> call.getArgument(0));

        var response = emailService.createDraft(1L, new CreateEmailRequest("제목", "본문", null));

        assertThat(response.status()).isEqualTo(EmailStatus.DRAFT);
        assertThat(response.recipients()).isEmpty();
    }

    @Test
    void 수신자_주소는_소문자로_저장된다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(sender()));
        given(emailRepository.save(any(Email.class))).willAnswer(call -> call.getArgument(0));

        var response = emailService.createDraft(1L, new CreateEmailRequest("제목", "본문",
                List.of(new RecipientDto("Partner@Example.com", RecipientType.TO))));

        assertThat(response.recipients().getFirst().address()).isEqualTo("partner@example.com");
    }
}
