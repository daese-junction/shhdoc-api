package com.shhdoc.upstage.pipeline.parse;

import java.util.List;

/**
 * Document Parse API 응답.
 *
 * @param content    문서 전체를 이어붙인 html/markdown/text
 * @param elements   레이아웃 요소별 분해 결과 (표/차트/문단 등)
 * @param pageCount  처리된 페이지 수
 */
public record ParsedDocument(
        ParsedContent content,
        List<ParsedElement> elements,
        int pageCount
) {
}
