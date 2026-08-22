package com.shhdoc.upstage.pipeline.generate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Upstage Generate(Solar LLM Chat Completion) API 실제 연동.
 *
 * <p>판정 사유 문장 생성처럼 짧고 가벼운 텍스트 생성에 쓰므로, 기본 추론(reasoning)을
 * 수행해 지연이 늘어나는 solar-pro4 대신 가벼운 solar-mini 모델을 쓴다.
 */
@Component
public class GeneratorImpl implements Generator {

    private static final String MODEL = "solar-mini";

    private final RestClient restClient;
    private final String apiKey;
    private final String endpoint;

    public GeneratorImpl(@Value("${upstage.api-key}") String apiKey,
                          @Value("${upstage.generate-url}") String endpoint) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public String generate(String prompt) {
        GenerateRequest request = new GenerateRequest(
                MODEL,
                List.of(new GenerateRequest.Message("user", prompt))
        );

        GenerateResponse response = restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(GenerateResponse.class);

        return response.choices().get(0).message().content();
    }

    /** Upstage 요청 스키마 (OpenAI Chat Completion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record GenerateRequest(
            String model,
            List<Message> messages
    ) {
        private record Message(String role, String content) {
        }
    }

    /** Upstage 응답 스키마(OpenAI ChatCompletion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record GenerateResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content) {
        }
    }
}
