package com.shhdoc.upstage.pipeline.extract;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shhdoc.upstage.pipeline.DocumentFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Upstage Information Extract API 실제 연동. 회사가 등록한 민감정보 유형 후보를
 * 요청마다 실어 보내고, 그 중 실제로 검출된 코드 + 보안등급을 받는다
 * ({@code classify.DocumentClassifierImpl}과 같은 회사어휘 패턴).
 *
 * <p>동기 Information Extract도 Document Parse와 동일하게 RPS 한도가 1이라
 * {@code extractSemaphore}로 동시 호출 1개로 제한한다. 세마포어는 "동시에 1개만"만
 * 보장하고 "초당 1개"까지 강제하진 않으므로, 실제로 429가 자주 발생하면 시간기반
 * rate limiter(Resilience4j 등)로 교체가 필요하다.
 */
@Component
public class InformationExtractorImpl implements InformationExtractor {

    private static final List<String> CLASSIFICATION_LEVELS = List.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "SECRET");

    private final Semaphore extractSemaphore = new Semaphore(1);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;
    private final String apiKey;
    private final String endpoint;

    public InformationExtractorImpl(@Value("${upstage.api-key}") String apiKey,
                                     @Value("${upstage.information-extract-url}") String endpoint) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public ExtractionResult extract(DocumentFile file, List<SensitiveInfoCategory> sensitiveTypes) {
        String base64Data = Base64.getEncoder().encodeToString(file.content());
        String dataUri = "data:application/octet-stream;base64," + base64Data;

        ExtractRequest request = new ExtractRequest(
                "information-extract",
                List.of(new ExtractRequest.Message("user",
                        List.of(new ExtractRequest.Content("image_url", new ExtractRequest.ImageUrl(dataUri))))),
                new ExtractRequest.ResponseFormat("json_schema",
                        new ExtractRequest.JsonSchema("shhdoc_sensitive_info", buildSchema(sensitiveTypes)))
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

    private Map<String, Object> buildSchema(List<SensitiveInfoCategory> sensitiveTypes) {
        List<String> codes = sensitiveTypes.stream().map(SensitiveInfoCategory::code).toList();
        String codeDescriptions = sensitiveTypes.stream()
                .map(t -> t.code() + "(" + t.description() + ")")
                .collect(Collectors.joining(", "));

        Map<String, Object> matchedSensitiveTypesProp = Map.of(
                "type", "array",
                "description", "문서에서 실제로 검출된 민감정보 유형 코드 목록. 후보: " + codeDescriptions,
                "items", Map.of("type", "string", "enum", codes)
        );
        Map<String, Object> classificationProp = Map.of(
                "type", "string",
                "enum", CLASSIFICATION_LEVELS,
                "description", "문서의 보안등급. PUBLIC(공개 가능) < INTERNAL(사내용) < CONFIDENTIAL(대외비) "
                        + "< SECRET(극비) 순으로 위험도가 높아짐. 명시적 표시나 내용상 민감도로 판단"
        );
        Map<String, Object> markingProp = Map.of(
                "type", "string",
                "description", "문서에 실제로 적힌 대외비/기밀 표시 문구. 없으면 빈 문자열"
        );

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("matched_sensitive_types", matchedSensitiveTypesProp);
        properties.put("classification", classificationProp);
        properties.put("confidentiality_marking", markingProp);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    private ExtractionResult toExtractionResult(ExtractResponse response) {
        String contentJson = response.choices().get(0).message().content();
        try {
            ExtractedData data = objectMapper.readValue(contentJson, ExtractedData.class);
            List<String> matched = data.matchedSensitiveTypes() == null ? List.of() : data.matchedSensitiveTypes();
            return new ExtractionResult(
                    matched,
                    data.classification(),
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

    /** message.content(JSON 문자열)를 파싱해 담는, 요청 스키마 그대로의 내부 전용 타입. */
    private record ExtractedData(
            @JsonProperty("matched_sensitive_types") List<String> matchedSensitiveTypes,
            String classification,
            @JsonProperty("confidentiality_marking") String confidentialityMarking
    ) {
    }
}
