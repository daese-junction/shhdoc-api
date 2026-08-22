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
        Mail mail = mailStore.get(event.mailId());
        if (!mail.tryMarkProcessing()) {
            log.warn("mail {} is already being processed or done, skip duplicate event", event.mailId());
            return;
        }

        MailRequest request = mail.request();
        Policy policy = policyService.findByCompany(mail.companyId());

        List<AttachmentResult> attachmentResults = request.attachments().stream()
                .map(attachment -> decide(request, policy, attachment))
                .toList();

        mail.markDone();
        gateway.publishDecision(new DecisionResponse(mail.mailId(), attachmentResults));
    }

    private AttachmentResult decide(MailRequest request, Policy policy, Attachment attachment) {
        DocumentFile file = attachmentLoader.load(attachment);
        DocumentAnalysisResult docResult = documentAnalyzer.analyze(file);
        MailContext context = contextBuilder.build(request, docResult);
        Verdict verdict = decisionEngine.decide(context, policy);
        return new AttachmentResult(attachment.storageKey(), verdict.status(), verdict.reason());
    }
}
