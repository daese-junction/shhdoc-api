package com.shhdoc.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shhdoc.attachment.dto.RegisterAttachmentRequest;
import com.shhdoc.attachment.dto.UploadUrlRequest;
import com.shhdoc.common.ApiException;
import com.shhdoc.company.Company;
import com.shhdoc.email.Email;
import com.shhdoc.email.EmailRepository;
import com.shhdoc.storage.AttachmentStorage;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    private static final Company COMPANY = new Company("쉿닥", "shhdoc.com");

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private AttachmentStorage storage;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttachmentService attachmentService;

    private static Email draft() {
        return new Email(new User(COMPANY, "bob@shhdoc.com", "hash", "bob", Role.USER), "제목", "본문");
    }

    @Test
    void 발송된_메일에는_첨부를_붙일_수_없다() {
        Email sent = draft();
        sent.markSent();
        given(emailRepository.findByIdAndSenderId(1L, 1L)).willReturn(Optional.of(sent));

        assertThatThrownBy(() -> attachmentService.createUploadUrl(1L, 1L, new UploadUrlRequest("a.pdf")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(storage, never()).presignUpload(anyString());
    }

    @Test
    void 남의_메일에는_업로드_URL_이_안_나온다() {
        given(emailRepository.findByIdAndSenderId(1L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.createUploadUrl(99L, 1L, new UploadUrlRequest("a.pdf")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(storage, never()).presignUpload(anyString());
    }

    /** 재사용을 걷어냈다. 이미 검사한 해시든 처음 보는 파일이든 똑같이 PENDING 으로 등록된다. */
    @Test
    void 이미_검사한_해시여도_다시_검사_대기_상태로_등록된다() {
        Email email = draft();
        given(emailRepository.findByIdAndSenderId(1L, 1L)).willReturn(Optional.of(email));
        given(storage.requireUploaded("key-2")).willReturn(2048L);
        given(storage.sha256("key-2")).willReturn("hash-abc");
        given(attachmentRepository.save(any(Attachment.class))).willAnswer(call -> call.getArgument(0));

        var response = attachmentService.register(1L, 1L,
                new RegisterAttachmentRequest("key-2", "새로_올린.pdf"));

        assertThat(response.scanStatus()).isEqualTo(ScanStatus.PENDING);
        assertThat(response.verdict()).isNull();
        assertThat(response.sizeBytes()).isEqualTo(2048L);
        verify(eventPublisher).publishEvent(any(AttachmentRegisteredEvent.class));
    }

    @Test
    void 일반_직원은_남의_첨부를_열_수_없다() {
        Email email = draft();
        Attachment attachment = new Attachment(email, "a.pdf", 1L, "key", "hash");
        given(attachmentRepository.findById(1L)).willReturn(Optional.of(attachment));
        given(emailRepository.findByIdAndSenderId(null, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.createDownloadUrl(99L, 1L, Role.USER, 1L))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(emailRepository, never()).findByIdAndSenderCompanyId(any(), any());
    }
}
