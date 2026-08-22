package com.shhdoc.upstage.pipeline.parse;

/**
 * Document Parse 응답의 content 필드 (문서 전체 또는 element 단위 공통 구조).
 *
 * @param html     HTML 형식 결과
 * @param markdown Markdown 형식 결과
 * @param text     원시 텍스트 결과 (equation 카테고리는 OCR 결과라 부정확할 수 있음)
 */
public record ParsedContent(
        String html,
        String markdown,
        String text
) {
}
