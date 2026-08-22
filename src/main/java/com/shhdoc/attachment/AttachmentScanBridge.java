package com.shhdoc.attachment;

import com.shhdoc.email.Email;
import com.shhdoc.upstage.Gateway;
import com.shhdoc.upstage.dto.AttachmentResult;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.dto.ScanStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 첨부 도메인과 분석 모듈(upstage)을 잇는 유일한 지점. 양방향 모두 여기만 지난다.
 *
 * <p>보낼 때는 {@link Gateway#enqueue}, 받을 때는 {@link DecisionResponse} 이벤트다.
 * 다른 클래스가 upstage 를 직접 호출하지 않도록 변환도 전부 여기서 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentScanBridge {

    private final AttachmentRepository attachmentRepository;
    private final Gateway gateway;

    /**
     * 커밋 이후에 검사를 요청한다. 첨부 한 건이 요청 하나다.
     *
     * <p>트랜잭션을 새로 연다. 커밋 이후에 도는 리스너라 세션이 이미 닫혀 있어서,
     * 그냥 조회하면 detached 엔티티가 나오고 email.getSender() 에서 터진다.
     * 기동 시 보정({@link PendingScanRecovery})도 트랜잭션 밖에서 부르므로 여기서 열어야 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener
    public void requestScan(AttachmentRegisteredEvent event) {
        attachmentRepository.findById(event.attachmentId())
                .ifPresent(attachment -> gateway.enqueue(toMailRequest(attachment)));
    }

    /** 분석이 끝나면 판정을 첨부 행에 기록한다. 워커 스레드에서 실행되므로 트랜잭션을 직접 연다. */
    @Transactional
    @org.springframework.context.event.EventListener
    public void applyDecision(DecisionResponse response) {
        for (AttachmentResult result : response.attachments()) {
            attachmentRepository.findByStorageKey(result.storageKey()).ifPresentOrElse(
                    attachment -> apply(attachment, result),
                    () -> log.warn("판정이 왔지만 첨부를 찾지 못함: storageKey={}", result.storageKey()));
        }
    }

    private static void apply(Attachment attachment, AttachmentResult result) {
        if (result.status() == ScanStatus.FAILED) {
            attachment.recordFailure(result.reason());
            return;
        }
        attachment.recordVerdict(toVerdict(result.status()), result.reason());
    }

    private static MailRequest toMailRequest(Attachment attachment) {
        Email email = attachment.getEmail();
        return new MailRequest(
                email.getId(),
                email.getSender().getCompany().getId(),
                email.getSenderAddress(),
                email.getSender().getId(),
                email.getSubject(),
                email.getBody(),
                email.getRecipients().stream().map(r -> new Recipient(r.getAddress())).toList(),
                List.of(new com.shhdoc.upstage.dto.Attachment(
                        attachment.getFilename(),
                        attachment.getSizeBytes(),
                        attachment.getStorageKey(),
                        attachment.getContentHash())));
    }

    /** upstage 는 ALLOW/REVIEW, 첨부 도메인은 ALLOWED/BLOCKED 로 부른다. */
    private static Verdict toVerdict(ScanStatus status) {
        return status == ScanStatus.ALLOW ? Verdict.ALLOWED : Verdict.BLOCKED;
    }
}
