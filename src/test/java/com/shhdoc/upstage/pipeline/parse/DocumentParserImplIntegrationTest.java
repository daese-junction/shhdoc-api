package com.shhdoc.upstage.pipeline.parse;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Upstage Document Parse API를 호출하는 통합 테스트.
 * {@code UPSTAGE_API_KEY} 환경변수가 없으면 전체 스킵된다 (CI/로컬 키 없는 환경 보호).
 *
 * <p>{@code src/test/resources/upstage.pipeline.parse/sample-payslip.{pdf,hwpx,xlsx}} 3개 파일이 필요하다.
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "UPSTAGE_API_KEY", matches = ".+")
class DocumentParserImplIntegrationTest {

    private static final String ENDPOINT = "https://api.upstage.ai/v1/document-digitization";

    private DocumentParserImpl documentParser;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("UPSTAGE_API_KEY");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "xlsx"})
    void parse는_실제_Upstage_API로_문서를_구조화한다(String extension) throws IOException {
        byte[] content = loadResource("sample-payslip." + extension);
        DocumentFile file = new DocumentFile("sample-payslip." + extension, content);

        ParsedDocument result = documentParser.parse(file);

        assertThat(result.pageCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.content().html()).isNotBlank();
        assertThat(result.elements()).isNotEmpty();

        // 급여명세서 샘플에 넣은 표/직원정보가 실제로 인식됐는지 느슨하게 확인
        String combinedText = result.elements().stream()
                .map(e -> e.content().text())
                .reduce("", String::concat);
        log.info("combinedText: {}", combinedText);
        assertThat(combinedText).contains("홍길동");
    }

    private byte[] loadResource(String fileName) throws IOException {
        String path = "upstage.pipeline.parse/" + fileName;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("test resource not found: " + path);
            }
            return in.readAllBytes();
        }
    }
}
