package com.shhdoc.upstage;

import com.shhdoc.upstage.context.ContextBuilder;
import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.decision.DecisionEngine;
import com.shhdoc.upstage.decision.Verdict;
import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.document.DocumentAnalyzer;
import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.dto.AttachmentResult;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.mail.Mail;
import com.shhdoc.upstage.mail.MailReceivedEvent;
import com.shhdoc.upstage.mail.MailStore;
import com.shhdoc.upstage.pipeline.DocumentFile;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 전체 처리 오케스트레이터. {@link MailReceivedEvent}를 받아 첨부파일마다
 * 분석→정책조회→컨텍스트조립→판정을 거쳐 {@link Gateway#publishDecision}까지 호출한다.
 *
 * <p>메일끼리는 서로 순서를 지킬 필요가 없어서 워커 스레드풀({@link MailProcessorConfig})로
 * 동시에 여러 건 처리한다. 같은 mailId가 중복 처리되는 건 {@link Mail#tryMarkProcessing()}이
 * 막는다. 첨부파일 간은 지금 순차 처리 — 병렬화는 나중 최적화 여지로 남겨둠.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailProcessor {

    private final MailStore mailStore;
    private final PolicyService policyService;
    private final AttachmentLoader attachmentLoader;
    private final DocumentAnalyzer documentAnalyzer;
    private final ContextBuilder contextBuilder;
    private final DecisionEngine decisionEngine;
    private final Gateway gateway;

    @Async(MailProcessorConfig.EXECUTOR_BEAN_NAME)
    @EventListener
    public void handle(MailReceivedEvent event) {
        Mail mail = mailStore.get(event.requestId());
        if (!mail.tryMarkProcessing()) {
            log.warn("request {} is already being processed or done, skip duplicate event", event.requestId());
            return;
        }

        MailRequest request = mail.request();
        List<AttachmentResult> attachmentResults;
        try {
            Policy policy = policyService.findByCompany(mail.companyId());
            attachmentResults = request.attachments().stream()
                    .map(attachment -> decide(request, policy, attachment))
                    .toList();
        } catch (RuntimeException e) {
            log.error("mail {} 처리에 실패해 전체를 보류로 넘긴다", mail.mailId(), e);
            attachmentResults = request.attachments().stream().map(MailProcessor::unchecked).toList();
        }

        // 실패해도 결과는 반드시 발행한다. 여기서 빠져나가면 첨부가 PENDING 에 영구히 남아
        // 화면은 "검사 중"을 무한히 돌고 발송은 계속 막힌다.
        mail.markDone();
        mailStore.remove(mail.requestId());
        gateway.publishDecision(new DecisionResponse(mail.mailId(), attachmentResults));
    }

    /** 검사하지 못한 첨부는 통과가 아니라 보류다. 못 본 파일을 그냥 내보내면 안 된다. */
    private static AttachmentResult unchecked(Attachment attachment) {
        return new AttachmentResult(attachment.storageKey(), ScanStatus.REVIEW,
                "자동 검사를 완료하지 못했습니다. 관리자 확인이 필요합니다.");
    }

    private AttachmentResult decide(MailRequest request, Policy policy, Attachment attachment) {
        try {
            return analyze(request, policy, attachment);
        } catch (RuntimeException e) {
            log.error("첨부 {} 분석에 실패해 보류로 넘긴다", attachment.storageKey(), e);
            return unchecked(attachment);
        }
    }

    private AttachmentResult analyze(MailRequest request, Policy policy, Attachment attachment) {
        DocumentFile file = attachmentLoader.load(attachment);
        DocumentAnalysisResult docResult = documentAnalyzer.analyze(file);
        MailContext context = contextBuilder.build(request, docResult);
        Verdict verdict = decisionEngine.decide(context, policy);
        return new AttachmentResult(attachment.storageKey(), verdict.status(), verdict.reason());
    }
}
