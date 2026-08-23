package com.shhdoc.upstage.pipeline.generate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Upstage Generate(Solar LLM Chat Completion) API 실제 연동.
 *
 * <p>판정 사유 문장 생성처럼 짧고 가벼운 텍스트 생성에 쓰므로, 기본 추론(reasoning)을
 * 수행해 지연이 늘어나는 solar-pro4 대신 가벼운 solar-mini 모델을 쓴다.
 *
 * <p>사유 문장과 에스컬레이션 신호를 한 번에 받아야 해서 {@code json_schema}
 * (Classify/Extract와 같은 패턴)로 구조화 응답을 받는다.
 */
@Slf4j
@Component
public class GeneratorImpl implements Generator {

    private static final String MODEL = "solar-mini";

    private final ObjectMapper objectMapper = new ObjectMapper();
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
    public GenerationResult generate(String prompt) {
        GenerateRequest request = new GenerateRequest(
                MODEL,
                List.of(new GenerateRequest.Message("user", prompt)),
                new GenerateRequest.ResponseFormat("json_schema",
                        new GenerateRequest.JsonSchema("shhdoc_decision_reason", buildSchema()))
        );

        long startedAt = System.currentTimeMillis();
        GenerateResponse response = restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(GenerateResponse.class);

        GenerationResult result = toGenerationResult(response);
        log.info("[GENERATE] escalateToReview={} {}ms", result.escalateToReview(), System.currentTimeMillis() - startedAt);
        return result;
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> reasonProp = Map.of(
                "type", "string",
                "description", "판정 사유를 한 문장으로 설명"
        );
        Map<String, Object> escalateProp = Map.of(
                "type", "boolean",
                "description", "전달받은 판정이 ALLOW인데 근거(민감정보/수신자/문서유형 등)를 보니 "
                        + "위험해 보이면 true, 안전하면 false. 판정이 이미 REVIEW면 항상 false"
        );

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("reason", reasonProp);
        properties.put("escalate_to_review", escalateProp);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    private GenerationResult toGenerationResult(GenerateResponse response) {
        String contentJson = response.choices().get(0).message().content();
        try {
            GenerationData data = objectMapper.readValue(contentJson, GenerationData.class);
            return new GenerationResult(data.reason(), data.escalateToReview());
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse generation response content: " + contentJson, e);
        }
    }

    /** Upstage 요청 스키마 (OpenAI Chat Completion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record GenerateRequest(
            String model,
            List<Message> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
        private record Message(String role, String content) {
        }

        private record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema) {
        }

        private record JsonSchema(String name, Map<String, Object> schema) {
        }
    }

    /** Upstage 응답 스키마(OpenAI ChatCompletion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record GenerateResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content) {
        }
    }

    /** message.content(JSON 문자열)를 파싱해 담는, 요청 스키마 그대로의 내부 전용 타입. */
    private record GenerationData(
            String reason,
            @JsonProperty("escalate_to_review") boolean escalateToReview
    ) {
    }
}
