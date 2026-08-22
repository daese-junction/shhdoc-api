package com.shhdoc.upstage.pipeline.classify;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Upstage Document Classification API 실제 연동.
 *
 * <p>동기 Document Classification도 Parse/Extract와 동일한 RPS=1 한도라
 * {@code classifySemaphore}로 동시 호출 1개로 제한한다 (Extract에서 429가
 * 실제로 발생해 세마포어를 추가했던 것과 같은 이유).
 */
@Component
public class DocumentClassifierImpl implements DocumentClassifier {

    private final Semaphore classifySemaphore = new Semaphore(1);
    private final RestClient restClient;
    private final String apiKey;
    private final String endpoint;

    public DocumentClassifierImpl(@Value("${upstage.api-key}") String apiKey,
                                   @Value("${upstage.document-classify-url}") String endpoint) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public ClassificationResult classify(DocumentFile file, List<DocumentCategory> categories) {
        String base64Data = Base64.getEncoder().encodeToString(file.content());
        String dataUri = "data:application/octet-stream;base64," + base64Data;

        ClassifyRequest request = new ClassifyRequest(
                "document-classify",
                List.of(new ClassifyRequest.Message("user",
                        List.of(new ClassifyRequest.Content("image_url", new ClassifyRequest.ImageUrl(dataUri))))),
                new ClassifyRequest.ResponseFormat("json_schema",
                        new ClassifyRequest.JsonSchema("document-classify",
                                new ClassifyRequest.Schema("string", toOneOf(categories))))
        );

        ClassifyResponse response;
        classifySemaphore.acquireUninterruptibly();
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ClassifyResponse.class);
        } finally {
            classifySemaphore.release();
        }

        return toClassificationResult(response);
    }

    private List<ClassifyRequest.OneOf> toOneOf(List<DocumentCategory> categories) {
        return categories.stream()
                .map(c -> new ClassifyRequest.OneOf(c.value(), c.description()))
                .toList();
    }

    private ClassificationResult toClassificationResult(ClassifyResponse response) {
        ClassifyResponse.Message message = response.choices().get(0).message();
        double confidenceScore = message.toolCalls().get(0).function().arguments().values().stream()
                .findFirst()
                .map(ClassifyResponse.ConfidenceValue::confidenceScore)
                .orElse(0.0);
        return new ClassificationResult(message.content(), confidenceScore);
    }

    /** Upstage 요청 스키마 (OpenAI Chat Completion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record ClassifyRequest(
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

        private record JsonSchema(String name, Schema schema) {
        }

        private record Schema(String type, @JsonProperty("oneOf") List<OneOf> oneOf) {
        }

        private record OneOf(@JsonProperty("const") String constValue, String description) {
        }
    }

    /** Upstage 응답 스키마(OpenAI ChatCompletion 호환) 그대로 바인딩하는 내부 전용 타입. */
    private record ClassifyResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content, @JsonProperty("tool_calls") List<ToolCall> toolCalls) {
        }

        private record ToolCall(Function function) {
        }

        private record Function(String name, Map<String, ConfidenceValue> arguments) {
        }

        private record ConfidenceValue(@JsonProperty("_value") String value,
                                        @JsonProperty("confidence_score") double confidenceScore) {
        }
    }
}
