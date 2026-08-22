package com.shhdoc.upstage.pipeline.extract;

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
 * 실제 Upstage Information Extract API를 호출하는 통합 테스트.
 * {@code UPSTAGE_API_KEY} 환경변수가 없으면 전체 스킵된다.
 *
 * <p>{@code src/test/resources/upstage.pipeline/sample-payslip.{pdf,xlsx}}를 재사용한다.
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "UPSTAGE_API_KEY", matches = ".+")
class InformationExtractorImplIntegrationTest {

    private static final String ENDPOINT = "https://api.upstage.ai/v1/information-extraction/chat/completions";

    private InformationExtractorImpl informationExtractor;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("UPSTAGE_API_KEY");
        informationExtractor = new InformationExtractorImpl(apiKey, ENDPOINT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "xlsx"})
    void extract는_급여명세서_샘플에서_민감정보를_추출한다(String extension) throws IOException {
        byte[] content = loadResource("sample-payslip." + extension);
        DocumentFile file = new DocumentFile("sample-payslip." + extension, content);

        ExtractionResult result = informationExtractor.extract(file);

        log.info("sensitiveItems={}, containsPersonalInfo={}, containsFinancialInfo={}, confidentialityMarking={}",
                result.sensitiveItems(), result.containsPersonalInfo(), result.containsFinancialInfo(),
                result.confidentialityMarking());

        assertThat(result.sensitiveItems()).isNotEmpty();
        assertThat(result.containsPersonalInfo()).isTrue();
        assertThat(result.sensitiveItems())
                .anyMatch(item -> item.value() != null && item.value().contains("홍길동"));
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
