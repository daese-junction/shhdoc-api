package com.shhdoc.upstage.pipeline.extract;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shhdoc.upstage.pipeline.DocumentFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Upstage Information Extract API 실제 연동. 문서유형과 무관한 고정 범용 민감정보 스키마를 쓴다.
 *
 * <p>동기 Information Extract도 Document Parse와 동일하게 RPS 한도가 1이라
 * {@code extractSemaphore}로 동시 호출 1개로 제한한다. 세마포어는 "동시에 1개만"만
 * 보장하고 "초당 1개"까지 강제하진 않으므로, 실제로 429가 자주 발생하면 시간기반
 * rate limiter(Resilience4j 등)로 교체가 필요하다.
 */
@Component
public class InformationExtractorImpl implements InformationExtractor {

    private final Semaphore extractSemaphore = new Semaphore(1);

    private static final String SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "sensitive_items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "type": {"type": "string", "description": "민감정보 유형 (이름/계좌번호/주민번호/전화번호/이메일/금액 등)"},
                      "value": {"type": "string", "description": "감지된 값"}
                    }
                  }
                },
                "contains_personal_info": {"type": "boolean", "description": "개인정보 포함 여부"},
                "contains_financial_info": {"type": "boolean", "description": "금액/재무정보 포함 여부"},
                "confidentiality_marking": {"type": "string", "description": "대외비/기밀 표시 문구, 없으면 빈 문자열"}
              }
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> schema;
    private final RestClient restClient;
    private final String apiKey;
    private final String endpoint;

    public InformationExtractorImpl(@Value("${upstage.api-key}") String apiKey,
                                     @Value("${upstage.information-extract-url}") String endpoint) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        try {
            this.schema = objectMapper.readValue(SCHEMA_JSON, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse fixed extraction schema", e);
        }
    }

    @Override
    public ExtractionResult extract(DocumentFile file) {
        String base64Data = Base64.getEncoder().encodeToString(file.content());
        String dataUri = "data:application/octet-stream;base64," + base64Data;

        ExtractRequest request = new ExtractRequest(
                "information-extract",
                List.of(new ExtractRequest.Message("user",
                        List.of(new ExtractRequest.Content("image_url", new ExtractRequest.ImageUrl(dataUri))))),
                new ExtractRequest.ResponseFormat("json_schema",
                        new ExtractRequest.JsonSchema("shhdoc_sensitive_info", schema))
        );

        ExtractResponse response;
        extractSemaphore.acquireUninterruptibly();
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ExtractResponse.class);
        } finally {
            extractSemaphore.release();
        }

        return toExtractionResult(response);
    }

    private ExtractionResult toExtractionResult(ExtractResponse response) {
        String contentJson = response.choices().get(0).message().content();
        try {
            ExtractedData data = objectMapper.readValue(contentJson, ExtractedData.class);
            List<SensitiveItem> items = data.sensitiveItems() == null
                    ? List.of()
                    : data.sensitiveItems().stream()
                            .map(i -> new SensitiveItem(i.type(), i.value()))
                            .toList();
            return new ExtractionResult(
                    items,
                    Boolean.TRUE.equals(data.containsPersonalInfo()),
                    Boolean.TRUE.equals(data.containsFinancialInfo()),
                    data.confidentialityMarking() == null ? "" : data.confidentialityMarking()
            );
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse extraction response content: " + contentJson, e);
        }
    }

    /** Upstage 요청 스키마 (OpenAI Chat Completion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record ExtractRequest(
            String model,
            List<Message> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
        private record Message(String role, List<Content> content) {
        }

        private record Content(String type, @JsonProperty("image_url") ImageUrl imageUrl) {
        }

        private record ImageUrl(String url) {
        }

        private record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema) {
        }

        private record JsonSchema(String name, Map<String, Object> schema) {
        }
    }

    /** Upstage 응답 스키마(OpenAI ChatCompletion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record ExtractResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content) {
        }
    }

    /** message.content(JSON 문자열)를 파싱해 담는, 우리 고정 스키마 그대로의 내부 전용 타입. */
    private record ExtractedData(
            @JsonProperty("sensitive_items") List<ExtractedSensitiveItem> sensitiveItems,
            @JsonProperty("contains_personal_info") Boolean containsPersonalInfo,
            @JsonProperty("contains_financial_info") Boolean containsFinancialInfo,
            @JsonProperty("confidentiality_marking") String confidentialityMarking
    ) {
        private record ExtractedSensitiveItem(String type, String value) {
        }
    }
}
