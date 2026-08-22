package com.shhdoc.upstage.pipeline.classify;

import com.shhdoc.upstage.pipeline.DocumentFile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Upstage Document Classification API를 호출하는 통합 테스트.
 * {@code UPSTAGE_API_KEY} 환경변수가 없으면 전체 스킵된다.
 *
 * <p>{@code src/test/resources/upstage.pipeline/sample-payslip.{pdf,xlsx}}를 재사용한다.
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "UPSTAGE_API_KEY", matches = ".+")
class DocumentClassifierImplIntegrationTest {

    private static final String ENDPOINT = "https://api.upstage.ai/v1/document-classification/chat/completions";

    private DocumentClassifierImpl documentClassifier;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("UPSTAGE_API_KEY");
        documentClassifier = new DocumentClassifierImpl(apiKey, ENDPOINT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "xlsx"})
    void classify는_급여명세서_샘플을_payslip으로_분류한다(String extension) throws IOException {
        byte[] content = loadResource("sample-payslip." + extension);
        DocumentFile file = new DocumentFile("sample-payslip." + extension, content);

        ClassificationResult result = documentClassifier.classify(file, DefaultDocumentCategories.ALL);

        log.info("category={}, confidenceScore={}", result.category(), result.confidenceScore());
        assertThat(result.category()).isEqualTo("payslip");
        assertThat(result.confidenceScore()).isGreaterThan(0.0);
    }

    private byte[] loadResource(String fileName) throws IOException {
        String path = "upstage.pipeline/" + fileName;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("test resource not found: " + path);
            }
            return in.readAllBytes();
        }
    }
}
