package com.shhdoc.upstage.pipeline.parse;

import com.shhdoc.upstage.pipeline.DocumentFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Upstage Document Parse API 실제 연동.
 *
 * <p>동기 API({@code POST /v1/document-digitization})만 호출한다. 이 API는 100페이지를
 * 넘는 문서는 에러 없이 앞 100페이지만 처리하고 나머지를 조용히 버리므로, 100페이지를
 * 넘는 문서에 대한 비동기 API 라우팅은 아직 구현하지 않았다 (TODO).
 *
 * <p>동기 Document Parse의 RPS 한도가 1이라 {@code parseSemaphore}로 동시 호출 1개로 제한한다.
 * 세마포어는 "동시에 1개만"만 보장하고 "초당 1개"까지 강제하진 않으므로, 실제로 429가
 * 자주 발생하면 시간기반 rate limiter(Resilience4j 등)로 교체가 필요하다.
 */
@Slf4j
@Component
public class DocumentParserImpl implements DocumentParser {

    private final Semaphore parseSemaphore = new Semaphore(1);
    private final RestClient restClient;
    private final String apiKey;
    private final String endpoint;

    public DocumentParserImpl(@Value("${upstage.api-key}") String apiKey,
                               @Value("${upstage.document-parse-url}") String endpoint) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public ParsedDocument parse(DocumentFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("document", new ByteArrayResource(file.content()) {
            @Override
            public String getFilename() {
                return file.fileName();
            }
        });
        body.add("model", "document-parse");
        body.add("merge_multipage_tables", "true");
        body.add("output_formats", "['html', 'markdown', 'text']");

        UpstageParseResponse response;
        long startedAt = System.currentTimeMillis();
        parseSemaphore.acquireUninterruptibly();
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(UpstageParseResponse.class);
        } finally {
            parseSemaphore.release();
        }

        ParsedDocument parsed = toParsedDocument(response);
        log.info("[PARSE] file={} pages={} {}ms", file.fileName(), parsed.pageCount(), System.currentTimeMillis() - startedAt);
        return parsed;
    }

    private ParsedDocument toParsedDocument(UpstageParseResponse response) {
        List<ParsedElement> elements = response.elements().stream()
                .map(e -> new ParsedElement(e.category(), toParsedContent(e.content()), e.page()))
                .toList();
        return new ParsedDocument(toParsedContent(response.content()), elements, response.usage().pages());
    }

    private ParsedContent toParsedContent(UpstageParseResponse.Content content) {
        return new ParsedContent(content.html(), content.markdown(), content.text());
    }

    /** Upstage 원본 응답 JSON 스키마 그대로 바인딩하는 내부 전용 타입. */
    private record UpstageParseResponse(
            String api,
            Content content,
            List<Element> elements,
            String model,
            Usage usage
    ) {
        private record Content(String html, String markdown, String text) {
        }

        private record Element(String category, Content content, int id, int page) {
        }

        private record Usage(int pages) {
        }
    }
}
