package com.shhdoc.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shhdoc.company.Company;
import com.shhdoc.email.Email;
import com.shhdoc.upstage.Gateway;
import com.shhdoc.upstage.dto.AttachmentResult;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 분석 모듈의 판정이 실제로 첨부 행에 반영되는지 확인한다. */
@ExtendWith(MockitoExtension.class)
class AttachmentScanBridgeTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private Gateway gateway;

    @Mock
    private com.shhdoc.email.EmailService emailService;

    @InjectMocks
    private AttachmentScanBridge bridge;

    /** 검사 요청은 id 를 실어 보내므로, DB 에서 읽어온 것처럼 채워둔다. */
    private static Attachment attachment() {
        Company company = new Company("쉿닥", "shhdoc.com");
        ReflectionTestUtils.setField(company, "id", 10L);
        User sender = new User(company, "bob@shhdoc.com", "hashed", "박영업", Role.USER);
        ReflectionTestUtils.setField(sender, "id", 20L);
        Email email = new Email(sender, "계약서 송부", "확인 부탁드립니다.");
        ReflectionTestUtils.setField(email, "id", 30L);
        Attachment attachment = new Attachment(email, "계약서.pdf", 1024L, "key-1", "hash-1");
        ReflectionTestUtils.setField(attachment, "id", 7L);
        return attachment;
    }

    @Test
    void ALLOW_판정은_ALLOWED_로_기록된다() {
        Attachment attachment = attachment();
        given(attachmentRepository.findByStorageKey("key-1")).willReturn(Optional.of(attachment));

        bridge.applyDecision(new DecisionResponse(1L,
                List.of(new AttachmentResult("key-1", ScanStatus.ALLOW, "공개 가능한 문서입니다."))));

        assertThat(attachment.getVerdict()).isEqualTo(Verdict.ALLOWED);
        // import 한 ScanStatus 는 upstage 쪽(ALLOW/REVIEW)이라 여기서는 첨부 쪽을 명시한다.
        assertThat(attachment.getScanStatus()).isEqualTo(com.shhdoc.attachment.ScanStatus.DONE);
        assertThat(attachment.getReason()).isEqualTo("공개 가능한 문서입니다.");
        assertThat(attachment.getScannedAt()).isNotNull();
    }

    @Test
    void REVIEW_판정은_BLOCKED_로_기록된다() {
        Attachment attachment = attachment();
        given(attachmentRepository.findByStorageKey("key-1")).willReturn(Optional.of(attachment));

        bridge.applyDecision(new DecisionResponse(1L,
                List.of(new AttachmentResult("key-1", ScanStatus.REVIEW, "급여 정보가 포함되어 있습니다."))));

        assertThat(attachment.getVerdict()).isEqualTo(Verdict.BLOCKED);
        assertThat(attachment.getReason()).isEqualTo("급여 정보가 포함되어 있습니다.");
    }

    /**
     * 검사 실패는 차단이 아니다. DONE/BLOCKED 로 굳으면 판정이 난 것처럼 보여
     * 재검사 대상에서 빠지고, 아무도 열어보지 못한 파일이 통과 판정으로 남는다.
     */
    @Test
    void 검사_실패는_FAILED_로_기록되고_판정은_비워둔다() {
        Attachment attachment = attachment();
        given(attachmentRepository.findByStorageKey("key-1")).willReturn(Optional.of(attachment));

        bridge.applyDecision(new DecisionResponse(1L, List.of(new AttachmentResult(
                "key-1", ScanStatus.FAILED, "자동 검사를 완료하지 못했습니다. 관리자 확인이 필요합니다."))));

        assertThat(attachment.getScanStatus()).isEqualTo(com.shhdoc.attachment.ScanStatus.FAILED);
        assertThat(attachment.getVerdict()).isNull();
        assertThat(attachment.getReason()).isEqualTo("자동 검사를 완료하지 못했습니다. 관리자 확인이 필요합니다.");
    }

    /**
     * 판정을 기록한 뒤 메일도 다시 본다. 승인 대기에 묶인 메일의 마지막 보류 사유가
     * 이 판정으로 사라졌을 수 있고, 그러면 발송함으로 풀어줘야 한다.
     */
    @Test
    void 판정을_기록한_뒤_메일_보류가_풀렸는지_확인한다() {
        Attachment attachment = attachment();
        given(attachmentRepository.findByStorageKey("key-1")).willReturn(Optional.of(attachment));

        bridge.applyDecision(new DecisionResponse(30L,
                List.of(new AttachmentResult("key-1", ScanStatus.ALLOW, "공개 가능한 문서입니다."))));

        verify(emailService).releaseIfCleared(30L);
    }

    /** 첨부가 지워진 뒤에 판정이 돌아올 수 있다. 그때 터지면 같은 응답의 나머지 첨부까지 날아간다. */
    @Test
    void 없는_첨부의_판정은_무시한다() {
        given(attachmentRepository.findByStorageKey("사라진키")).willReturn(Optional.empty());

        bridge.applyDecision(new DecisionResponse(1L,
                List.of(new AttachmentResult("사라진키", ScanStatus.ALLOW, "-"))));
    }

    @Test
    void 검사_요청은_첨부_한_건을_담아_보낸다() {
        Attachment attachment = attachment();
        given(attachmentRepository.findById(7L)).willReturn(Optional.of(attachment));

        bridge.requestScan(new AttachmentRegisteredEvent(7L));

        ArgumentCaptor<MailRequest> captor = ArgumentCaptor.forClass(MailRequest.class);
        verify(gateway).enqueue(captor.capture());
        MailRequest request = captor.getValue();
        assertThat(request.senderAddress()).isEqualTo("bob@shhdoc.com");
        assertThat(request.attachments()).singleElement()
                .satisfies(a -> {
                    assertThat(a.storageKey()).isEqualTo("key-1");
                    assertThat(a.fileName()).isEqualTo("계약서.pdf");
                    assertThat(a.hash()).isEqualTo("hash-1");
                });
    }

    @Test
    void 이미_지워진_첨부는_검사를_요청하지_않는다() {
        given(attachmentRepository.findById(7L)).willReturn(Optional.empty());

        bridge.requestScan(new AttachmentRegisteredEvent(7L));

        verify(gateway, never()).enqueue(any());
    }
}
