package com.shhdoc.upstage.pipeline.generate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Upstage Generate(Solar) API를 호출하는 통합 테스트.
 * {@code UPSTAGE_API_KEY} 환경변수가 없으면 전체 스킵된다.
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "UPSTAGE_API_KEY", matches = ".+")
class GeneratorImplIntegrationTest {

    private static final String ENDPOINT = "https://api.upstage.ai/v1/chat/completions";

    private GeneratorImpl generator;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("UPSTAGE_API_KEY");
        generator = new GeneratorImpl(apiKey, ENDPOINT);
    }

    @Test
    void generate는_판정사유_문장을_생성한다() {
        String prompt = """
                다음 근거로 이메일 발송에 대한 판정 사유를 한 문장으로 작성해줘.
                - 문서유형: 급여명세서
                - 개인정보 포함: 있음 (성명, 계좌번호)
                - 수신자: 외부 도메인 (승인된 파트너 아님)
                - 정책: 급여정보는 승인된 파트너 외 외부 발송 시 REVIEW
                """;

        String result = generator.generate(prompt);

        log.info("generated reason: {}", result);
        assertThat(result).isNotBlank();
    }
}
