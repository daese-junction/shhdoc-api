package com.shhdoc.upstage;

import com.shhdoc.TestcontainersConfiguration;
import com.shhdoc.upstage.dto.Attachment;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.pipeline.DocumentFile;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Gateway.enqueue()부터 publishDecision()까지 실제로 동작하는지 확인하는 e2e 테스트.
 * 스토리지(MinIO)는 컨테이너 없이 {@link AttachmentLoader}만 테스트용으로 바꿔치기해서
 * classpath 샘플파일을 바로 돌려준다 — pipeline(실 Upstage API)/큐/정책/판정 로직은 전부 실제.
 *
 * <p>{@code UPSTAGE_API_KEY} 없으면 전체 스킵.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, GatewayEndToEndTest.StubAttachmentLoaderConfig.class})
@EnabledIfEnvironmentVariable(named = "UPSTAGE_API_KEY", matches = ".+")
@RecordApplicationEvents
class GatewayEndToEndTest {

    @Autowired
    private Gateway gateway;

    @Autowired
    private ApplicationEvents events;

    private static final String SENDER = "sender@company.com";
    private static final String INTERNAL_RECIPIENT = "colleague@company.com";
    private static final String EXTERNAL_RECIPIENT = "recipient@external-partner.com";

    static Stream<Arguments> documents() {
        return Stream.of(
                // 급여명세서, 외부발송: mock 정책상 payslip+외부(미분류) 룰 → REVIEW
                Arguments.of(1001, "sample-payslip.pdf", "storage-key-payslip-ext", EXTERNAL_RECIPIENT, ScanStatus.REVIEW),
                // 급여명세서, 내부발송(같은 도메인): payslip+internal 룰 → ALLOW
                Arguments.of(1005, "sample-payslip.pdf", "storage-key-payslip-int", INTERNAL_RECIPIENT, ScanStatus.ALLOW),
                // 계약서, 외부발송: payslip 전용 룰 안 걸림 → 와일드카드 폴백 → ALLOW
                Arguments.of(1002, "sample-contract.pdf", "storage-key-contract", EXTERNAL_RECIPIENT, ScanStatus.ALLOW),
                // 공지문, 외부발송: 민감정보 없음 → ALLOW (false positive 안 나는지 검증)
                Arguments.of(1003, "sample-notice.pdf", "storage-key-notice", EXTERNAL_RECIPIENT, ScanStatus.ALLOW),
                // 사업계획서, 외부발송: 개인정보 없이 재무+대외비만 → ALLOW (payslip과 다른 신호조합)
                Arguments.of(1004, "sample-business-plan.pdf", "storage-key-business-plan", EXTERNAL_RECIPIENT, ScanStatus.ALLOW)
        );
    }

    @ParameterizedTest
    @MethodSource("documents")
    void enqueue부터_publishDecision까지_문서유형별로_실제로_동작한다(
            Integer mailId, String fileName, String storageKey, String recipientAddress, ScanStatus expectedStatus) {
        Attachment attachment = new Attachment(fileName, 1, storageKey, "fake-hash");
        MailRequest request = new MailRequest(
                mailId, 1, SENDER, 1, "제목", "본문",
                List.of(new Recipient(recipientAddress)),
                List.of(attachment)
        );

        gateway.enqueue(request);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<DecisionResponse> published = events.stream(DecisionResponse.class)
                    .filter(response -> response.mailId().equals(mailId))
                    .toList();

            assertThat(published).hasSize(1);
            assertThat(published.get(0).attachments()).hasSize(1);
            assertThat(published.get(0).attachments().get(0).storageKey()).isEqualTo(storageKey);
            assertThat(published.get(0).attachments().get(0).status()).isEqualTo(expectedStatus);
        });
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
}
