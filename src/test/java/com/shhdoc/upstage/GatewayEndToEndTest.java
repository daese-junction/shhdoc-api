package com.shhdoc.upstage;

import com.shhdoc.TestcontainersConfiguration;
import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.pipeline.DocumentFile;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.PolicyService;
import com.shhdoc.upstage.policy.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Gateway.enqueue()부터 publishDecision()까지 실제로 동작하는지 확인하는 e2e 테스트.
 * 스토리지(MinIO)와 정책 데이터(실 DB)는 컨테이너/시드 데이터 없이 각각
 * {@link AttachmentLoader}/{@link PolicyService}만 테스트용으로 바꿔치기한다 —
 * pipeline(실 Upstage API)/큐/판정 로직은 전부 실제.
 *
 * <p>5건을 전부 먼저 enqueue한 다음 한꺼번에 기다린다 — {@code MailProcessor}의
 * 워커풀(4개)이 실제로 동시에 여러 메일을 처리하는지까지 검증한다 (한 건씩 순차로
 * enqueue→대기를 반복하면 워커풀은 있으나마나라 동시처리가 검증 안 됨).
 *
 * <p>{@code UPSTAGE_API_KEY} 없으면 전체 스킵.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, GatewayEndToEndTest.StubAttachmentLoaderConfig.class,
        GatewayEndToEndTest.StubPolicyServiceConfig.class})
@EnabledIfEnvironmentVariable(named = "UPSTAGE_API_KEY", matches = ".+")
@RecordApplicationEvents
class GatewayEndToEndTest {

    private static final String SENDER = "sender@company.com";
    private static final String INTERNAL_RECIPIENT = "colleague@company.com";
    private static final String EXTERNAL_RECIPIENT = "recipient@external-partner.com";

    @Autowired
    private Gateway gateway;

    @Autowired
    private ApplicationEvents events;

    private record DocumentCase(
            Long mailId, String fileName, String storageKey, String recipientAddress, ScanStatus expectedStatus) {
    }

    private static final List<DocumentCase> CASES = List.of(
            // 급여명세서, 외부발송: mock 정책상 payslip+외부(미분류) 룰 → REVIEW
            new DocumentCase(1001L, "sample-payslip.pdf", "storage-key-payslip-ext", EXTERNAL_RECIPIENT, ScanStatus.REVIEW),
            // 급여명세서, 내부발송(같은 도메인): payslip+internal 룰 → ALLOW
            new DocumentCase(1005L, "sample-payslip.pdf", "storage-key-payslip-int", INTERNAL_RECIPIENT, ScanStatus.ALLOW),
            // 계약서, 외부발송: payslip 전용 룰 안 걸림 → 와일드카드 폴백 → ALLOW
            new DocumentCase(1002L, "sample-contract.pdf", "storage-key-contract", EXTERNAL_RECIPIENT, ScanStatus.ALLOW),
            // 공지문, 외부발송: 민감정보 없음 → ALLOW (false positive 안 나는지 검증)
            new DocumentCase(1003L, "sample-notice.pdf", "storage-key-notice", EXTERNAL_RECIPIENT, ScanStatus.ALLOW),
            // 사업계획서, 외부발송: 개인정보 없이 재무+대외비만 → ALLOW (payslip과 다른 신호조합)
            new DocumentCase(1004L, "sample-business-plan.pdf", "storage-key-business-plan", EXTERNAL_RECIPIENT, ScanStatus.ALLOW)
    );

    @Test
    void enqueue부터_publishDecision까지_5건을_동시에_처리한다() {
        for (DocumentCase testCase : CASES) {
            Attachment attachment = new Attachment(testCase.fileName(), 1L, testCase.storageKey(), "fake-hash");
            MailRequest request = new MailRequest(
                    testCase.mailId(), 1L, SENDER, 1L, "제목", "본문",
                    List.of(new Recipient(testCase.recipientAddress())),
                    List.of(attachment)
            );
            gateway.enqueue(request);
        }

        await().atMost(Duration.ofSeconds(60))
                .until(() -> events.stream(DecisionResponse.class).count() >= CASES.size());

        List<DecisionResponse> published = events.stream(DecisionResponse.class).toList();
        System.out.println(published);

        assertThat(published).hasSize(CASES.size());

        for (DocumentCase testCase : CASES) {
            DecisionResponse response = published.stream()
                    .filter(r -> r.mailId().equals(testCase.mailId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("mailId " + testCase.mailId() + " 결과 없음"));

            assertThat(response.attachments()).hasSize(1);
            assertThat(response.attachments().get(0).storageKey()).isEqualTo(testCase.storageKey());
            assertThat(response.attachments().get(0).status()).isEqualTo(testCase.expectedStatus());
        }
    }

    @TestConfiguration
    static class StubAttachmentLoaderConfig {

        @Bean
        @Primary
        AttachmentLoader stubAttachmentLoader() {
            return attachment -> {
                try (InputStream in = getClass().getClassLoader()
                        .getResourceAsStream("upstage.pipeline/" + attachment.fileName())) {
                    if (in == null) {
                        throw new IllegalStateException("test resource not found: " + attachment.fileName());
                    }
                    return new DocumentFile(attachment.fileName(), in.readAllBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    @TestConfiguration
    static class StubPolicyServiceConfig {

        @Bean
        @Primary
        PolicyService stubPolicyService() {
            List<Rule> rules = List.of(
                    new Rule("payslip", "internal", ScanStatus.ALLOW),
                    new Rule("payslip", null, ScanStatus.REVIEW),
                    new Rule(null, null, ScanStatus.ALLOW)
            );
            return companyId -> new Policy(companyId, rules);
        }
    }
}
