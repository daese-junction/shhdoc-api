package com.shhdoc.upstage;

import com.shhdoc.upstage.context.ContextBuilder;
import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.decision.DecisionEngine;
import com.shhdoc.upstage.decision.Verdict;
import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.document.DocumentAnalyzer;
import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.QueueStatus;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.mail.Mail;
import com.shhdoc.upstage.mail.MailReceivedEvent;
import com.shhdoc.upstage.mail.MailStore;
import com.shhdoc.upstage.pipeline.DocumentFile;
import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.parse.ParsedContent;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailProcessorTest {

    @Mock
    private PolicyService policyService;
    @Mock
    private AttachmentLoader attachmentLoader;
    @Mock
    private DocumentAnalyzer documentAnalyzer;
    @Mock
    private ContextBuilder contextBuilder;
    @Mock
    private DecisionEngine decisionEngine;
    @Mock
    private Gateway gateway;

    private final MailStore mailStore = new MailStore();
    private MailProcessor mailProcessor;

    @BeforeEach
    void setUp() {
        mailProcessor = new MailProcessor(mailStore, policyService, attachmentLoader,
                documentAnalyzer, contextBuilder, decisionEngine, gateway);
    }

    private MailRequest requestWithOneAttachment() {
        Attachment attachment = new Attachment("f.pdf", 100, "storage-key-1", "hash");
        return new MailRequest(1, 100, "a@a.com", 1, "제목", "본문", List.of(), List.of(attachment));
    }

    @Test
    void 정상흐름_첨부파일마다_판정하고_DONE으로_바꾸고_publishDecision을_호출한다() {
        Mail mail = new Mail(requestWithOneAttachment());
        mailStore.save(mail);

        DocumentFile file = new DocumentFile("f.pdf", new byte[0]);
        DocumentAnalysisResult docResult = new DocumentAnalysisResult(
                new ParsedDocument(new ParsedContent("", "", ""), List.of(), 1),
                new ClassificationResult("payslip", 0.9),
                new ExtractionResult(List.of(), true, false, ""));
        MailContext context = new MailContext("a@a.com", List.of(), null, "payslip", List.of(), true, false, "");
        Policy policy = new Policy(100, List.of());
        Verdict verdict = new Verdict(ScanStatus.REVIEW, "사유");

        when(attachmentLoader.load(any())).thenReturn(file);
        when(documentAnalyzer.analyze(file)).thenReturn(docResult);
        when(contextBuilder.build(any(), any())).thenReturn(context);
        when(policyService.findByCompany(100)).thenReturn(policy);
        when(decisionEngine.decide(context, policy)).thenReturn(verdict);

        mailProcessor.handle(new MailReceivedEvent(1));

        assertThat(mail.status()).isEqualTo(QueueStatus.DONE);
        verify(gateway).publishDecision(new DecisionResponse(1,
                List.of(new com.shhdoc.upstage.dto.AttachmentResult("storage-key-1", ScanStatus.REVIEW, "사유"))));
    }

    /**
     * 분석이 터져도 결과는 반드시 나가야 한다. 여기서 빠져나가면 첨부가 PENDING 에 영구히
     * 남아 화면은 "검사 중"을 무한히 돌고 발송은 계속 막힌다.
     */
    @Test
    void 분석이_실패해도_보류_판정으로_결과를_발행한다() {
        Mail mail = new Mail(requestWithOneAttachment());
        mailStore.save(mail);

        when(policyService.findByCompany(100)).thenReturn(new Policy(100, List.of()));
        when(attachmentLoader.load(any())).thenThrow(new RuntimeException("스토리지 접근 실패"));

        mailProcessor.handle(new MailReceivedEvent(1));

        assertThat(mail.status()).isEqualTo(QueueStatus.DONE);
        verify(gateway).publishDecision(new DecisionResponse(1,
                List.of(new com.shhdoc.upstage.dto.AttachmentResult("storage-key-1", ScanStatus.REVIEW,
                        "자동 검사를 완료하지 못했습니다. 관리자 확인이 필요합니다."))));
    }

    /** 정책 조회처럼 첨부 루프 밖에서 터지는 경우도 마찬가지다. */
    @Test
    void 정책_조회가_실패해도_보류_판정으로_결과를_발행한다() {
        Mail mail = new Mail(requestWithOneAttachment());
        mailStore.save(mail);

        when(policyService.findByCompany(100)).thenThrow(new RuntimeException("정책 없음"));

        mailProcessor.handle(new MailReceivedEvent(1));

        assertThat(mail.status()).isEqualTo(QueueStatus.DONE);
        verify(gateway).publishDecision(new DecisionResponse(1,
                List.of(new com.shhdoc.upstage.dto.AttachmentResult("storage-key-1", ScanStatus.REVIEW,
                        "자동 검사를 완료하지 못했습니다. 관리자 확인이 필요합니다."))));
        verifyNoInteractions(documentAnalyzer, contextBuilder, decisionEngine);
    }

    @Test
    void 이미_처리중인_메일이면_아무것도_안하고_스킵한다() {
        Mail mail = new Mail(requestWithOneAttachment());
        mail.tryMarkProcessing();
        mailStore.save(mail);

        mailProcessor.handle(new MailReceivedEvent(1));

        verifyNoInteractions(documentAnalyzer, contextBuilder, decisionEngine);
        verify(gateway, never()).publishDecision(any());
    }
}
